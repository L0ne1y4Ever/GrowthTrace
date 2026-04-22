package com.growthtrace.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SkillPayload {

    @NotBlank
    @Size(max = 128)
    private String skillName;

    @NotBlank
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED",
             message = "skillLevel 必须是 BEGINNER / INTERMEDIATE / ADVANCED")
    private String skillLevel;

    /** LANGUAGE / FRAMEWORK / TOOL / DOMAIN / SOFT；可空，兜底 DOMAIN。 */
    @Pattern(regexp = "LANGUAGE|FRAMEWORK|TOOL|DOMAIN|SOFT|",
             message = "category 非法")
    private String category;

    @Size(max = 2000)
    private String evidence;
}
