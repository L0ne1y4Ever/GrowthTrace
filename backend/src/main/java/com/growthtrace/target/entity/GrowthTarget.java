package com.growthtrace.target.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("growth_target")
public class GrowthTarget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String targetType;
    private String title;
    private String description;
    private String templateKey;
    private String status;
    private LocalDate deadline;
    private LocalDateTime achievedAt;
    private Integer isPrimary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
