package com.growthtrace.ai.prompt;

import com.growthtrace.ai.dto.TaskDraftContext;
import com.growthtrace.common.util.JsonUtils;
import org.springframework.stereotype.Component;

@Component
public class TaskDraftPrompt {

    private static final String HEADER = """
            你是 GrowthTrace 的成长执行规划助手。用户正在把目标要求或诊断建议转成一个可执行任务。

            请基于上下文生成一个具体、可验收、可打卡的任务草案。严格返回 JSON：
            {
              "title": "动词开头、可执行的任务标题，不超过 40 字",
              "description": "任务说明，写清为什么做、做什么、做到什么程度，不超过 500 字",
              "priority": "HIGH|MEDIUM|LOW",
              "due_date": "YYYY-MM-DD 或 null",
              "planned_effort_minutes": 预计投入分钟数，整数,
              "acceptance_criteria": ["可判断完成的验收标准，2-4 条"],
              "check_in_plan": ["建议打卡步骤，2-4 条"],
              "evidence_suggestions": ["完成时应提交或记录的证据，1-3 条"]
            }

            规则：
            1) 不要编造用户没有的背景；
            2) 任务必须能在 1-7 天内推进，避免空泛目标；
            3) 验收标准必须可观察，例如代码提交、笔记链接、截图、练习数量、复盘文字；
            4) 如果上下文很少，也要给出保守可执行的草案；
            5) 只返回 JSON，不要 Markdown，不要解释。

            上下文（JSON）：
            """;

    public String build(TaskDraftContext context) {
        return HEADER + JsonUtils.toJson(context);
    }
}
