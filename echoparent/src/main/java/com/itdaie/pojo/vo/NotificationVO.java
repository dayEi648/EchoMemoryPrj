package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知视图对象
 *
 * @author itdaie
 */
@Data
public class NotificationVO {

    private Long id;

    private Integer userId;

    private String type;

    private Integer senderId;

    private String senderName;

    private String senderAvatar;

    private String sourceType;

    private Long sourceId;

    private Long sourceParentId;

    private String title;

    private String content;

    private String extra;

    private Boolean isRead;

    private LocalDateTime readTime;

    private LocalDateTime createTime;
}
