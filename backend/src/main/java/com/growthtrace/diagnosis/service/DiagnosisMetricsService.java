package com.growthtrace.diagnosis.service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Diagnosis 本地规则底座：7 类指标计算。
 * 必须在任何 AI 总结调用前完成，AI 不参与这里的计算。
 */
public interface DiagnosisMetricsService {

    /**
     * 计算指定时间窗口的 7 类指标，返回可直接序列化为 JSON 的 map。
     * 指标项：journal_count / journal_streak / task_completion_rate /
     *       new_skills_count / profile_completeness / target_requirement_progress / activity_intensity
     */
    Map<String, Object> computeMetrics(Long userId, LocalDateTime windowStart, LocalDateTime windowEnd);
}
