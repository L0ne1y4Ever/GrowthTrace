package com.growthtrace.target.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("target_requirement")
public class TargetRequirement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long targetId;
    private String reqName;
    private String reqType;
    private String description;
    private String status;
    private Integer sortOrder;
    private LocalDate dueDate;
    private Long linkedSkillId;
    private Long linkedExperienceId;
    private Integer progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
