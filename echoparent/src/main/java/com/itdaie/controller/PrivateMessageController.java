package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.PrivateMessageDTO;
import com.itdaie.pojo.vo.ConversationVO;
import com.itdaie.pojo.vo.PageDataVo;
import com.itdaie.service.PrivateMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 私信控制器
 *
 * @author itdaie
 */
@RestController
@RequestMapping("/api/private-messages")
@Slf4j
public class PrivateMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    /**
     * 获取当前用户的会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> conversations(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(privateMessageService.getConversations(userId));
    }

    /**
     * 分页查询某个会话的消息
     */
    @GetMapping
    public Result<PageDataVo> messages(@RequestParam String conversationKey,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "20") int pageSize,
                                        HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(privateMessageService.getMessages(userId, conversationKey, pageNum, pageSize));
    }

    /**
     * 发送私信
     */
    @PostMapping
    public Result<Void> send(@RequestBody PrivateMessageDTO dto, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        privateMessageService.sendMessage(userId, dto);
        return Result.success("发送成功", null);
    }

    /**
     * 标记某会话为已读
     */
    @PutMapping("/read")
    public Result<Void> markAsRead(@RequestParam String conversationKey, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        privateMessageService.markAsRead(userId, conversationKey);
        return Result.success("已读", null);
    }
}
