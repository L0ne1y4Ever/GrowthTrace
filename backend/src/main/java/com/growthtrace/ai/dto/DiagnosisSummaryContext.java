package com.growthtrace.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 阶段诊断交给 AI 的上下文：规则指标 + 画像 + 目标 + 最近归档事件 + 任务完成情况。 */
@Data
@Builder
public class DiagnosisSummaryContext {

    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;

    private Map<String, Object> metrics;                      // 7 指标 JSON
    private Map<String, Object> profileSummary;
    private List<Map<String, Object>> targets;                // 当前 ACTIVE targets + requirements
    private List<Map<String, Object>> recentExtractions;      // 最近 N 条 CONFIRMED 抽取事件
    private Map<String, Object> taskSnapshot;                 // 任务完成率摘要
}
