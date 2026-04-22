package com.growthtrace.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateJournalRequest {

    @NotBlank
    @Size(min = 10, max = 10000, message = "随记长度需在 10–10000 字之间")
    private String content;

    /** 可选：GREAT / GOOD / NORMAL / BAD / BLOCKED。 */
    @Pattern(regexp = "GREAT|GOOD|NORMAL|BAD|BLOCKED|",
             message = "mood 非法")
    private String mood;

    /** 用户自填的标签数组。 */
    private List<@Size(max = 32) String> tags = new ArrayList<>();
}
