package com.growthtrace.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("growth_snapshot")
public class GrowthSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer profileVersion;
    private LocalDateTime snapshotTime;

    private String profileJson;            // JSON
    private String skillsSnapshot;         // JSON
    private String experiencesSnapshot;    // JSON
    private String targetsSnapshot;        // JSON
    private String metricsSnapshot;        // JSON

    private String triggerSource;          // DIAGNOSIS / MANUAL
    private Long stageAssessmentId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
