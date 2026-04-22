package com.growthtrace.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DiagnosisSummaryResult {

    private String stageSummary;
    private List<Map<String, Object>> keyProblems;             // {title, description}
    private List<Map<String, Object>> suggestions;             // {title, detail, priority}
    private List<Map<String, Object>> correctionDirections;    // {direction, rationale}
}
