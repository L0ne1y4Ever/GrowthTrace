-- ============================================================================
-- GrowthTrace · V1 种子数据（demo 用户）
--
-- 目的：
--   - 冒烟验证 backend 能连通 schema；
--   - 提供答辩 demo 的"有数据"初始态，避免 dashboard 空屏；
--   - 前端联调无需先走 onboarding/journal/diagnosis 三条主链。
--
-- 使用（在 schema.sql 之后执行）：
--     USE growthtrace;
--     SOURCE sql/seed.sql;
--
-- 幂等：顶部先按显式 id 清除 demo 用户及其关联行，再 INSERT，可重复运行。
--
-- demo 账号：
--     username : demo
--     password : password       （BCrypt 10 轮哈希，Spring Security 文档公开 fixture）
--   若密码不匹配（某些环境 BCrypt 行为差异）请注册新用户 POST /api/auth/register。
--
-- 约定与 schema.sql 保持一致；所有枚举严格对齐 schema CHECK 约束。
-- ============================================================================

SET NAMES utf8mb4;

-- ----------------------------------------------------------------------------
-- 0. 幂等清理：仅清 demo 用户 (user_id=1) 的痕迹，不触碰其他数据
-- ----------------------------------------------------------------------------
DELETE FROM growth_snapshot     WHERE user_id = 1;
DELETE FROM stage_assessment    WHERE user_id = 1;
DELETE FROM growth_task         WHERE user_id = 1;
DELETE FROM journal_extraction  WHERE user_id = 1;
DELETE FROM growth_journal      WHERE user_id = 1;
DELETE FROM target_requirement  WHERE target_id IN (SELECT id FROM growth_target WHERE user_id = 1);
DELETE FROM growth_target       WHERE user_id = 1;
DELETE FROM profile_experience  WHERE user_id = 1;
DELETE FROM profile_skill       WHERE user_id = 1;
DELETE FROM growth_profile      WHERE user_id = 1;
DELETE FROM sys_user            WHERE id      = 1;

-- ----------------------------------------------------------------------------
-- 1. sys_user —— demo 用户
-- ----------------------------------------------------------------------------
INSERT INTO sys_user (id, username, password_hash, email, nickname, avatar_url, status, last_login_at, created_at, updated_at)
VALUES (
    1,
    'demo',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',   -- BCrypt("password")
    'demo@growthtrace.example',
    '演示用户',
    NULL,
    'ACTIVE',
    '2026-04-20 22:10:00',
    '2026-02-01 09:00:00',
    '2026-04-20 22:10:00'
);

-- ----------------------------------------------------------------------------
-- 2. growth_profile —— 成长档案（version=1；completeness 依公式=90）
--    公式（10+10+5+5 + skill档20 + exp档20 + target10 + req10 = 90）
-- ----------------------------------------------------------------------------
INSERT INTO growth_profile (
    id, user_id, version, self_intro, strengths, weaknesses, summary,
    completeness, raw_onboarding_text, source, created_at, updated_at
) VALUES (
    1, 1, 1,
    '大三计算机专业学生，Java 后端方向，正在准备 2026 秋招。平时刷算法、做课程项目，喜欢把踩过的坑写成复盘笔记。',
    JSON_ARRAY('Java 基础扎实', 'Spring Boot 项目经验', '算法刷题能坚持'),
    JSON_ARRAY('分布式系统理解偏浅', '前端工程化几乎没碰过'),
    'Java 后端方向的大三学生，具备基础框架与算法能力，需补齐系统设计、分布式与项目规模化经验以冲刺一线秋招。',
    90,
    '我是 XX 大学计算机系大三学生，主要方向是 Java 后端。做过一个电商秒杀的课程项目，接触过 Redis 和 MQ。擅长刷算法，LeetCode 做到了 300+。不擅长前端，也没有真正的分布式系统经验。打算准备 2026 秋招，目标一线互联网 Java 后端 SDE。',
    'ONBOARDING',
    '2026-02-01 09:30:00',
    '2026-04-15 20:00:00'
);

-- ----------------------------------------------------------------------------
-- 3. profile_skill —— 4 条技能（满足 skill 档位 3→20）
-- ----------------------------------------------------------------------------
INSERT INTO profile_skill
    (id, user_id, skill_name, skill_level, category, added_in_profile_version, source, status, evidence, created_at, updated_at)
