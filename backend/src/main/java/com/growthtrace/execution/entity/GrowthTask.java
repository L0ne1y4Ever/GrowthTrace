package com.growthtrace.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("growth_task")
public class GrowthTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long targetId;
    private Long requirementId;
    private String title;
    private String description;
    private String status;           // TODO / IN_PROGRESS / DONE / ABANDONED
    private String priority;         // HIGH / MEDIUM / LOW
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private String checkInDates;     // JSON 数组字符串
    private Integer checkInCount;
    private Integer plannedEffortMinutes;
    private Integer actualEffortMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
