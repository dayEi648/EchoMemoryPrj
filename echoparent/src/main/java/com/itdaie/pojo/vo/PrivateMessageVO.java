package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信消息视图对象
 *
 * @author itdaie
 */
@Data
public class PrivateMessageVO {

    private Long id;

    private Integer senderId;

    private String senderName;

    private String senderAvatar;

    private Integer receiverId;

    private String conversationKey;

    private String content;

    private Boolean isRead;

    private LocalDateTime createTime;
}
