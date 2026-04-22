package com.growthtrace.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("profile_skill")
public class ProfileSkill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String skillName;
    private String skillLevel;
    private String category;
    private Integer addedInProfileVersion;
    private String source;
    private String status;
    private String evidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
