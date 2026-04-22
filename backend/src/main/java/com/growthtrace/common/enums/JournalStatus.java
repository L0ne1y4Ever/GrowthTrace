package com.growthtrace.common.enums;

/**
 * 成长随记原文生命周期。
 *  - POSTED：用户已提交入档，进入成长记录流水（默认值）；
 *  - ARCHIVED：用户主动归档隐藏，数据保留但不参与默认列表/诊断窗口。
 *
 * 故意避开 ACTIVE —— ACTIVE 已用于 sys_user.status 与 profile_skill.status，
 * 语义是"对象当前在用"，而 journal 的语义是"这条原文处于哪个生命阶段"，应当区分。
 * 抽取确认态见 ExtractionStatus，不挂在 journal.status 上。
 */
public enum JournalStatus {
    POSTED,
    ARCHIVED
}
