package com.growthtrace.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.growthtrace.common.exception.AiException;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 轻量 OpenAI-compatible HTTP 客户端。
 * 支持任何遵循 OpenAI Chat Completions 协议的后端（OpenAI、DeepSeek、阿里云百炼、本地 vLLM 等）。
 * 不依赖任何 AI SDK；仅用 JDK HttpClient + Jackson。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleProvider implements AiProvider {

    private final AiProperties aiProperties;

    @Override
    public String chat(String prompt, Duration timeout) {
        if (!StringUtils.hasText(aiProperties.apiKey())) {
            throw AiException.unavailable("AI apiKey 未配置（growthtrace.ai.api-key）", null);
        }

        Map<String, Object> payload = Map.of(
                "model", aiProperties.model(),
                "temperature", aiProperties.temperatureOrDefault(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system",
                                "content", "你是 GrowthTrace 成长管理系统的分析助手。严格按要求返回 JSON，不要输出任何额外文字。"),
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(aiProperties.baseUrl() + "/chat/completions"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiProperties.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(payload)))
                .build();

        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("AI 调用非 2xx: status={}, body={}", resp.statusCode(), resp.body());
                throw AiException.unavailable("AI 返回 HTTP " + resp.statusCode(), null);
            }
            JsonNode root = JsonUtils.mapper().readTree(resp.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw AiException.parseError("AI 响应缺少 choices[0].message.content");
            }
            return content.asText();
        } catch (HttpTimeoutException ex) {
            throw AiException.timeout("AI 调用超时: " + ex.getMessage());
        } catch (AiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw AiException.unavailable("AI 调用失败: " + ex.getMessage(), ex);
        }
    }
}
