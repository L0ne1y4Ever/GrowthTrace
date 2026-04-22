package com.growthtrace.profile.service;

import com.growthtrace.ai.dto.ProfileExtractResult;
import com.growthtrace.profile.dto.ExperiencePayload;
import com.growthtrace.profile.dto.OnboardingConfirmRequest;
import com.growthtrace.profile.dto.SkillPayload;
import com.growthtrace.profile.vo.ExperienceView;
import com.growthtrace.profile.vo.ProfileView;
import com.growthtrace.profile.vo.SkillView;

/**
 * Profile 主 Service。
 * 负责建档引导的 AI 草稿 / 用户确认入档、画像读取、技能与经历的手动 CRUD。
 *
 * 约束（见 memory: project_growthtrace_ai_diagnosis.md）：
 *   - AI 调用走 AiService；本 Service 不直接调 provider；
 *   - 任何写操作后必须触发 completeness 重算（唯一口径走 ProfileCompletenessCalculator）。
 */
public interface ProfileService {

    /** 建档自由文本 → AI 生成画像草稿（未入档）。 */
    ProfileExtractResult extractDraft(String onboardingText);

    /** 用户确认画像草稿 → 写 growth_profile + profile_skill + profile_experience + 重算 completeness。 */
    void confirmOnboarding(Long userId, OnboardingConfirmRequest payload);

    /** 当前用户完整画像视图。 */
    ProfileView getCurrent(Long userId);

    /** 触发完整度重算并回写缓存列。 */
    int recalculateCompleteness(Long userId);

    SkillView addSkill(Long userId, SkillPayload payload);

    SkillView updateSkill(Long userId, Long skillId, SkillPayload payload);

    void removeSkill(Long userId, Long skillId);

    ExperienceView addExperience(Long userId, ExperiencePayload payload);

    ExperienceView updateExperience(Long userId, Long experienceId, ExperiencePayload payload);

    void removeExperience(Long userId, Long experienceId);
}
