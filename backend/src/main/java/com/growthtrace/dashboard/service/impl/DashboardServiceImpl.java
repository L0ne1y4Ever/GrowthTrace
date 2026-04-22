package com.growthtrace.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.dashboard.service.DashboardService;
import com.growthtrace.dashboard.service.HeatmapService;
import com.growthtrace.dashboard.vo.DashboardOverviewView;
import com.growthtrace.dashboard.vo.GrowthCurvePoint;
import com.growthtrace.dashboard.vo.HeatmapPoint;
import com.growthtrace.dashboard.vo.JournalDigest;
import com.growthtrace.dashboard.vo.LatestDiagnosisDigest;
import com.growthtrace.dashboard.vo.PrimaryTargetView;
import com.growthtrace.diagnosis.entity.GrowthSnapshot;
import com.growthtrace.diagnosis.entity.StageAssessment;
import com.growthtrace.diagnosis.mapper.GrowthSnapshotMapper;
import com.growthtrace.diagnosis.mapper.StageAssessmentMapper;
import com.growthtrace.execution.service.TaskService;
import com.growthtrace.execution.vo.WeeklyProgressView;
import com.growthtrace.journal.entity.GrowthJournal;
import com.growthtrace.journal.entity.JournalExtraction;
import com.growthtrace.journal.mapper.GrowthJournalMapper;
import com.growthtrace.journal.mapper.JournalExtractionMapper;
import com.growthtrace.profile.entity.GrowthProfile;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
import com.growthtrace.target.entity.GrowthTarget;
import com.growthtrace.target.entity.TargetRequirement;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_JOURNAL_LIMIT = 5;
    private static final int GROWTH_CURVE_LIMIT = 30;
    private static final int DEFAULT_HEATMAP_DAYS = 90;
    private static final int JOURNAL_EXCERPT_LEN = 120;

    private final GrowthProfileMapper profileMapper;
    private final GrowthTargetMapper targetMapper;
    private final TargetRequirementMapper requirementMapper;
    private final GrowthJournalMapper journalMapper;
    private final JournalExtractionMapper extractionMapper;
    private final StageAssessmentMapper assessmentMapper;
    private final GrowthSnapshotMapper snapshotMapper;

    private final HeatmapService heatmapService;
    private final TaskService taskService;

    @Override
    public DashboardOverviewView overview(Long userId) {
        Integer completeness = null;
        GrowthProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<GrowthProfile>()
                .eq(GrowthProfile::getUserId, userId));
        if (profile != null) {
            completeness = profile.getCompleteness();
        }

        PrimaryTargetView primary = loadPrimaryTarget(userId);
        WeeklyProgressView weekly = taskService.weeklyProgress(userId);
        LatestDiagnosisDigest latest = loadLatestDiagnosis(userId);
        List<JournalDigest> recentJournals = loadRecentJournals(userId);
        List<GrowthCurvePoint> curve = growthCurve(userId, GROWTH_CURVE_LIMIT);
        List<HeatmapPoint> heat = heatmap(userId, DEFAULT_HEATMAP_DAYS);

        return DashboardOverviewView.builder()
                .profileCompleteness(completeness)
                .primaryTarget(primary)
                .weeklyTask(weekly)
                .latestDiagnosis(latest)
                .recentJournals(recentJournals)
                .growthCurve(curve)
                .heatmap(heat)
                .build();
    }

    @Override
    public List<HeatmapPoint> heatmap(Long userId, int windowDays) {
        return heatmapService.compute(userId, windowDays <= 0 ? DEFAULT_HEATMAP_DAYS : windowDays);
    }

    @Override
    public List<GrowthCurvePoint> growthCurve(Long userId, int limit) {
        int safe = Math.min(100, Math.max(1, limit));
        List<GrowthSnapshot> snapshots = snapshotMapper.selectList(new LambdaQueryWrapper<GrowthSnapshot>()
                .eq(GrowthSnapshot::getUserId, userId)
                .orderByDesc(GrowthSnapshot::getSnapshotTime)
                .last("LIMIT " + safe));
        // 翻转为升序，方便绘制
        Collections.reverse(snapshots);

        List<GrowthCurvePoint> out = new ArrayList<>();
        for (GrowthSnapshot s : snapshots) {
            Map<String, Object> profileJson = parseMap(s.getProfileJson());
            Integer completeness = null;
            Object c = profileJson.get("completeness");
            if (c instanceof Number n) completeness = n.intValue();

            Map<String, Object> metrics = parseMap(s.getMetricsSnapshot());
            Double rate = null;
            Object rObj = metrics.get("task_completion_rate");
            if (rObj instanceof Number n) rate = n.doubleValue();
            Integer newSkills = null;
            Object ns = metrics.get("new_skills_count");
            if (ns instanceof Number n) newSkills = n.intValue();

            out.add(GrowthCurvePoint.builder()
                    .snapshotId(s.getId())
                    .snapshotTime(s.getSnapshotTime())
                    .date(s.getSnapshotTime() == null ? null : s.getSnapshotTime().toLocalDate())
                    .profileVersion(s.getProfileVersion())
                    .completeness(completeness)
                    .taskCompletionRate(rate)
                    .newSkillsCount(newSkills)
                    .build());
        }
        return out;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private PrimaryTargetView loadPrimaryTarget(Long userId) {
        GrowthTarget t = targetMapper.selectOne(new LambdaQueryWrapper<GrowthTarget>()
                .eq(GrowthTarget::getUserId, userId)
                .eq(GrowthTarget::getStatus, "ACTIVE")
                .eq(GrowthTarget::getIsPrimary, 1)
                .last("LIMIT 1"));
        if (t == null) {
            return null;
        }
        List<TargetRequirement> reqs = requirementMapper.selectList(new LambdaQueryWrapper<TargetRequirement>()
                .eq(TargetRequirement::getTargetId, t.getId()));
        int total = reqs.size();
        int met = (int) reqs.stream().filter(r -> "MET".equals(r.getStatus())).count();
        double ratio = total == 0 ? 0.0 : Math.round(met * 100.0 / total) / 100.0;
        return PrimaryTargetView.builder()
                .id(t.getId())
                .targetType(t.getTargetType())
                .title(t.getTitle())
                .description(t.getDescription())
                .deadline(t.getDeadline())
                .requirementCount(total)
                .requirementMetCount(met)
                .metRatio(ratio)
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private LatestDiagnosisDigest loadLatestDiagnosis(Long userId) {
        StageAssessment row = assessmentMapper.selectOne(new LambdaQueryWrapper<StageAssessment>()
                .eq(StageAssessment::getUserId, userId)
                .orderByDesc(StageAssessment::getCreatedAt)
                .last("LIMIT 1"));
        if (row == null) {
            return null;
        }
        List<Map<String, Object>> suggestions = parseMapList(row.getSuggestions());
        List<Map<String, Object>> corrections = parseMapList(row.getCorrectionDirections());
        List<String> topSuggestions = suggestions.stream()
                .limit(3)
                .map(m -> toStr(m.get("title")))
                .filter(StringUtils::hasText)
                .toList();
        List<String> topCorrections = corrections.stream()
                .limit(3)
                .map(m -> toStr(m.get("direction")))
                .filter(StringUtils::hasText)
                .toList();
        return LatestDiagnosisDigest.builder()
                .id(row.getId())
                .triggerTime(row.getTriggerTime())
                .stageSummaryExcerpt(excerpt(row.getStageSummary(), 180))
                .topSuggestions(topSuggestions)
                .topCorrections(topCorrections)
                .aiStatus(row.getAiStatus())
                .build();
    }

    private List<JournalDigest> loadRecentJournals(Long userId) {
        List<GrowthJournal> journals = journalMapper.selectList(new LambdaQueryWrapper<GrowthJournal>()
                .eq(GrowthJournal::getUserId, userId)
                .orderByDesc(GrowthJournal::getCreatedAt)
                .last("LIMIT " + RECENT_JOURNAL_LIMIT));
        if (journals.isEmpty()) {
            return List.of();
        }
        List<Long> ids = journals.stream().map(GrowthJournal::getId).toList();
        Map<Long, String> statusById = extractionMapper.selectList(new LambdaQueryWrapper<JournalExtraction>()
                .in(JournalExtraction::getJournalId, ids))
                .stream()
                .collect(Collectors.toMap(
                        JournalExtraction::getJournalId,
                        JournalExtraction::getExtractionStatus,
                        (a, b) -> a));
        return journals.stream()
                .map(j -> JournalDigest.builder()
                        .id(j.getId())
                        .contentExcerpt(excerpt(j.getContent(), JOURNAL_EXCERPT_LEN))
                        .mood(j.getMood())
                        .wordCount(j.getWordCount())
                        .createdAt(j.getCreatedAt())
                        .extractionStatus(statusById.get(j.getId()))
                        .build())
                .toList();
    }

    private static Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) return new HashMap<>();
        try {
            Map<String, Object> m = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return m == null ? new LinkedHashMap<>() : m;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static List<Map<String, Object>> parseMapList(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            List<Map<String, Object>> list = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String toStr(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String excerpt(String content, int max) {
        if (!StringUtils.hasText(content)) return null;
        return content.length() <= max ? content : content.substring(0, max) + "…";
    }
}
