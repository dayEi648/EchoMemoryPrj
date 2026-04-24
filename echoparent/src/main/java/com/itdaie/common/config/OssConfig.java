package com.itdaie.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云OSS配置属性。
 */
@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    private String endpoint;
    private String bucketName;
    private String region;
    private String accessKeyId;
    private String accessKeySecret;
}
