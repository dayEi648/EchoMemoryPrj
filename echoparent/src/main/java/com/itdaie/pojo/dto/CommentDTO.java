package com.itdaie.pojo.dto;

import lombok.Data;

/**
 * 评论新增/修改数据传输对象
 *
 * @author itdaie
 */
@Data
public class CommentDTO {

    /**
     * 评论ID，修改时必填
     */
    private Integer id;

    /**
     * 场景类型：music/playlist/space
     */
    private String sceneType;

    /**
     * 场景对象ID（music_id/playlist_id/blog_id/space_id）
     */
    private Integer sceneId;

    /**
     * 评论用户ID
     */
    private Integer userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 是否回复
     */
    private Boolean isReply;

    /**
     * 回复目标用户ID
     */
    private Integer replyUserId;

    /**
     * 回复的评论ID
     */
    private Integer replyCommentId;

    /**
     * 是否嵌套回复
     */
    private Boolean isNestedReply;

    /**
     * 嵌套回复目标用户ID
     */
    private Integer nestedReplyUserId;

    /**
     * 嵌套回复的回复ID
     */
    private Integer nestedReplyCommentId;
}
