package com.growthtrace.target.service;

import com.growthtrace.target.dto.CreateTargetRequest;
import com.growthtrace.target.dto.RequirementPayload;
import com.growthtrace.target.dto.UpdateRequirementStatusRequest;
import com.growthtrace.target.dto.UpdateTargetRequest;
import com.growthtrace.target.dto.UpdateTargetStatusRequest;
import com.growthtrace.target.vo.RequirementView;
import com.growthtrace.target.vo.TargetDetailView;
import com.growthtrace.target.vo.TargetTemplateVO;
import com.growthtrace.target.vo.TargetView;

import java.util.List;

public interface TargetService {

    // ------ templates ------
    List<TargetTemplateVO> listTemplates();

    // ------ targets ------
    TargetDetailView create(Long userId, CreateTargetRequest payload);

    List<TargetView> listTargets(Long userId, String statusFilter);

    TargetDetailView getDetail(Long userId, Long targetId);

    TargetView update(Long userId, Long targetId, UpdateTargetRequest payload);

    TargetView updateStatus(Long userId, Long targetId, UpdateTargetStatusRequest payload);

    void remove(Long userId, Long targetId);

    TargetView setPrimary(Long userId, Long targetId);

    // ------ requirements ------
    RequirementView addRequirement(Long userId, Long targetId, RequirementPayload payload);

    RequirementView updateRequirement(Long userId, Long targetId, Long reqId, RequirementPayload payload);

    RequirementView updateRequirementStatus(Long userId, Long targetId, Long reqId, UpdateRequirementStatusRequest payload);

    void removeRequirement(Long userId, Long targetId, Long reqId);
}
