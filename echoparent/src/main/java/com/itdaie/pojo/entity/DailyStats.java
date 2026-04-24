package com.itdaie.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 每日热度增量表实体类，对应 daily_stats 表。
 * 记录每个作品在特定日期的播放/收藏/评论增量。
 *
 * @author itdaie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("daily_stats")
public class DailyStats {

    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 统计日期
     */
    @TableField("stat_date")
    private LocalDate statDate;

    /**
     * 当日新增播放数
     */
    @TableField("daily_play_count")
    private Integer dailyPlayCount;

    /**
     * 当日新增收藏数
     */
    @TableField("daily_collect_count")
    private Integer dailyCollectCount;

    /**
     * 当日新增评论数
     */
    @TableField("daily_comment_count")
    private Integer dailyCommentCount;
}
