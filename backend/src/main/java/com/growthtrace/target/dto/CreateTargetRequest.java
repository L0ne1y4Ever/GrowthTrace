package com.growthtrace.target.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreateTargetRequest {

    @NotBlank
    @Pattern(regexp = "JOB_SEEKING|POSTGRAD|SKILL_GROWTH",
             message = "targetType 必须是 JOB_SEEKING / POSTGRAD / SKILL_GROWTH")
    private String targetType;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    /** 若基于模板创建；纯手动时留空。 */
    @Size(max = 64)
    private String templateKey;

    private LocalDate deadline;

    /** 若 true，创建后会把其他目标的 is_primary 清零。 */
    private Boolean isPrimary = false;

    @Valid
    private List<RequirementPayload> requirements = new ArrayList<>();
}
