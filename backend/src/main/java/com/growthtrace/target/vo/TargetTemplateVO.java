package com.growthtrace.target.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 发给前端的模板视图（与 TargetTemplateCatalog 解耦，保证 JSON 结构稳定）。 */
@Data
@Builder
public class TargetTemplateVO {

    private String templateKey;
    private String targetType;
    private String title;
    private String description;
    private List<RequirementTemplateVO> defaultRequirements;

    @Data
    @Builder
    public static class RequirementTemplateVO {
        private String reqName;
        private String reqType;
        private String description;
    }
}
