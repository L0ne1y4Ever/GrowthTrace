package com.growthtrace.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnboardingExtractRequest {

    @NotBlank
    @Size(min = 10, max = 4000, message = "自述长度需在 10–4000 字之间")
    private String rawText;
}
