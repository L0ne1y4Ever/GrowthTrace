package com.growthtrace.target.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateTargetStatusRequest {

    @NotBlank
    @Pattern(regexp = "ACTIVE|ACHIEVED|ABANDONED",
             message = "status 必须是 ACTIVE / ACHIEVED / ABANDONED")
    private String status;
}
