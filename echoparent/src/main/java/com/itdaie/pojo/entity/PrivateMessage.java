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
 * 私信实体类，对应 private_message 表
 *
 * @author itdaie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("private_message")
public class PrivateMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送者ID
     */
    @TableField("sender_id")
    private Integer senderId;

    /**
     * 接收者ID
     */
    @TableField("receiver_id")
    private Integer receiverId;

    /**
     * 会话键，格式为 "小ID:大ID"
     */
    @TableField("conversation_key")
    private String conversationKey;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 是否已读
     */
    @TableField("is_read")
    private Boolean isRead;

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
     * 是否已删除
     */
    private Boolean deleted;
}
