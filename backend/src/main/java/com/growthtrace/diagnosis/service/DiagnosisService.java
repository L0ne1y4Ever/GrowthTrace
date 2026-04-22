package com.growthtrace.diagnosis.service;

import com.growthtrace.common.result.PageResult;
import com.growthtrace.diagnosis.dto.TriggerDiagnosisRequest;
import com.growthtrace.diagnosis.dto.UpdateReviewNotesRequest;
import com.growthtrace.diagnosis.vo.DiagnosisSummary;
import com.growthtrace.diagnosis.vo.DiagnosisView;

/**
 * 阶段诊断服务。诊断由用户手动触发，内部流程：
 *   1) 解析时间窗（默认 30 天）
 *   2) 通过 DiagnosisMetricsService 本地计算 7 类指标
 *   3) 组装 AI 上下文（画像 + 目标 + 最近 CONFIRMED 抽取事件 + 任务摘要）
 *   4) 调 AiService.summarizeDiagnosis（失败时写 ai_status=FAILED，仍保留 metrics）
 *   5) 写 stage_assessment
 *   6) 通过 SnapshotService 写 growth_snapshot（trigger_source=DIAGNOSIS）
 *
 * 每次触发消耗 1 次 AI 预算（允许 3 个 AI 场景之一）。
 */
public interface DiagnosisService {

    /** 触发一次诊断；返回新建的诊断视图。 */
    DiagnosisView trigger(Long userId, TriggerDiagnosisRequest payload);

    /** 获取诊断详情（含 reviewNotes）。 */
    DiagnosisView get(Long userId, Long diagnosisId);

    /** 分页返回历史列表（按 created_at DESC）。 */
    PageResult<DiagnosisSummary> listHistory(Long userId, int page, int size);

    /** 更新轻复盘（wins / learnings / nextFocus / userFreeform）。 */
    DiagnosisView updateReview(Long userId, Long diagnosisId, UpdateReviewNotesRequest payload);
}
