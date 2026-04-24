package com.itdaie.service.impl;

import com.itdaie.mapper.DailyStatsMapper;
import com.itdaie.service.DailyStatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日热度增量服务实现类
 *
 * @author itdaie
 */
@Slf4j
@Service
public class DailyStatsServiceImpl implements DailyStatsService {

    @Autowired
    private DailyStatsMapper dailyStatsMapper;

    /**
     * 获取当前业务日期（以凌晨4点为界）。
     * 04:00 之前算昨天，04:00 及之后算今天。
     */
    private LocalDate getBusinessDate() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() < 4) {
            return now.toLocalDate().minusDays(1);
        }
        return now.toLocalDate();
    }

    @Override
    public void recordPlay(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return;
        }
        dailyStatsMapper.upsertIncrementPlayCount(targetType, targetId, getBusinessDate());
    }

    @Override
    public void recordCollect(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return;
        }
        dailyStatsMapper.upsertIncrementCollectCount(targetType, targetId, getBusinessDate());
    }

    @Override
    public void cancelCollect(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return;
        }
        dailyStatsMapper.decrementCollectCount(targetType, targetId, getBusinessDate());
    }

    @Override
    public void recordComment(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return;
        }
        dailyStatsMapper.upsertIncrementCommentCount(targetType, targetId, getBusinessDate());
    }
}
