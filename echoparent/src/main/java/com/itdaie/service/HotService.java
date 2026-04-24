package com.itdaie.service;

import java.time.LocalDate;

/**
 * 热度服务接口
 * 负责热度分数计算与批量更新。
 *
 * @author itdaie
 */
public interface HotService {

    /**
     * 基于前一日 daily_stats 数据，批量计算并更新所有作品的热度。
     * 由定时任务每天凌晨4点调用。
     *
     * @param yesterday 昨天日期（计算数据源）
     * @param today     今天日期（创建新的 daily_stats 记录）
     */
    void batchUpdateHotFromDailyStats(LocalDate yesterday, LocalDate today);
}
