-- ============================================================================
-- GrowthTrace · V1 数据库 Schema
-- 计算机专业学生成长跟踪与阶段诊断平台
--
-- Target: MySQL 8.0+ (CHECK 约束需要 8.0.16+)
-- Charset: utf8mb4 / utf8mb4_unicode_ci
-- Engine:  InnoDB
--
-- 使用：
--   1) CREATE DATABASE growthtrace DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   2) USE growthtrace;
--   3) SOURCE sql/schema.sql;
--
-- 约定：
--   - 逻辑删除字段 is_deleted TINYINT NOT NULL DEFAULT 0，与 MyBatis-Plus @TableLogic 对齐
--   - 审计字段 created_at / updated_at 由 MySQL 默认维护
--   - 外键不使用物理 FK，仅保留 xxx_id 字段，引用一致性由应用层保证
--   - 枚举字段使用 VARCHAR(32) + CHECK 约束，不使用 MySQL ENUM 类型
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 1. sys_user —— 用户账号
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT        COMMENT '用户ID',
    username        VARCHAR(64)     NOT NULL                       COMMENT '登录用户名',
    password_hash   VARCHAR(100)    NOT NULL                       COMMENT 'BCrypt 密码哈希',
    email           VARCHAR(128)    NULL                           COMMENT '邮箱',
    nickname        VARCHAR(64)     NULL                           COMMENT '昵称',
    avatar_url      VARCHAR(255)    NULL                           COMMENT '头像URL',
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE'      COMMENT '状态：ACTIVE / LOCKED / DISABLED',
    last_login_at   DATETIME        NULL                           COMMENT '最近登录时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_email (email),
    KEY idx_sys_user_status (status),
    CONSTRAINT ck_sys_user_status CHECK (status IN ('ACTIVE','LOCKED','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号';

-- ----------------------------------------------------------------------------
-- 2. growth_profile —— 成长档案（版本化，一人一行，UPDATE in place）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS growth_profile;
CREATE TABLE growth_profile (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT UNSIGNED NOT NULL                  COMMENT '所属用户ID',
    version                 INT             NOT NULL DEFAULT 1        COMMENT '画像版本号，每次用户确认更新 +1',
    self_intro              TEXT            NULL                      COMMENT '用户自我介绍原文',
    strengths               JSON            NULL                      COMMENT '擅长点数组',
    weaknesses              JSON            NULL                      COMMENT '不擅长点数组',
    summary                 TEXT            NULL                      COMMENT 'AI 或用户确认后的画像总结',
    completeness            TINYINT UNSIGNED NOT NULL DEFAULT 0       COMMENT '画像完整度 0–100，由 ProfileCompletenessCalculator 统一写入',
    raw_onboarding_text     TEXT            NULL                      COMMENT '建档原始自由文本',
    source                  VARCHAR(32)     NOT NULL DEFAULT 'ONBOARDING' COMMENT 'ONBOARDING / DIAGNOSIS_UPDATED / MANUAL_EDIT',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted              TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_growth_profile_user (user_id),
    KEY idx_growth_profile_user_version (user_id, version),
    CONSTRAINT ck_growth_profile_source       CHECK (source IN ('ONBOARDING','DIAGNOSIS_UPDATED','MANUAL_EDIT')),
    CONSTRAINT ck_growth_profile_completeness CHECK (completeness BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长档案主表';

-- ----------------------------------------------------------------------------
-- 3. profile_skill —— 技能明细（归属 user_id）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS profile_skill;
CREATE TABLE profile_skill (
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id                     BIGINT UNSIGNED NOT NULL,
    skill_name                  VARCHAR(128)    NOT NULL                COMMENT '技能名称',
    skill_level                 VARCHAR(32)     NOT NULL DEFAULT 'BEGINNER' COMMENT 'BEGINNER / INTERMEDIATE / ADVANCED',
    category                    VARCHAR(64)     NULL                    COMMENT 'LANGUAGE / FRAMEWORK / TOOL / DOMAIN / SOFT',
    added_in_profile_version    INT             NOT NULL DEFAULT 1,
    source                      VARCHAR(32)     NOT NULL DEFAULT 'ONBOARDING' COMMENT 'ONBOARDING / JOURNAL / DIAGNOSIS / MANUAL',
    status                      VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / ARCHIVED',
    evidence                    TEXT            NULL,
    created_at                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted                  TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_profile_skill_user_name (user_id, skill_name),
    KEY idx_profile_skill_user_status (user_id, status),
    CONSTRAINT ck_profile_skill_level  CHECK (skill_level IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    CONSTRAINT ck_profile_skill_source CHECK (source IN ('ONBOARDING','JOURNAL','DIAGNOSIS','MANUAL')),
    CONSTRAINT ck_profile_skill_status CHECK (status IN ('ACTIVE','ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能明细';

-- ----------------------------------------------------------------------------
-- 4. profile_experience —— 经历（实习/项目/竞赛/课程/科研等）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS profile_experience;
CREATE TABLE profile_experience (
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id                     BIGINT UNSIGNED NOT NULL,
    exp_type                    VARCHAR(32)     NOT NULL                COMMENT 'INTERNSHIP / PROJECT / AWARD / COURSE / RESEARCH / OTHER',
    title                       VARCHAR(255)    NOT NULL,
    description                 TEXT            NULL,
    role                        VARCHAR(128)    NULL,
    outcome                     TEXT            NULL,
    start_date                  DATE            NULL,
    end_date                    DATE            NULL,
    added_in_profile_version    INT             NOT NULL DEFAULT 1,
    source                      VARCHAR(32)     NOT NULL DEFAULT 'ONBOARDING',
    created_at                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted                  TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_profile_exp_user (user_id),
    KEY idx_profile_exp_user_type (user_id, exp_type),
    CONSTRAINT ck_profile_exp_type   CHECK (exp_type IN ('INTERNSHIP','PROJECT','AWARD','COURSE','RESEARCH','OTHER')),
    CONSTRAINT ck_profile_exp_source CHECK (source IN ('ONBOARDING','JOURNAL','DIAGNOSIS','MANUAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='经历明细';

-- ----------------------------------------------------------------------------
-- 5. growth_target —— 目标（V1 三类 + is_primary 主目标）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS growth_target;
CREATE TABLE growth_target (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    target_type     VARCHAR(32)     NOT NULL                COMMENT 'JOB_SEEKING / POSTGRAD / SKILL_GROWTH',
    title           VARCHAR(255)    NOT NULL,
    description     TEXT            NULL,
    template_key    VARCHAR(64)     NULL                    COMMENT '使用的模板标识，null=纯手动',
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / ACHIEVED / ABANDONED',
    deadline        DATE            NULL,
    achieved_at     DATETIME        NULL,
    is_primary      TINYINT         NOT NULL DEFAULT 0      COMMENT '是否当前主目标；每用户至多一条 is_primary=1，由 TargetService 事务保证',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_growth_target_user (user_id),
    KEY idx_growth_target_user_status (user_id, status),
    KEY idx_growth_target_user_type (user_id, target_type),
    KEY idx_growth_target_user_primary (user_id, is_primary),
    CONSTRAINT ck_growth_target_type    CHECK (target_type IN ('JOB_SEEKING','POSTGRAD','SKILL_GROWTH')),
    CONSTRAINT ck_growth_target_status  CHECK (status IN ('ACTIVE','ACHIEVED','ABANDONED')),
    CONSTRAINT ck_growth_target_primary CHECK (is_primary IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长目标';

-- ----------------------------------------------------------------------------
-- 6. target_requirement —— 目标要求（TODO / IN_PROGRESS / MET）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS target_requirement;
CREATE TABLE target_requirement (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    target_id               BIGINT UNSIGNED NOT NULL,
    req_name                VARCHAR(255)    NOT NULL,
    req_type                VARCHAR(32)     NOT NULL                COMMENT 'SKILL / KNOWLEDGE / EXPERIENCE / OTHER',
    description             TEXT            NULL,
    status                  VARCHAR(32)     NOT NULL DEFAULT 'TODO' COMMENT 'TODO / IN_PROGRESS / MET',
    sort_order              INT             NOT NULL DEFAULT 0,
    due_date                DATE            NULL,
    linked_skill_id         BIGINT UNSIGNED NULL,
    linked_experience_id    BIGINT UNSIGNED NULL,
    progress                TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '进度百分比 0–100',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted              TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_target_req_target (target_id),
    KEY idx_target_req_target_status (target_id, status),
    KEY idx_target_req_type (req_type),
    CONSTRAINT ck_target_req_type     CHECK (req_type IN ('SKILL','KNOWLEDGE','EXPERIENCE','OTHER')),
    CONSTRAINT ck_target_req_status   CHECK (status IN ('TODO','IN_PROGRESS','MET')),
    CONSTRAINT ck_target_req_progress CHECK (progress BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目标要求明细';

-- ----------------------------------------------------------------------------
-- 7. growth_journal —— 成长随记（原文生命周期 POSTED / ARCHIVED）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS growth_journal;
CREATE TABLE growth_journal (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    content         TEXT            NOT NULL                COMMENT '随记原文',
    mood            VARCHAR(32)     NULL                    COMMENT 'GREAT / GOOD / NORMAL / BAD / BLOCKED',
    tags            JSON            NULL                    COMMENT '用户自填标签数组',
    word_count      INT             NOT NULL DEFAULT 0,
    status          VARCHAR(32)     NOT NULL DEFAULT 'POSTED' COMMENT '原文生命周期：POSTED / ARCHIVED（不使用 ACTIVE，与其他表 ACTIVE 语义区分）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_growth_journal_user_created (user_id, created_at),
    KEY idx_growth_journal_user_status (user_id, status),
    CONSTRAINT ck_growth_journal_status CHECK (status IN ('POSTED','ARCHIVED')),
    CONSTRAINT ck_growth_journal_mood   CHECK (mood IS NULL OR mood IN ('GREAT','GOOD','NORMAL','BAD','BLOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长随记原文';

-- ----------------------------------------------------------------------------
-- 8. journal_extraction —— AI 事件抽取草稿/归档（1:1 journal）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS journal_extraction;
CREATE TABLE journal_extraction (
    id                                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    journal_id                          BIGINT UNSIGNED NOT NULL,
    user_id                             BIGINT UNSIGNED NOT NULL,
    extraction_status                   VARCHAR(32)     NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT 'PENDING_CONFIRM / CONFIRMED / DISCARDED',
    ai_raw_response                     JSON            NULL                COMMENT 'AI 原始 JSON 响应',
    draft_new_skills                    JSON            NULL,
    draft_related_requirements          JSON            NULL,
    draft_events                        JSON            NULL,
    draft_blockers                      JSON            NULL,
    confirmed_new_skills                JSON            NULL,
    confirmed_related_requirements      JSON            NULL,
    confirmed_events                    JSON            NULL,
    confirmed_blockers                  JSON            NULL,
    confirmed_at                        DATETIME        NULL,
    created_at                          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted                          TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_journal_extraction_journal (journal_id),
    KEY idx_journal_extraction_user_status (user_id, extraction_status),
    CONSTRAINT ck_journal_extraction_status CHECK (extraction_status IN ('PENDING_CONFIRM','CONFIRMED','DISCARDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随记 AI 事件抽取';

-- ----------------------------------------------------------------------------
-- 9. stage_assessment —— 阶段诊断（含轻复盘 review_notes；单向指向 snapshot 已删除）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS stage_assessment;
CREATE TABLE stage_assessment (
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id                     BIGINT UNSIGNED NOT NULL,
    trigger_time                DATETIME        NOT NULL,
    window_start                DATETIME        NOT NULL,
    window_end                  DATETIME        NOT NULL,
    profile_version_at_trigger  INT             NOT NULL,
    metrics                     JSON            NOT NULL                COMMENT '本地规则底座 7 指标',
    ai_raw_response             JSON            NULL,
    stage_summary               TEXT            NULL,
    key_problems                JSON            NULL,
    suggestions                 JSON            NULL,
    correction_directions       JSON            NULL,
    review_notes                JSON            NULL                    COMMENT '轻复盘：{wins, learnings, next_focus, user_freeform}',
    ai_status                   VARCHAR(32)     NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS / FALLBACK / FAILED',
    created_at                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted                  TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_stage_assessment_user_time (user_id, created_at),
    CONSTRAINT ck_stage_assessment_ai_status CHECK (ai_status IN ('SUCCESS','FALLBACK','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阶段诊断（含轻复盘）';

-- ----------------------------------------------------------------------------
-- 10. growth_task —— 成长任务（TODO / IN_PROGRESS / DONE / ABANDONED）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS growth_task;
CREATE TABLE growth_task (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT UNSIGNED NOT NULL,
    target_id               BIGINT UNSIGNED NULL,
    requirement_id          BIGINT UNSIGNED NULL,
    title                   VARCHAR(255)    NOT NULL,
    description             TEXT            NULL,
    status                  VARCHAR(32)     NOT NULL DEFAULT 'TODO' COMMENT 'TODO / IN_PROGRESS / DONE / ABANDONED',
    priority                VARCHAR(32)     NOT NULL DEFAULT 'MEDIUM' COMMENT 'HIGH / MEDIUM / LOW',
    due_date                DATE            NULL,
    completed_at            DATETIME        NULL,
    check_in_dates          JSON            NULL                    COMMENT '打卡日期数组 ["2026-04-21", ...]',
    check_in_count          INT             NOT NULL DEFAULT 0,
    planned_effort_minutes  INT             NULL,
    actual_effort_minutes   INT             NOT NULL DEFAULT 0,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted              TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_growth_task_user_status (user_id, status),
    KEY idx_growth_task_user_due (user_id, due_date),
    KEY idx_growth_task_target (target_id),
    KEY idx_growth_task_requirement (requirement_id),
    CONSTRAINT ck_growth_task_status   CHECK (status IN ('TODO','IN_PROGRESS','DONE','ABANDONED')),
    CONSTRAINT ck_growth_task_priority CHECK (priority IN ('HIGH','MEDIUM','LOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长任务（执行核心）';

-- ----------------------------------------------------------------------------
-- 11. growth_snapshot —— 成长快照（画像冻结 + 指标冻结；指向 stage_assessment）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS growth_snapshot;
CREATE TABLE growth_snapshot (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT UNSIGNED NOT NULL,
    profile_version         INT             NOT NULL,
    snapshot_time           DATETIME        NOT NULL,
    profile_json            JSON            NOT NULL                COMMENT 'growth_profile 完整序列化',
    skills_snapshot         JSON            NULL,
    experiences_snapshot    JSON            NULL,
    targets_snapshot        JSON            NULL,
    metrics_snapshot        JSON            NULL                    COMMENT '7 指标（与 stage_assessment.metrics 同源）',
    trigger_source          VARCHAR(32)     NOT NULL DEFAULT 'DIAGNOSIS' COMMENT 'DIAGNOSIS / MANUAL',
    stage_assessment_id     BIGINT UNSIGNED NULL                    COMMENT '来源诊断（可为空；反向引用 stage_assessment 已不存在）',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted              TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_growth_snapshot_user_time (user_id, snapshot_time),
    KEY idx_growth_snapshot_assessment (stage_assessment_id),
    CONSTRAINT ck_growth_snapshot_source CHECK (trigger_source IN ('DIAGNOSIS','MANUAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长快照';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- END of schema.sql
-- 枚举速查：
--   sys_user.status              ACTIVE / LOCKED / DISABLED
--   growth_profile.source        ONBOARDING / DIAGNOSIS_UPDATED / MANUAL_EDIT
--   profile_skill.skill_level    BEGINNER / INTERMEDIATE / ADVANCED
--   profile_skill.source         ONBOARDING / JOURNAL / DIAGNOSIS / MANUAL
--   profile_skill.status         ACTIVE / ARCHIVED
--   profile_experience.exp_type  INTERNSHIP / PROJECT / AWARD / COURSE / RESEARCH / OTHER
--   growth_target.target_type    JOB_SEEKING / POSTGRAD / SKILL_GROWTH
--   growth_target.status         ACTIVE / ACHIEVED / ABANDONED
--   target_requirement.req_type  SKILL / KNOWLEDGE / EXPERIENCE / OTHER
--   target_requirement.status    TODO / IN_PROGRESS / MET
--   growth_journal.status        POSTED / ARCHIVED
--   growth_journal.mood          GREAT / GOOD / NORMAL / BAD / BLOCKED（可空）
--   journal_extraction.status    PENDING_CONFIRM / CONFIRMED / DISCARDED
--   stage_assessment.ai_status   SUCCESS / FALLBACK / FAILED
--   growth_task.status           TODO / IN_PROGRESS / DONE / ABANDONED
--   growth_task.priority         HIGH / MEDIUM / LOW
--   growth_snapshot.trigger      DIAGNOSIS / MANUAL
-- ============================================================================
