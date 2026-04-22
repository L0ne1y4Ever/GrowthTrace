package com.growthtrace.target.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RequirementView {

    private Long id;
    private Long targetId;
    private String reqName;
    private String reqType;
    private String description;
    private String status;
    private Integer sortOrder;
    private LocalDate dueDate;
    private Long linkedSkillId;
    private Long linkedExperienceId;
    private Integer progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
