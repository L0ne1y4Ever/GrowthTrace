package com.growthtrace.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("profile_experience")
public class ProfileExperience {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String expType;
    private String title;
    private String description;
    private String role;
    private String outcome;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer addedInProfileVersion;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
