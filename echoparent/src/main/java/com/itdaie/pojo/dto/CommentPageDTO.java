package com.itdaie.pojo.dto;

import lombok.Data;

/**
 * 评论分页查询数据传输对象
 * 支持多种查询维度，利用数据库索引优化
 *
 * @author itdaie
 */
@Data
public class CommentPageDTO {

    /**
     * 页码，默认1
     */
    private Long pageNum;

    /**
     * 每页大小，默认10，最大200
     */
    private Long pageSize;

    /**
     * 场景类型：music/playlist/space
     * 与sceneId配合使用，利用条件索引
     */
    private String sceneType;

    /**
     * 场景对象ID
     */
    private Integer sceneId;

    /**
     * 评论用户ID
     * 使用索引：idx_comments_user_id
     */
    private Integer userId;

    /**
     * 回复的评论ID
     * 使用索引：idx_comments_reply_comment_id（条件索引 WHERE is_reply = true）
     */
    private Integer replyCommentId;

    /**
     * 嵌套回复的回复ID
     * 使用索引：idx_comments_nested_reply_comment_id（条件索引 WHERE is_nested_reply = true）
     */
    private Integer nestedReplyCommentId;

    /**
     * 是否只查询回复评论（true=只查回复，false=只查主评论，null=都查）
     */
    private Boolean isReply;

    /**
     * 是否只查询精选评论
     */
    private Boolean isRecommended;

    /**
     * 排序字段白名单：like_count/dislike_count/safety/create_time
     */
    private String sortBy;

    /**
     * 排序方向：asc/desc
     */
    private String sortOrder;
}
