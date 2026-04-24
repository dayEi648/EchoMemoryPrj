package com.itdaie.service;

import com.itdaie.pojo.dto.PrivateMessageDTO;
import com.itdaie.pojo.vo.ConversationVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.pojo.vo.PrivateMessageVO;

import java.util.List;

/**
 * 私信服务接口
 *
 * @author itdaie
 */
public interface PrivateMessageService {

    /**
     * 获取当前用户的会话列表
     *
     * @param userId 当前用户ID
     * @return 会话列表
     */
    List<ConversationVO> getConversations(Integer userId);

    /**
     * 分页查询某个会话的消息
     *
     * @param userId          当前用户ID
     * @param conversationKey 会话键
     * @param pageNum         页码
     * @param pageSize        每页大小
     * @return 分页结果
     */
    PageDataVo getMessages(Integer userId, String conversationKey, int pageNum, int pageSize);

    /**
     * 发送私信
     *
     * @param userId 发送者ID
     * @param dto    私信内容
     */
    void sendMessage(Integer userId, PrivateMessageDTO dto);

    /**
     * 标记某会话为已读
     *
     * @param userId          当前用户ID
     * @param conversationKey 会话键
     */
    void markAsRead(Integer userId, String conversationKey);

    /**
     * 获取用户未读私信数
     *
     * @param userId 用户ID
     * @return 未读数
     */
    long getUnreadCount(Integer userId);
}
