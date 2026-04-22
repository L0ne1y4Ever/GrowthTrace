package com.growthtrace.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewSkillConfirm {

    @NotBlank
    @Size(max = 128)
    private String name;

    @NotBlank
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED")
    private String level;

    @Pattern(regexp = "LANGUAGE|FRAMEWORK|TOOL|DOMAIN|SOFT|")
    private String category;

    @Size(max = 2000)
    private String evidence;
}
