package com.itdaie.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AI 对话请求 DTO。
 * 字段名映射为 snake_case 以适配 Python FastAPI 服务。
 */
@Data
public class ChatRequestDTO {

    private String message;

    @JsonProperty("session_id")
    private String sessionId;
}
