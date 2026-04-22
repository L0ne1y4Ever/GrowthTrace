package com.growthtrace.diagnosis.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻复盘编辑载荷。对应 stage_assessment.review_notes JSON：
 *   {wins:[], learnings:[], nextFocus:[], userFreeform:""}
 * 与 Diagnosis 的时间维度绑定，不独立成 Review 模块。
 */
@Data
public class UpdateReviewNotesRequest {

    private List<@Size(max = 200) String> wins = new ArrayList<>();

    private List<@Size(max = 200) String> learnings = new ArrayList<>();

    private List<@Size(max = 200) String> nextFocus = new ArrayList<>();

    @Size(max = 2000, message = "自由复盘不超过 2000 字")
    private String userFreeform;
}
