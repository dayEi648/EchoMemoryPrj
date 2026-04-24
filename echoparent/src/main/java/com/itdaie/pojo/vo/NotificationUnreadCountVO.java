package com.itdaie.pojo.vo;

import lombok.Data;

/**
 * 通知未读数统计视图对象
 *
 * @author itdaie
 */
@Data
public class NotificationUnreadCountVO {

    /**
     * 总未读数（通知 + 私信）
     */
    private long total;

    /**
     * @我的 未读数
     */
    private long mention;

    /**
     * 评论/回复 未读数
     */
    private long reply;

    /**
     * 通知（关注/收藏/点赞/系统）未读数
     */
    private long notify;

    /**
     * 私信未读数
     */
    private long privateMessage;
}
