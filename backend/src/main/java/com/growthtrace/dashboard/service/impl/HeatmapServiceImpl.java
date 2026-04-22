package com.growthtrace.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.dashboard.service.HeatmapService;
import com.growthtrace.dashboard.vo.HeatmapPoint;
import com.growthtrace.diagnosis.entity.GrowthSnapshot;
import com.growthtrace.diagnosis.mapper.GrowthSnapshotMapper;
import com.growthtrace.execution.entity.GrowthTask;
import com.growthtrace.execution.mapper.GrowthTaskMapper;
import com.growthtrace.journal.entity.GrowthJournal;
import com.growthtrace.journal.mapper.GrowthJournalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class HeatmapServiceImpl implements HeatmapService {

    private final GrowthJournalMapper journalMapper;
    private final GrowthTaskMapper taskMapper;
    private final GrowthSnapshotMapper snapshotMapper;

    @Override
    public List<HeatmapPoint> compute(Long userId, int windowDays) {
        int days = Math.min(365, Math.max(7, windowDays));
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = today.plusDays(1).atStartOfDay();

        TreeMap<LocalDate, Integer> scores = new TreeMap<>();
        for (int i = 0; i < days; i++) {
            scores.put(start.plusDays(i), 0);
        }

        // journals
        List<GrowthJournal> journals = journalMapper.selectList(new LambdaQueryWrapper<GrowthJournal>()
                .eq(GrowthJournal::getUserId, userId)
                .ge(GrowthJournal::getCreatedAt, startDt)
                .lt(GrowthJournal::getCreatedAt, endDt));
        for (GrowthJournal j : journals) {
            if (j.getCreatedAt() != null) {
                LocalDate d = j.getCreatedAt().toLocalDate();
                if (scores.containsKey(d)) {
                    scores.merge(d, 1, Integer::sum);
                }
            }
        }

        // task check-ins
        List<GrowthTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<GrowthTask>()
                .eq(GrowthTask::getUserId, userId)
                .isNotNull(GrowthTask::getCheckInDates));
        for (GrowthTask t : tasks) {
            List<String> dates = parseStringList(t.getCheckInDates());
            for (String s : dates) {
                if (!StringUtils.hasText(s)) continue;
                try {
                    LocalDate d = LocalDate.parse(s);
                    if (scores.containsKey(d)) {
                        scores.merge(d, 1, Integer::sum);
                    }
                } catch (Exception ignored) { /* skip invalid */ }
            }
        }

        // snapshots
        List<GrowthSnapshot> snapshots = snapshotMapper.selectList(new LambdaQueryWrapper<GrowthSnapshot>()
                .eq(GrowthSnapshot::getUserId, userId)
                .ge(GrowthSnapshot::getSnapshotTime, startDt)
                .lt(GrowthSnapshot::getSnapshotTime, endDt));
        for (GrowthSnapshot s : snapshots) {
            if (s.getSnapshotTime() != null) {
                LocalDate d = s.getSnapshotTime().toLocalDate();
                if (scores.containsKey(d)) {
                    scores.merge(d, 1, Integer::sum);
                }
            }
        }

        List<HeatmapPoint> out = new ArrayList<>(scores.size());
        scores.forEach((date, score) -> out.add(HeatmapPoint.builder().date(date).score(score).build()));
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
}
