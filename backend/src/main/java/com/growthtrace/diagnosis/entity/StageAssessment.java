package com.growthtrace.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stage_assessment")
public class StageAssessment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private LocalDateTime triggerTime;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer profileVersionAtTrigger;

    private String metrics;                // JSON
    private String aiRawResponse;          // JSON
    private String stageSummary;
    private String keyProblems;            // JSON
    private String suggestions;            // JSON
    private String correctionDirections;   // JSON
    private String reviewNotes;            // JSON 轻复盘

    private String aiStatus;               // SUCCESS / FALLBACK / FAILED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
