package com.growthtrace.execution.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenerateTaskDraftRequest {

    @Pattern(regexp = "REQUIREMENT|DIAGNOSIS_SUGGESTION|DIAGNOSIS_CORRECTION|MANUAL|", message = "sourceType 非法")
    private String sourceType;

    private Long targetId;
    private Long requirementId;

    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;
}
