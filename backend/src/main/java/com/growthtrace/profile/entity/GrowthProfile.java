package com.growthtrace.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "growth_profile", autoResultMap = true)
public class GrowthProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer version;
    private String selfIntro;
    private String strengths;        // JSON 数组字符串，读取时反序列化
    private String weaknesses;       // JSON 数组字符串
    private String summary;
    private Integer completeness;
    private String rawOnboardingText;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
