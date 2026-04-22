package com.growthtrace.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户确认画像草稿（可能已做编辑）后提交给后端。
 * 后端据此写 growth_profile + profile_skill + profile_experience，并重算 completeness。
 */
@Data
public class OnboardingConfirmRequest {

    @NotBlank
    @Size(min = 10, max = 4000)
    private String rawText;

    /** 一句话自我介绍；为空时后端取 rawText 的前 200 字。 */
    @Size(max = 500)
    private String selfIntro;

    @Size(max = 800)
    private String summary;

    private List<@Size(max = 64) String> strengths = new ArrayList<>();
    private List<@Size(max = 64) String> weaknesses = new ArrayList<>();

    @Valid
    private List<SkillPayload> skills = new ArrayList<>();

    @Valid
    private List<ExperiencePayload> experiences = new ArrayList<>();
}
