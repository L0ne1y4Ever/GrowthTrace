package com.growthtrace.target.controller;

import com.growthtrace.common.result.R;
import com.growthtrace.common.security.SecurityUserDetails;
import com.growthtrace.target.dto.CreateTargetRequest;
import com.growthtrace.target.dto.RequirementPayload;
import com.growthtrace.target.dto.UpdateRequirementStatusRequest;
import com.growthtrace.target.dto.UpdateTargetRequest;
import com.growthtrace.target.dto.UpdateTargetStatusRequest;
import com.growthtrace.target.service.TargetService;
import com.growthtrace.target.vo.RequirementView;
import com.growthtrace.target.vo.TargetDetailView;
import com.growthtrace.target.vo.TargetTemplateVO;
import com.growthtrace.target.vo.TargetView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/target")
@RequiredArgsConstructor
public class TargetController {

    private final TargetService targetService;

    // -------- templates --------

    @GetMapping("/templates")
    public R<List<TargetTemplateVO>> templates() {
        return R.ok(targetService.listTemplates());
    }

    // -------- targets --------

    @PostMapping
    public R<TargetDetailView> create(@Valid @RequestBody CreateTargetRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(targetService.create(userId, payload));
    }

    @GetMapping
    public R<List<TargetView>> list(@RequestParam(value = "status", required = false) String status) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(targetService.listTargets(userId, status));
    }

    @GetMapping("/{id}")
    public R<TargetDetailView> detail(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(targetService.getDetail(userId, id));
    }

    @PutMapping("/{id}")
    public R<TargetView> update(@PathVariable("id") Long id,
                                @Valid @RequestBody UpdateTargetRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(targetService.update(userId, id, payload));
    }

    @PutMapping("/{id}/status")
    public R<TargetView> updateStatus(@PathVariable("id") Long id,
                                      @Valid @RequestBody UpdateTargetStatusRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(targetService.updateStatus(userId, id, payload));
    }

    @PostMapping("/{id}/primary")
    public R<TargetView> setPrimary(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(targetService.setPrimary(userId, id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        targetService.remove(userId, id);
        return R.ok();
    }

    // -------- requirements --------

    @PostMapping("/{id}/requirements")
    public R<RequirementView> addRequirement(@PathVariable("id") Long id,
                                             @Valid @RequestBody RequirementPayload payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(targetService.addRequirement(userId, id, payload));
    }

    @PutMapping("/{id}/requirements/{reqId}")
    public R<RequirementView> updateRequirement(@PathVariable("id") Long id,
                                                @PathVariable("reqId") Long reqId,
                                                @Valid @RequestBody RequirementPayload payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(targetService.updateRequirement(userId, id, reqId, payload));
    }

    @PutMapping("/{id}/requirements/{reqId}/status")
    public R<RequirementView> updateRequirementStatus(@PathVariable("id") Long id,
                                                      @PathVariable("reqId") Long reqId,
                                                      @Valid @RequestBody UpdateRequirementStatusRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(targetService.updateRequirementStatus(userId, id, reqId, payload));
    }

    @DeleteMapping("/{id}/requirements/{reqId}")
    public R<Void> deleteRequirement(@PathVariable("id") Long id,
                                     @PathVariable("reqId") Long reqId) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        targetService.removeRequirement(userId, id, reqId);
        return R.ok();
    }
}
