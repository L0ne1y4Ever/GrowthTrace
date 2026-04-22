package com.growthtrace.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class JournalExtractResult {

    private List<Map<String, Object>> newSkills;               // {name, level, category}
    private List<Map<String, Object>> relatedRequirements;     // {requirementId, newStatus, evidence}
    private List<Map<String, Object>> events;                  // {type, title, description, outcome}
    private List<String> blockers;
}
