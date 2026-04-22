package com.growthtrace.profile.service;

/**
 * 画像完整度统一口径。系统内任何需要 profile_completeness 的地方都必须调用本计算器，
 * 禁止在 Dashboard / Diagnosis / Profile 各处自行实现。
 *
 * 总分 100，见 memory: project_growthtrace_ai_diagnosis.md
 */
public interface ProfileCompletenessCalculator {

    /** 实时计算用户画像完整度（0-100），不读缓存列。 */
    int calculate(Long userId);
}
