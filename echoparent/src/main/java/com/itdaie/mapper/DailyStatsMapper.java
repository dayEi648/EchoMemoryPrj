package com.itdaie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itdaie.pojo.entity.DailyStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日热度增量表 Mapper
 *
 * @author itdaie
 */
@Mapper
public interface DailyStatsMapper extends BaseMapper<DailyStats> {

    /**
     * 播放数 +1（不存在则插入，存在则累加）
     */
    int upsertIncrementPlayCount(@Param("targetType") String targetType,
                                 @Param("targetId") Long targetId,
                                 @Param("statDate") LocalDate statDate);

    /**
     * 收藏数 +1（不存在则插入，存在则累加）
     */
    int upsertIncrementCollectCount(@Param("targetType") String targetType,
                                    @Param("targetId") Long targetId,
                                    @Param("statDate") LocalDate statDate);

    /**
     * 收藏数 -1（安全：最低到0）
     */
    int decrementCollectCount(@Param("targetType") String targetType,
                              @Param("targetId") Long targetId,
                              @Param("statDate") LocalDate statDate);

    /**
     * 评论数 +1（不存在则插入，存在则累加）
     */
    int upsertIncrementCommentCount(@Param("targetType") String targetType,
                                    @Param("targetId") Long targetId,
                                    @Param("statDate") LocalDate statDate);

    /**
     * 批量插入今日记录（冲突时忽略）
     */
    void batchInsertTodayRecords(@Param("list") List<DailyStats> list);
}
