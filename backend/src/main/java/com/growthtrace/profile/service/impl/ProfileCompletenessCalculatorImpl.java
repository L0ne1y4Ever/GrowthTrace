package com.growthtrace.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.profile.entity.GrowthProfile;
import com.growthtrace.profile.entity.ProfileExperience;
import com.growthtrace.profile.entity.ProfileSkill;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
import com.growthtrace.profile.mapper.ProfileExperienceMapper;
import com.growthtrace.profile.mapper.ProfileSkillMapper;
import com.growthtrace.profile.service.ProfileCompletenessCalculator;
import com.growthtrace.target.entity.GrowthTarget;
import com.growthtrace.target.entity.TargetRequirement;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileCompletenessCalculatorImpl implements ProfileCompletenessCalculator {

    private final GrowthProfileMapper profileMapper;
    private final ProfileSkillMapper skillMapper;
    private final ProfileExperienceMapper experienceMapper;
    private final GrowthTargetMapper targetMapper;
    private final TargetRequirementMapper requirementMapper;

    @Override
    public int calculate(Long userId) {
        int score = 0;
        score += selfAwareness(userId);         // 0-20
        score += strengthsWeaknesses(userId);   // 0-10
        score += skillBucket(userId);           // 0-25
        score += experienceBucket(userId);      // 0-25
        score += targetBucket(userId);          // 0-20
        return Math.min(100, Math.max(0, score));
    }

    private int selfAwareness(Long userId) {
        GrowthProfile p = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));
        if (p == null) {
            return 0;
        }
        int s = 0;
        if (StringUtils.hasText(p.getSelfIntro())) s += 10;
        if (StringUtils.hasText(p.getSummary())) s += 10;
        return s;
    }

    private int strengthsWeaknesses(Long userId) {
        GrowthProfile p = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));
        if (p == null) {
            return 0;
        }
        int s = 0;
        if (nonEmptyJsonArray(p.getStrengths())) s += 5;
        if (nonEmptyJsonArray(p.getWeaknesses())) s += 5;
        return s;
    }

    private int skillBucket(Long userId) {
        long n = skillMapper.selectCount(new LambdaQueryWrapper<ProfileSkill>()
                .eq(ProfileSkill::getUserId, userId)
                .eq(ProfileSkill::getStatus, "ACTIVE"));
        if (n >= 5) return 25;
        if (n >= 3) return 20;
        if (n >= 1) return 10;
        return 0;
    }

    private int experienceBucket(Long userId) {
        long n = experienceMapper.selectCount(new LambdaQueryWrapper<ProfileExperience>()
                .eq(ProfileExperience::getUserId, userId));
        if (n >= 3) return 25;
        if (n >= 2) return 20;
        if (n >= 1) return 10;
        return 0;
    }

    private int targetBucket(Long userId) {
        List<GrowthTarget> activeTargets = targetMapper.selectList(new LambdaQueryWrapper<GrowthTarget>()
                .eq(GrowthTarget::getUserId, userId)
                .eq(GrowthTarget::getStatus, "ACTIVE"));
        if (activeTargets.isEmpty()) {
            return 0;
        }
        int s = 10;
        List<Long> targetIds = activeTargets.stream().map(GrowthTarget::getId).toList();
        long reqCount = requirementMapper.selectCount(new LambdaQueryWrapper<TargetRequirement>()
                .in(TargetRequirement::getTargetId, targetIds));
        if (reqCount >= 3) s += 10;
        else if (reqCount >= 1) s += 5;
        return s;
    }

    private boolean nonEmptyJsonArray(String json) {
        if (!StringUtils.hasText(json)) return false;
        try {
            List<Object> list = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return list != null && !list.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }
}
