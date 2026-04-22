package com.growthtrace.ai.prompt;

import com.growthtrace.ai.dto.JournalExtractContext;
import com.growthtrace.common.util.JsonUtils;
import org.springframework.stereotype.Component;

@Component
public class JournalExtractPrompt {

    private static final String HEADER = """
            你是成长随记分析助手。请从用户的一段自然语言随记中抽取结构化成长事件。
            返回 JSON：
            {
              "new_skills": [{"name":"...", "level":"BEGINNER|INTERMEDIATE|ADVANCED", "category":"..."}],
              "related_requirements": [{"requirement_id": 数字, "new_status":"TODO|IN_PROGRESS|MET", "evidence":"..."}],
              "events": [{"type":"PROJECT|COURSE|RESEARCH|OTHER", "title":"...", "description":"...", "outcome":"..."}],
              "blockers": ["..."]
            }

            规则：
            1) new_skills 只包含"画像中尚未出现"的新技能；已在画像里的不要再抽出来。
            2) related_requirements.requirement_id 必须来自上下文里列出的 activeRequirements；如果该要求状态发生变化，给出 new_status 与 evidence。
            3) 禁止编造任何 requirement_id。
            4) 无匹配则对应字段返回空数组 []。

            上下文（JSON）：
            """;

    public String build(JournalExtractContext context) {
        StringBuilder sb = new StringBuilder(HEADER);
        sb.append(JsonUtils.toJson(context)).append("\n\n用户随记原文：\n");
        sb.append(context == null || context.getJournalContent() == null ? "" : context.getJournalContent());
        return sb.toString();
    }
}