VALUES
    (1, 1, 'Java',               'INTERMEDIATE', 'LANGUAGE',  1, 'ONBOARDING', 'ACTIVE',
     '学过并发、JVM 基础；能独立用 Spring Boot 实现 CRUD 服务。',
     '2026-02-01 09:30:00', '2026-04-15 20:00:00'),
    (2, 1, 'Spring Boot',        'INTERMEDIATE', 'FRAMEWORK', 1, 'ONBOARDING', 'ACTIVE',
     '课程项目中用 Spring Boot + MyBatis 实现后端服务。',
     '2026-02-01 09:30:00', '2026-04-15 20:00:00'),
    (3, 1, 'MySQL',              'INTERMEDIATE', 'TOOL',      1, 'ONBOARDING', 'ACTIVE',
     '熟悉基本 SQL、事务、索引；了解执行计划。',
     '2026-02-01 09:30:00', '2026-04-15 20:00:00'),
    (4, 1, '数据结构与算法',     'INTERMEDIATE', 'DOMAIN',    1, 'ONBOARDING', 'ACTIVE',
     'LeetCode 300+，中等题顺手，hard 需要查资料。',
     '2026-02-01 09:30:00', '2026-04-15 20:00:00');

-- ----------------------------------------------------------------------------
-- 4. profile_experience —— 2 条经历（满足 exp 档位 2→20）
-- ----------------------------------------------------------------------------
INSERT INTO profile_experience
    (id, user_id, exp_type, title, description, role, outcome, start_date, end_date,
     added_in_profile_version, source, created_at, updated_at)
VALUES
    (1, 1, 'PROJECT',
     '电商秒杀系统课程项目',
     '用 Spring Boot + Redis + RabbitMQ 实现限流、库存扣减、订单异步化。',
     '后端主力',
     '掌握 Redis 热点 key 处理、MQ 削峰；对幂等与一致性有了初步理解。',
     '2025-09-01', '2025-12-01',
     1, 'ONBOARDING',
     '2026-02-01 09:30:00', '2026-04-15 20:00:00'),
    (2, 1, 'COURSE',
     '分布式系统课程设计',
     '阅读 Raft 论文，基于模板实现简化版 Leader 选举与日志复制。',
     '组长',
     '完成简易 Raft 选举流程；理解了 term、quorum 与持久化的关系。',
     '2026-02-15', '2026-04-05',
     1, 'ONBOARDING',
     '2026-02-15 10:00:00', '2026-04-15 20:00:00');

-- ----------------------------------------------------------------------------
-- 5. growth_target —— 1 个主目标（is_primary=1）
-- ----------------------------------------------------------------------------
INSERT INTO growth_target
    (id, user_id, target_type, title, description, template_key, status, deadline, achieved_at, is_primary, created_at, updated_at)
VALUES
    (1, 1, 'JOB_SEEKING',
     '2026 秋招 Java 后端 offer',
     '拿到一线互联网公司 Java 后端 SDE 岗秋招 offer；目标岗位方向：分布式后端 / 中间件 / 业务架构。',
     'job_seeking_java_backend',
     'ACTIVE',
     '2026-10-31',
     NULL,
     1,
     '2026-02-05 19:00:00',
     '2026-04-10 21:00:00');

-- ----------------------------------------------------------------------------
-- 6. target_requirement —— 3 条（覆盖 TODO / IN_PROGRESS / MET）
-- ----------------------------------------------------------------------------
INSERT INTO target_requirement
    (id, target_id, req_name, req_type, description, status, sort_order, due_date,
     linked_skill_id, linked_experience_id, progress, created_at, updated_at)
VALUES
    (1, 1, 'Java 核心 (并发 / JVM)', 'SKILL',
     '理解 JMM、常见并发容器、线程池；能独立分析 GC 日志与内存溢出。',
     'IN_PROGRESS', 10, '2026-07-31',
     1, NULL, 55,
     '2026-02-05 19:00:00', '2026-04-15 20:00:00'),
    (2, 1, '分布式系统基础',        'KNOWLEDGE',
     '读完 Raft / MIT 6.824 前 5 讲，能讲清 Leader 选举与一致性模型。',
     'TODO', 20, '2026-08-31',
     NULL, 2, 10,
     '2026-02-05 19:00:00', '2026-04-15 20:00:00'),
    (3, 1, '完整项目 2 个以上',     'EXPERIENCE',
     '至少两个可写进简历的后端项目，覆盖高并发场景一次、分布式/一致性场景一次。',
     'MET', 30, NULL,
     NULL, 1, 100,
     '2026-02-05 19:00:00', '2026-04-15 20:00:00');

