package com.growthtrace.journal.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JournalView {

    private Long id;
    private Long userId;
    private String content;
    private String mood;
    private List<String> tags;
    private Integer wordCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
