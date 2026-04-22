package com.growthtrace.dashboard.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Dashboard 最近随记摘要（最多 5 条）。
 */
@Data
@Builder
public class JournalDigest {
    private Long id;
    private String contentExcerpt;
    private String mood;
    private Integer wordCount;
    private LocalDateTime createdAt;
    /** 该随记当前的抽取状态；null 表示从未抽取过。 */
    private String extractionStatus;
}
