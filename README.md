# GrowthTrace · 计算机专业学生成长跟踪与阶段诊断平台

面向计算机专业学生的成长管理系统 —— 自然语言建档、AI 抽取 + 用户确认、画像演化、阶段诊断与复盘纠偏。

## 项目状态

V1 Step 6 逐模块细化 —— 蓝图 / 目录 / Schema / 后端 / 种子数据 / 前端骨架完成；
`auth / profile / journal / target / diagnosis / execution / dashboard` 七大模块已闭环。

## 目录结构

```
GrowthTrace/
├── backend/        Spring Boot 3.x + Java 21 单体后端
├── frontend/       Vue 3 + TypeScript + Vite + Pinia + Tailwind 前端骨架
├── sql/
│   ├── schema.sql  V1 11 张表 DDL
│   └── seed.sql    demo 用户 + 最小演示数据（可选）
└── docs/           答辩与课程文档（待生成）
```

## 后端技术栈

Spring Boot 3.2.5 · Java 21 · Spring Security + JWT · MyBatis-Plus 3.5.7 · MySQL · Lombok · 轻量 OpenAI-compatible AI adapter。

## 快速启动（后端）

1. 创建 MySQL 数据库：
   ```sql
   CREATE DATABASE growthtrace DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. 执行 DDL：
   ```bash
   mysql -u root -p growthtrace < sql/schema.sql
   ```
3. （可选）加载演示数据：
   ```bash
   mysql -u root -p growthtrace < sql/seed.sql
   ```
   demo 账号：`demo` / `password`（BCrypt 哈希，幂等可重复执行）。
4. 设置环境变量（或改 `backend/src/main/resources/application-dev.yml`）：
   - `DB_USER`, `DB_PASSWORD`
   - `JWT_SECRET`（≥ 32 字符）
   - `AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL`（OpenAI 兼容端点）
5. 启动后端：
   ```bash
   cd backend
   mvn spring-boot:run
   ```
6. 默认地址 `http://localhost:8080/api`；健康检查 `GET /api/actuator/health`（若未启 actuator 可用任意公开端点代替）。

## 端点速查（V1）

| 模块        | 主要端点                                                                         |
|-------------|----------------------------------------------------------------------------------|
| Auth        | `POST /auth/register` `POST /auth/login` `GET /auth/me`                           |
| Profile     | `POST /profile/onboarding/{extract,confirm}` · `GET /profile` · `POST /profile/completeness/refresh` · 技能/经历 CRUD |
| Target      | `/target` CRUD · `POST /target/{id}/primary` · `/target/{id}/requirements` CRUD · `GET /target/templates` |
| Journal     | `/journal` CRUD · `POST /journal/{id}/extract` · `POST /journal/{id}/extract/confirm`    |
| Diagnosis   | `POST /diagnosis/trigger` · `GET /diagnosis/history` · `GET /diagnosis/{id}` · `PUT /diagnosis/{id}/review` |
| Execution   | `/execution/task` CRUD · `PUT /execution/task/{id}/status` · `POST /execution/task/{id}/check-in` · `GET /execution/weekly-progress` |
| Dashboard   | `GET /dashboard/overview` · `GET /dashboard/heatmap?windowDays=` · `GET /dashboard/growth-curve?limit=` |

## 核心原则（V1 必须遵守）

1. 高价值字段必须走 "AI 抽取 → 用户确认 → 正式入档"
2. 每次用户动作最多触发 1 次核心 AI 调用
3. AI 不直接写死任何正式档案字段
4. AI 是增强层，不是主裁决层
5. V1 必须轻量、可实现、可部署、可答辩、可写文档

## 快速启动（前端）

```bash
cd frontend
npm install
npm run dev        # http://127.0.0.1:5173
```

详细说明见 [`frontend/README.md`](frontend/README.md)。构建命令 `npm run build` 会先跑 `vue-tsc --noEmit` 再打包。

## 下一步

- V1 验收：端到端冒烟（建档 → 目标 → 随记 → 抽取确认 → 打卡 → 诊断 → 复盘 → Dashboard 渲染）
- 文档：课程答辩材料、接口清单、演示脚本（docs/）
- V2 候选：growth_plan 分组、多模型切换、增量诊断缓存（均不在 V1 范围）

## 验收与演示文档

- [V1 验收清单](docs/V1-%E9%AA%8C%E6%94%B6%E6%B8%85%E5%8D%95.md)
- [V1 演示脚本](docs/V1-%E6%BC%94%E7%A4%BA%E8%84%9A%E6%9C%AC.md)
- [联调与启动说明](docs/%E8%81%94%E8%B0%83%E4%B8%8E%E5%90%AF%E5%8A%A8%E8%AF%B4%E6%98%8E.md)
