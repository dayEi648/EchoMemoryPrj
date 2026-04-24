package com.itdaie.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.itdaie.common.handler.IntegerListTypeHandler;
import com.itdaie.common.handler.JsonbTypeHandler;
import com.itdaie.common.handler.StringListTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空间说说实体类，对应 space_posts 表
 *
 * @author itdaie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "space_posts", autoResultMap = true)
public class SpacePost {

    /**
     * 主键ID，BIGSERIAL自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发布用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 说说内容
     */
    private String content;

    /**
     * 图片URL数组
     */
    @TableField(value = "images", typeHandler = StringListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<String> images;

    /**
     * 帖子类型：original(原创) / forward(转发)
     */
    @TableField("post_type")
    private String postType;

    /**
     * 转发来源说说ID
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 转发来源类型
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 转发来源用户ID
     */
    @TableField("source_user_id")
    private Integer sourceUserId;

    /**
     * 额外数据（JSON格式，如@提及用户ID列表）
     */
    @TableField(value = "extra", typeHandler = JsonbTypeHandler.class)
    private String extra;

    /**
     * 是否仅自己可见
     */
    @TableField("private")
    private Boolean isPrivate;

    /**
     * 点赞用户ID数组
     */
    @TableField(value = "like_ids", typeHandler = IntegerListTypeHandler.class, jdbcType = JdbcType.ARRAY)
    private List<Integer> likeIds;

    /**
     * 评论数
     */
    @TableField("comment_count")
    private Integer commentCount;

    /**
     * 转发数
     */
    @TableField("forward_count")
    private Integer forwardCount;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;

    /**
     * 是否已删除（逻辑删除）
     */
    @TableField("deleted")
    private Boolean deleted;
}
