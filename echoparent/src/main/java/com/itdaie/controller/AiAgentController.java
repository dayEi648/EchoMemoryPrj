package com.itdaie.controller;

import com.itdaie.common.Result;
import com.itdaie.pojo.dto.ChatRequestDTO;
import com.itdaie.service.AiAgentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI Agent 代理控制器。
 * 作为前端与 Python FastAPI 服务之间的网关，负责：
 * <ul>
 *   <li>鉴权校验（登录态 + 管理员权限）</li>
 *   <li>JWT Token 透传</li>
 *   <li>SSE 流式响应转发</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiAgentController {

    @Autowired
    private AiAgentService aiAgentService;

    private String extractJwtToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader;
    }

    private Integer getUserId(HttpServletRequest request) {
        return (Integer) request.getAttribute("userId");
    }

    // ==================== 对话接口 ====================

    @PostMapping(value = "/chat", produces = "application/json")
    public Result<?> chat(@RequestBody ChatRequestDTO dto, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.chat(dto, token);
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    public SseEmitter chatStream(@RequestBody ChatRequestDTO dto, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);

        Integer userId = getUserId(request);
        String token = extractJwtToken(request);
        if (userId == null || token == null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"code\":401,\"msg\":\"未登录\"}"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        try {
            return aiAgentService.chatStream(dto, token);
        } catch (Exception e) {
            log.error("AI 流式对话异常", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"code\":500,\"msg\":\"AI 服务调用失败\"}"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
            return emitter;
        }
    }

    // ==================== 会话管理 ====================

    @GetMapping("/sessions")
    public Result<?> listSessions(HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.listSessions(token);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<?> getMessages(@PathVariable String sessionId,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "20") Integer pageSize,
                                 HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.getMessages(sessionId, pageNum, pageSize, token);
    }

    @PostMapping("/sessions/{sessionId}/heartbeat")
    public Result<?> heartbeat(@PathVariable String sessionId, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.heartbeat(sessionId, token);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<?> deleteSession(@PathVariable String sessionId, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.deleteSession(sessionId, token);
    }

    // ==================== 知识库管理（管理员） ====================

    @PostMapping("/knowledge")
    public Result<?> uploadKnowledge(@RequestParam MultipartFile file,
                                     @RequestParam(required = false) String title,
                                     HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) {
            return Result.fail(403, "无权限");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.uploadKnowledge(file, title, token);
    }

    @GetMapping("/knowledge")
    public Result<?> listKnowledge(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                   HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) {
            return Result.fail(403, "无权限");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.listKnowledge(pageNum, pageSize, token);
    }

    @DeleteMapping("/knowledge/{docId}")
    public Result<?> deleteKnowledge(@PathVariable String docId, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) {
            return Result.fail(403, "无权限");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.deleteKnowledge(docId, token);
    }

    @PostMapping("/knowledge/search")
    public Result<?> searchKnowledge(@RequestBody String body, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) {
            return Result.fail(403, "无权限");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.searchKnowledge(body, null, token);
    }

    @GetMapping("/knowledge/{docId}")
    public Result<?> getKnowledgeDetail(@PathVariable String docId, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) {
            return Result.fail(403, "无权限");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.getKnowledgeDetail(docId, token);
    }

    @PostMapping("/sessions")
    public Result<?> createSession(HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.createSession(token);
    }

    @DeleteMapping("/sessions/{sessionId}/memory")
    public Result<?> clearMemory(@PathVariable String sessionId, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.clearMemory(sessionId, token);
    }

    /**
     * 兼容 navigator.sendBeacon（仅支持 POST）的内存清理接口。
     * 功能与 DELETE /sessions/{sessionId}/memory 完全一致。
     */
    @PostMapping("/sessions/{sessionId}/memory")
    public Result<?> clearMemoryPost(@PathVariable String sessionId, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        String token = extractJwtToken(request);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        return aiAgentService.clearMemory(sessionId, token);
    }


}
