package com.growthtrace.execution.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.ai.dto.TaskDraftContext;
import com.growthtrace.ai.dto.TaskDraftResult;
import com.growthtrace.ai.service.AiService;
import com.growthtrace.common.exception.BusinessException;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.execution.dto.CheckInRequest;
import com.growthtrace.execution.dto.CreateTaskRequest;
import com.growthtrace.execution.dto.GenerateTaskDraftRequest;
import com.growthtrace.execution.dto.UpdateTaskRequest;
import com.growthtrace.execution.dto.UpdateTaskStatusRequest;
import com.growthtrace.execution.entity.GrowthTask;
import com.growthtrace.execution.mapper.GrowthTaskMapper;
import com.growthtrace.execution.service.TaskService;
import com.growthtrace.execution.vo.TaskDraftView;
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
    private final AiService aiService;

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
    public TaskDraftView generateDraft(Long userId, GenerateTaskDraftRequest payload) {
        GrowthTarget target = null;
        TargetRequirement requirement = null;
        if (payload.getTargetId() != null || payload.getRequirementId() != null) {
            validateTargetAndRequirement(userId, payload.getTargetId(), payload.getRequirementId());
        }
        if (payload.getTargetId() != null) {
            target = targetMapper.selectById(payload.getTargetId());
        }
        if (payload.getRequirementId() != null) {
            requirement = requirementMapper.selectById(payload.getRequirementId());
            if (target == null && requirement != null) {
                target = targetMapper.selectById(requirement.getTargetId());
            }
        }

        TaskDraftContext context = TaskDraftContext.builder()
                .sourceType(StringUtils.hasText(payload.getSourceType()) ? payload.getSourceType() : "MANUAL")
                .seedTitle(payload.getTitle())
                .seedDescription(payload.getDescription())
                .targetType(target == null ? null : target.getTargetType())
                .targetTitle(target == null ? null : target.getTitle())
                .targetDescription(target == null ? null : target.getDescription())
                .requirementName(requirement == null ? null : requirement.getReqName())
                .requirementType(requirement == null ? null : requirement.getReqType())
                .requirementStatus(requirement == null ? null : requirement.getStatus())
                .requirementDescription(requirement == null ? null : requirement.getDescription())
                .build();

        try {
            return toDraftView("SUCCESS", aiService.generateTaskDraft(context), context);
        } catch (Exception e) {
            log.warn("AI task draft fallback: userId={}, targetId={}, requirementId={}, reason={}",
                    userId, payload.getTargetId(), payload.getRequirementId(), e.getMessage());
            return toDraftView("FALLBACK", fallbackDraft(context), context);
        }
    }

    @Override
    public List<TaskView> list(Long userId, String statusFilter, Long targetIdFilter, Long requirementIdFilter) {
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
        if (requirementIdFilter != null) {
            q.eq(GrowthTask::getRequirementId, requirementIdFilter);
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
        return rows.stream().map(this::toView).toList();
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
            String oldStatus = t.getStatus();
            if ("DONE".equals(newStatus)) {
                applyCompletionEvidence(t, payload);
            }
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
            log.info("task status changed: taskId={}, {} -> {}", taskId, oldStatus, newStatus);
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

    private TaskDraftResult fallbackDraft(TaskDraftContext context) {
        String baseTitle = firstText(context.getSeedTitle(), context.getRequirementName(), "推进一个成长任务");
        String target = firstText(context.getTargetTitle(), "当前目标");
        String requirement = firstText(context.getRequirementName(), "当前要求");
        return TaskDraftResult.builder()
                .title(baseTitle.startsWith("推进") ? baseTitle : "推进：" + baseTitle)
                .description("""
                        围绕「%s」中的「%s」做一次可观察推进。
                        请先明确本次要产出的最小成果，再用打卡记录过程，完成时补充证据。
                        """.formatted(target, requirement).trim())
                .priority("MEDIUM")
                .dueDate(LocalDate.now().plusDays(7))
                .plannedEffortMinutes(180)
                .acceptanceCriteria(List.of(
                        "形成一份可查看的学习/实践产出",
                        "至少完成 1 次打卡并记录投入时间",
                        "完成时填写证据或复盘说明"))
                .checkInPlan(List.of(
                        "第 1 次打卡：拆出本任务的最小可交付成果",
                        "中间打卡：记录实际推进内容、卡点和下一步",
                        "完成前：对照验收标准补齐证据"))
                .evidenceSuggestions(List.of("提交链接、截图、笔记、代码记录或不少于 50 字复盘"))
                .build();
    }

    private TaskDraftView toDraftView(String aiStatus, TaskDraftResult result, TaskDraftContext context) {
        String title = firstText(result.getTitle(), context.getSeedTitle(), context.getRequirementName(), "成长推进任务");
        List<String> criteria = defaultIfEmpty(result.getAcceptanceCriteria(), List.of("完成后能给出明确证据"));
        List<String> checkIns = defaultIfEmpty(result.getCheckInPlan(), List.of("至少完成一次打卡，记录实际推进内容"));
        List<String> evidence = defaultIfEmpty(result.getEvidenceSuggestions(), List.of("完成说明、截图、链接、代码或笔记"));
        String description = firstText(result.getDescription(), context.getSeedDescription(), "");
        String structured = appendGuidance(description, criteria, checkIns, evidence);
        return TaskDraftView.builder()
                .aiStatus(aiStatus)
                .title(limit(title, 255))
                .description(limit(structured, 2000))
                .priority(resolvePriority(result.getPriority()))
                .dueDate(result.getDueDate() == null ? LocalDate.now().plusDays(7) : result.getDueDate())
                .plannedEffortMinutes(result.getPlannedEffortMinutes() == null ? 180 : result.getPlannedEffortMinutes())
                .acceptanceCriteria(criteria)
                .checkInPlan(checkIns)
                .evidenceSuggestions(evidence)
                .build();
    }

    private String appendGuidance(String description, List<String> criteria, List<String> checkIns, List<String> evidence) {
        StringBuilder sb = new StringBuilder(StringUtils.hasText(description) ? description.trim() : "按下列标准推进并验收。");
        sb.append("\n\n验收标准：");
        appendList(sb, criteria);
        sb.append("\n\n建议打卡计划：");
        appendList(sb, checkIns);
        sb.append("\n\n完成证据建议：");
        appendList(sb, evidence);
        return sb.toString();
    }

    private void appendList(StringBuilder sb, List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            sb.append("\n").append(i + 1).append(". ").append(list.get(i));
        }
    }

    private void applyCompletionEvidence(GrowthTask t, UpdateTaskStatusRequest payload) {
        String evidence = payload.getCompletionEvidence();
        boolean hasEvidence = StringUtils.hasText(evidence);
        int checkInCount = t.getCheckInCount() == null ? 0 : t.getCheckInCount();
        if (!hasEvidence && checkInCount <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "完成任务前至少需要一次打卡或填写完成证据");
        }
        if (hasEvidence) {
            String current = t.getDescription() == null ? "" : t.getDescription().trim();
            String addition = "\n\n完成证据（%s）：\n%s".formatted(LocalDate.now(), evidence.trim());
            t.setDescription(limit((current + addition).trim(), 2000));
        }
        if (payload.getEffortMinutes() != null && payload.getEffortMinutes() > 0) {
            int current = t.getActualEffortMinutes() == null ? 0 : t.getActualEffortMinutes();
            t.setActualEffortMinutes(current + payload.getEffortMinutes());
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return "";
    }

    private List<String> defaultIfEmpty(List<String> values, List<String> fallback) {
        return values == null || values.isEmpty() ? fallback : values;
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    private TaskView toView(GrowthTask t) {
        List<String> dates = parseDateList(t.getCheckInDates());
        Set<String> set = new HashSet<>(dates);
        boolean checkedToday = set.contains(LocalDate.now().toString());
        GrowthTarget target = t.getTargetId() == null ? null : targetMapper.selectById(t.getTargetId());
        TargetRequirement requirement = t.getRequirementId() == null ? null : requirementMapper.selectById(t.getRequirementId());
        return TaskView.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .targetId(t.getTargetId())
                .requirementId(t.getRequirementId())
                .targetTitle(target == null ? null : target.getTitle())
                .requirementName(requirement == null ? null : requirement.getReqName())
                .requirementStatus(requirement == null ? null : requirement.getStatus())
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
