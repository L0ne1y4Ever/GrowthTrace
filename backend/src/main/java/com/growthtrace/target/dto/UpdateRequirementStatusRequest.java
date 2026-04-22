package com.growthtrace.target.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateRequirementStatusRequest {

    @NotBlank
    @Pattern(regexp = "TODO|IN_PROGRESS|MET")
    private String status;

    /** 可选：不传则按 status 自动派生（TODO≤10 / IN_PROGRESS≥50 / MET=100）。 */
    private Integer progress;
}
