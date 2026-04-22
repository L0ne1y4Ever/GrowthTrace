package com.growthtrace.diagnosis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.diagnosis.entity.GrowthSnapshot;
import com.growthtrace.diagnosis.mapper.GrowthSnapshotMapper;
import com.growthtrace.diagnosis.service.SnapshotService;
import com.growthtrace.profile.entity.GrowthProfile;
import com.growthtrace.profile.entity.ProfileExperience;
import com.growthtrace.profile.entity.ProfileSkill;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
import com.growthtrace.profile.mapper.ProfileExperienceMapper;
import com.growthtrace.profile.mapper.ProfileSkillMapper;
import com.growthtrace.target.entity.GrowthTarget;
import com.growthtrace.target.entity.TargetRequirement;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotServiceImpl implements SnapshotService {

    private final GrowthSnapshotMapper snapshotMapper;
    private final GrowthProfileMapper profileMapper;
    private final ProfileSkillMapper skillMapper;
    private final ProfileExperienceMapper experienceMapper;
    private final GrowthTargetMapper targetMapper;
    private final TargetRequirementMapper requirementMapper;

    @Override
    public GrowthSnapshot takeSnapshot(Long userId, Long stageAssessmentId,
                                       Map<String, Object> metrics, String triggerSource) {
        GrowthProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));
        // 允许画像不存在（极端情况：系统被直接通过 API 触发而未建档）；此时写入空 profileJson
        Map<String, Object> profileJson = new HashMap<>();
        int version = 0;
        if (profile != null) {
            profileJson.put("id", profile.getId());
            profileJson.put("version", profile.getVersion());
            profileJson.put("selfIntro", profile.getSelfIntro());
            profileJson.put("summary", profile.getSummary());
            profileJson.put("strengths", profile.getStrengths());
            profileJson.put("weaknesses", profile.getWeaknesses());
            profileJson.put("completeness", profile.getCompleteness());
            profileJson.put("source", profile.getSource());
            version = profile.getVersion() == null ? 0 : profile.getVersion();
        }

        List<ProfileSkill> skills = skillMapper.selectList(new LambdaQueryWrapper<ProfileSkill>()
                .eq(ProfileSkill::getUserId, userId));
        List<ProfileExperience> experiences = experienceMapper.selectList(new LambdaQueryWrapper<ProfileExperience>()
                .eq(ProfileExperience::getUserId, userId));
        List<GrowthTarget> targets = targetMapper.selectList(new LambdaQueryWrapper<GrowthTarget>()
                .eq(GrowthTarget::getUserId, userId));
        List<Long> targetIds = targets.stream().map(GrowthTarget::getId).toList();
        List<TargetRequirement> requirements = targetIds.isEmpty()
                ? Collections.emptyList()
                : requirementMapper.selectList(new LambdaQueryWrapper<TargetRequirement>()
                        .in(TargetRequirement::getTargetId, targetIds));

        Map<String, Object> targetsSnapshot = new HashMap<>();
        targetsSnapshot.put("targets", targets);
        targetsSnapshot.put("requirements", requirements);

        GrowthSnapshot row = new GrowthSnapshot();
        row.setUserId(userId);
        row.setProfileVersion(version);
        row.setSnapshotTime(LocalDateTime.now());
        row.setProfileJson(JsonUtils.toJson(profileJson));
        row.setSkillsSnapshot(JsonUtils.toJson(skills));
        row.setExperiencesSnapshot(JsonUtils.toJson(experiences));
        row.setTargetsSnapshot(JsonUtils.toJson(targetsSnapshot));
        row.setMetricsSnapshot(metrics == null ? null : JsonUtils.toJson(metrics));
        row.setTriggerSource(triggerSource == null ? "DIAGNOSIS" : triggerSource);
        row.setStageAssessmentId(stageAssessmentId);
        snapshotMapper.insert(row);
        log.info("snapshot created: userId={}, snapshotId={}, stageAssessmentId={}, source={}",
                userId, row.getId(), stageAssessmentId, row.getTriggerSource());
        return row;
    }

    @Override
    public List<GrowthSnapshot> listRecent(Long userId, int limit) {
        int safe = Math.max(1, Math.min(limit, 100));
        LambdaQueryWrapper<GrowthSnapshot> q = new LambdaQueryWrapper<GrowthSnapshot>()
                .eq(GrowthSnapshot::getUserId, userId)
                .orderByDesc(GrowthSnapshot::getSnapshotTime);
        Page<GrowthSnapshot> page = snapshotMapper.selectPage(new Page<>(1, safe), q);
        // 翻转为升序，方便曲线绘制
        List<GrowthSnapshot> records = page.getRecords();
        Collections.reverse(records);
        return records;
    }
}
