package com.growthtrace.diagnosis.vo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻复盘视图，反序列化自 stage_assessment.review_notes。
 * 字段缺失时取空值，不抛错。
 */
@Data
@Builder
public class ReviewNotesView {

    private List<String> wins;
    private List<String> learnings;
    private List<String> nextFocus;
    private String userFreeform;

    public static ReviewNotesView empty() {
        return ReviewNotesView.builder()
                .wins(new ArrayList<>())
                .learnings(new ArrayList<>())
                .nextFocus(new ArrayList<>())
                .userFreeform(null)
                .build();
    }
}
