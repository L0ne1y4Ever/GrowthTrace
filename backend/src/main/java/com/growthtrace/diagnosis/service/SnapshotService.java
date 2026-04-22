package com.growthtrace.diagnosis.service;

import com.growthtrace.diagnosis.entity.GrowthSnapshot;

import java.util.List;
import java.util.Map;

/**
 * 成长快照服务。快照在 Diagnosis 触发时自动生成（trigger_source=DIAGNOSIS），
 * 也可单独手动触发（trigger_source=MANUAL）。快照会冻结画像 + 技能 + 经历 + 目标 + 指标。
 */
public interface SnapshotService {

    /**
     * 生成一次快照并写入 growth_snapshot。
     * @param userId            当前用户
     * @param stageAssessmentId 关联的诊断 id；MANUAL 快照传 null
     * @param metrics           指标 JSON（与 stage_assessment.metrics 同源）；可为 null
     * @param triggerSource     DIAGNOSIS / MANUAL
     * @return 已写入的 snapshot（含 id）
     */
    GrowthSnapshot takeSnapshot(Long userId, Long stageAssessmentId, Map<String, Object> metrics, String triggerSource);

    /**
     * 最近 N 条快照（时间升序）用于成长曲线。
     * 在 Dashboard 模块会复用。
     */
    List<GrowthSnapshot> listRecent(Long userId, int limit);
}
