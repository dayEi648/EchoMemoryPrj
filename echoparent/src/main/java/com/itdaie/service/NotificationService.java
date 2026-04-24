package com.itdaie.service;

import com.itdaie.pojo.dto.NotificationPageDTO;
import com.itdaie.pojo.vo.NotificationUnreadCountVO;
import com.itdaie.pojo.vo.NotificationVO;
import com.itdaie.pojo.vo.PageDataVo;

import java.util.List;

/**
 * 通知服务接口
 *
 * @author itdaie
 */
public interface NotificationService {

    /**
     * 分页查询当前用户的通知列表
     *
     * @param userId 当前用户ID
     * @param dto    分页查询参数
     * @return 分页结果
     */
    PageDataVo pageQuery(Integer userId, NotificationPageDTO dto);

    /**
     * 获取当前用户的未读通知统计
     *
     * @param userId 当前用户ID
     * @return 各分类未读数
     */
    NotificationUnreadCountVO getUnreadCount(Integer userId);

    /**
     * 批量标记通知为已读
     *
     * @param userId 当前用户ID
     * @param ids    通知ID列表
     */
    void markAsRead(Integer userId, List<Long> ids);

    /**
     * 按类型全部标记为已读
     *
     * @param userId 当前用户ID
     * @param type   通知类型，null表示全部
     */
    void markAllAsRead(Integer userId, String type);

    /**
     * 发送通知（内部调用，用于业务触发）
     *
     * @param userId     接收者ID
     * @param type       通知类型
     * @param senderId   发送者ID
     * @param sourceType 来源类型
     * @param sourceId   来源ID
     * @param title      标题
     * @param content    内容
     */
    void sendNotification(Integer userId, String type, Integer senderId,
                          String sourceType, Long sourceId, String title, String content);
}
