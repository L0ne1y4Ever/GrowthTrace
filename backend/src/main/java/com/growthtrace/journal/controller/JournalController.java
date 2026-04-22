package com.growthtrace.journal.controller;

import com.growthtrace.common.result.PageResult;
import com.growthtrace.common.result.R;
import com.growthtrace.common.security.SecurityUserDetails;
import com.growthtrace.journal.dto.ConfirmExtractionRequest;
import com.growthtrace.journal.dto.CreateJournalRequest;
import com.growthtrace.journal.dto.UpdateJournalRequest;
import com.growthtrace.journal.service.ExtractionService;
import com.growthtrace.journal.service.JournalService;
import com.growthtrace.journal.vo.ExtractionView;
import com.growthtrace.journal.vo.JournalDetailView;
import com.growthtrace.journal.vo.JournalSummary;
import com.growthtrace.journal.vo.JournalView;
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
@RequestMapping("/journal")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;
    private final ExtractionService extractionService;

    @PostMapping
    public R<JournalView> create(@Valid @RequestBody CreateJournalRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(journalService.create(userId, payload));
    }

    @GetMapping
    public R<PageResult<JournalSummary>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(journalService.list(userId, status, page, size));
    }

    @GetMapping("/{id}")
    public R<JournalDetailView> detail(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(journalService.getDetail(userId, id));
    }

    @PutMapping("/{id}")
    public R<JournalView> update(@PathVariable("id") Long id,
                                 @Valid @RequestBody UpdateJournalRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(journalService.update(userId, id, payload));
    }

    @PostMapping("/{id}/extract")
    public R<ExtractionView> extract(@PathVariable("id") Long id) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(extractionService.extractDraft(userId, id));
    }

    @PostMapping("/{id}/extract/confirm")
    public R<ExtractionView> confirmExtraction(@PathVariable("id") Long id,
                                               @Valid @RequestBody ConfirmExtractionRequest payload) {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        return R.ok(extractionService.confirm(userId, id, payload));
    }
}
