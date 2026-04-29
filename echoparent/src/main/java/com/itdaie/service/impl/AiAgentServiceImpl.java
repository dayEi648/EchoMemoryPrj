package com.itdaie.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itdaie.common.Result;
import com.itdaie.common.exception.BusinessException;
import com.itdaie.pojo.dto.ChatRequestDTO;
import com.itdaie.service.AiAgentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

@Slf4j
@Service
public class AiAgentServiceImpl implements AiAgentService {

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiBaseUrl;

    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    public AiAgentServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        this.aiBaseUrl = this.aiBaseUrl.endsWith("/")
                ? this.aiBaseUrl.substring(0, this.aiBaseUrl.length() - 1)
                : this.aiBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PreDestroy
    public void destroy() {
        log.info("AiAgentService 销毁");
    }

    @Override
    public Result<?> chat(ChatRequestDTO request, String jwtToken) {
        return forwardPost("/chat", request, jwtToken);
    }

    @Override
    public SseEmitter chatStream(ChatRequestDTO request, String jwtToken) {
        SseEmitter emitter = new SseEmitter(300_000L);

        try {
            String json = objectMapper.writeValueAsString(request);
            // SSE 流式请求不设置请求级超时，避免 LLM 生成较长时连接被强制断开
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Authorization", jwtToken)
                    .uri(URI.create(aiBaseUrl + "/chat/stream"))
                    .header("Accept", "text/event-stream")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            CompletableFuture<HttpResponse<Void>> future = httpClient.sendAsync(
                    httpRequest,
                    HttpResponse.BodyHandlers.fromLineSubscriber(new SseLineSubscriber(emitter))
            );

            emitter.onCompletion(() -> future.cancel(true));
            emitter.onTimeout(() -> {
                future.cancel(true);
                emitter.complete();
            });
            emitter.onError(e -> future.cancel(true));

        } catch (Exception e) {
            log.error("启动 SSE 流失败", e);
            emitter.completeWithError(new BusinessException("启动 AI 流式对话失败"));
        }

        return emitter;
    }

    @Override
    public Result<?> listSessions(String jwtToken) {
        return forwardGet("/sessions", jwtToken);
    }

    @Override
    public Result<?> getMessages(String sessionId, Integer pageNum, Integer pageSize, String jwtToken) {
        String path = String.format("/sessions/%s/messages?page_num=%d&page_size=%d",
                sessionId, pageNum, pageSize);
        return forwardGet(path, jwtToken);
    }

    @Override
    public Result<?> heartbeat(String sessionId, String jwtToken) {
        String path = "/sessions/" + sessionId + "/heartbeat";
        return forwardPost(path, null, jwtToken);
    }

    @Override
    public Result<?> deleteSession(String sessionId, String jwtToken) {
        String path = "/sessions/" + sessionId;
        return forwardDelete(path, jwtToken);
    }

