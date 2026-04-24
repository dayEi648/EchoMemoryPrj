package com.itdaie.scheduler;

import com.itdaie.service.HotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 热度计算定时任务
 * 每天凌晨4点执行：基于前一日 daily_stats 计算并更新所有作品热度。
 *
 * @author itdaie
 */
@Slf4j
@Component
public class HotScoreScheduler {

    @Autowired
    private HotService hotService;

    /**
     * 每天凌晨4点执行热度刷新。
     * cron = 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void dailyHotRefresh() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        log.info("=== 定时任务启动：每日热度刷新 === yesterday={}, today={}", yesterday, today);
        try {
            hotService.batchUpdateHotFromDailyStats(yesterday, today);
            log.info("=== 定时任务完成：每日热度刷新 ===");
        } catch (Exception e) {
            log.error("每日热度刷新任务执行失败", e);
        }
    }
}
