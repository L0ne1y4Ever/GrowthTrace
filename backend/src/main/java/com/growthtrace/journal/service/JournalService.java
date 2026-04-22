package com.growthtrace.journal.service;

import com.growthtrace.common.result.PageResult;
import com.growthtrace.journal.dto.CreateJournalRequest;
import com.growthtrace.journal.dto.UpdateJournalRequest;
import com.growthtrace.journal.vo.JournalDetailView;
import com.growthtrace.journal.vo.JournalSummary;
import com.growthtrace.journal.vo.JournalView;

public interface JournalService {

    JournalView create(Long userId, CreateJournalRequest payload);

    PageResult<JournalSummary> list(Long userId, String statusFilter, int page, int size);

    JournalDetailView getDetail(Long userId, Long journalId);

    JournalView update(Long userId, Long journalId, UpdateJournalRequest payload);
}
