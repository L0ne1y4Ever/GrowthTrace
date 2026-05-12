package com.growthtrace.execution.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TaskDraftView {

    private String aiStatus;
    private String title;
    private String description;
    private String priority;
    private LocalDate dueDate;
    private Integer plannedEffortMinutes;
    private List<String> acceptanceCriteria;
    private List<String> checkInPlan;
    private List<String> evidenceSuggestions;
}
