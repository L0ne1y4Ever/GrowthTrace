package com.growthtrace.journal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("journal_extraction")
public class JournalExtraction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long journalId;
    private Long userId;
    private String extractionStatus;       // PENDING_CONFIRM / CONFIRMED / DISCARDED

    private String aiRawResponse;          // JSON
    private String draftNewSkills;         // JSON
    private String draftRelatedRequirements;
    private String draftEvents;
    private String draftBlockers;

    private String confirmedNewSkills;
    private String confirmedRelatedRequirements;
    private String confirmedEvents;
    private String confirmedBlockers;

    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
