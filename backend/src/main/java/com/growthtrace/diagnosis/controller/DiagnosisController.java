package com.growthtrace.diagnosis.controller;

import com.growthtrace.common.result.PageResult;
import com.growthtrace.common.result.R;
import com.growthtrace.common.security.SecurityUserDetails;
import com.growthtrace.diagnosis.dto.TriggerDiagnosisRequest;
import com.growthtrace.diagnosis.dto.UpdateReviewNotesRequest;
import com.growthtrace.diagnosis.service.DiagnosisService;
import com.growthtrace.diagnosis.vo.DiagnosisSummary;
import com.growthtrace.diagnosis.vo.DiagnosisView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping("/trigger")
    public R<DiagnosisView> trigger(@Valid @RequestBody(required = false) TriggerDiagnosisRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(diagnosisService.trigger(userId, payload == null ? new TriggerDiagnosisRequest() : payload));
    }

    @GetMapping("/history")
    public R<PageResult<DiagnosisSummary>> history(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(diagnosisService.listHistory(userId, page, size));
    }

    @GetMapping("/{id}")
    public R<DiagnosisView> detail(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(diagnosisService.get(userId, id));
    }

    @PutMapping("/{id}/review")
    public R<DiagnosisView> updateReview(@PathVariable("id") Long id,
                                         @Valid @RequestBody UpdateReviewNotesRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(diagnosisService.updateReview(userId, id, payload));
    }
}
