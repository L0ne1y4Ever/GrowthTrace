package com.growthtrace.execution.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.growthtrace.ai.service.AiService;
import com.growthtrace.common.exception.BusinessException;
import com.growthtrace.execution.dto.UpdateTaskStatusRequest;
import com.growthtrace.execution.entity.GrowthTask;
import com.growthtrace.execution.mapper.GrowthTaskMapper;
import com.growthtrace.execution.vo.TaskView;
import com.growthtrace.target.entity.GrowthTarget;
import com.growthtrace.target.entity.TargetRequirement;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private GrowthTaskMapper taskMapper;
    @Mock private GrowthTargetMapper targetMapper;
    @Mock private TargetRequirementMapper requirementMapper;
    @Mock private AiService aiService;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void returnsTargetAndRequirementContextInTaskView() {
        GrowthTask task = new GrowthTask();
        task.setId(10L);
        task.setUserId(1L);
        task.setTargetId(2L);
        task.setRequirementId(3L);
        task.setTitle("推进 Redis");
        task.setStatus("TODO");
        task.setPriority("MEDIUM");
        task.setCheckInDates("[]");
        task.setCheckInCount(0);
        task.setActualEffortMinutes(0);

        GrowthTarget target = new GrowthTarget();
        target.setId(2L);
        target.setTitle("后端求职主线");

        TargetRequirement requirement = new TargetRequirement();
        requirement.setId(3L);
        requirement.setReqName("补齐缓存与并发基础");
        requirement.setStatus("IN_PROGRESS");

        when(taskMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(task)));
        when(targetMapper.selectById(2L)).thenReturn(target);
        when(requirementMapper.selectById(3L)).thenReturn(requirement);

        List<TaskView> result = taskService.list(1L, null, null, 3L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTargetTitle()).isEqualTo("后端求职主线");
        assertThat(result.getFirst().getRequirementName()).isEqualTo("补齐缓存与并发基础");
        assertThat(result.getFirst().getRequirementStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void appliesRequirementFilterWhenListingTasks() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                GrowthTask.class);
        when(taskMapper.selectList(any())).thenReturn(new ArrayList<>());

        taskService.list(1L, null, null, 42L);

        ArgumentCaptor<LambdaQueryWrapper<GrowthTask>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(taskMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("requirement_id");
    }

    @Test
    void rejectsDoneWithoutCheckInOrEvidence() {
        GrowthTask task = baseOwnedTask();
        task.setCheckInCount(0);
        when(taskMapper.selectById(10L)).thenReturn(task);

        UpdateTaskStatusRequest payload = new UpdateTaskStatusRequest();
        payload.setStatus("DONE");

        assertThatThrownBy(() -> taskService.updateStatus(1L, 10L, payload))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少需要一次打卡或填写完成证据");
    }

    @Test
    void appendsCompletionEvidenceWhenMarkingDone() {
        GrowthTask task = baseOwnedTask();
        task.setDescription("原任务说明");
        when(taskMapper.selectById(10L)).thenReturn(task);

        UpdateTaskStatusRequest payload = new UpdateTaskStatusRequest();
        payload.setStatus("DONE");
        payload.setCompletionEvidence("已提交代码并完成复盘");
        payload.setEffortMinutes(45);

        TaskView result = taskService.updateStatus(1L, 10L, payload);

        verify(taskMapper).updateById(task);
        assertThat(task.getDescription()).contains("完成证据").contains("已提交代码并完成复盘");
        assertThat(task.getActualEffortMinutes()).isEqualTo(45);
        assertThat(result.getStatus()).isEqualTo("DONE");
    }

    private GrowthTask baseOwnedTask() {
        GrowthTask task = new GrowthTask();
        task.setId(10L);
        task.setUserId(1L);
        task.setTitle("任务");
        task.setStatus("TODO");
        task.setPriority("MEDIUM");
        task.setCheckInDates("[]");
        task.setCheckInCount(0);
        task.setActualEffortMinutes(0);
        return task;
    }
}
