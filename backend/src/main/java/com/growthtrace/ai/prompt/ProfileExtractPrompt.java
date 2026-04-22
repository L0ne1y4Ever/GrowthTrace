package com.growthtrace.ai.prompt;

import org.springframework.stereotype.Component;

@Component
public class ProfileExtractPrompt {

    private static final String TEMPLATE = """
            你是成长档案助手。请根据下面的用户自我介绍（可能涉及专业背景、擅长与不擅长、经历等），抽取结构化画像草稿。
            严格返回以下 JSON 结构，不要输出任何解释性文字：
            {
              "summary": "一段 60-150 字的画像总结",
              "strengths": ["..."],
              "weaknesses": ["..."],
              "skills": [{"name":"...", "level":"BEGINNER|INTERMEDIATE|ADVANCED", "category":"LANGUAGE|FRAMEWORK|TOOL|DOMAIN|SOFT"}],
              "experiences": [{"type":"INTERNSHIP|PROJECT|AWARD|COURSE|RESEARCH|OTHER", "title":"...", "role":"...", "outcome":"..."}],
              "completeness_hint": 0-100
            }

            规则：
            1) 只根据用户文本抽取，不要编造事实。
            2) 不确定的 level 填 BEGINNER。
            3) 技能 category 必须使用枚举值之一；不匹配时用 DOMAIN。
            4) completeness_hint 给出一个 0-100 整数，表示信息完整度。

            用户输入：
            """;

    public String build(String onboardingText) {
        return TEMPLATE + (onboardingText == null ? "" : onboardingText);
    }
}
