package com.itdaie.service;

/**
 * 每日热度增量服务接口
 * 负责记录用户行为产生的播放/收藏/评论增量，并维护 daily_stats 表。
 *
 * @author itdaie
 */
public interface DailyStatsService {

    /**
     * 记录一次播放行为。
     * 对应当前业务日期的 daily_stats 记录，播放数 +1（不存在则自动创建）。
     *
     * @param targetType 目标类型：song / playlist / album
     * @param targetId   目标ID
     */
    void recordPlay(String targetType, Long targetId);

    /**
     * 记录一次收藏行为。
     * 对应当前业务日期的 daily_stats 记录，收藏数 +1（不存在则自动创建）。
     *
     * @param targetType 目标类型：song / playlist / album
     * @param targetId   目标ID
     */
    void recordCollect(String targetType, Long targetId);

    /**
     * 记录一次取消收藏行为。
     * 对应当前业务日期的 daily_stats 记录，收藏数 -1（安全：最低到0）。
     *
     * @param targetType 目标类型：song / playlist / album
     * @param targetId   目标ID
     */
    void cancelCollect(String targetType, Long targetId);

    /**
     * 记录一次评论行为。
     * 对应当前业务日期的 daily_stats 记录，评论数 +1（不存在则自动创建）。
     *
     * @param targetType 目标类型：song / playlist / album
     * @param targetId   目标ID
     */
    void recordComment(String targetType, Long targetId);
}
