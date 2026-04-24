package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.NotificationPageDTO;
import com.itdaie.pojo.vo.NotificationUnreadCountVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.NotificationService;
import com.itdaie.service.PrivateMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器
 *
 * @author itdaie
 */
@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PrivateMessageService privateMessageService;

    /**
     * 获取当前用户的未读通知统计（含私信）
     */
    @GetMapping("/unread-count")
    public Result<NotificationUnreadCountVO> unreadCount(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        NotificationUnreadCountVO vo = notificationService.getUnreadCount(userId);
        long pmUnread = privateMessageService.getUnreadCount(userId);
        vo.setPrivateMessage(pmUnread);
        vo.setTotal(vo.getTotal() + pmUnread);
        return Result.success(vo);
    }

    /**
     * 分页查询通知列表
     */
    @GetMapping
    public Result<PageDataVo> list(NotificationPageDTO dto, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(notificationService.pageQuery(userId, dto));
    }

    /**
     * 批量标记通知为已读
     */
    @PutMapping("/read")
    public Result<Void> markAsRead(@RequestBody List<Long> ids, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        notificationService.markAsRead(userId, ids);
        return Result.success("标记成功", null);
    }

    /**
     * 按分类全部标记为已读
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestParam(required = false) String category,
                                       HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        notificationService.markAllAsRead(userId, category);
        return Result.success("全部已读", null);
    }
}
