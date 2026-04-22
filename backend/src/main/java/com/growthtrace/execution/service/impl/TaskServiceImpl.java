package com.growthtrace.execution.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.common.exception.BusinessException;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.execution.dto.CheckInRequest;
import com.growthtrace.execution.dto.CreateTaskRequest;
import com.growthtrace.execution.dto.UpdateTaskRequest;
import com.growthtrace.execution.dto.UpdateTaskStatusRequest;
import com.growthtrace.execution.entity.GrowthTask;
import com.growthtrace.execution.mapper.GrowthTaskMapper;
import com.growthtrace.execution.service.TaskService;
import com.growthtrace.execution.vo.TaskView;
import com.growthtrace.execution.vo.WeeklyProgressView;
import com.growthtrace.target.entity.GrowthTarget;
import com.growthtrace.target.entity.TargetRequirement;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final GrowthTaskMapper taskMapper;
    private final GrowthTargetMapper targetMapper;
    private final TargetRequirementMapper requirementMapper;

    // ----------------- CRUD -----------------

    @Override
    @Transactional
    public TaskView create(Long userId, CreateTaskRequest payload) {
        validateTargetAndRequirement(userId, payload.getTargetId(), payload.getRequirementId());

        GrowthTask t = new GrowthTask();
        t.setUserId(userId);
        t.setTitle(payload.getTitle().trim());
        t.setDescription(payload.getDescription());
        t.setStatus("TODO");
        t.setPriority(resolvePriority(payload.getPriority()));
        t.setDueDate(payload.getDueDate());
        t.setTargetId(payload.getTargetId());
        t.setRequirementId(payload.getRequirementId());
        t.setPlannedEffortMinutes(payload.getPlannedEffortMinutes());
        t.setActualEffortMinutes(0);
        t.setCheckInCount(0);
        t.setCheckInDates(JsonUtils.toJson(new ArrayList<String>()));
        taskMapper.insert(t);
        log.info("task created: userId={}, taskId={}", userId, t.getId());
        return toView(taskMapper.selectById(t.getId()));
    }

    @Override
    public List<TaskView> list(Long userId, String statusFilter, Long targetIdFilter) {
        LambdaQueryWrapper<GrowthTask> q = new LambdaQueryWrapper<GrowthTask>()
                .eq(GrowthTask::getUserId, userId);
        if (StringUtils.hasText(statusFilter)) {
            q.eq(GrowthTask::getStatus, statusFilter);
        } else {
            // 默认不展示 ABANDONED
            q.in(GrowthTask::getStatus, List.of("TODO", "IN_PROGRESS", "DONE"));
        }
        if (targetIdFilter != null) {
            q.eq(GrowthTask::getTargetId, targetIdFilter);
        }
        // 优先级 HIGH > MEDIUM > LOW；同级按 due_date 升序（null 靠后），再按 id 降序。
        q.orderByAsc(GrowthTask::getDueDate).orderByDesc(GrowthTask::getId);
        List<GrowthTask> rows = taskMapper.selectList(q);
        rows.sort((a, b) -> {
            int pa = priorityOrder(a.getPriority());
            int pb = priorityOrder(b.getPriority());
            if (pa != pb) return Integer.compare(pa, pb);
            if (a.getDueDate() == null && b.getDueDate() == null) return 0;
            if (a.getDueDate() == null) return 1;
            if (b.getDueDate() == null) return -1;
            return a.getDueDate().compareTo(b.getDueDate());
        });
        return rows.stream().map(TaskServiceImpl::toView).toList();
    }

    @Override
    public TaskView get(Long userId, Long taskId) {
        return toView(requireOwned(userId, taskId));
    }

    @Override
    @Transactional
    public TaskView update(Long userId, Long taskId, UpdateTaskRequest payload) {
        GrowthTask t = requireOwned(userId, taskId);
        validateTargetAndRequirement(userId, payload.getTargetId(), payload.getRequirementId());

        t.setTitle(payload.getTitle().trim());
        t.setDescription(payload.getDescription());
        t.setPriority(resolvePriority(payload.getPriority()));
        t.setDueDate(payload.getDueDate());
        t.setTargetId(payload.getTargetId());
        t.setRequirementId(payload.getRequirementId());
        t.setPlannedEffortMinutes(payload.getPlannedEffortMinutes());
        taskMapper.updateById(t);
        return toView(taskMapper.selectById(taskId));
    }

    @Override
    @Transactional
    public TaskView updateStatus(Long userId, Long taskId, UpdateTaskStatusRequest payload) {
        GrowthTask t = requireOwned(userId, taskId);
        String newStatus = payload.getStatus();
        if (!newStatus.equals(t.getStatus())) {
            t.setStatus(newStatus);
            if ("DONE".equals(newStatus)) {
                if (t.getCompletedAt() == null) {
                    t.setCompletedAt(LocalDateTime.now());
                }
            } else {
                // 回退出 DONE：清空完成时间
                t.setCompletedAt(null);
            }
            taskMapper.updateById(t);
            log.info("task status changed: taskId={}, {} -> {}", taskId, t.getStatus(), newStatus);
        }
        return toView(taskMapper.selectById(taskId));
    }

    @Override
    @Transactional
    public void remove(Long userId, Long taskId) {
        requireOwned(userId, taskId);
        taskMapper.deleteById(taskId);
    }

    // ----------------- Check-in -----------------

    @Override
    @Transactional
    public TaskView checkIn(Long userId, Long taskId, CheckInRequest payload) {
        GrowthTask t = requireOwned(userId, taskId);
        if ("ABANDONED".equals(t.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "已放弃任务不能打卡");
        }
        LocalDate date = payload == null || payload.getDate() == null
                ? LocalDate.now()
                : payload.getDate();
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不允许对未来日期打卡");
        }

        List<String> dates = parseDateList(t.getCheckInDates());
        String key = date.toString();
        boolean alreadyLogged = dates.contains(key);
        if (!alreadyLogged) {
            dates.add(key);
            // 保留排序，便于查看
            Collections.sort(dates);
            t.setCheckInDates(JsonUtils.toJson(dates));
            t.setCheckInCount((t.getCheckInCount() == null ? 0 : t.getCheckInCount()) + 1);
        }
        if (payload != null && payload.getEffortMinutes() != null && payload.getEffortMinutes() > 0) {
            int cur = t.getActualEffortMinutes() == null ? 0 : t.getActualEffortMinutes();
            t.setActualEffortMinutes(cur + payload.getEffortMinutes());
        }

        // 首次打卡：TODO 自动切到 IN_PROGRESS
        if ("TODO".equals(t.getStatus())) {
            t.setStatus("IN_PROGRESS");
        }

        taskMapper.updateById(t);
        log.info("task checked in: taskId={}, date={}, alreadyLogged={}", taskId, key, alreadyLogged);
        return toView(taskMapper.selectById(taskId));
    }

    // ----------------- Weekly progress -----------------

    @Override
    public WeeklyProgressView weeklyProgress(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        List<GrowthTask> allTasks = taskMapper.selectList(new LambdaQueryWrapper<GrowthTask>()
                .eq(GrowthTask::getUserId, userId));

        // due this week
        int dueThisWeek = 0;
        int doneThisWeek = 0;
        for (GrowthTask t : allTasks) {
            LocalDate due = t.getDueDate();
            if (due != null && !due.isBefore(monday) && !due.isAfter(sunday)) {
                dueThisWeek++;
                if ("DONE".equals(t.getStatus())) {
                    doneThisWeek++;
                }
            }
        }
        double rate = dueThisWeek == 0 ? 0.0 : Math.round(doneThisWeek * 100.0 / dueThisWeek) / 100.0;

        // check-ins per day across all tasks
        TreeMap<LocalDate, Integer> perDay = new TreeMap<>();
        for (int i = 0; i <= 6; i++) {
            perDay.put(monday.plusDays(i), 0);
        }
        for (GrowthTask t : allTasks) {
            for (String s : parseDateList(t.getCheckInDates())) {
                try {
                    LocalDate d = LocalDate.parse(s);
                    if (perDay.containsKey(d)) {
                        perDay.merge(d, 1, Integer::sum);
                    }
                } catch (Exception ignored) { /* 忽略非法日期 */ }
            }
        }
        List<WeeklyProgressView.DayPoint> points = perDay.entrySet().stream()
                .map(e -> WeeklyProgressView.DayPoint.builder()
                        .date(e.getKey())
                        .count(e.getValue())
                        .build())
                .toList();

        return WeeklyProgressView.builder()
                .weekStart(monday)
                .weekEnd(sunday)
                .dueThisWeek(dueThisWeek)
                .doneThisWeek(doneThisWeek)
                .completionRate(rate)
                .checkInPerDay(points)
                .build();
    }

    // ----------------- helpers -----------------

    private GrowthTask requireOwned(Long userId, Long taskId) {
        GrowthTask t = taskMapper.selectById(taskId);
        if (t == null || !userId.equals(t.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在或不属于当前用户");
        }
        return t;
    }

    private void validateTargetAndRequirement(Long userId, Long targetId, Long requirementId) {
        if (targetId == null && requirementId == null) {
            return;
        }
        if (targetId != null) {
            GrowthTarget target = targetMapper.selectById(targetId);
            if (target == null || !userId.equals(target.getUserId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "targetId 非法或不属于当前用户");
            }
        }
        if (requirementId != null) {
            TargetRequirement req = requirementMapper.selectById(requirementId);
            if (req == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "requirementId 不存在");
            }
            if (targetId != null && !targetId.equals(req.getTargetId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "requirementId 不属于该目标");
            }
            // 若只给了 requirementId，必须校验其目标归属当前用户
            if (targetId == null) {
                GrowthTarget target = targetMapper.selectById(req.getTargetId());
                if (target == null || !userId.equals(target.getUserId())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "requirementId 非法或不属于当前用户");
                }
            }
        }
    }

    private static String resolvePriority(String raw) {
        if (!StringUtils.hasText(raw)) return "MEDIUM";
        return switch (raw.toUpperCase()) {
            case "HIGH", "MEDIUM", "LOW" -> raw.toUpperCase();
            default -> "MEDIUM";
        };
    }

    private static int priorityOrder(String p) {
        if ("HIGH".equals(p)) return 0;
        if ("MEDIUM".equals(p)) return 1;
        return 2;
    }

    private static List<String> parseDateList(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            List<String> list = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static TaskView toView(GrowthTask t) {
        List<String> dates = parseDateList(t.getCheckInDates());
        Set<String> set = new HashSet<>(dates);
        boolean checkedToday = set.contains(LocalDate.now().toString());
        return TaskView.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .targetId(t.getTargetId())
                .requirementId(t.getRequirementId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .dueDate(t.getDueDate())
                .completedAt(t.getCompletedAt())
                .checkInDates(dates)
                .checkInCount(t.getCheckInCount())
                .plannedEffortMinutes(t.getPlannedEffortMinutes())
                .actualEffortMinutes(t.getActualEffortMinutes())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .checkedInToday(checkedToday)
                .build();
    }
}
