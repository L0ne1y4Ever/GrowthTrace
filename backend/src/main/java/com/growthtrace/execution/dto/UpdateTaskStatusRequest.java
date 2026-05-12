package com.growthtrace.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {

    @NotBlank
    @Pattern(regexp = "TODO|IN_PROGRESS|DONE|ABANDONED", message = "status 非法")
    private String status;

    @Size(max = 2000)
    private String completionEvidence;

    @Min(0)
    @Max(100000)
    private Integer effortMinutes;
}
