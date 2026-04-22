package com.growthtrace.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.ai.dto.ProfileExtractResult;
import com.growthtrace.ai.service.AiService;
import com.growthtrace.common.exception.BusinessException;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.profile.dto.ExperiencePayload;
import com.growthtrace.profile.dto.OnboardingConfirmRequest;
import com.growthtrace.profile.dto.SkillPayload;
import com.growthtrace.profile.entity.GrowthProfile;
import com.growthtrace.profile.entity.ProfileExperience;
import com.growthtrace.profile.entity.ProfileSkill;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
import com.growthtrace.profile.mapper.ProfileExperienceMapper;
import com.growthtrace.profile.mapper.ProfileSkillMapper;
import com.growthtrace.profile.service.ProfileCompletenessCalculator;
import com.growthtrace.profile.service.ProfileService;
import com.growthtrace.profile.vo.ExperienceView;
import com.growthtrace.profile.vo.ProfileView;
import com.growthtrace.profile.vo.SkillView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final int SELF_INTRO_FALLBACK_LEN = 200;

    private final AiService aiService;
    private final GrowthProfileMapper profileMapper;
    private final ProfileSkillMapper skillMapper;
    private final ProfileExperienceMapper experienceMapper;
    private final ProfileCompletenessCalculator completenessCalculator;

    // ------------------------------------------------------------------
    // AI 草稿 + 确认入档
    // ------------------------------------------------------------------

    @Override
    public ProfileExtractResult extractDraft(String onboardingText) {
        return aiService.extractProfile(onboardingText);
    }

    @Override
    @Transactional
    public void confirmOnboarding(Long userId, OnboardingConfirmRequest payload) {
        String rawText = payload.getRawText() == null ? "" : payload.getRawText().trim();
        String selfIntro = StringUtils.hasText(payload.getSelfIntro())
                ? payload.getSelfIntro().trim()
                : truncate(rawText, SELF_INTRO_FALLBACK_LEN);

        List<String> strengths = payload.getStrengths() == null ? List.of() : payload.getStrengths();
        List<String> weaknesses = payload.getWeaknesses() == null ? List.of() : payload.getWeaknesses();

        GrowthProfile existing = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));

        int newVersion;
        if (existing == null) {
            GrowthProfile created = new GrowthProfile();
            created.setUserId(userId);
            created.setVersion(1);
            created.setSelfIntro(selfIntro);
            created.setSummary(payload.getSummary());
            created.setStrengths(JsonUtils.toJson(strengths));
            created.setWeaknesses(JsonUtils.toJson(weaknesses));
            created.setRawOnboardingText(rawText);
            created.setSource("ONBOARDING");
            created.setCompleteness(0);
            profileMapper.insert(created);
            newVersion = 1;
        } else {
            newVersion = (existing.getVersion() == null ? 0 : existing.getVersion()) + 1;
            existing.setVersion(newVersion);
            existing.setSelfIntro(selfIntro);
            existing.setSummary(payload.getSummary());
            existing.setStrengths(JsonUtils.toJson(strengths));
            existing.setWeaknesses(JsonUtils.toJson(weaknesses));
            existing.setRawOnboardingText(rawText);
            existing.setSource("ONBOARDING");
            profileMapper.updateById(existing);
        }

        upsertSkillsFromOnboarding(userId, newVersion, payload.getSkills());
        appendExperiencesFromOnboarding(userId, newVersion, payload.getExperiences());

        recalculateCompleteness(userId);
        log.info("onboarding confirmed: userId={}, newVersion={}, skills={}, experiences={}",
                userId, newVersion,
                payload.getSkills() == null ? 0 : payload.getSkills().size(),
                payload.getExperiences() == null ? 0 : payload.getExperiences().size());
    }

    // ------------------------------------------------------------------
    // 画像读取
    // ------------------------------------------------------------------

    @Override
    public ProfileView getCurrent(Long userId) {
        GrowthProfile p = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "画像尚未建档，请先完成建档引导");
        }

        List<ProfileSkill> skills = skillMapper.selectList(new LambdaQueryWrapper<ProfileSkill>()
                .eq(ProfileSkill::getUserId, userId)
                .orderByAsc(ProfileSkill::getStatus)
                .orderByDesc(ProfileSkill::getUpdatedAt));

        List<ProfileExperience> experiences = experienceMapper.selectList(new LambdaQueryWrapper<ProfileExperience>()
                .eq(ProfileExperience::getUserId, userId)
                .orderByDesc(ProfileExperience::getStartDate)
                .orderByDesc(ProfileExperience::getCreatedAt));

        return ProfileView.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .version(p.getVersion())
                .selfIntro(p.getSelfIntro())
                .strengths(parseStringList(p.getStrengths()))
                .weaknesses(parseStringList(p.getWeaknesses()))
                .summary(p.getSummary())
                .completeness(p.getCompleteness())
                .source(p.getSource())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .skills(skills.stream().map(ProfileServiceImpl::toSkillView).toList())
                .experiences(experiences.stream().map(ProfileServiceImpl::toExperienceView).toList())
                .build();
    }

    @Override
    public int recalculateCompleteness(Long userId) {
        int c = completenessCalculator.calculate(userId);
        profileMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<GrowthProfile>()
                        .eq(GrowthProfile::getUserId, userId)
                        .set(GrowthProfile::getCompleteness, c));
        return c;
    }

    // ------------------------------------------------------------------
    // Skill CRUD
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public SkillView addSkill(Long userId, SkillPayload payload) {
        String name = requireTrimmed(payload.getSkillName(), "skillName 不能为空");
        boolean dup = skillMapper.selectCount(new LambdaQueryWrapper<ProfileSkill>()
                .eq(ProfileSkill::getUserId, userId)
                .eq(ProfileSkill::getSkillName, name)) > 0;
        if (dup) {
            throw new BusinessException(ResultCode.CONFLICT, "技能已存在：" + name);
        }

        ProfileSkill s = new ProfileSkill();
        s.setUserId(userId);
        s.setSkillName(name);
        s.setSkillLevel(payload.getSkillLevel());
        s.setCategory(resolveCategory(payload.getCategory()));
        s.setAddedInProfileVersion(currentProfileVersion(userId));
        s.setSource("MANUAL");
        s.setStatus("ACTIVE");
        s.setEvidence(payload.getEvidence());
        skillMapper.insert(s);
        recalculateCompleteness(userId);
        return toSkillView(skillMapper.selectById(s.getId()));
    }

    @Override
    @Transactional
    public SkillView updateSkill(Long userId, Long skillId, SkillPayload payload) {
        ProfileSkill s = requireOwnedSkill(userId, skillId);
        String name = requireTrimmed(payload.getSkillName(), "skillName 不能为空");
        if (!name.equals(s.getSkillName())) {
            boolean dup = skillMapper.selectCount(new LambdaQueryWrapper<ProfileSkill>()
                    .eq(ProfileSkill::getUserId, userId)
                    .eq(ProfileSkill::getSkillName, name)
                    .ne(ProfileSkill::getId, skillId)) > 0;
            if (dup) {
                throw new BusinessException(ResultCode.CONFLICT, "技能已存在：" + name);
            }
            s.setSkillName(name);
        }
        s.setSkillLevel(payload.getSkillLevel());
        if (StringUtils.hasText(payload.getCategory())) {
            s.setCategory(payload.getCategory());
        }
        s.setEvidence(payload.getEvidence());
        skillMapper.updateById(s);
        recalculateCompleteness(userId);
        return toSkillView(skillMapper.selectById(s.getId()));
    }

    @Override
    @Transactional
    public void removeSkill(Long userId, Long skillId) {
        requireOwnedSkill(userId, skillId);
        skillMapper.deleteById(skillId);
        recalculateCompleteness(userId);
    }

    // ------------------------------------------------------------------
    // Experience CRUD
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ExperienceView addExperience(Long userId, ExperiencePayload payload) {
        ProfileExperience e = new ProfileExperience();
        e.setUserId(userId);
        e.setExpType(payload.getExpType());
        e.setTitle(requireTrimmed(payload.getTitle(), "title 不能为空"));
        e.setRole(payload.getRole());
        e.setDescription(payload.getDescription());
        e.setOutcome(payload.getOutcome());
        e.setStartDate(payload.getStartDate());
        e.setEndDate(payload.getEndDate());
        e.setAddedInProfileVersion(currentProfileVersion(userId));
        e.setSource("MANUAL");
        experienceMapper.insert(e);
        recalculateCompleteness(userId);
        return toExperienceView(experienceMapper.selectById(e.getId()));
    }

    @Override
    @Transactional
    public ExperienceView updateExperience(Long userId, Long experienceId, ExperiencePayload payload) {
        ProfileExperience e = requireOwnedExperience(userId, experienceId);
        e.setExpType(payload.getExpType());
        e.setTitle(requireTrimmed(payload.getTitle(), "title 不能为空"));
        e.setRole(payload.getRole());
        e.setDescription(payload.getDescription());
        e.setOutcome(payload.getOutcome());
        e.setStartDate(payload.getStartDate());
        e.setEndDate(payload.getEndDate());
        experienceMapper.updateById(e);
        recalculateCompleteness(userId);
        return toExperienceView(experienceMapper.selectById(e.getId()));
    }

    @Override
    @Transactional
    public void removeExperience(Long userId, Long experienceId) {
        requireOwnedExperience(userId, experienceId);
        experienceMapper.deleteById(experienceId);
        recalculateCompleteness(userId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void upsertSkillsFromOnboarding(Long userId, int version, List<SkillPayload> skills) {
        if (skills == null || skills.isEmpty()) {
            return;
        }
        for (SkillPayload sp : skills) {
            if (sp == null || !StringUtils.hasText(sp.getSkillName())) {
                continue;
            }
            String name = sp.getSkillName().trim();
            ProfileSkill exists = skillMapper.selectOne(new LambdaQueryWrapper<ProfileSkill>()
                    .eq(ProfileSkill::getUserId, userId)
                    .eq(ProfileSkill::getSkillName, name));
            if (exists == null) {
                ProfileSkill s = new ProfileSkill();
                s.setUserId(userId);
                s.setSkillName(name);
                s.setSkillLevel(resolveLevel(sp.getSkillLevel()));
                s.setCategory(resolveCategory(sp.getCategory()));
                s.setAddedInProfileVersion(version);
                s.setSource("ONBOARDING");
                s.setStatus("ACTIVE");
                s.setEvidence(sp.getEvidence());
                skillMapper.insert(s);
            } else {
                exists.setSkillLevel(resolveLevel(sp.getSkillLevel()));
                if (StringUtils.hasText(sp.getCategory())) {
                    exists.setCategory(sp.getCategory());
                }
                if (StringUtils.hasText(sp.getEvidence())) {
                    exists.setEvidence(sp.getEvidence());
                }
                exists.setStatus("ACTIVE");
                skillMapper.updateById(exists);
            }
        }
    }

    private void appendExperiencesFromOnboarding(Long userId, int version, List<ExperiencePayload> experiences) {
        if (experiences == null || experiences.isEmpty()) {
            return;
        }
        for (ExperiencePayload ep : experiences) {
            if (ep == null || !StringUtils.hasText(ep.getTitle())) {
                continue;
            }
            String title = ep.getTitle().trim();
            boolean exists = experienceMapper.selectCount(new LambdaQueryWrapper<ProfileExperience>()
                    .eq(ProfileExperience::getUserId, userId)
                    .eq(ProfileExperience::getTitle, title)) > 0;
            if (exists) {
                continue;
            }
            ProfileExperience e = new ProfileExperience();
            e.setUserId(userId);
            e.setExpType(ep.getExpType());
            e.setTitle(title);
            e.setRole(ep.getRole());
            e.setDescription(ep.getDescription());
            e.setOutcome(ep.getOutcome());
            e.setStartDate(ep.getStartDate());
            e.setEndDate(ep.getEndDate());
            e.setAddedInProfileVersion(version);
            e.setSource("ONBOARDING");
            experienceMapper.insert(e);
        }
    }

    private int currentProfileVersion(Long userId) {
        GrowthProfile p = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));
        return p == null || p.getVersion() == null ? 1 : p.getVersion();
    }

    private ProfileSkill requireOwnedSkill(Long userId, Long skillId) {
        ProfileSkill s = skillMapper.selectById(skillId);
        if (s == null || !userId.equals(s.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能不存在或不属于当前用户");
        }
        return s;
    }

    private ProfileExperience requireOwnedExperience(Long userId, Long experienceId) {
        ProfileExperience e = experienceMapper.selectById(experienceId);
        if (e == null || !userId.equals(e.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "经历不存在或不属于当前用户");
        }
        return e;
    }

    private static String resolveCategory(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "DOMAIN";
        }
        return switch (raw.toUpperCase()) {
            case "LANGUAGE", "FRAMEWORK", "TOOL", "DOMAIN", "SOFT" -> raw.toUpperCase();
            default -> "DOMAIN";
        };
    }

    private static String resolveLevel(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "BEGINNER";
        }
        return switch (raw.toUpperCase()) {
            case "BEGINNER", "INTERMEDIATE", "ADVANCED" -> raw.toUpperCase();
            default -> "BEGINNER";
        };
    }

    private static String requireTrimmed(String raw, String errIfBlank) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, errIfBlank);
        }
        return raw.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static SkillView toSkillView(ProfileSkill s) {
        return SkillView.builder()
                .id(s.getId())
                .skillName(s.getSkillName())
                .skillLevel(s.getSkillLevel())
                .category(s.getCategory())
                .addedInProfileVersion(s.getAddedInProfileVersion())
                .source(s.getSource())
                .status(s.getStatus())
                .evidence(s.getEvidence())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private static ExperienceView toExperienceView(ProfileExperience e) {
        return ExperienceView.builder()
                .id(e.getId())
                .expType(e.getExpType())
                .title(e.getTitle())
                .description(e.getDescription())
                .role(e.getRole())
                .outcome(e.getOutcome())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .addedInProfileVersion(e.getAddedInProfileVersion())
                .source(e.getSource())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
