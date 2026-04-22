package com.growthtrace.journal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.growthtrace.common.exception.BusinessException;
import com.growthtrace.common.result.PageResult;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.common.util.JsonUtils;
import com.growthtrace.journal.dto.CreateJournalRequest;
import com.growthtrace.journal.dto.UpdateJournalRequest;
import com.growthtrace.journal.entity.GrowthJournal;
import com.growthtrace.journal.entity.JournalExtraction;
import com.growthtrace.journal.mapper.GrowthJournalMapper;
import com.growthtrace.journal.mapper.JournalExtractionMapper;
import com.growthtrace.journal.service.JournalService;
import com.growthtrace.journal.vo.ExtractionView;
import com.growthtrace.journal.vo.JournalDetailView;
import com.growthtrace.journal.vo.JournalSummary;
import com.growthtrace.journal.vo.JournalView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private static final int EXCERPT_MAX = 200;

    private final GrowthJournalMapper journalMapper;
    private final JournalExtractionMapper extractionMapper;

    @Override
    @Transactional
    public JournalView create(Long userId, CreateJournalRequest payload) {
        GrowthJournal j = new GrowthJournal();
        j.setUserId(userId);
        j.setContent(payload.getContent().trim());
        j.setMood(StringUtils.hasText(payload.getMood()) ? payload.getMood() : null);
        j.setTags(JsonUtils.toJson(payload.getTags() == null ? List.of() : payload.getTags()));
        j.setWordCount(j.getContent().length());
        j.setStatus("POSTED");
        journalMapper.insert(j);
        return toView(j);
    }

    @Override
    public PageResult<JournalSummary> list(Long userId, String statusFilter, int page, int size) {
        int pageNo = Math.max(1, page);
        int pageSize = Math.min(Math.max(1, size), 50);

        LambdaQueryWrapper<GrowthJournal> q = new LambdaQueryWrapper<GrowthJournal>()
                .eq(GrowthJournal::getUserId, userId)
                .orderByDesc(GrowthJournal::getCreatedAt);
        if (StringUtils.hasText(statusFilter)) {
            q.eq(GrowthJournal::getStatus, statusFilter);
        }

        IPage<GrowthJournal> mpPage = journalMapper.selectPage(new Page<>(pageNo, pageSize), q);
        List<GrowthJournal> records = mpPage.getRecords();

        Map<Long, String> extractionStatusById = loadExtractionStatuses(records);

        List<JournalSummary> summaries = records.stream()
                .map(j -> JournalSummary.builder()
                        .id(j.getId())
                        .contentExcerpt(excerpt(j.getContent()))
                        .mood(j.getMood())
                        .tags(parseStringList(j.getTags()))
                        .wordCount(j.getWordCount())
                        .status(j.getStatus())
                        .extractionStatus(extractionStatusById.get(j.getId()))
                        .createdAt(j.getCreatedAt())
                        .build())
                .toList();

        return PageResult.of(summaries, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize());
    }

    @Override
    public JournalDetailView getDetail(Long userId, Long journalId) {
        GrowthJournal j = requireOwned(userId, journalId);
        JournalExtraction ex = extractionMapper.selectOne(new LambdaQueryWrapper<JournalExtraction>()
                .eq(JournalExtraction::getJournalId, journalId));
        return JournalDetailView.builder()
                .journal(toView(j))
                .extraction(ex == null ? null : toExtractionView(ex))
                .build();
    }

    @Override
    @Transactional
    public JournalView update(Long userId, Long journalId, UpdateJournalRequest payload) {
        GrowthJournal j = requireOwned(userId, journalId);
        j.setContent(payload.getContent().trim());
        j.setMood(StringUtils.hasText(payload.getMood()) ? payload.getMood() : null);
        j.setTags(JsonUtils.toJson(payload.getTags() == null ? List.of() : payload.getTags()));
        j.setWordCount(j.getContent().length());
        if (StringUtils.hasText(payload.getStatus())) {
            j.setStatus(payload.getStatus());
        }
        journalMapper.updateById(j);
        return toView(j);
    }

    // ----------------- helpers -----------------

    private GrowthJournal requireOwned(Long userId, Long journalId) {
        GrowthJournal j = journalMapper.selectById(journalId);
        if (j == null || !userId.equals(j.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "随记不存在或不属于当前用户");
        }
        return j;
    }

    private Map<Long, String> loadExtractionStatuses(List<GrowthJournal> records) {
        if (records.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> ids = records.stream().map(GrowthJournal::getId).toList();
        List<JournalExtraction> extractions = extractionMapper.selectList(new LambdaQueryWrapper<JournalExtraction>()
                .in(JournalExtraction::getJournalId, ids));
        return extractions.stream().collect(Collectors.toMap(
                JournalExtraction::getJournalId,
                JournalExtraction::getExtractionStatus,
                (a, b) -> a
        ));
    }

    private static String excerpt(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return content.length() <= EXCERPT_MAX ? content : content.substring(0, EXCERPT_MAX) + "…";
    }

    private static JournalView toView(GrowthJournal j) {
        return JournalView.builder()
                .id(j.getId())
                .userId(j.getUserId())
                .content(j.getContent())
                .mood(j.getMood())
                .tags(parseStringList(j.getTags()))
                .wordCount(j.getWordCount())
                .status(j.getStatus())
                .createdAt(j.getCreatedAt())
                .updatedAt(j.getUpdatedAt())
                .build();
    }

    static ExtractionView toExtractionView(JournalExtraction ex) {
        return ExtractionView.builder()
                .id(ex.getId())
                .journalId(ex.getJournalId())
                .extractionStatus(ex.getExtractionStatus())
                .draftNewSkills(parseMapList(ex.getDraftNewSkills()))
                .draftRelatedRequirements(parseMapList(ex.getDraftRelatedRequirements()))
                .draftEvents(parseMapList(ex.getDraftEvents()))
                .draftBlockers(parseStringList(ex.getDraftBlockers()))
                .confirmedNewSkills(parseMapList(ex.getConfirmedNewSkills()))
                .confirmedRelatedRequirements(parseMapList(ex.getConfirmedRelatedRequirements()))
                .confirmedEvents(parseMapList(ex.getConfirmedEvents()))
                .confirmedBlockers(parseStringList(ex.getConfirmedBlockers()))
                .confirmedAt(ex.getConfirmedAt())
                .createdAt(ex.getCreatedAt())
                .updatedAt(ex.getUpdatedAt())
                .build();
    }

    private static List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static List<Map<String, Object>> parseMapList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> list = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
