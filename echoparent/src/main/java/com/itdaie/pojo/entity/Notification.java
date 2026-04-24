package com.itdaie.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知实体类，对应 notifications 表
 * 支持类型：mention(@我的)/like(点赞)/collect(收藏)/follow(关注)/system(系统)/reply(回复)/comment(评论)
 *
 * @author itdaie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notifications")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收通知的用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 通知类型：mention/like/collect/follow/system/reply/comment
     */
    private String type;

    /**
     * 发送者用户ID
     */
    @TableField("sender_id")
    private Integer senderId;

    /**
     * 来源类型：comment/playlist/space/song/album
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 来源对象ID
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 来源父对象ID（如评论所属的作品ID）
     */
    @TableField("source_parent_id")
    private Long sourceParentId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 额外信息（JSONB）
     */
    private String extra;

    /**
     * 是否已读
     */
    @TableField("is_read")
    private Boolean isRead;

    /**
     * 已读时间
     */
    @TableField("read_time")
    private LocalDateTime readTime;

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

    /**
     * 是否已删除（逻辑删除）
     */
    private Boolean deleted;
}
