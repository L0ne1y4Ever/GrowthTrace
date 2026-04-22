package com.growthtrace.execution.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "title 不能为空")
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    /** HIGH / MEDIUM / LOW；默认 MEDIUM。 */
    @Pattern(regexp = "HIGH|MEDIUM|LOW|", message = "priority 非法")
    private String priority;

    private LocalDate dueDate;

    /** 可选：任务关联的目标（必须属于当前用户）。 */
    private Long targetId;

    /** 可选：任务关联的目标要求（必须属于 targetId）。 */
    private Long requirementId;

    /** 计划投入分钟数；可选。 */
    @Min(0)
    @Max(100000)
    private Integer plannedEffortMinutes;
}