-- ----------------------------------------------------------------------------
-- 7. growth_journal —— 2 条随记（POSTED）
-- ----------------------------------------------------------------------------
INSERT INTO growth_journal
    (id, user_id, content, mood, tags, word_count, status, created_at, updated_at)
VALUES
    (1, 1,
     '今天刷完 LeetCode 中等题 3 题，二分查找的边界又踩坑了。顺手回看了一下秒杀项目里 MQ 消费幂等的设计，发现之前漏掉了重复消费 + 部分失败的 corner case，晚点补一个重试去重表。',
     'GOOD',
     JSON_ARRAY('algorithm', 'review'),
     95, 'POSTED',
     '2026-04-18 22:15:00', '2026-04-18 22:15:00'),
    (2, 1,
     '读了 MIT 6.824 Lecture 1 和 Raft 论文前 4 节。Leader election 部分比想象中绕，term 和投票的关系要再画一张时序图。明天继续推 log replication。',
     'NORMAL',
     JSON_ARRAY('distributed', 'paper'),
     76, 'POSTED',
     '2026-04-20 21:30:00', '2026-04-20 21:30:00');

-- ----------------------------------------------------------------------------
-- 8. journal_extraction —— 1 CONFIRMED + 1 PENDING_CONFIRM
-- ----------------------------------------------------------------------------
INSERT INTO journal_extraction
    (id, journal_id, user_id, extraction_status, ai_raw_response,
     draft_new_skills, draft_related_requirements, draft_events, draft_blockers,
     confirmed_new_skills, confirmed_related_requirements, confirmed_events, confirmed_blockers,
     confirmed_at, created_at, updated_at)
VALUES
    (1, 1, 1, 'CONFIRMED',
     JSON_OBJECT('model', 'demo', 'scenario', 'JOURNAL_EXTRACT'),
     JSON_ARRAY(),
     JSON_ARRAY(JSON_OBJECT('requirement_id', 1, 'note', '并发/基础延伸')),
     JSON_ARRAY(
         JSON_OBJECT('type', 'ALGORITHM_PRACTICE', 'summary', 'LeetCode 中等题 3 题'),
         JSON_OBJECT('type', 'PROJECT_REVIEW',    'summary', '秒杀项目 MQ 幂等复盘，发现 corner case')
     ),
     JSON_ARRAY(JSON_OBJECT('title', '二分查找边界反复出错')),
     JSON_ARRAY(),
     JSON_ARRAY(JSON_OBJECT('requirement_id', 1, 'note', '并发/基础延伸')),
     JSON_ARRAY(
         JSON_OBJECT('type', 'ALGORITHM_PRACTICE', 'summary', 'LeetCode 中等题 3 题'),
         JSON_OBJECT('type', 'PROJECT_REVIEW',    'summary', '秒杀项目 MQ 幂等复盘，发现 corner case')
     ),
     JSON_ARRAY(JSON_OBJECT('title', '二分查找边界反复出错')),
     '2026-04-19 09:00:00',
     '2026-04-18 22:15:30', '2026-04-19 09:00:00'),

    (2, 2, 1, 'PENDING_CONFIRM',
     JSON_OBJECT('model', 'demo', 'scenario', 'JOURNAL_EXTRACT'),
     JSON_ARRAY(JSON_OBJECT('skill_name', 'Raft', 'category', 'DOMAIN', 'level', 'BEGINNER')),
     JSON_ARRAY(JSON_OBJECT('requirement_id', 2, 'note', '分布式系统基础推进')),
     JSON_ARRAY(JSON_OBJECT('type', 'PAPER_READING', 'summary', '精读 Raft 论文前 4 节')),
     JSON_ARRAY(JSON_OBJECT('title', 'Leader election 的 term 演进不清晰')),
     NULL, NULL, NULL, NULL,
     NULL,
     '2026-04-20 21:30:30', '2026-04-20 21:30:30');

