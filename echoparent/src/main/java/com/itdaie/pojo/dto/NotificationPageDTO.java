package com.itdaie.pojo.dto;

import lombok.Data;

/**
 * 通知分页查询数据传输对象
 *
 * @author itdaie
 */
@Data
public class NotificationPageDTO {

    /**
     * 页码，默认1
     */
    private Long pageNum;

    /**
     * 每页大小，默认10，最大50
     */
    private Long pageSize;

    /**
     * 通知类型：mention/like/collect/follow/system/reply/comment
     * 为空时查询所有类型
     */
    private String type;

    /**
     * 分类筛选：mention(@我的)/reply(评论回复)/notify(关注收藏系统)
     * 优先级高于 type，用于前端聚合展示
     */
    private String category;

    /**
     * 是否只查询未读
     */
    private Boolean unreadOnly;
}
