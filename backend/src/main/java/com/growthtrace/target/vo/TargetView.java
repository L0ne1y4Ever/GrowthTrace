package com.growthtrace.target.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TargetView {

    private Long id;
    private Long userId;
    private String targetType;
    private String title;
    private String description;
    private String templateKey;
    private String status;
    private LocalDate deadline;
    private LocalDateTime achievedAt;
    private Boolean isPrimary;
    private Integer requirementCount;
    private Integer requirementMetCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
