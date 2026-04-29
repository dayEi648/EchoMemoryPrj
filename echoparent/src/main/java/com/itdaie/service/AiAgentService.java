package com.itdaie.service;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.ChatRequestDTO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI Agent 代理服务。
 * 负责通过 HTTP 将前端请求透传给 Python FastAPI 服务，并负责 SSE 流式响应的转发。
 */
public interface AiAgentService {

    /**
     * 非流式 AI 对话。
     */
    Result<?> chat(ChatRequestDTO request, String jwtToken);

    /**
     * 流式 AI 对话（SSE）。
     */
    SseEmitter chatStream(ChatRequestDTO request, String jwtToken);

    /**
     * 获取当前用户的会话列表。
     */
    Result<?> listSessions(String jwtToken);

    /**
     * 分页获取会话消息。
     */
    Result<?> getMessages(String sessionId, Integer pageNum, Integer pageSize, String jwtToken);

    /**
     * 会话心跳，延长会话过期时间。
     */
    Result<?> heartbeat(String sessionId, String jwtToken);

    /**
     * 删除会话。
     */
    Result<?> deleteSession(String sessionId, String jwtToken);

    /**
     * 上传知识库文档（管理员）。
     */
    Result<?> uploadKnowledge(MultipartFile file, String title, String jwtToken);

    /**
     * 查询知识库文档列表（管理员）。
     */
    Result<?> listKnowledge(Integer pageNum, Integer pageSize, String jwtToken);

    /**
     * 删除知识库文档（管理员）。
     */
    Result<?> deleteKnowledge(String docId, String jwtToken);

    /**
     * 知识库向量检索（管理员）。
     */
    Result<?> searchKnowledge(String query, Integer topK, String jwtToken);

    /**
     * 查看知识库文档分块详情（管理员）。
     */
    Result<?> getKnowledgeDetail(String docId, String jwtToken);

    /**
     * 创建新会话。
     */
    Result<?> createSession(String jwtToken);

    /**
     * 清理会话 Redis 缓存。
     */
    Result<?> clearMemory(String sessionId, String jwtToken);
}
