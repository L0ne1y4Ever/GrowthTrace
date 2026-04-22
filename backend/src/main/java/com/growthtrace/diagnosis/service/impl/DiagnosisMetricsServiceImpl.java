package com.growthtrace.diagnosis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.growthtrace.diagnosis.service.DiagnosisMetricsService;
import com.growthtrace.execution.entity.GrowthTask;
import com.growthtrace.execution.mapper.GrowthTaskMapper;
import com.growthtrace.journal.entity.GrowthJournal;
import com.growthtrace.journal.mapper.GrowthJournalMapper;
import com.growthtrace.profile.entity.ProfileSkill;
import com.growthtrace.profile.mapper.ProfileSkillMapper;
import com.growthtrace.profile.service.ProfileCompletenessCalculator;
import com.growthtrace.target.entity.GrowthTarget;
import com.growthtrace.target.entity.TargetRequirement;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import com.growthtrace.diagnosis.entity.GrowthSnapshot;
import com.growthtrace.diagnosis.mapper.GrowthSnapshotMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DiagnosisMetricsServiceImpl implements DiagnosisMetricsService {

    private final GrowthJournalMapper journalMapper;
    private final GrowthTaskMapper taskMapper;
    private final ProfileSkillMapper skillMapper;
    private final GrowthTargetMapper targetMapper;
    private final TargetRequirementMapper requirementMapper;
    private final GrowthSnapshotMapper snapshotMapper;
    private final ProfileCompletenessCalculator completenessCalculator;

    @Override
    public Map<String, Object> computeMetrics(Long userId, LocalDateTime windowStart, LocalDateTime windowEnd) {
        Map<String, Object> m = new LinkedHashMap<>();

        List<GrowthJournal> journals = journalMapper.selectList(new LambdaQueryWrapper<GrowthJournal>()
                .eq(GrowthJournal::getUserId, userId)
                .ge(GrowthJournal::getCreatedAt, windowStart)
                .le(GrowthJournal::getCreatedAt, windowEnd));
        m.put("journal_count", journals.size());
        m.put("journal_streak", computeJournalStreak(userId, windowEnd.toLocalDate()));

        m.put("task_completion_rate", computeTaskCompletionRate(userId));
        m.put("new_skills_count", countNewSkills(userId, windowStart, windowEnd));
        m.put("profile_completeness", completenessCalculator.calculate(userId));
        m.put("target_requirement_progress", computeRequirementProgress(userId));
        m.put("activity_intensity", computeActivityIntensity(userId, windowStart, windowEnd, journals));

        return m;
    }

    // ---- individual metrics ----

    /** 从 windowEnd 那天起往前数，连续有随记的天数。 */
    private int computeJournalStreak(Long userId, LocalDate anchor) {
        // 取 anchor 之前 60 天的数据一次查完，避免日查询
        LocalDateTime lowerBound = anchor.minusDays(60).atStartOfDay();
        List<GrowthJournal> recent = journalMapper.selectList(new LambdaQueryWrapper<GrowthJournal>()
                .eq(GrowthJournal::getUserId, userId)
                .ge(GrowthJournal::getCreatedAt, lowerBound)
                .le(GrowthJournal::getCreatedAt, anchor.plusDays(1).atStartOfDay()));
        if (recent.isEmpty()) return 0;

        java.util.Set<LocalDate> days = new java.util.HashSet<>();
        for (GrowthJournal j : recent) {
            if (j.getCreatedAt() != null) {
                days.add(j.getCreatedAt().toLocalDate());
            }
        }
        int streak = 0;
        LocalDate cursor = anchor;
        // 允许今天没有随记：从昨天开始数也算
        if (!days.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
            if (streak > 60) break; // 安全阈值
        }
        return streak;
    }

    private double computeTaskCompletionRate(Long userId) {
        List<GrowthTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<GrowthTask>()
                .eq(GrowthTask::getUserId, userId)
                .in(GrowthTask::getStatus, List.of("TODO", "IN_PROGRESS", "DONE")));
        if (tasks.isEmpty()) return 0.0;
        long done = tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        return round2(done * 1.0 / tasks.size());
    }

    private int countNewSkills(Long userId, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return Math.toIntExact(skillMapper.selectCount(new LambdaQueryWrapper<ProfileSkill>()
                .eq(ProfileSkill::getUserId, userId)
                .ge(ProfileSkill::getCreatedAt, windowStart)
                .le(ProfileSkill::getCreatedAt, windowEnd)));
    }

    private Map<String, Object> computeRequirementProgress(Long userId) {
        List<GrowthTarget> activeTargets = targetMapper.selectList(new LambdaQueryWrapper<GrowthTarget>()
                .eq(GrowthTarget::getUserId, userId)
                .eq(GrowthTarget::getStatus, "ACTIVE"));
        if (activeTargets.isEmpty()) {
            return Map.of("total", 0, "met", 0, "in_progress", 0, "todo", 0, "met_ratio", 0.0);
        }
        List<Long> ids = activeTargets.stream().map(GrowthTarget::getId).toList();
        List<TargetRequirement> reqs = requirementMapper.selectList(new LambdaQueryWrapper<TargetRequirement>()
                .in(TargetRequirement::getTargetId, ids));
        int total = reqs.size();
        int met = (int) reqs.stream().filter(r -> "MET".equals(r.getStatus())).count();
        int inProgress = (int) reqs.stream().filter(r -> "IN_PROGRESS".equals(r.getStatus())).count();
        int todo = (int) reqs.stream().filter(r -> "TODO".equals(r.getStatus())).count();
        double ratio = total == 0 ? 0.0 : round2(met * 1.0 / total);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("met", met);
        m.put("in_progress", inProgress);
        m.put("todo", todo);
        m.put("met_ratio", ratio);
        return m;
    }

    /**
     * 活动强度：按天 score = journal + task check-in + snapshot
     * 只返回 score>0 的天，按日期升序。
     */
    private List<Map<String, Object>> computeActivityIntensity(
            Long userId, LocalDateTime windowStart, LocalDateTime windowEnd, List<GrowthJournal> journals) {
        TreeMap<LocalDate, Integer> scores = new TreeMap<>();

        for (GrowthJournal j : journals) {
            if (j.getCreatedAt() == null) continue;
            scores.merge(j.getCreatedAt().toLocalDate(), 1, Integer::sum);
        }

        // task check-ins
        List<GrowthTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<GrowthTask>()
                .eq(GrowthTask::getUserId, userId)
                .isNotNull(GrowthTask::getCheckInDates));
        for (GrowthTask t : tasks) {
            List<String> checkIns = parseStringList(t.getCheckInDates());
            for (String s : checkIns) {
                if (!StringUtils.hasText(s)) continue;
                try {
                    LocalDate d = LocalDate.parse(s.substring(0, Math.min(10, s.length())));
                    if (!d.isBefore(windowStart.toLocalDate()) && !d.isAfter(windowEnd.toLocalDate())) {
                        scores.merge(d, 1, Integer::sum);
                    }
                } catch (Exception ignored) {
                    // 忽略非法日期
                }
            }
        }

        // snapshots
        List<GrowthSnapshot> snapshots = snapshotMapper.selectList(new LambdaQueryWrapper<GrowthSnapshot>()
                .eq(GrowthSnapshot::getUserId, userId)
                .ge(GrowthSnapshot::getSnapshotTime, windowStart)
                .le(GrowthSnapshot::getSnapshotTime, windowEnd));
        for (GrowthSnapshot s : snapshots) {
            if (s.getSnapshotTime() != null) {
                scores.merge(s.getSnapshotTime().toLocalDate(), 1, Integer::sum);
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> e : scores.entrySet()) {
            if (e.getValue() <= 0) continue;
            Map<String, Object> one = new HashMap<>();
            one.put("date", e.getKey().toString());
            one.put("score", e.getValue());
            out.add(one);
        }
        // sanity: duration check 不超过 90 天
        if (Duration.between(windowStart, windowEnd).toDays() > 365) {
            return out.subList(Math.max(0, out.size() - 365), out.size());
        }
        return out;
    }

    private static List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            List<String> list = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
