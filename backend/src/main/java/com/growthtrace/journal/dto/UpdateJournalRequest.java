package com.growthtrace.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateJournalRequest {

    @NotBlank
    @Size(min = 10, max = 10000)
    private String content;

    @Pattern(regexp = "GREAT|GOOD|NORMAL|BAD|BLOCKED|")
    private String mood;

    private List<@Size(max = 32) String> tags = new ArrayList<>();

    /** POSTED / ARCHIVED；不传视为保持原状。 */
    @Pattern(regexp = "POSTED|ARCHIVED|")
    private String status;
}
