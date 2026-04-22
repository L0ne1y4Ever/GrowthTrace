package com.growthtrace.diagnosis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.ai.dto.DiagnosisSummaryContext;
import com.growthtrace.ai.dto.DiagnosisSummaryResult;
import com.growthtrace.ai.service.AiService;
import com.growthtrace.common.exception.AiException;
import com.growthtrace.common.exception.BusinessException;
import com.growthtrace.common.result.PageResult;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.diagnosis.dto.TriggerDiagnosisRequest;
import com.growthtrace.diagnosis.dto.UpdateReviewNotesRequest;
import com.growthtrace.diagnosis.entity.GrowthSnapshot;
import com.growthtrace.diagnosis.entity.StageAssessment;
import com.growthtrace.diagnosis.mapper.GrowthSnapshotMapper;
import com.growthtrace.diagnosis.mapper.StageAssessmentMapper;
import com.growthtrace.diagnosis.service.DiagnosisMetricsService;
import com.growthtrace.diagnosis.service.DiagnosisService;
import com.growthtrace.diagnosis.service.SnapshotService;
import com.growthtrace.diagnosis.vo.DiagnosisSummary;
import com.growthtrace.diagnosis.vo.DiagnosisView;
import com.growthtrace.diagnosis.vo.ReviewNotesView;
import com.growthtrace.execution.entity.GrowthTask;
import com.growthtrace.execution.mapper.GrowthTaskMapper;
import com.growthtrace.journal.entity.GrowthJournal;
import com.growthtrace.journal.entity.JournalExtraction;
import com.growthtrace.journal.mapper.GrowthJournalMapper;
import com.growthtrace.journal.mapper.JournalExtractionMapper;
import com.growthtrace.profile.entity.GrowthProfile;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisServiceImpl implements DiagnosisService {

    private static final int DEFAULT_WINDOW_DAYS = 30;
    private static final int RECENT_EXTRACTION_LIMIT = 10;
    private static final int SUMMARY_EXCERPT_LEN = 120;

    private final DiagnosisMetricsService metricsService;
    private final SnapshotService snapshotService;
    private final AiService aiService;
    private final ProfileCompletenessCalculator completenessCalculator;

    private final StageAssessmentMapper assessmentMapper;
    private final GrowthSnapshotMapper snapshotMapper;

    private final GrowthProfileMapper profileMapper;

    private final GrowthTargetMapper targetMapper;
    private final TargetRequirementMapper requirementMapper;

    private final GrowthJournalMapper journalMapper;
    private final JournalExtractionMapper extractionMapper;

    private final GrowthTaskMapper taskMapper;

    // =====================================================
    // trigger
    // =====================================================

    @Override
    @Transactional
    public DiagnosisView trigger(Long userId, TriggerDiagnosisRequest payload) {
        int days = resolveWindowDays(payload);
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusDays(days);

        GrowthProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "尚未建档，请先完成建档引导再触发诊断");
        }

        // memory: project_growthtrace_ai_diagnosis — "诊断触发首行"需先重算画像完整度，
        // 保证 snapshot / metrics / DashboardOverview 三处数值一致
        int freshCompleteness = completenessCalculator.calculate(userId);
        profileMapper.update(null, new LambdaUpdateWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId)
                .set(GrowthProfile::getCompleteness, freshCompleteness));
        profile.setCompleteness(freshCompleteness);

        // 1) 本地规则指标
        Map<String, Object> metrics = metricsService.computeMetrics(userId, windowStart, windowEnd);

        // 2) AI 上下文 + 调 AI（失败走降级）
        DiagnosisSummaryContext aiContext = buildAiContext(userId, profile, windowStart, windowEnd, metrics);

        DiagnosisSummaryResult aiResult = null;
        String aiStatus = "SUCCESS";
        String aiRawResponse = null;
        try {
            aiResult = aiService.summarizeDiagnosis(aiContext);
            Map<String, Object> rawBody = new LinkedHashMap<>();
            rawBody.put("stage_summary", aiResult.getStageSummary()); // 可能 null
            rawBody.put("key_problems", defaultList(aiResult.getKeyProblems()));
            rawBody.put("suggestions", defaultList(aiResult.getSuggestions()));
            rawBody.put("correction_directions", defaultList(aiResult.getCorrectionDirections()));
            aiRawResponse = JsonUtils.toJson(rawBody);
        } catch (AiException ex) {
            log.warn("AI 阶段诊断失败，降级写入 FAILED 状态: userId={}, reason={}", userId, ex.getMessage());
            aiStatus = "FAILED";
        } catch (Exception ex) {
            log.error("AI 阶段诊断意外异常，降级写入 FAILED: userId={}", userId, ex);
            aiStatus = "FAILED";
        }

        // 3) 写 stage_assessment
        StageAssessment row = new StageAssessment();
        row.setUserId(userId);
        row.setTriggerTime(windowEnd);
        row.setWindowStart(windowStart);
        row.setWindowEnd(windowEnd);
        row.setProfileVersionAtTrigger(profile.getVersion() == null ? 1 : profile.getVersion());
        row.setMetrics(JsonUtils.toJson(metrics));
        row.setAiRawResponse(aiRawResponse);
        row.setAiStatus(aiStatus);
        if (aiResult != null) {
            row.setStageSummary(aiResult.getStageSummary());
            row.setKeyProblems(JsonUtils.toJson(defaultList(aiResult.getKeyProblems())));
            row.setSuggestions(JsonUtils.toJson(defaultList(aiResult.getSuggestions())));
            row.setCorrectionDirections(JsonUtils.toJson(defaultList(aiResult.getCorrectionDirections())));
        }
        // 初始化空 reviewNotes
        Map<String, Object> emptyReview = new LinkedHashMap<>();
        emptyReview.put("wins", List.of());
        emptyReview.put("learnings", List.of());
        emptyReview.put("nextFocus", List.of());
        emptyReview.put("userFreeform", "");
        row.setReviewNotes(JsonUtils.toJson(emptyReview));
        assessmentMapper.insert(row);

        // 4) 快照；无论 AI 成功与否都生成
        GrowthSnapshot snapshot = snapshotService.takeSnapshot(userId, row.getId(), metrics, "DIAGNOSIS");

        log.info("diagnosis triggered: userId={}, assessmentId={}, snapshotId={}, aiStatus={}",
                userId, row.getId(), snapshot.getId(), aiStatus);

        return toDetailView(row, snapshot.getId());
    }

    // =====================================================
    // query
    // =====================================================

    @Override
    public DiagnosisView get(Long userId, Long diagnosisId) {
        StageAssessment row = requireOwned(userId, diagnosisId);
        Long snapshotId = findSnapshotId(row.getId());
        return toDetailView(row, snapshotId);
    }

    @Override
    public PageResult<DiagnosisSummary> listHistory(Long userId, int page, int size) {
        int pageNo = Math.max(1, page);
        int pageSize = Math.min(Math.max(1, size), 50);
        IPage<StageAssessment> mpPage = assessmentMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<StageAssessment>()
                        .eq(StageAssessment::getUserId, userId)
                        .orderByDesc(StageAssessment::getCreatedAt));
        List<DiagnosisSummary> items = mpPage.getRecords().stream()
                .map(DiagnosisServiceImpl::toSummary)
                .toList();
        return PageResult.of(items, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize());
    }

    @Override
    @Transactional
    public DiagnosisView updateReview(Long userId, Long diagnosisId, UpdateReviewNotesRequest payload) {
        StageAssessment row = requireOwned(userId, diagnosisId);
        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("wins", payload.getWins() == null ? List.of() : payload.getWins());
        notes.put("learnings", payload.getLearnings() == null ? List.of() : payload.getLearnings());
        notes.put("nextFocus", payload.getNextFocus() == null ? List.of() : payload.getNextFocus());
        notes.put("userFreeform", payload.getUserFreeform() == null ? "" : payload.getUserFreeform());
        row.setReviewNotes(JsonUtils.toJson(notes));
        assessmentMapper.updateById(row);
        return toDetailView(row, findSnapshotId(row.getId()));
    }

    // =====================================================
    // helpers
    // =====================================================

    private StageAssessment requireOwned(Long userId, Long id) {
        StageAssessment row = assessmentMapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "诊断记录不存在或不属于当前用户");
        }
        return row;
    }

    private Long findSnapshotId(Long assessmentId) {
        GrowthSnapshot s = snapshotMapper.selectOne(new LambdaQueryWrapper<GrowthSnapshot>()
                .eq(GrowthSnapshot::getStageAssessmentId, assessmentId)
                .orderByDesc(GrowthSnapshot::getId)
                .last("LIMIT 1"));
        return s == null ? null : s.getId();
    }

    private int resolveWindowDays(TriggerDiagnosisRequest payload) {
        if (payload == null || payload.getWindowDays() == null || payload.getWindowDays() <= 0) {
            return DEFAULT_WINDOW_DAYS;
        }
        return Math.min(365, payload.getWindowDays());
    }

    private DiagnosisSummaryContext buildAiContext(Long userId,
                                                   GrowthProfile profile,
                                                   LocalDateTime windowStart,
                                                   LocalDateTime windowEnd,
                                                   Map<String, Object> metrics) {
        Map<String, Object> profileSummary = new LinkedHashMap<>();
        profileSummary.put("version", profile.getVersion());
        profileSummary.put("selfIntro", profile.getSelfIntro());
        profileSummary.put("summary", profile.getSummary());
        profileSummary.put("strengths", parseStringList(profile.getStrengths()));
        profileSummary.put("weaknesses", parseStringList(profile.getWeaknesses()));
        profileSummary.put("completeness", profile.getCompleteness());

        // active targets + 他们的 requirements
        List<GrowthTarget> activeTargets = targetMapper.selectList(new LambdaQueryWrapper<GrowthTarget>()
                .eq(GrowthTarget::getUserId, userId)
                .eq(GrowthTarget::getStatus, "ACTIVE")
                .orderByDesc(GrowthTarget::getIsPrimary)
                .orderByDesc(GrowthTarget::getUpdatedAt));
        List<Long> targetIds = activeTargets.stream().map(GrowthTarget::getId).toList();
        Map<Long, List<TargetRequirement>> reqMap = new LinkedHashMap<>();
        if (!targetIds.isEmpty()) {
            List<TargetRequirement> reqs = requirementMapper.selectList(new LambdaQueryWrapper<TargetRequirement>()
                    .in(TargetRequirement::getTargetId, targetIds)
                    .orderByAsc(TargetRequirement::getSortOrder));
            for (TargetRequirement r : reqs) {
                reqMap.computeIfAbsent(r.getTargetId(), k -> new ArrayList<>()).add(r);
            }
        }
        List<Map<String, Object>> targetsCtx = activeTargets.stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("type", t.getTargetType());
                    m.put("title", t.getTitle());
                    m.put("isPrimary", t.getIsPrimary() != null && t.getIsPrimary() == 1);
                    m.put("deadline", t.getDeadline());
                    m.put("requirements", reqMap.getOrDefault(t.getId(), List.of()).stream()
                            .map(r -> Map.<String, Object>of(
                                    "name", r.getReqName(),
                                    "type", r.getReqType(),
                                    "status", r.getStatus(),
                                    "progress", r.getProgress()))
                            .toList());
                    return m;
                })
                .toList();

        // recent CONFIRMED extractions within window
        List<JournalExtraction> confirmed = extractionMapper.selectList(new LambdaQueryWrapper<JournalExtraction>()
                .eq(JournalExtraction::getUserId, userId)
                .eq(JournalExtraction::getExtractionStatus, "CONFIRMED")
                .ge(JournalExtraction::getConfirmedAt, windowStart)
                .le(JournalExtraction::getConfirmedAt, windowEnd)
                .orderByDesc(JournalExtraction::getConfirmedAt)
                .last("LIMIT " + RECENT_EXTRACTION_LIMIT));
        List<Map<String, Object>> extractionsCtx = confirmed.stream()
                .map(ex -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("confirmedAt", ex.getConfirmedAt());
                    m.put("newSkills", parseMapList(ex.getConfirmedNewSkills()));
                    m.put("events", parseMapList(ex.getConfirmedEvents()));
                    m.put("blockers", parseStringList(ex.getConfirmedBlockers()));
                    return m;
                })
                .toList();

        // task snapshot
        List<GrowthTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<GrowthTask>()
                .eq(GrowthTask::getUserId, userId));
        Map<String, Object> taskSnapshot = buildTaskSnapshot(tasks);

        // 最近随记原文摘录（不超过 5 条），帮助 AI 理解近期上下文
        List<GrowthJournal> recentJournals = journalMapper.selectList(new LambdaQueryWrapper<GrowthJournal>()
                .eq(GrowthJournal::getUserId, userId)
                .ge(GrowthJournal::getCreatedAt, windowStart)
                .le(GrowthJournal::getCreatedAt, windowEnd)
                .orderByDesc(GrowthJournal::getCreatedAt)
                .last("LIMIT 5"));
        Map<String, Object> journalContext = new LinkedHashMap<>();
        journalContext.put("count", recentJournals.size());
        journalContext.put("samples", recentJournals.stream()
                .map(j -> {
                    Map<String, Object> sample = new LinkedHashMap<>();
                    sample.put("createdAt", j.getCreatedAt());
                    sample.put("mood", j.getMood()); // 可为 null
                    sample.put("excerpt", excerpt(j.getContent(), 120));
                    return sample;
                })
                .toList());

        // 将最近随记并入 taskSnapshot 旁的上下文字段（AI prompt 里只认 taskSnapshot 的 key，
        // 所以我们把它挂在 taskSnapshot.extraJournalContext 下而不是改 Prompt schema）
        taskSnapshot.put("recentJournals", journalContext);

        return DiagnosisSummaryContext.builder()
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .metrics(metrics)
                .profileSummary(profileSummary)
                .targets(targetsCtx)
                .recentExtractions(extractionsCtx)
                .taskSnapshot(taskSnapshot)
                .build();
    }

    private Map<String, Object> buildTaskSnapshot(List<GrowthTask> tasks) {
        Map<String, Object> m = new LinkedHashMap<>();
        int todo = 0, inProgress = 0, done = 0, abandoned = 0;
        for (GrowthTask t : tasks) {
            switch (String.valueOf(t.getStatus())) {
                case "TODO" -> todo++;
                case "IN_PROGRESS" -> inProgress++;
                case "DONE" -> done++;
                case "ABANDONED" -> abandoned++;
                default -> { /* ignore */ }
            }
        }
        int active = todo + inProgress + done;
        double completionRate = active == 0 ? 0.0 : Math.round(done * 100.0 / active) / 100.0;
        m.put("total", tasks.size());
        m.put("todo", todo);
        m.put("in_progress", inProgress);
        m.put("done", done);
        m.put("abandoned", abandoned);
        m.put("completion_rate", completionRate);
        return m;
    }

    // =====================================================
    // view conversion
    // =====================================================

    private DiagnosisView toDetailView(StageAssessment row, Long snapshotId) {
        return DiagnosisView.builder()
                .id(row.getId())
                .userId(row.getUserId())
                .triggerTime(row.getTriggerTime())
                .windowStart(row.getWindowStart())
                .windowEnd(row.getWindowEnd())
                .profileVersionAtTrigger(row.getProfileVersionAtTrigger())
                .metrics(parseMap(row.getMetrics()))
                .stageSummary(row.getStageSummary())
                .keyProblems(parseMapList(row.getKeyProblems()))
                .suggestions(parseMapList(row.getSuggestions()))
                .correctionDirections(parseMapList(row.getCorrectionDirections()))
                .reviewNotes(parseReviewNotes(row.getReviewNotes()))
                .aiStatus(row.getAiStatus())
                .snapshotId(snapshotId)
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    private static DiagnosisSummary toSummary(StageAssessment row) {
        int keyProblemCount = parseMapList(row.getKeyProblems()).size();
        int suggestionCount = parseMapList(row.getSuggestions()).size();
        return DiagnosisSummary.builder()
                .id(row.getId())
                .triggerTime(row.getTriggerTime())
                .windowStart(row.getWindowStart())
                .windowEnd(row.getWindowEnd())
                .profileVersionAtTrigger(row.getProfileVersionAtTrigger())
                .stageSummaryExcerpt(excerpt(row.getStageSummary(), SUMMARY_EXCERPT_LEN))
                .keyProblemCount(keyProblemCount)
                .suggestionCount(suggestionCount)
                .aiStatus(row.getAiStatus())
                .createdAt(row.getCreatedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static ReviewNotesView parseReviewNotes(String json) {
        if (!StringUtils.hasText(json)) return ReviewNotesView.empty();
        try {
            Map<String, Object> m = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            if (m == null) return ReviewNotesView.empty();
            return ReviewNotesView.builder()
                    .wins(asStringList(m.get("wins")))
                    .learnings(asStringList(m.get("learnings")))
                    .nextFocus(asStringList(m.get("nextFocus")))
                    .userFreeform(m.get("userFreeform") == null ? null : String.valueOf(m.get("userFreeform")))
                    .build();
        } catch (Exception e) {
            return ReviewNotesView.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object raw) {
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) out.add(String.valueOf(o));
            }
            return out;
        }
        return new ArrayList<>();
    }

    private static Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) return new LinkedHashMap<>();
        try {
            Map<String, Object> m = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return m == null ? new LinkedHashMap<>() : m;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private static List<Map<String, Object>> parseMapList(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            List<Map<String, Object>> list = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            List<String> list = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static <T> List<T> defaultList(List<T> in) {
        return in == null ? List.of() : in;
    }

    private static String excerpt(String content, int max) {
        if (!StringUtils.hasText(content)) return null;
        return content.length() <= max ? content : content.substring(0, max) + "…";
    }
}
