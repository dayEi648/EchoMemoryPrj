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
 * 热度表实体类，对应 hot 表。
 * 存储歌曲/歌单/专辑的热度统计数据。
 * hot_level 和 trend 为数据库生成列，由 PostgreSQL 自动维护。
 *
 * @author itdaie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("hot")
public class Hot {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 目标类型：song / playlist / album
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标ID
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 7日播放增量
     */
    @TableField("play_count_7d")
    private Integer playCount7d;

    /**
     * 24小时播放增量
     */
    @TableField("play_count_24h")
    private Integer playCount24h;

    /**
     * 7日收藏增量
     */
    @TableField("collect_count_7d")
    private Integer collectCount7d;

    /**
     * 24小时收藏增量
     */
    @TableField("collect_count_24h")
    private Integer collectCount24h;

    /**
     * 7日评论增量
     */
    @TableField("comment_count_7d")
    private Integer commentCount7d;

    /**
     * 24小时评论增量
     */
    @TableField("comment_count_24h")
    private Integer commentCount24h;

    /**
     * 当前热度值（0~1000）
     */
    @TableField("hot_score")
    private Integer hotScore;

    /**
     * 热度等级（0冷/1温/2热/3爆）—— 数据库生成列
     */
    @TableField(exist = false)
    private Integer hotLevel;

    /**
     * 上一批热度值
     */
    @TableField("prev_hot_score")
    private Integer prevHotScore;

    /**
     * 趋势（up/down/stable）—— 数据库生成列
     */
    @TableField(exist = false)
    private String trend;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
