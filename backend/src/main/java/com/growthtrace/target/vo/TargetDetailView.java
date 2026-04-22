package com.growthtrace.target.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TargetDetailView {

    private TargetView target;
    private List<RequirementView> requirements;
}
