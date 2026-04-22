package com.growthtrace.journal.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户确认随记事件抽取草稿（可能已编辑）。
 * confirmed_* 字段与 schema 列对齐，会被完整写入 journal_extraction.confirmed_*。
 * new_skills 中的每一条在确认后会落地到 profile_skill（source=JOURNAL）。
 * related_requirements 中的 newStatus 会更新 target_requirement.status（必须归当前用户）。
 */
@Data
public class ConfirmExtractionRequest {

    @Valid
    private List<NewSkillConfirm> newSkills = new ArrayList<>();

    @Valid
    private List<RequirementUpdateConfirm> relatedRequirements = new ArrayList<>();

    /** 事件对象形如 {type, title, description, outcome}，保留为 JSON 透传。 */
    private List<Map<String, Object>> events = new ArrayList<>();

    private List<String> blockers = new ArrayList<>();
}
