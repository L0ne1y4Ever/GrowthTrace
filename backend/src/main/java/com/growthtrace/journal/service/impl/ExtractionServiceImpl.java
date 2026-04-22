package com.growthtrace.journal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.growthtrace.ai.dto.JournalExtractContext;
import com.growthtrace.ai.dto.JournalExtractResult;
import com.growthtrace.ai.service.AiService;
import com.growthtrace.common.exception.BusinessException;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.journal.dto.ConfirmExtractionRequest;
import com.growthtrace.journal.dto.NewSkillConfirm;
import com.growthtrace.journal.dto.RequirementUpdateConfirm;
import com.growthtrace.journal.entity.GrowthJournal;
import com.growthtrace.journal.entity.JournalExtraction;
import com.growthtrace.journal.mapper.GrowthJournalMapper;
import com.growthtrace.journal.mapper.JournalExtractionMapper;
import com.growthtrace.journal.service.ExtractionService;
import com.growthtrace.journal.vo.ExtractionView;
import com.growthtrace.profile.entity.GrowthProfile;
import com.growthtrace.profile.entity.ProfileSkill;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
import com.growthtrace.profile.mapper.ProfileSkillMapper;
import com.growthtrace.profile.service.ProfileCompletenessCalculator;
import com.growthtrace.target.entity.GrowthTarget;
import com.growthtrace.target.entity.TargetRequirement;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionServiceImpl implements ExtractionService {

    private final AiService aiService;

    private final GrowthJournalMapper journalMapper;
    private final JournalExtractionMapper extractionMapper;

    private final GrowthProfileMapper profileMapper;
    private final ProfileSkillMapper skillMapper;
    private final ProfileCompletenessCalculator completenessCalculator;

    private final GrowthTargetMapper targetMapper;
    private final TargetRequirementMapper requirementMapper;

    // ---------------------------------------------------
    // 抽取草稿
    // ---------------------------------------------------

    @Override
    @Transactional
    public ExtractionView extractDraft(Long userId, Long journalId) {
        GrowthJournal journal = requireOwnedJournal(userId, journalId);
        JournalExtraction existing = extractionMapper.selectOne(new LambdaQueryWrapper<JournalExtraction>()
                .eq(JournalExtraction::getJournalId, journalId));
        if (existing != null && "CONFIRMED".equals(existing.getExtractionStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "该随记已确认入档，不能重新抽取；如需重做，先编辑随记再试");
        }

        JournalExtractContext context = buildContext(userId, journal.getContent());
        JournalExtractResult result = aiService.extractJournal(context);

        String rawJson = JsonUtils.toJson(Map.of(
                "new_skills", defaultList(result.getNewSkills()),
                "related_requirements", defaultList(result.getRelatedRequirements()),
                "events", defaultList(result.getEvents()),
                "blockers", defaultList(result.getBlockers())
        ));

        if (existing == null) {
            JournalExtraction row = new JournalExtraction();
            row.setJournalId(journalId);
            row.setUserId(userId);
            row.setExtractionStatus("PENDING_CONFIRM");
            row.setAiRawResponse(rawJson);
            row.setDraftNewSkills(JsonUtils.toJson(defaultList(result.getNewSkills())));
            row.setDraftRelatedRequirements(JsonUtils.toJson(defaultList(result.getRelatedRequirements())));
            row.setDraftEvents(JsonUtils.toJson(defaultList(result.getEvents())));
            row.setDraftBlockers(JsonUtils.toJson(defaultList(result.getBlockers())));
            extractionMapper.insert(row);
            log.info("extraction created: journalId={}, status=PENDING_CONFIRM", journalId);
            return JournalServiceImpl.toExtractionView(row);
        }

        existing.setExtractionStatus("PENDING_CONFIRM");
        existing.setAiRawResponse(rawJson);
        existing.setDraftNewSkills(JsonUtils.toJson(defaultList(result.getNewSkills())));
        existing.setDraftRelatedRequirements(JsonUtils.toJson(defaultList(result.getRelatedRequirements())));
        existing.setDraftEvents(JsonUtils.toJson(defaultList(result.getEvents())));
        existing.setDraftBlockers(JsonUtils.toJson(defaultList(result.getBlockers())));
        extractionMapper.updateById(existing);
        log.info("extraction re-extracted: journalId={}", journalId);
        return JournalServiceImpl.toExtractionView(existing);
    }

    // ---------------------------------------------------
    // 确认草稿 → 落地
    // ---------------------------------------------------

    @Override
    @Transactional
    public ExtractionView confirm(Long userId, Long journalId, ConfirmExtractionRequest payload) {
        requireOwnedJournal(userId, journalId);
        JournalExtraction ex = extractionMapper.selectOne(new LambdaQueryWrapper<JournalExtraction>()
                .eq(JournalExtraction::getJournalId, journalId));
        if (ex == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该随记尚未生成抽取草稿");
        }

        int profileVersion = currentProfileVersion(userId);

        // 1) 落地新技能（source=JOURNAL）
        if (payload.getNewSkills() != null) {
            for (NewSkillConfirm ns : payload.getNewSkills()) {
                if (ns == null || !StringUtils.hasText(ns.getName())) continue;
                String name = ns.getName().trim();
                ProfileSkill exists = skillMapper.selectOne(new LambdaQueryWrapper<ProfileSkill>()
                        .eq(ProfileSkill::getUserId, userId)
                        .eq(ProfileSkill::getSkillName, name));
                if (exists == null) {
                    ProfileSkill s = new ProfileSkill();
                    s.setUserId(userId);
                    s.setSkillName(name);
                    s.setSkillLevel(ns.getLevel());
                    s.setCategory(StringUtils.hasText(ns.getCategory()) ? ns.getCategory() : "DOMAIN");
                    s.setAddedInProfileVersion(profileVersion);
                    s.setSource("JOURNAL");
                    s.setStatus("ACTIVE");
                    s.setEvidence(ns.getEvidence());
                    skillMapper.insert(s);
                } else {
                    // 已存在：不覆盖 level，仅补 evidence（保留用户主动维护的数据）
                    if (StringUtils.hasText(ns.getEvidence()) && !StringUtils.hasText(exists.getEvidence())) {
                        exists.setEvidence(ns.getEvidence());
                        skillMapper.updateById(exists);
                    }
                }
            }
        }

        // 2) 更新 requirement.status（需 ownership 校验）
        if (payload.getRelatedRequirements() != null) {
            for (RequirementUpdateConfirm ru : payload.getRelatedRequirements()) {
                if (ru == null || ru.getRequirementId() == null || !StringUtils.hasText(ru.getNewStatus())) continue;
                TargetRequirement req = requirementMapper.selectById(ru.getRequirementId());
                if (req == null) continue;
                GrowthTarget target = targetMapper.selectById(req.getTargetId());
                if (target == null || !userId.equals(target.getUserId())) {
                    // 防止越权；忽略非本人要求
                    continue;
                }
                String newStatus = ru.getNewStatus();
                req.setStatus(newStatus);
                req.setProgress(progressFromStatus(newStatus, req.getProgress()));
                requirementMapper.updateById(req);
            }
        }

        // 3) 写 confirmed_* 并置 CONFIRMED
        ex.setExtractionStatus("CONFIRMED");
        ex.setConfirmedNewSkills(JsonUtils.toJson(defaultList(payload.getNewSkills())));
        ex.setConfirmedRelatedRequirements(JsonUtils.toJson(defaultList(payload.getRelatedRequirements())));
        ex.setConfirmedEvents(JsonUtils.toJson(defaultList(payload.getEvents())));
        ex.setConfirmedBlockers(JsonUtils.toJson(defaultList(payload.getBlockers())));
        ex.setConfirmedAt(LocalDateTime.now());
        extractionMapper.updateById(ex);

        // 4) 新技能可能改变 completeness
        int c = completenessCalculator.calculate(userId);
        profileMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<GrowthProfile>()
                        .eq(GrowthProfile::getUserId, userId)
                        .set(GrowthProfile::getCompleteness, c));

        log.info("extraction confirmed: journalId={}, newCompleteness={}", journalId, c);
        return JournalServiceImpl.toExtractionView(ex);
    }

    // ---------------------------------------------------
    // helpers
    // ---------------------------------------------------

    private JournalExtractContext buildContext(Long userId, String journalContent) {
        GrowthProfile p = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));

        Map<String, Object> profileSummary = new LinkedHashMap<>();
        if (p != null) {
            profileSummary.put("selfIntro", p.getSelfIntro());
            profileSummary.put("summary", p.getSummary());
            List<ProfileSkill> skills = skillMapper.selectList(new LambdaQueryWrapper<ProfileSkill>()
                    .eq(ProfileSkill::getUserId, userId)
                    .eq(ProfileSkill::getStatus, "ACTIVE"));
            profileSummary.put("existingSkills", skills.stream()
                    .map(s -> Map.of("name", s.getSkillName(), "level", s.getSkillLevel()))
                    .toList());
        }

        List<GrowthTarget> activeTargets = targetMapper.selectList(new LambdaQueryWrapper<GrowthTarget>()
                .eq(GrowthTarget::getUserId, userId)
                .eq(GrowthTarget::getStatus, "ACTIVE"));
        List<Long> targetIds = activeTargets.stream().map(GrowthTarget::getId).toList();

        List<Map<String, Object>> activeReqs = List.of();
        if (!targetIds.isEmpty()) {
            List<TargetRequirement> reqs = requirementMapper.selectList(new LambdaQueryWrapper<TargetRequirement>()
                    .in(TargetRequirement::getTargetId, targetIds));
            activeReqs = reqs.stream()
                    .map(r -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("requirementId", r.getId());
                        m.put("reqName", r.getReqName());
                        m.put("reqType", r.getReqType());
                        m.put("currentStatus", r.getStatus());
                        return m;
                    })
                    .toList();
        }

        return JournalExtractContext.builder()
                .journalContent(journalContent)
                .currentProfileSummary(profileSummary)
                .activeRequirements(activeReqs)
                .build();
    }

    private GrowthJournal requireOwnedJournal(Long userId, Long journalId) {
        GrowthJournal j = journalMapper.selectById(journalId);
        if (j == null || !userId.equals(j.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "随记不存在或不属于当前用户");
        }
        return j;
    }

    private int currentProfileVersion(Long userId) {
        GrowthProfile p = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));
        return p == null || p.getVersion() == null ? 1 : p.getVersion();
    }

    private static int progressFromStatus(String newStatus, Integer currentProgress) {
        int cur = currentProgress == null ? 0 : currentProgress;
        return switch (newStatus) {
            case "TODO" -> Math.min(cur, 10);
            case "IN_PROGRESS" -> Math.max(cur, 50);
            case "MET" -> 100;
            default -> cur;
        };
    }

    private static <T> List<T> defaultList(List<T> in) {
        return in == null ? List.of() : in;
    }
}
