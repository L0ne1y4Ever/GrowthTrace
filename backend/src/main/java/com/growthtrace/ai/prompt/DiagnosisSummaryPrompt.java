package com.growthtrace.ai.prompt;

import com.growthtrace.ai.dto.DiagnosisSummaryContext;
import com.growthtrace.common.util.JsonUtils;
import org.springframework.stereotype.Component;

@Component
public class DiagnosisSummaryPrompt {

    private static final String HEADER = """
            你是成长诊断助手。下面是用户近期的结构化上下文：
            - metrics 是系统本地计算的 7 类指标（journal_count / journal_streak / task_completion_rate /
              new_skills_count / profile_completeness / target_requirement_progress / activity_intensity）；
            - profileSummary 是当前画像；
            - targets 是用户当前 ACTIVE 目标与其 requirements；
            - recentExtractions 是最近 N 条已确认的随记抽取事件；
            - taskSnapshot 是任务完成情况。

            请基于上述信息，生成用户能读懂的阶段总结。严格返回 JSON：
            {
              "stage_summary": "一段 150-300 字的总结，客观、具体",
              "key_problems": [{"title":"...", "description":"..."}],
              "suggestions": [{"title":"...", "detail":"...", "priority":"HIGH|MEDIUM|LOW"}],
              "correction_directions": [{"direction":"...", "rationale":"..."}]
            }

            规则：
            1) 必须基于 metrics 的实际数值做判断，例如 task_completion_rate 低于 0.3 时应该点出问题；
            2) 不要编造用户没有的经历；
            3) suggestions 数量 2-5 条，不要罗列过多；
            4) correction_directions 至少 1 条、不超过 3 条。

            上下文（JSON）：
            """;

    public String build(DiagnosisSummaryContext context) {
        return HEADER + JsonUtils.toJson(context);
    }
}
