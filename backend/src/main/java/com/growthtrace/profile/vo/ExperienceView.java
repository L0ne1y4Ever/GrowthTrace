package com.growthtrace.profile.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ExperienceView {

    private Long id;
    private String expType;
    private String title;
    private String description;
    private String role;
    private String outcome;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer addedInProfileVersion;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
