package com.growthtrace.profile.service.impl;

import com.growthtrace.ai.dto.ProfileExtractResult;
import com.growthtrace.ai.service.AiService;
import com.growthtrace.profile.mapper.GrowthProfileMapper;
import com.growthtrace.profile.mapper.ProfileExperienceMapper;
import com.growthtrace.profile.mapper.ProfileSkillMapper;
import com.growthtrace.profile.service.ProfileCompletenessCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock private AiService aiService;
    @Mock private GrowthProfileMapper profileMapper;
    @Mock private ProfileSkillMapper skillMapper;
    @Mock private ProfileExperienceMapper experienceMapper;
    @Mock private ProfileCompletenessCalculator completenessCalculator;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Test
    void returnsAiDraftFromExtractDraft() {
        ProfileExtractResult expected = ProfileExtractResult.builder()
                .summary("summary")
                .strengths(List.of("Java"))
                .weaknesses(List.of("前端"))
                .skills(List.of(Map.of("name", "Spring Boot", "level", "INTERMEDIATE")))
                .experiences(List.of())
                .completenessHint(80)
                .build();
        when(aiService.extractProfile("hello")).thenReturn(expected);

        ProfileExtractResult actual = profileService.extractDraft("hello");

        assertThat(actual).isEqualTo(expected);
    }
}
