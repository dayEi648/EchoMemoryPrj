package com.itdaie.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图对象
 * 返回给前端的评论数据
 *
 * @author itdaie
 */
@Data
public class CommentVO {

    private Integer id;

    // 场景信息（与 comments 表一致）
    private Boolean inMusic;
    private Boolean inPlaylist;
    private Boolean inSpace;
    private Integer musicId;
    private Integer playlistId;
    private Integer spaceId;

    /**
     * 场景类型：music/playlist/space（由上面场景列推导，便于前端使用）
     */
    private String sceneType;

    /**
     * 场景对象ID
     */
    private Integer sceneId;

    // 用户信息
    private Integer userId;
    private String userName;

    // 内容
    private String content;

    // 点赞/点踩用户ID数组（用于前端判断当前用户是否已点赞/点踩）
    private List<Integer> likeIds;
    private List<Integer> dislikeIds;

    // 统计
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer answerCount;

    // 回复信息
    private Boolean isReply;
    private Integer replyUserId;
    private Integer replyCommentId;
    private Boolean isNestedReply;
    private Integer nestedReplyUserId;
    private Integer nestedReplyCommentId;

    // 其他
    /**
     * 安全指数，范围0-10
     */
    private Integer safety;

    private Boolean isRecommended;
    private Boolean isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
