package com.growthtrace.dashboard.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard 最近诊断洞察摘要；可能为空（用户未触发过任何诊断）。
 */
@Data
@Builder
public class LatestDiagnosisDigest {

    private Long id;
    private LocalDateTime triggerTime;
    private String stageSummaryExcerpt;

    /** 最多 3 条建议的标题。 */
    private List<String> topSuggestions;

    /** 最多 3 条纠偏方向。 */
    private List<String> topCorrections;

    /** SUCCESS / FALLBACK / FAILED。 */
    private String aiStatus;
}
