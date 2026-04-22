package com.growthtrace.execution.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CheckInRequest {

    /** 打卡日期；null 时默认今天。 */
    private LocalDate date;

    /** 本次投入分钟数；可选，叠加到 actual_effort_minutes。 */
    @Min(0)
    @Max(100000)
    private Integer effortMinutes;
}
