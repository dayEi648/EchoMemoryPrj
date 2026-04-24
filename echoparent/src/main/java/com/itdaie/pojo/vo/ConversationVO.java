package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表视图对象
 *
 * @author itdaie
 */
@Data
public class ConversationVO {

    /**
     * 会话键
     */
    private String conversationKey;

    /**
     * 对方用户ID
     */
    private Integer otherUserId;

    /**
     * 对方用户名
     */
    private String otherUserName;

    /**
     * 对方用户头像
     */
    private String otherUserAvatar;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数
     */
    private Long unreadCount;
}
