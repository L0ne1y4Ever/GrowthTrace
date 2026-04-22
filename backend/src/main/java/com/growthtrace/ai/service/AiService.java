package com.growthtrace.ai.service;

import com.growthtrace.ai.dto.DiagnosisSummaryContext;
import com.growthtrace.ai.dto.DiagnosisSummaryResult;
import com.growthtrace.ai.dto.JournalExtractContext;
import com.growthtrace.ai.dto.JournalExtractResult;
import com.growthtrace.ai.dto.ProfileExtractResult;

/**
 * 所有 AI 调用的统一入口。业务层只认这三个方法，不得直接访问 Prompt / Parser / Provider。
 * 三个场景一一对应系统里允许的 3 次 AI 预算。
 */
public interface AiService {

    ProfileExtractResult extractProfile(String onboardingText);

    JournalExtractResult extractJournal(JournalExtractContext context);

    DiagnosisSummaryResult summarizeDiagnosis(DiagnosisSummaryContext context);
}
