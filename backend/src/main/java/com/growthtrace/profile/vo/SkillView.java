package com.growthtrace.profile.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SkillView {

    private Long id;
    private String skillName;
    private String skillLevel;
    private String category;
    private Integer addedInProfileVersion;
    private String source;
    private String status;
    private String evidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
