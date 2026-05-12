package com.growthtrace.target.service.impl;

import com.growthtrace.execution.entity.GrowthTask;
import com.growthtrace.execution.mapper.GrowthTaskMapper;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
import com.growthtrace.profile.service.ProfileCompletenessCalculator;
import com.growthtrace.target.entity.GrowthTarget;
import com.growthtrace.target.entity.TargetRequirement;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import com.growthtrace.target.template.TargetTemplateCatalog;
import com.growthtrace.target.vo.TargetDetailView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetServiceImplTest {

    @Mock private GrowthTargetMapper targetMapper;
    @Mock private TargetRequirementMapper requirementMapper;
    @Mock private TargetTemplateCatalog templateCatalog;
    @Mock private ProfileCompletenessCalculator completenessCalculator;
    @Mock private GrowthProfileMapper profileMapper;
    @Mock private GrowthTaskMapper taskMapper;

    @InjectMocks
    private TargetServiceImpl targetService;

    @Test
    void includesTaskCountsForRequirementsInTargetDetail() {
        GrowthTarget target = new GrowthTarget();
        target.setId(2L);
        target.setUserId(1L);
        target.setTargetType("JOB_SEEKING");
        target.setTitle("后端求职主线");
        target.setStatus("ACTIVE");
        target.setIsPrimary(1);

        TargetRequirement requirement = new TargetRequirement();
        requirement.setId(3L);
        requirement.setTargetId(2L);
        requirement.setReqName("补齐缓存与并发基础");
        requirement.setReqType("KNOWLEDGE");
        requirement.setStatus("IN_PROGRESS");
        requirement.setProgress(50);

        GrowthTask todo = task(100L, "TODO");
        GrowthTask inProgress = task(101L, "IN_PROGRESS");
        GrowthTask done = task(102L, "DONE");

        when(targetMapper.selectById(2L)).thenReturn(target);
        when(requirementMapper.selectList(any())).thenReturn(List.of(requirement));
        when(taskMapper.selectList(any())).thenReturn(List.of(todo, inProgress, done));

        TargetDetailView result = targetService.getDetail(1L, 2L);

        assertThat(result.getRequirements()).hasSize(1);
        assertThat(result.getRequirements().getFirst().getTaskCount()).isEqualTo(3);
        assertThat(result.getRequirements().getFirst().getActiveTaskCount()).isEqualTo(2);
    }

    private static GrowthTask task(Long id, String status) {
        GrowthTask task = new GrowthTask();
        task.setId(id);
        task.setStatus(status);
        return task;
    }
}
