package com.growthtrace.execution.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskView {

    private Long id;
    private Long userId;
    private Long targetId;
    private Long requirementId;
    private String title;
    private String description;

    /** TODO / IN_PROGRESS / DONE / ABANDONED */
    private String status;
    /** HIGH / MEDIUM / LOW */
    private String priority;

    private LocalDate dueDate;
    private LocalDateTime completedAt;

    private List<String> checkInDates;
    private Integer checkInCount;

    private Integer plannedEffortMinutes;
    private Integer actualEffortMinutes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 是否已经在今天打过卡（便于前端禁用按钮）。 */
    private boolean checkedInToday;
}
