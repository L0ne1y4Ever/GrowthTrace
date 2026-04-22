package com.growthtrace.journal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequirementUpdateConfirm {

    @NotNull
    private Long requirementId;

    /** TODO / IN_PROGRESS / MET；不传则忽略此条。 */
    @Pattern(regexp = "TODO|IN_PROGRESS|MET|")
    private String newStatus;

    @Size(max = 2000)
    private String evidence;
}
