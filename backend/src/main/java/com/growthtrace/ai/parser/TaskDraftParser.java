package com.growthtrace.ai.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.ai.dto.TaskDraftResult;
import com.growthtrace.common.exception.AiException;
import com.growthtrace.common.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class TaskDraftParser {

    public TaskDraftResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw AiException.parseError("AI 响应为空");
        }
        try {
            Map<String, Object> root = JsonUtils.fromJson(raw, new TypeReference<>() {
            });
            return TaskDraftResult.builder()
                    .title(asString(root.get("title")))
                    .description(asString(root.get("description")))
                    .priority(normalizePriority(asString(root.get("priority"))))
                    .dueDate(asDate(root.get("due_date")))
                    .plannedEffortMinutes(asInt(root.get("planned_effort_minutes")))
                    .acceptanceCriteria(asStringList(root.get("acceptance_criteria")))
                    .checkInPlan(asStringList(root.get("check_in_plan")))
                    .evidenceSuggestions(asStringList(root.get("evidence_suggestions")))
                    .build();
        } catch (Exception e) {
            throw AiException.parseError("任务草案解析失败: " + e.getMessage());
        }
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private List<String> asStringList(Object o) {
        if (o instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return List.of();
    }

    private Integer asInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private LocalDate asDate(Object o) {
        String s = asString(o);
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s.trim())) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizePriority(String raw) {
        if (raw == null) return "MEDIUM";
        return switch (raw.trim().toUpperCase()) {
            case "HIGH", "LOW" -> raw.trim().toUpperCase();
            default -> "MEDIUM";
        };
    }
}
