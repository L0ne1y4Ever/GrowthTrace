package com.growthtrace.journal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("growth_journal")
public class GrowthJournal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String content;
    private String mood;
    private String tags;             // JSON 数组字符串
    private Integer wordCount;
    private String status;           // POSTED / ARCHIVED（原文生命周期，抽取态见 journal_extraction）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
