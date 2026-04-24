package com.itdaie.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.itdaie.common.handler.IntegerListTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论实体类，对应comments表
 * 支持4种场景：音乐/歌单/博客/空间
 * 支持两级回复：回复 -> 嵌套回复
 *
 * @author itdaie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "comments", autoResultMap = true)
public class Comment {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    // ========== 场景标记字段（4选1约束）==========

    /**
     * 是否为音乐评论
     */
    @TableField("in_music")
    private Boolean inMusic;

    /**
     * 是否为歌单评论
     */
    @TableField("in_playlist")
    private Boolean inPlaylist;

    /**
     * 是否为空间评论
     */
    @TableField("in_space")
    private Boolean inSpace;

    // ========== 场景ID字段 ==========

    /**
     * 音乐ID
     */
    @TableField("music_id")
    private Integer musicId;

    /**
     * 歌单ID
     */
    @TableField("playlist_id")
    private Integer playlistId;

    /**
     * 空间ID
     */
    @TableField("space_id")
    private Integer spaceId;

    // ========== 用户信息 ==========

    /**
     * 评论用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 用户昵称
     */
    @TableField("user_name")
    private String userName;

    // ========== 评论内容 ==========

    /**
     * 评论内容
     */
    private String content;

    // ========== 统计字段 ==========

    /**
     * 点赞用户ID数组
     */
    @TableField(value = "like_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> likeIds;

    /**
     * 点踩用户ID数组
     */
    @TableField(value = "dislike_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> dislikeIds;

    /**
     * 点赞数（GENERATED ALWAYS，只读）
     */
    @TableField(value = "like_count", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer likeCount;

    /**
     * 点踩数（GENERATED ALWAYS，只读）
     */
    @TableField(value = "dislike_count", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer dislikeCount;

    /**
     * 回复数
     */
    @TableField("answer_count")
    private Integer answerCount;

    // ========== 回复相关字段 ==========

    /**
     * 是否是回复评论
     */
    @TableField("is_reply")
    private Boolean isReply;

    /**
     * 回复的目标用户ID
     */
    @TableField("reply_user_id")
    private Integer replyUserId;

    /**
     * 回复的评论ID
     */
    @TableField("reply_comment_id")
    private Integer replyCommentId;

    /**
     * 是否是嵌套回复
     */
    @TableField("is_nested_reply")
    private Boolean isNestedReply;

    /**
     * 嵌套回复的目标用户ID
     */
    @TableField("nested_reply_user_id")
    private Integer nestedReplyUserId;

    /**
     * 嵌套回复的回复ID
     */
    @TableField("nested_reply_comment_id")
    private Integer nestedReplyCommentId;

    // ========== 其他字段 ==========

    /**
     * 安全指数，范围0-10
     */
    private Integer safety;

    /**
     * 是否为精选评论
     */
    @TableField("is_recommended")
    private Boolean isRecommended;

    /**
     * 是否已删除（逻辑删除）
     */
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
