package com.growthtrace.ai.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.ai.dto.DiagnosisSummaryResult;
import com.growthtrace.common.exception.AiException;
import com.growthtrace.common.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DiagnosisSummaryParser {

    @SuppressWarnings("unchecked")
    public DiagnosisSummaryResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw AiException.parseError("AI 响应为空");
        }
        try {
            Map<String, Object> root = JsonUtils.fromJson(raw, new TypeReference<>() {
            });
            return DiagnosisSummaryResult.builder()
                    .stageSummary((String) root.get("stage_summary"))
                    .keyProblems((List<Map<String, Object>>) root.getOrDefault("key_problems", List.of()))
                    .suggestions((List<Map<String, Object>>) root.getOrDefault("suggestions", List.of()))
                    .correctionDirections((List<Map<String, Object>>) root.getOrDefault("correction_directions", List.of()))
                    .build();
        } catch (Exception e) {
            throw AiException.parseError("阶段诊断解析失败: " + e.getMessage());
        }
    }
}