-- ----------------------------------------------------------------------------
-- 9. growth_task —— 3 任务（DONE / IN_PROGRESS / TODO 各 1）
-- ----------------------------------------------------------------------------
INSERT INTO growth_task
    (id, user_id, target_id, requirement_id, title, description, status, priority,
     due_date, completed_at, check_in_dates, check_in_count,
     planned_effort_minutes, actual_effort_minutes, created_at, updated_at)
VALUES
    (1, 1, 1, 3,
     '复盘秒杀项目并写入简历',
     '整理项目架构图、难点与量化指标；简历控制在半页以内。',
     'DONE', 'HIGH',
     '2026-03-31', '2026-03-30 23:10:00',
     JSON_ARRAY('2026-03-25', '2026-03-28', '2026-03-30'), 3,
     240, 310,
     '2026-03-20 20:00:00', '2026-03-30 23:10:00'),

    (2, 1, 1, 1,
     '刷完 LeetCode 热题 100',
     '以并发/动态规划/字符串三大类为主，每道题写中文题解放入 notes/。',
     'IN_PROGRESS', 'HIGH',
     '2026-06-30', NULL,
     JSON_ARRAY('2026-04-01','2026-04-02','2026-04-05','2026-04-08','2026-04-10',
                '2026-04-12','2026-04-14','2026-04-16','2026-04-17','2026-04-18',
                '2026-04-19','2026-04-20'), 12,
     60, 720,
     '2026-04-01 19:00:00', '2026-04-20 22:00:00'),

    (3, 1, 1, 2,
     '读完 Raft 论文并写一份总结',
     '目标：讲清楚 leader election、log replication、safety 三块；中文笔记 ≥ 3000 字。',
     'TODO', 'MEDIUM',
     '2026-05-15', NULL,
     JSON_ARRAY(), 0,
     300, 0,
     '2026-04-20 21:40:00', '2026-04-20 21:40:00');

-- ----------------------------------------------------------------------------
-- 10. stage_assessment —— 1 次阶段诊断（含 7 指标 + 轻复盘）
--     metrics 口径：journal_count / journal_streak / task_completion_rate /
--                   new_skills_count / profile_completeness /
--                   target_requirement_progress{total,met,in_progress,todo,met_ratio} /
--                   activity_intensity[{date,score}]
-- ----------------------------------------------------------------------------
INSERT INTO stage_assessment
    (id, user_id, trigger_time, window_start, window_end, profile_version_at_trigger,
     metrics, ai_raw_response, stage_summary, key_problems, suggestions, correction_directions,
     review_notes, ai_status, created_at, updated_at)
VALUES
    (1, 1,
     '2026-04-21 10:00:00',
     '2026-04-07 00:00:00',
     '2026-04-21 00:00:00',
     1,
     JSON_OBJECT(
         'journal_count', 2,
         'journal_streak', 2,
         'task_completion_rate', 0.33,
         'new_skills_count', 0,
         'profile_completeness', 90,
         'target_requirement_progress', JSON_OBJECT(
             'total', 3, 'met', 1, 'in_progress', 1, 'todo', 1, 'met_ratio', 0.33
         ),
         'activity_intensity', JSON_ARRAY(
             JSON_OBJECT('date', '2026-04-14', 'score', 2),
             JSON_OBJECT('date', '2026-04-16', 'score', 1),
             JSON_OBJECT('date', '2026-04-17', 'score', 1),
             JSON_OBJECT('date', '2026-04-18', 'score', 3),
             JSON_OBJECT('date', '2026-04-19', 'score', 2),
             JSON_OBJECT('date', '2026-04-20', 'score', 3)
         )
     ),
     JSON_OBJECT('model', 'demo', 'scenario', 'DIAGNOSIS_SUMMARY'),
     '本阶段输出稳定：持续打卡刷题、论文阅读起步，但任务完成率偏低（33%），分布式方向刚起步需要保持节奏。',
     JSON_ARRAY(
         '分布式系统基础仍为 TODO，风险：下阶段项目断档',
         '任务完成率 33%，低于健康线 50%',
         '新增技能 0：输入型活动多、产出型活动偏少'
     ),
     JSON_ARRAY(
         '把 Raft 论文总结拆成 3 个小任务，避免大任务长期占坑',
         '每周末写一次短周报，沉淀成长事件而不是只有随记',
         '秋招前再挑一个分布式方向的实战项目，对接 requirement#2'
     ),
     JSON_ARRAY(
         '执行节奏：从"输入为主"转向"输入+产出"',
         '目标对齐：requirement#2 需要配具体任务，当前只有 1 个 TODO'
     ),
     JSON_OBJECT(
         'wins',       JSON_ARRAY('秒杀项目已写入简历（requirement#3 MET）', '连续两天有高质量随记'),
         'learnings',  JSON_ARRAY('MQ 幂等要配去重表', 'Raft 的 term 是时序与安全的关键'),
         'next_focus', JSON_ARRAY('把 Raft 任务拆小', '补齐分布式方向一次实战'),
         'user_freeform', '节奏还行，但产出偏少，下阶段要让"写"跟上"读"。'
     ),
     'SUCCESS',
     '2026-04-21 10:00:10', '2026-04-21 10:00:10');

