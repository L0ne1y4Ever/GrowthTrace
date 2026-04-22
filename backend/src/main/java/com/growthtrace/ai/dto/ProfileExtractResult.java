package com.growthtrace.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProfileExtractResult {

    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<Map<String, Object>> skills;       // {name, level, category}
    private List<Map<String, Object>> experiences;  // {type, title, role, outcome, startDate, endDate}
    private Integer completenessHint;
}
