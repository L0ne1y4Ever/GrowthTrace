package com.growthtrace.target.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RequirementPayload {

    @NotBlank
    @Size(max = 255)
    private String reqName;

    @NotBlank
    @Pattern(regexp = "SKILL|KNOWLEDGE|EXPERIENCE|OTHER",
             message = "reqType 必须是 SKILL / KNOWLEDGE / EXPERIENCE / OTHER")
    private String reqType;

    @Size(max = 2000)
    private String description;

    private Integer sortOrder;

    private LocalDate dueDate;

    /** 仅在 PUT /requirements/{id} 时可选传入；POST 时默认 TODO。 */
    @Pattern(regexp = "TODO|IN_PROGRESS|MET|")
    private String status;

    @Min(0)
    @Max(100)
    private Integer progress;

    private Long linkedSkillId;

    private Long linkedExperienceId;
}
