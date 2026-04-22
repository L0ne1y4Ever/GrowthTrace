package com.growthtrace.dashboard.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成长曲线节点，来源于 growth_snapshot；每个节点对应一次快照时间点的 completeness + 关键度量。
 */
@Data
@Builder
public class GrowthCurvePoint {
    private Long snapshotId;
    private LocalDateTime snapshotTime;
    private LocalDate date;
    private Integer profileVersion;
    private Integer completeness;
    private Double taskCompletionRate;
    private Integer newSkillsCount;
}
