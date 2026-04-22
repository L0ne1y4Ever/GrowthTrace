package com.growthtrace.profile.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProfileView {

    private Long id;
    private Long userId;
    private Integer version;
    private String selfIntro;
    private List<String> strengths;
    private List<String> weaknesses;
    private String summary;
    private Integer completeness;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<SkillView> skills;
    private List<ExperienceView> experiences;
}
