package com.growthtrace.diagnosis.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Diagnosis 历史列表摘要视图；比 DiagnosisView 精简，只含时间 + 摘要头 + 状态。
 */
@Data
@Builder
public class DiagnosisSummary {

    private Long id;
    private LocalDateTime triggerTime;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer profileVersionAtTrigger;

    /** stage_summary 的前 N 个字符；详情需请求 /diagnosis/{id}。 */
    private String stageSummaryExcerpt;

    /** 关键问题条数 / 建议条数（给列表展示用）。 */
    private int keyProblemCount;
    private int suggestionCount;

    /** SUCCESS / FALLBACK / FAILED。 */
    private String aiStatus;

    private LocalDateTime createdAt;
}
