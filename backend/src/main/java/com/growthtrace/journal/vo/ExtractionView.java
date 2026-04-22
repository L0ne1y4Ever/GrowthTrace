package com.growthtrace.journal.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * journal_extraction 的视图。
 * draft_* 是 AI 原始产出（用户不直接改）；
 * confirmed_* 是用户确认时的最终值；
 * status 未 CONFIRMED 时 confirmed_* 字段可能为 null。
 */
@Data
@Builder
public class ExtractionView {

    private Long id;
    private Long journalId;
    private String extractionStatus;

    private List<Map<String, Object>> draftNewSkills;
    private List<Map<String, Object>> draftRelatedRequirements;
    private List<Map<String, Object>> draftEvents;
    private List<String> draftBlockers;

    private List<Map<String, Object>> confirmedNewSkills;
    private List<Map<String, Object>> confirmedRelatedRequirements;
    private List<Map<String, Object>> confirmedEvents;
    private List<String> confirmedBlockers;

    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
