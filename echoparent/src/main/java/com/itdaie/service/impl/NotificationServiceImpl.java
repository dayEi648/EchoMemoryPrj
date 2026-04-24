package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.mapper.NotificationMapper;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.NotificationPageDTO;
import com.itdaie.pojo.entity.Notification;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.NotificationUnreadCountVO;
import com.itdaie.pojo.vo.NotificationVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务实现类
 *
 * @author itdaie
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageDataVo pageQuery(Integer userId, NotificationPageDTO dto) {
        if (userId == null) {
            return new PageDataVo(0L, List.of());
        }

        long pageNum = dto.getPageNum() != null && dto.getPageNum() > 0 ? dto.getPageNum() : 1;
        long pageSize = dto.getPageSize() != null && dto.getPageSize() > 0 ? dto.getPageSize() : 10;
        if (pageSize > 50) {
            pageSize = 50;
        }

        Page<Notification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getDeleted, false);

        // category 优先级高于 type，用于聚合展示
        if (StringUtils.hasText(dto.getCategory())) {
            switch (dto.getCategory()) {
                case "mention" -> wrapper.eq(Notification::getType, "mention");
                case "reply", "comment" -> wrapper.in(Notification::getType, List.of("reply", "comment"));
                case "notify" -> wrapper.in(Notification::getType, List.of("follow", "collect", "like", "system"));
                default -> wrapper.eq(Notification::getType, dto.getCategory());
            }
        } else if (StringUtils.hasText(dto.getType())) {
            wrapper.eq(Notification::getType, dto.getType());
        }

        if (Boolean.TRUE.equals(dto.getUnreadOnly())) {
            wrapper.eq(Notification::getIsRead, false);
        }

        wrapper.orderByDesc(Notification::getCreateTime);

        Page<Notification> resultPage = notificationMapper.selectPage(page, wrapper);
        List<NotificationVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), records);
    }

    @Override
    public NotificationUnreadCountVO getUnreadCount(Integer userId) {
        NotificationUnreadCountVO vo = new NotificationUnreadCountVO();
        if (userId == null) {
            return vo;
        }

        List<NotificationMapper.TypeCount> typeCounts = notificationMapper.selectUnreadCountByType(userId);
        long mention = 0;
        long reply = 0;
        long notify = 0;

        for (NotificationMapper.TypeCount tc : typeCounts) {
            long cnt = tc.getCnt() != null ? tc.getCnt() : 0;
            switch (tc.getType()) {
                case "mention" -> mention = cnt;
                case "reply", "comment" -> reply += cnt;
                case "follow", "collect", "like", "system" -> notify += cnt;
            }
        }

        vo.setMention(mention);
        vo.setReply(reply);
        vo.setNotify(notify);
        // 私信未读数由 PrivateMessageService 提供，此处先不设置
        vo.setTotal(mention + reply + notify);
        return vo;
    }

    @Override
    @Transactional
    public void markAsRead(Integer userId, List<Long> ids) {
        if (userId == null || ids == null || ids.isEmpty()) {
            return;
        }
        String idList = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        notificationMapper.markAsRead(idList, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer userId, String category) {
        if (userId == null) {
            return;
        }
        List<String> types = null;
        if (StringUtils.hasText(category)) {
            switch (category) {
                case "mention" -> types = List.of("mention");
                case "reply", "comment" -> types = List.of("reply", "comment");
                case "notify" -> types = List.of("follow", "collect", "like", "system");
                default -> types = List.of(category);
            }
        }
        notificationMapper.markAllAsRead(userId, types);
    }

    @Override
    @Transactional
    public void sendNotification(Integer userId, String type, Integer senderId,
                                 String sourceType, Long sourceId, String title, String content) {
        if (userId == null || userId.equals(senderId)) {
            return;
        }
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .senderId(senderId)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .title(title)
                .content(content)
                .isRead(false)
                .deleted(false)
                .build();
        notificationMapper.insert(notification);
    }

    private NotificationVO convertToVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setUserId(n.getUserId());
        vo.setType(n.getType());
        vo.setSenderId(n.getSenderId());
        vo.setSourceType(n.getSourceType());
        vo.setSourceId(n.getSourceId());
        vo.setSourceParentId(n.getSourceParentId());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setExtra(n.getExtra());
        vo.setIsRead(n.getIsRead());
        vo.setReadTime(n.getReadTime());
        vo.setCreateTime(n.getCreateTime());

        // 查询发送者信息
        if (n.getSenderId() != null) {
            User sender = userMapper.selectById(n.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getName());
                vo.setSenderAvatar(sender.getAvatar());
            }
        }
        return vo;
    }
}
