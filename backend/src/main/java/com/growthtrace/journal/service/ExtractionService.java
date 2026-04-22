package com.growthtrace.journal.service;

import com.growthtrace.journal.dto.ConfirmExtractionRequest;
import com.growthtrace.journal.vo.ExtractionView;

public interface ExtractionService {

    /** AI 抽取草稿。若 extraction 不存在则创建 PENDING_CONFIRM；若 PENDING_CONFIRM 则覆盖 draft_*；若 CONFIRMED 则拒绝。 */
    ExtractionView extractDraft(Long userId, Long journalId);

    /** 用户确认草稿。落地 confirmed_* + 把新技能写 profile_skill + 按需更新 target_requirement.status；事务。 */
    ExtractionView confirm(Long userId, Long journalId, ConfirmExtractionRequest payload);
}
