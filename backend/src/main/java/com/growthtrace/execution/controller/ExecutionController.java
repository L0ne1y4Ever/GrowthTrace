package com.growthtrace.execution.controller;

import com.growthtrace.common.result.R;
import com.growthtrace.common.security.SecurityUserDetails;
import com.growthtrace.execution.dto.CheckInRequest;
import com.growthtrace.execution.dto.CreateTaskRequest;
import com.growthtrace.execution.dto.UpdateTaskRequest;
import com.growthtrace.execution.dto.UpdateTaskStatusRequest;
import com.growthtrace.execution.service.TaskService;
import com.growthtrace.execution.vo.TaskView;
import com.growthtrace.execution.vo.WeeklyProgressView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/execution")
@RequiredArgsConstructor
public class ExecutionController {

    private final TaskService taskService;

    @PostMapping("/task")
    public R<TaskView> create(@Valid @RequestBody CreateTaskRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(taskService.create(userId, payload));
    }

    @GetMapping("/task")
    public R<List<TaskView>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "targetId", required = false) Long targetId) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(taskService.list(userId, status, targetId));
    }

    @GetMapping("/task/{id}")
    public R<TaskView> detail(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(taskService.get(userId, id));
    }

    @PutMapping("/task/{id}")
    public R<TaskView> update(@PathVariable("id") Long id,
                              @Valid @RequestBody UpdateTaskRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(taskService.update(userId, id, payload));
    }

    @PutMapping("/task/{id}/status")
    public R<TaskView> updateStatus(@PathVariable("id") Long id,
                                    @Valid @RequestBody UpdateTaskStatusRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(taskService.updateStatus(userId, id, payload));
    }

    @PostMapping("/task/{id}/check-in")
    public R<TaskView> checkIn(@PathVariable("id") Long id,
                               @Valid @RequestBody(required = false) CheckInRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(taskService.checkIn(userId, id, payload == null ? new CheckInRequest() : payload));
    }

    @DeleteMapping("/task/{id}")
    public R<Void> delete(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        taskService.remove(userId, id);
        return R.ok();
    }

    @GetMapping("/weekly-progress")
    public R<WeeklyProgressView> weeklyProgress() {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(taskService.weeklyProgress(userId));
    }
}
