package com.growthtrace.diagnosis.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Diagnosis 详情视图。
 * 对应一条 stage_assessment；aiStatus=FAILED 时 stageSummary/keyProblems/suggestions/correctionDirections 可能为空。
 */
@Data
@Builder
public class DiagnosisView {

    private Long id;
    private Long userId;

    private LocalDateTime triggerTime;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer profileVersionAtTrigger;

    /** 本地规则底座 7 指标。 */
    private Map<String, Object> metrics;

    private String stageSummary;
    private List<Map<String, Object>> keyProblems;
    private List<Map<String, Object>> suggestions;
    private List<Map<String, Object>> correctionDirections;

    private ReviewNotesView reviewNotes;

    /** SUCCESS / FALLBACK / FAILED；FAILED 时前端展示"AI 总结失败，可重新触发"。 */
    private String aiStatus;

    private Long snapshotId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
