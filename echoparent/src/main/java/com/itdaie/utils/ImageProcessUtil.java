package com.itdaie.utils;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图片处理工具类。
 * 提供图片压缩、裁剪等功能，基于 thumbnailator 实现。
 */
@Component
public class ImageProcessUtil {

    /**
     * 判断文件是否为图片。
     */
    public static boolean isImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 压缩图片。
     * 按比例缩放至最大尺寸内，输出 JPEG 格式。
     *
     * @param file       原始图片文件
     * @param maxWidth   最大宽度
     * @param maxHeight  最大高度
     * @param quality    输出质量（0.0 ~ 1.0）
     * @return 压缩后的图片输入流
     */
    public static InputStream compress(MultipartFile file, int maxWidth, int maxHeight, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .size(maxWidth, maxHeight)
                .outputQuality(quality)
                .outputFormat("jpg")
                .toOutputStream(baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * 裁剪并压缩图片。
     * 先按指定区域裁剪，再缩放至最大尺寸内，输出 JPEG 格式。
     *
     * @param file       原始图片文件
     * @param x          裁剪起始 X 坐标
     * @param y          裁剪起始 Y 坐标
     * @param width      裁剪宽度
     * @param height     裁剪高度
     * @param maxWidth   压缩后最大宽度
     * @param maxHeight  压缩后最大高度
     * @param quality    输出质量（0.0 ~ 1.0）
     * @return 裁剪压缩后的图片输入流
     */
    public static InputStream cropAndCompress(MultipartFile file,
                                               int x, int y, int width, int height,
                                               int maxWidth, int maxHeight, float quality) throws IOException {
        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new IOException("无法读取图片文件");
        }
        // 边界校验
        int imgWidth = original.getWidth();
        int imgHeight = original.getHeight();
        int cropX = Math.max(0, Math.min(x, imgWidth));
        int cropY = Math.max(0, Math.min(y, imgHeight));
        int cropW = Math.min(width, imgWidth - cropX);
        int cropH = Math.min(height, imgHeight - cropY);
        if (cropW <= 0 || cropH <= 0) {
            throw new IOException("裁剪参数无效");
        }
        BufferedImage cropped = original.getSubimage(cropX, cropY, cropW, cropH);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(cropped)
                .size(maxWidth, maxHeight)
                .outputQuality(quality)
                .outputFormat("jpg")
                .toOutputStream(baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * 将图片居中裁剪为正方形后压缩（适用于头像）。
     *
     * @param file       原始图片文件
     * @param size       输出正方形边长
     * @param quality    输出质量（0.0 ~ 1.0）
     * @return 处理后的图片输入流
     */
    public static InputStream cropToSquareAndCompress(MultipartFile file, int size, float quality) throws IOException {
        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new IOException("无法读取图片文件");
        }
        int imgWidth = original.getWidth();
        int imgHeight = original.getHeight();
        int cropSize = Math.min(imgWidth, imgHeight);
        int cropX = (imgWidth - cropSize) / 2;
        int cropY = (imgHeight - cropSize) / 2;
        BufferedImage cropped = original.getSubimage(cropX, cropY, cropSize, cropSize);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(cropped)
                .size(size, size)
                .outputQuality(quality)
                .outputFormat("jpg")
                .toOutputStream(baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }
}
