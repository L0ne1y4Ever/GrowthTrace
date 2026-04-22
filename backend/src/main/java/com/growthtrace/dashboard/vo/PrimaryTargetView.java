package com.growthtrace.dashboard.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dashboard 当前主目标（ACTIVE + is_primary=1）。为空时不展示该卡片。
 */
@Data
@Builder
public class PrimaryTargetView {
    private Long id;
    private String targetType;
    private String title;
    private String description;
    private LocalDate deadline;
    private int requirementCount;
    private int requirementMetCount;
    private double metRatio;
    private LocalDateTime updatedAt;
}
