package com.growthtrace.execution.service;

import com.growthtrace.execution.dto.CheckInRequest;
import com.growthtrace.execution.dto.CreateTaskRequest;
import com.growthtrace.execution.dto.UpdateTaskRequest;
import com.growthtrace.execution.dto.UpdateTaskStatusRequest;
import com.growthtrace.execution.vo.TaskView;
import com.growthtrace.execution.vo.WeeklyProgressView;

import java.util.List;

/**
 * 成长任务服务。V1 以 growth_task 为执行核心，不引入 growth_plan。
 * 打卡 = check_in_dates JSON 数组 append + counter +1 +（可选）effort 累加。
 */
public interface TaskService {

    TaskView create(Long userId, CreateTaskRequest payload);

    /**
     * 列出任务；statusFilter 为 null/空时返回 TODO/IN_PROGRESS/DONE（默认隐藏 ABANDONED，UI 用专门标签打开）。
     * targetIdFilter 可选。
     */
    List<TaskView> list(Long userId, String statusFilter, Long targetIdFilter);

    TaskView get(Long userId, Long taskId);

    TaskView update(Long userId, Long taskId, UpdateTaskRequest payload);

    TaskView updateStatus(Long userId, Long taskId, UpdateTaskStatusRequest payload);

    void remove(Long userId, Long taskId);

    /**
     * 打卡：同一天重复打卡返回最新视图但不叠加计数；effortMinutes 仍然累加。
     */
    TaskView checkIn(Long userId, Long taskId, CheckInRequest payload);

    WeeklyProgressView weeklyProgress(Long userId);
}
