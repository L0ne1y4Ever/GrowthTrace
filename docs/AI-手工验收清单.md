# AI 手工验收清单

## 目标

验证 GrowthTrace 三条 AI 链路在真实模型服务下可稳定运行、可失败降级、可快速排障：

- 建档抽取
- 随记抽取
- 阶段诊断

## 前置条件

- 后端已启动
- 前端已启动
- `application-dev.local.yml` 中已配置：
  - `growthtrace.ai.base-url`
  - `growthtrace.ai.api-key`
  - `growthtrace.ai.model`
  - `growthtrace.ai.timeout-seconds`
- 浏览器已登录有效账号

## 验收用例

### 1. 建档抽取成功

- 进入 `/onboarding`
- 输入 100 字以上自然语言自述
- 点击“AI 抽取画像草稿”
- 预期：
  - 前端出现等待态
  - 后端日志出现 `AI request start: scenario=PROFILE_EXTRACT`
  - 页面拿到草稿并可编辑

### 2. 建档抽取超时

- 临时切换到响应较慢模型，或把 `timeout-seconds` 改小
- 再次触发建档抽取
- 预期：
  - 页面提示“AI 调用超时”
  - 后端日志出现 `AI request timeout`
  - 第三方平台通常有调用记录

### 3. 随记抽取成功

- 新建一条包含“学到的新技能 / 遇到的问题 / 对目标要求影响”的随记
- 进入详情页点击“AI 抽取草稿”
- 预期：
  - 页面拿到 `newSkills / relatedRequirements / events / blockers`
  - 后端日志场景为 `JOURNAL_EXTRACT`

### 4. 随记抽取失败可重试

- 故意配置错误的 `base-url` 或 `api-key`
- 点击“AI 抽取草稿”
- 预期：
  - 页面出现分类错误提示
  - 草稿不入库为 `CONFIRMED`
  - 修正配置后可直接重新抽取

### 5. 阶段诊断成功

- 进入 `/diagnosis`
- 点击“触发新一次诊断”
- 预期：
  - 页面显示 7 项本地指标
  - 返回 `stageSummary / suggestions / correctionDirections`
  - 后端日志场景为 `DIAGNOSIS_SUMMARY`

### 6. 阶段诊断失败降级

- 配置错误 `api-key` 或临时禁用模型服务
- 点击“触发新一次诊断”
- 预期：
  - 页面看到失败提示
  - 新记录仍生成
  - `aiStatus=FAILED`
  - 指标区仍能展示
  - 历史页和 Dashboard 能识别这是一次 AI 失败记录

## 排障定位顺序

1. 浏览器 Network 是否发出 `/api/...` 请求
2. 后端日志是否出现 `AI request start`
3. `AI config summary` 里的 `baseUrl / model / apiKeyConfigured` 是否正确
4. 看失败类型：
   - `AI request timeout`
   - `AI request failed: status=...`
   - `AI 响应缺少 choices[0].message.content`
5. 若 404：检查 `base-url` 是否少了 `/v1`
6. 若 401/403：检查 `api-key`
7. 若 parse error：检查模型是否真的兼容 OpenAI Chat Completions
