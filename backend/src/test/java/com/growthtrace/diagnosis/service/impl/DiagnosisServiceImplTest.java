package com.growthtrace.diagnosis.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.growthtrace.ai.service.AiService;
import com.growthtrace.common.exception.AiException;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.diagnosis.dto.TriggerDiagnosisRequest;
import com.growthtrace.diagnosis.entity.GrowthSnapshot;
import com.growthtrace.diagnosis.entity.StageAssessment;
import com.growthtrace.diagnosis.mapper.GrowthSnapshotMapper;
import com.growthtrace.diagnosis.mapper.StageAssessmentMapper;
import com.growthtrace.diagnosis.service.DiagnosisMetricsService;
import com.growthtrace.diagnosis.service.SnapshotService;
import com.growthtrace.diagnosis.vo.DiagnosisView;
import com.growthtrace.execution.mapper.GrowthTaskMapper;
import com.growthtrace.journal.mapper.GrowthJournalMapper;
import com.growthtrace.journal.mapper.JournalExtractionMapper;
import com.growthtrace.profile.entity.GrowthProfile;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
import com.growthtrace.profile.service.ProfileCompletenessCalculator;
import com.growthtrace.target.mapper.GrowthTargetMapper;
import com.growthtrace.target.mapper.TargetRequirementMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisServiceImplTest {

    @Mock private DiagnosisMetricsService metricsService;
    @Mock private SnapshotService snapshotService;
    @Mock private AiService aiService;
    @Mock private ProfileCompletenessCalculator completenessCalculator;
    @Mock private StageAssessmentMapper assessmentMapper;
    @Mock private GrowthSnapshotMapper snapshotMapper;
    @Mock private GrowthProfileMapper profileMapper;
    @Mock private GrowthTargetMapper targetMapper;
    @Mock private TargetRequirementMapper requirementMapper;
    @Mock private GrowthJournalMapper journalMapper;
    @Mock private JournalExtractionMapper extractionMapper;
    @Mock private GrowthTaskMapper taskMapper;

    @InjectMocks
    private DiagnosisServiceImpl diagnosisService;

    @Test
    void writesFailedAiStatusAndReturnsMetricsWhenAiSummaryFails() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                GrowthProfile.class);

        GrowthProfile profile = new GrowthProfile();
        profile.setId(1L);
        profile.setUserId(1L);
        profile.setVersion(2);
        profile.setCompleteness(70);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("journal_count", 3);
        metrics.put("task_completion_rate", 0.5);

        when(profileMapper.selectOne(any())).thenReturn(profile);
        when(completenessCalculator.calculate(1L)).thenReturn(88);
        when(metricsService.computeMetrics(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(metrics);
        when(targetMapper.selectList(any())).thenReturn(List.of());
        when(extractionMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(journalMapper.selectList(any())).thenReturn(List.of());
        when(aiService.summarizeDiagnosis(any())).thenThrow(new AiException(ResultCode.AI_TIMEOUT, "AI 调用超时"));
        doAnswer(invocation -> {
            StageAssessment row = invocation.getArgument(0);
            row.setId(99L);
            return 1;
        }).when(assessmentMapper).insert(any(StageAssessment.class));
        when(snapshotService.takeSnapshot(anyLong(), anyLong(), any(), any())).thenAnswer(invocation -> {
            GrowthSnapshot snapshot = new GrowthSnapshot();
            snapshot.setId(123L);
            snapshot.setStageAssessmentId(invocation.getArgument(1));
            return snapshot;
        });

        DiagnosisView result = diagnosisService.trigger(1L, new TriggerDiagnosisRequest());

        assertThat(result.getAiStatus()).isEqualTo("FAILED");
        assertThat(result.getMetrics()).containsEntry("journal_count", 3);
        assertThat(result.getSnapshotId()).isEqualTo(123L);
    }
}
