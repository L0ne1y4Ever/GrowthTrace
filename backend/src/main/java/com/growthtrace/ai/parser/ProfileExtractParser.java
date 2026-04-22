package com.growthtrace.ai.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.ai.dto.ProfileExtractResult;
import com.growthtrace.common.exception.AiException;
import com.growthtrace.common.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProfileExtractParser {

    public ProfileExtractResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw AiException.parseError("AI 响应为空");
        }
        try {
            Map<String, Object> root = JsonUtils.fromJson(raw, new TypeReference<>() {
            });
            return ProfileExtractResult.builder()
                    .summary(asString(root.get("summary")))
                    .strengths(asStringList(root.get("strengths")))
                    .weaknesses(asStringList(root.get("weaknesses")))
                    .skills(asMapList(root.get("skills")))
                    .experiences(asMapList(root.get("experiences")))
                    .completenessHint(asInt(root.get("completeness_hint")))
                    .build();
        } catch (Exception e) {
            throw AiException.parseError("画像抽取解析失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object o) {
        if (o instanceof List<?> l) {
            return l.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object o) {
        if (o instanceof List<?> l) {
            return (List<Map<String, Object>>) l;
        }
        return List.of();
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Integer asInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
