package com.growthtrace.diagnosis.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TriggerDiagnosisRequest {

    /** 诊断回看窗口天数；null 或 <=0 走默认 30 天；上限 365。 */
    @Min(value = 1, message = "windowDays 至少 1 天")
    @Max(value = 365, message = "windowDays 最多 365 天")
    private Integer windowDays;
}
