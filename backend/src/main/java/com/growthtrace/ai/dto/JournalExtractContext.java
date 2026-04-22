package com.growthtrace.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 随记事件抽取时传给 AI 的结构化上下文（附在用户随记原文之外）。 */
@Data
@Builder
public class JournalExtractContext {

    private String journalContent;
    private Map<String, Object> currentProfileSummary;           // 当前画像摘要（供 AI 判断 new vs 已有技能）
    private List<Map<String, Object>> activeRequirements;        // 当前目标的 ACTIVE requirement 列表
}
