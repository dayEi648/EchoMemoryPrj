package com.itdaie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.mapper.PrivateMessageMapper;
import com.itdaie.mapper.UserMapper;
import com.itdaie.pojo.dto.PrivateMessageDTO;
import com.itdaie.pojo.entity.PrivateMessage;
import com.itdaie.pojo.entity.User;
import com.itdaie.pojo.vo.ConversationVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PrivateMessageVO;
import com.itdaie.service.PrivateMessageService;
import com.itdaie.utils.ConversationKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 私信服务实现类
 *
 * @author itdaie
 */
@Service
public class PrivateMessageServiceImpl implements PrivateMessageService {

    @Autowired
    private PrivateMessageMapper privateMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<ConversationVO> getConversations(Integer userId) {
        if (userId == null) {
            return List.of();
        }

        // 查询用户参与的所有消息
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateMessage::getDeleted, false)
                .and(w -> w.eq(PrivateMessage::getSenderId, userId)
                        .or()
                        .eq(PrivateMessage::getReceiverId, userId))
                .orderByDesc(PrivateMessage::getCreateTime);

        List<PrivateMessage> allMessages = privateMessageMapper.selectList(wrapper);
        if (allMessages.isEmpty()) {
            return List.of();
        }

        // 按 conversation_key 分组
        Map<String, List<PrivateMessage>> grouped = allMessages.stream()
                .collect(Collectors.groupingBy(PrivateMessage::getConversationKey));

        List<ConversationVO> conversations = new ArrayList<>();
        for (Map.Entry<String, List<PrivateMessage>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            List<PrivateMessage> messages = entry.getValue();

            // 最后一条消息
            PrivateMessage lastMsg = messages.stream()
                    .max(Comparator.comparing(PrivateMessage::getCreateTime))
                    .orElse(null);

            if (lastMsg == null) {
                continue;
            }

            // 未读数（当前用户是接收者且未读）
            long unreadCount = messages.stream()
                    .filter(m -> userId.equals(m.getReceiverId()) && Boolean.FALSE.equals(m.getIsRead()))
                    .count();

            // 对方用户ID
            Integer otherUserId = ConversationKeyUtil.getOtherUserId(key, userId);
            User otherUser = otherUserId != null ? userMapper.selectById(otherUserId) : null;

            ConversationVO vo = new ConversationVO();
            vo.setConversationKey(key);
            vo.setOtherUserId(otherUserId);
            vo.setOtherUserName(otherUser != null ? otherUser.getName() : "未知用户");
            vo.setOtherUserAvatar(otherUser != null ? otherUser.getAvatar() : null);
            vo.setLastMessage(lastMsg.getContent());
            vo.setLastMessageTime(lastMsg.getCreateTime());
            vo.setUnreadCount(unreadCount);
            conversations.add(vo);
        }

        // 按最后消息时间倒序
        conversations.sort((a, b) -> {
            if (a.getLastMessageTime() == null || b.getLastMessageTime() == null) {
                return 0;
            }
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return conversations;
    }

    @Override
    public PageDataVo getMessages(Integer userId, String conversationKey, int pageNum, int pageSize) {
        if (userId == null || !StringUtils.hasText(conversationKey)) {
            return new PageDataVo(0L, List.of());
        }
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 20;
        }

        Page<PrivateMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateMessage::getConversationKey, conversationKey)
                .eq(PrivateMessage::getDeleted, false)
                .orderByDesc(PrivateMessage::getCreateTime);

        Page<PrivateMessage> resultPage = privateMessageMapper.selectPage(page, wrapper);
        List<PrivateMessageVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        return new PageDataVo(resultPage.getTotal(), records);
    }

    @Override
    @Transactional
    public void sendMessage(Integer userId, PrivateMessageDTO dto) {
        if (userId == null) {
            throw new BusinessException("发送者ID不能为空");
        }
        if (dto == null || dto.getReceiverId() == null) {
            throw new BusinessException("接收者ID不能为空");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            throw new BusinessException("消息内容不能为空");
        }
        if (dto.getContent().length() > 2000) {
            throw new BusinessException("消息内容不能超过2000字符");
        }
        if (userId.equals(dto.getReceiverId())) {
            throw new BusinessException("不能给自己发私信");
        }

        User receiver = userMapper.selectById(dto.getReceiverId());
        if (receiver == null || Boolean.TRUE.equals(receiver.getIsDeleted())) {
            throw new BusinessException("接收者不存在");
        }

        String conversationKey = ConversationKeyUtil.build(userId, dto.getReceiverId());

        PrivateMessage message = PrivateMessage.builder()
                .senderId(userId)
                .receiverId(dto.getReceiverId())
                .conversationKey(conversationKey)
                .content(dto.getContent().trim())
                .isRead(false)
                .deleted(false)
                .build();

        privateMessageMapper.insert(message);
    }

    @Override
    @Transactional
    public void markAsRead(Integer userId, String conversationKey) {
        if (userId == null || !StringUtils.hasText(conversationKey)) {
            return;
        }
        privateMessageMapper.markConversationAsRead(conversationKey, userId);
    }

    @Override
    public long getUnreadCount(Integer userId) {
        if (userId == null) {
            return 0;
        }
        return privateMessageMapper.countUnreadByUserId(userId);
    }

    private PrivateMessageVO convertToVO(PrivateMessage msg) {
        PrivateMessageVO vo = new PrivateMessageVO();
        vo.setId(msg.getId());
        vo.setSenderId(msg.getSenderId());
        vo.setReceiverId(msg.getReceiverId());
        vo.setConversationKey(msg.getConversationKey());
        vo.setContent(msg.getContent());
        vo.setIsRead(msg.getIsRead());
        vo.setCreateTime(msg.getCreateTime());

        if (msg.getSenderId() != null) {
            User sender = userMapper.selectById(msg.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getName());
                vo.setSenderAvatar(sender.getAvatar());
            }
        }
        return vo;
    }
}
