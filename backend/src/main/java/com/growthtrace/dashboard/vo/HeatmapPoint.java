package com.growthtrace.dashboard.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 热力图单元（按天）。score 是 journal + task check-in + snapshot 的综合活动强度。
 */
@Data
@Builder
public class HeatmapPoint {
    private LocalDate date;
    private int score;
}
