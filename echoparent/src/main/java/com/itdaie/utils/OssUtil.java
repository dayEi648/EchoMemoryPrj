package com.itdaie.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.itdaie.common.config.OssConfig;
import com.itdaie.common.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * 阿里云OSS文件上传工具。
 */
@Slf4j
@Component
public class OssUtil {

    @Autowired
    private OssConfig ossConfig;

    /**
     * 上传文件到 OSS 指定目录前缀下。
     *
     * @param file         上传的文件
     * @param folderPrefix 目录前缀，如 {@code avatarimage/}；若未以 {@code /} 结尾会自动补上
     * @return 文件访问 URL
     */
    public String upload(MultipartFile file, String folderPrefix) {
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            return upload(file.getInputStream(), folderPrefix, suffix);
        } catch (Exception e) {
            throw new FileUploadException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 使用指定输入流上传文件到 OSS。
     *
     * @param inputStream  文件输入流
     * @param folderPrefix 目录前缀
     * @param suffix       文件后缀（如 {@code .jpg}）
     * @return 文件访问 URL
     */
    public String upload(InputStream inputStream, String folderPrefix, String suffix) {
        if (!hasOssCredentials()) {
            throw new FileUploadException(
                    "未配置阿里云 OSS：请在配置中设置 aliyun.oss（endpoint、bucket-name、access-key-id、access-key-secret）"
                            + "或环境变量 ALIYUN_OSS_ENDPOINT、ALIYUN_OSS_BUCKET、ALIYUN_OSS_ACCESS_KEY_ID、ALIYUN_OSS_ACCESS_KEY_SECRET");
        }
        String prefix = normalizeFolderPrefix(folderPrefix);
        String endpointForSdk = endpointForOssClient();
        OSS ossClient = new OSSClientBuilder().build(
                endpointForSdk,
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
        );
        try {
            String objectName = prefix + UUID.randomUUID() + suffix;
            ossClient.putObject(ossConfig.getBucketName(), objectName, inputStream);
            return "https://" + ossConfig.getBucketName() + "." + endpointHost() + "/" + objectName;
        } catch (Exception e) {
            throw new FileUploadException("文件上传失败: " + e.getMessage());
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 若字段被替换为新 URL，删除本 Bucket 内旧对象（与 {@link #upload} 生成的公网 URL 同域）。
     * 非本服务 OSS 地址、删除失败均忽略，不抛异常，避免影响业务更新。
     */
    public void deleteByPublicUrlIfReplaced(String oldUrl, String newUrl) {
        if (!StringUtils.hasText(oldUrl)) {
            return;
        }
        if (Objects.equals(oldUrl, newUrl)) {
            return;
        }
        deleteByPublicUrl(oldUrl);
    }

    /**
     * 按上传时返回的 HTTPS URL 删除对象；仅处理当前配置 Bucket 虚拟域名。
     */
    public void deleteByPublicUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        if (!hasOssCredentials()) {
            return;
        }
        String expectedHost = ossConfig.getBucketName() + "." + endpointHost();
        try {
            URI uri = URI.create(url.trim());
            if (uri.getHost() == null || !expectedHost.equalsIgnoreCase(uri.getHost())) {
                return;
            }
            String path = uri.getPath();
            if (!StringUtils.hasText(path) || path.length() <= 1) {
                return;
            }
            String objectKey = path.startsWith("/") ? path.substring(1) : path;
            OSS ossClient = new OSSClientBuilder().build(
                    endpointForOssClient(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret()
            );
            try {
                ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            } finally {
                ossClient.shutdown();
            }
        } catch (Exception e) {
            log.warn("OSS 删除对象失败 url={} : {}", url, e.getMessage());
        }
    }

    /**
     * 批量尝试删除（空串与非法 URL 在 {@link #deleteByPublicUrl} 内忽略）。
     */
    public void deleteByPublicUrls(String... urls) {
        if (urls == null) {
            return;
        }
        for (String u : urls) {
            deleteByPublicUrl(u);
        }
    }

    private boolean hasOssCredentials() {
        return StringUtils.hasText(ossConfig.getEndpoint())
                && StringUtils.hasText(ossConfig.getBucketName())
                && StringUtils.hasText(ossConfig.getAccessKeyId())
                && StringUtils.hasText(ossConfig.getAccessKeySecret());
    }

    /**
     * 虚拟域名公网 URL 仅允许 host 为 bucket.endpointHost，需去掉配置里误写的 {@code http(s)://}。
     */
    private String endpointHost() {
        String e = ossConfig.getEndpoint().trim();
        if (e.startsWith("https://")) {
            e = e.substring(8);
        } else if (e.startsWith("http://")) {
            e = e.substring(7);
        }
        while (e.endsWith("/")) {
            e = e.substring(0, e.length() - 1);
        }
        return e;
    }

    /**
     * 与阿里云 Java SDK 约定一致：传入带协议的 Endpoint。
     */
    private String endpointForOssClient() {
        String host = endpointHost();
        if (!StringUtils.hasText(host)) {
            return ossConfig.getEndpoint();
        }
        return "https://" + host;
    }

    private static String normalizeFolderPrefix(String folderPrefix) {
        if (!StringUtils.hasText(folderPrefix)) {
            throw new FileUploadException("OSS 目录前缀不能为空");
        }
        String p = folderPrefix.trim();
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        return p;
    }
}