-- ----------------------------------------------------------------------------
-- 11. growth_snapshot —— 1 次快照（profile + 指标 冻结；指向 assessment=1）
-- ----------------------------------------------------------------------------
INSERT INTO growth_snapshot
    (id, user_id, profile_version, snapshot_time, profile_json,
     skills_snapshot, experiences_snapshot, targets_snapshot, metrics_snapshot,
     trigger_source, stage_assessment_id, created_at, updated_at)
VALUES
    (1, 1, 1, '2026-04-21 10:00:11',
     JSON_OBJECT(
         'version', 1,
         'self_intro', '大三计算机专业学生，Java 后端方向，正在准备 2026 秋招。',
         'strengths', JSON_ARRAY('Java 基础扎实','Spring Boot 项目经验','算法刷题能坚持'),
         'weaknesses', JSON_ARRAY('分布式系统理解偏浅','前端工程化几乎没碰过'),
         'summary', 'Java 后端方向的大三学生，需补齐系统设计与分布式经验。',
         'completeness', 90
     ),
     JSON_ARRAY(
         JSON_OBJECT('id', 1, 'skill_name', 'Java',           'level', 'INTERMEDIATE'),
         JSON_OBJECT('id', 2, 'skill_name', 'Spring Boot',    'level', 'INTERMEDIATE'),
         JSON_OBJECT('id', 3, 'skill_name', 'MySQL',          'level', 'INTERMEDIATE'),
         JSON_OBJECT('id', 4, 'skill_name', '数据结构与算法', 'level', 'INTERMEDIATE')
     ),
     JSON_ARRAY(
         JSON_OBJECT('id', 1, 'exp_type', 'PROJECT', 'title', '电商秒杀系统课程项目'),
         JSON_OBJECT('id', 2, 'exp_type', 'COURSE',  'title', '分布式系统课程设计')
     ),
     JSON_ARRAY(
         JSON_OBJECT('id', 1, 'target_type', 'JOB_SEEKING', 'status', 'ACTIVE', 'is_primary', 1,
                     'requirements', JSON_ARRAY(
                         JSON_OBJECT('id', 1, 'status', 'IN_PROGRESS', 'progress', 55),
                         JSON_OBJECT('id', 2, 'status', 'TODO',        'progress', 10),
                         JSON_OBJECT('id', 3, 'status', 'MET',         'progress', 100)
                     ))
     ),
     JSON_OBJECT(
         'journal_count', 2,
         'journal_streak', 2,
         'task_completion_rate', 0.33,
         'new_skills_count', 0,
         'profile_completeness', 90,
         'target_requirement_progress', JSON_OBJECT(
             'total', 3, 'met', 1, 'in_progress', 1, 'todo', 1, 'met_ratio', 0.33
         )
     ),
     'DIAGNOSIS',
     1,
     '2026-04-21 10:00:11', '2026-04-21 10:00:11');

-- ============================================================================
-- END of seed.sql
--   demo 用户     : id=1, username=demo, password=password
--   demo 目标     : id=1, JOB_SEEKING, is_primary=1
--   demo 随记     : id=1(CONFIRMED) / id=2(PENDING_CONFIRM)
--   demo 任务     : id=1(DONE) / id=2(IN_PROGRESS) / id=3(TODO)
--   demo 诊断     : id=1, ai_status=SUCCESS, 7 指标已就绪
--   demo 快照     : id=1, 指向 stage_assessment=1
-- ============================================================================
