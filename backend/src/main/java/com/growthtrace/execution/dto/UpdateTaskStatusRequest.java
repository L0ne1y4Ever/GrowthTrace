package com.growthtrace.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {

    @NotBlank
    @Pattern(regexp = "TODO|IN_PROGRESS|DONE|ABANDONED", message = "status 非法")
    private String status;
}