    @Override
    public Result<?> uploadKnowledge(MultipartFile file, String title, String jwtToken) {
        try {
            String boundary = UUID.randomUUID().toString();
            HttpRequest.BodyPublisher body = buildMultipartBody(file, title, boundary);

            HttpRequest request = buildRequestBuilder(jwtToken)
                    .uri(URI.create(aiBaseUrl + "/knowledge"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(body)
                    .build();

            return executeRequest(request);
        } catch (IOException e) {
            log.error("构建知识库上传请求失败", e);
            throw new BusinessException("文件读取失败");
        }
    }

    @Override
    public Result<?> listKnowledge(Integer pageNum, Integer pageSize, String jwtToken) {
        String path = String.format("/knowledge?page_num=%d&page_size=%d", pageNum, pageSize);
        return forwardGet(path, jwtToken);
    }

    @Override
    public Result<?> deleteKnowledge(String docId, String jwtToken) {
        String path = "/knowledge/" + docId;
        return forwardDelete(path, jwtToken);
    }

    @Override
    public Result<?> searchKnowledge(String query, Integer topK, String jwtToken) {
        return forwardPost("/knowledge/search", query, jwtToken);
    }

    @Override
    public Result<?> getKnowledgeDetail(String docId, String jwtToken) {
        String path = "/knowledge/" + docId;
        return forwardGet(path, jwtToken);
    }

    @Override
    public Result<?> createSession(String jwtToken) {
        return forwardPost("/sessions", null, jwtToken);
    }

    @Override
    public Result<?> clearMemory(String sessionId, String jwtToken) {
        String path = "/sessions/" + sessionId + "/memory";
        return forwardDelete(path, jwtToken);
    }

    // ==================== 私有通用方法 ====================

    private HttpRequest.Builder buildRequestBuilder(String jwtToken) {
        return HttpRequest.newBuilder()
                .header("Authorization", jwtToken)
                .timeout(Duration.ofSeconds(30));
    }

    private Result<?> forwardGet(String path, String jwtToken) {
        HttpRequest request = buildRequestBuilder(jwtToken)
                .uri(URI.create(aiBaseUrl + path))
                .GET()
                .build();
        return executeRequest(request);
    }

    private Result<?> forwardPost(String path, Object body, String jwtToken) {
        try {
            HttpRequest.Builder builder = buildRequestBuilder(jwtToken)
                    .uri(URI.create(aiBaseUrl + path))
                    .header("Content-Type", "application/json");
            if (body != null) {
                // 如果 body 已经是 String（如知识库搜索的原始 JSON），直接透传，避免二次序列化
                String bodyString;
                if (body instanceof String) {
                    bodyString = (String) body;
                } else {
                    bodyString = objectMapper.writeValueAsString(body);
                }
                log.debug("Forward POST to {}, body: {}", aiBaseUrl + path, bodyString);
                builder.POST(HttpRequest.BodyPublishers.ofString(bodyString));
            } else {
                log.debug("Forward POST to {}, body: (empty)", aiBaseUrl + path);
                builder.POST(HttpRequest.BodyPublishers.noBody());
            }
            return executeRequest(builder.build());
        } catch (Exception e) {
            log.error("构建 POST 请求失败: {}", path, e);
            throw new BusinessException("请求构建失败");
        }
    }

    private Result<?> forwardDelete(String path, String jwtToken) {
        HttpRequest request = buildRequestBuilder(jwtToken)
                .uri(URI.create(aiBaseUrl + path))
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        return executeRequest(request);
    }

    private Result<?> executeRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response);
        } catch (IOException e) {
            log.error("调用 AI 服务 IO 异常", e);
            throw new BusinessException(500, "AI 服务不可用");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("调用 AI 服务被中断", e);
            throw new BusinessException(500, "AI 服务调用被中断");
        }
    }

    private Result<?> parseResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String body = response.body();

        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("code") && node.has("msg")) {
                int code = node.get("code").asInt();
                String msg = node.get("msg").asText();
                if (code == 200) {
                    return Result.success(msg, node.has("data") ? node.get("data") : null);
                }
                return Result.fail(code, msg);
            }
        } catch (Exception e) {
            log.warn("AI 服务响应非标准 JSON: {}", body);
        }

        if (statusCode >= 200 && statusCode < 300) {
            return Result.success(body);
        }
        return Result.fail(statusCode, "AI 服务错误");
    }

    private HttpRequest.BodyPublisher buildMultipartBody(MultipartFile file, String title, String boundary) throws IOException {
        byte[] fileBytes = file.getBytes();
        String rawFilename = file.getOriginalFilename();
        // 防御性过滤：防止文件名中的特殊字符破坏 multipart header
        String filename = rawFilename != null
                ? rawFilename.replace("\r", "").replace("\n", "").replace("\"", "")
                : "unknown";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        // 构建 title 字段（如果有）
        String titlePart = "";
        if (title != null && !title.isEmpty()) {
            titlePart = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"title\"\r\n\r\n" +
                    title + "\r\n";
        }

        String header = titlePart +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
                "Content-Type: " + contentType + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        return HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(header, StandardCharsets.UTF_8),
                HttpRequest.BodyPublishers.ofByteArray(fileBytes),
                HttpRequest.BodyPublishers.ofString(footer, StandardCharsets.UTF_8)
        );
    }

    // ==================== SSE 行解析订阅者 ====================

    private static class SseLineSubscriber implements Flow.Subscriber<String> {

        private final SseEmitter emitter;
        private Flow.Subscription subscription;
        private String currentEvent = "message";
        private final StringBuilder currentData = new StringBuilder();
        private boolean hasData = false;

        SseLineSubscriber(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(String line) {
            try {
                if (line.startsWith("event:")) {
                    flushIfNeeded();
                    currentEvent = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    if (hasData) {
                        currentData.append("\n");
                    }
                    currentData.append(line.substring(5).trim());
                    hasData = true;
                } else if (line.isEmpty()) {
                    flushIfNeeded();
                }
                subscription.request(1);
            } catch (Exception e) {
                subscription.cancel();
                emitter.completeWithError(e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("SSE 流读取异常", throwable);
            emitter.completeWithError(throwable);
        }

        @Override
        public void onComplete() {
            try {
                flushIfNeeded();
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }

        private void flushIfNeeded() throws IOException {
            if (hasData) {
                emitter.send(SseEmitter.event()
                        .name(currentEvent)
                        .data(currentData.toString()));
                currentData.setLength(0);
                hasData = false;
            }
        }
    }
}
