package com.growthtrace.ai.service.impl;

import com.growthtrace.ai.adapter.AiProvider;
import com.growthtrace.ai.dto.DiagnosisSummaryContext;
import com.growthtrace.ai.dto.DiagnosisSummaryResult;
import com.growthtrace.ai.dto.JournalExtractContext;
import com.growthtrace.ai.dto.JournalExtractResult;
import com.growthtrace.ai.dto.ProfileExtractResult;
import com.growthtrace.ai.parser.DiagnosisSummaryParser;
import com.growthtrace.ai.parser.JournalExtractParser;
import com.growthtrace.ai.parser.ProfileExtractParser;
import com.growthtrace.ai.prompt.DiagnosisSummaryPrompt;
import com.growthtrace.ai.prompt.JournalExtractPrompt;
import com.growthtrace.ai.prompt.ProfileExtractPrompt;
import com.growthtrace.ai.service.AiService;
import com.growthtrace.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiProvider provider;
    private final AiProperties aiProperties;

    private final ProfileExtractPrompt profilePrompt;
    private final JournalExtractPrompt journalPrompt;
    private final DiagnosisSummaryPrompt diagnosisPrompt;

    private final ProfileExtractParser profileParser;
    private final JournalExtractParser journalParser;
    private final DiagnosisSummaryParser diagnosisParser;

    @Override
    public ProfileExtractResult extractProfile(String onboardingText) {
        String prompt = profilePrompt.build(onboardingText);
        String raw = provider.chat(prompt, timeout());
        log.debug("AI#PROFILE_EXTRACT raw length={}", raw.length());
        return profileParser.parse(raw);
    }

    @Override
    public JournalExtractResult extractJournal(JournalExtractContext context) {
        String prompt = journalPrompt.build(context);
        String raw = provider.chat(prompt, timeout());
        log.debug("AI#JOURNAL_EXTRACT raw length={}", raw.length());
        return journalParser.parse(raw);
    }

    @Override
    public DiagnosisSummaryResult summarizeDiagnosis(DiagnosisSummaryContext context) {
        String prompt = diagnosisPrompt.build(context);
        String raw = provider.chat(prompt, timeout());
        log.debug("AI#DIAGNOSIS_SUMMARY raw length={}", raw.length());
        return diagnosisParser.parse(raw);
    }

    private Duration timeout() {
        return Duration.ofSeconds(aiProperties.timeoutSecondsOrDefault());
    }
}
