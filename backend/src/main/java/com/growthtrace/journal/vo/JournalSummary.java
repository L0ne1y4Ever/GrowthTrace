package com.growthtrace.journal.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 列表里的摘要；content 截断到 200 字以内。 */
@Data
@Builder
public class JournalSummary {

    private Long id;
    private String contentExcerpt;
    private String mood;
    private List<String> tags;
    private Integer wordCount;
    private String status;
    private String extractionStatus;    // null / PENDING_CONFIRM / CONFIRMED / DISCARDED
    private LocalDateTime createdAt;
}
