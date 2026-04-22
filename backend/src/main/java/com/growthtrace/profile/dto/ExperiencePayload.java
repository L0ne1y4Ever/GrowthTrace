package com.growthtrace.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExperiencePayload {

    @NotBlank
    @Pattern(regexp = "INTERNSHIP|PROJECT|AWARD|COURSE|RESEARCH|OTHER",
             message = "expType 枚举非法")
    private String expType;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 128)
    private String role;

    @Size(max = 2000)
    private String description;

    @Size(max = 2000)
    private String outcome;

    private LocalDate startDate;

    private LocalDate endDate;
}
