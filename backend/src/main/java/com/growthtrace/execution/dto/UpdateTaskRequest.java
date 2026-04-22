package com.growthtrace.execution.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateTaskRequest {

    @NotBlank(message = "title 不能为空")
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    @Pattern(regexp = "HIGH|MEDIUM|LOW|", message = "priority 非法")
    private String priority;

    private LocalDate dueDate;

    private Long targetId;
    private Long requirementId;

    @Min(0)
    @Max(100000)
    private Integer plannedEffortMinutes;
}
