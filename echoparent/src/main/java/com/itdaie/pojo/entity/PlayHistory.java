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
 * 播放历史实体类，对应 play_history 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("play_history")
public class PlayHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Integer userId;

    @TableField("song_id")
    private Integer songId;

    @TableField("played_at")
    private LocalDateTime playedAt;
}
