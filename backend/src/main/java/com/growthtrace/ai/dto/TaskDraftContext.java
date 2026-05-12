package com.growthtrace.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskDraftContext {

    private String sourceType;
    private String seedTitle;
    private String seedDescription;

    private String targetType;
    private String targetTitle;
    private String targetDescription;

    private String requirementName;
    private String requirementType;
    private String requirementStatus;
    private String requirementDescription;
}
