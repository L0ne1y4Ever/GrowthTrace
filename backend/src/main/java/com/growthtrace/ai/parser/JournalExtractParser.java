package com.growthtrace.ai.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.ai.dto.JournalExtractResult;
import com.growthtrace.common.exception.AiException;
import com.growthtrace.common.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JournalExtractParser {

    @SuppressWarnings("unchecked")
    public JournalExtractResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw AiException.parseError("AI 响应为空");
        }
        try {
            Map<String, Object> root = JsonUtils.fromJson(raw, new TypeReference<>() {
            });
            return JournalExtractResult.builder()
                    .newSkills((List<Map<String, Object>>) root.getOrDefault("new_skills", List.of()))
                    .relatedRequirements((List<Map<String, Object>>) root.getOrDefault("related_requirements", List.of()))
                    .events((List<Map<String, Object>>) root.getOrDefault("events", List.of()))
                    .blockers(((List<?>) root.getOrDefault("blockers", List.of()))
                            .stream().map(String::valueOf).toList())
                    .build();
        } catch (Exception e) {
            throw AiException.parseError("随记事件抽取解析失败: " + e.getMessage());
        }
    }
}
