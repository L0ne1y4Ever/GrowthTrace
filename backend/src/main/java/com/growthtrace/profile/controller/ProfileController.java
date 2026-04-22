package com.growthtrace.profile.controller;

import com.growthtrace.common.result.R;
import com.growthtrace.common.security.SecurityUserDetails;
import com.growthtrace.profile.dto.ExperiencePayload;
import com.growthtrace.profile.dto.OnboardingConfirmRequest;
import com.growthtrace.profile.dto.OnboardingExtractRequest;
import com.growthtrace.profile.dto.SkillPayload;
import com.growthtrace.profile.service.ProfileService;
import com.growthtrace.profile.vo.ExperienceView;
import com.growthtrace.profile.vo.ProfileView;
import com.growthtrace.profile.vo.SkillView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // -------- 建档引导 --------

    @PostMapping("/onboarding/extract")
    public R<Object> extract(@Valid @RequestBody OnboardingExtractRequest request) {
        SecurityUserDetails.requireCurrentUserId();
        return R.ok(profileService.extractDraft(request.getRawText()));
    }

    @PostMapping("/onboarding/confirm")
    public R<Void> confirm(@Valid @RequestBody OnboardingConfirmRequest request) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        profileService.confirmOnboarding(userId, request);
        return R.ok();
    }

    // -------- 画像读取 / 完整度 --------

    @GetMapping
    public R<ProfileView> current() {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(profileService.getCurrent(userId));
    }

    @PostMapping("/completeness/refresh")
    public R<Integer> refreshCompleteness() {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(profileService.recalculateCompleteness(userId));
    }

    // -------- Skill CRUD --------

    @PostMapping("/skills")
    public R<SkillView> addSkill(@Valid @RequestBody SkillPayload payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(profileService.addSkill(userId, payload));
    }

    @PutMapping("/skills/{id}")
    public R<SkillView> updateSkill(@PathVariable("id") Long id,
                                    @Valid @RequestBody SkillPayload payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(profileService.updateSkill(userId, id, payload));
    }

    @DeleteMapping("/skills/{id}")
    public R<Void> deleteSkill(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        profileService.removeSkill(userId, id);
        return R.ok();
    }

    // -------- Experience CRUD --------

    @PostMapping("/experiences")
    public R<ExperienceView> addExperience(@Valid @RequestBody ExperiencePayload payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(profileService.addExperience(userId, payload));
    }

    @PutMapping("/experiences/{id}")
    public R<ExperienceView> updateExperience(@PathVariable("id") Long id,
                                              @Valid @RequestBody ExperiencePayload payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(profileService.updateExperience(userId, id, payload));
    }

    @DeleteMapping("/experiences/{id}")
    public R<Void> deleteExperience(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        profileService.removeExperience(userId, id);
        return R.ok();
    }
}
