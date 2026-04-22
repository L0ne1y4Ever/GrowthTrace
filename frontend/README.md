# GrowthTrace Frontend

Vue 3 + TypeScript + Vite + Pinia + Vue Router + Tailwind CSS 的 V1 前端骨架。

## 状态

骨架阶段 —— 路由、store、请求封装、布局、11 个空页面、API 模块桩已就位；业务逻辑留到 Step 6 逐模块细化。

## 目录结构

```
frontend/
├── index.html
├── vite.config.ts / tsconfig(.node).json / tailwind.config.ts / postcss.config.cjs
├── .env.development / .env.production
└── src/
    ├── main.ts · App.vue · style.css · env.d.ts
    ├── router/                # 路由 + 守卫
    ├── stores/                # Pinia（auth）
    ├── utils/request.ts       # Axios + JWT + 拦截器
    ├── types/api.ts           # 响应体契约
    ├── api/                   # 按模块拆分的 API 桩
    ├── layouts/               # BlankLayout / AppLayout
    └── pages/                 # 11 个空页面骨架
```

## 启动

```bash
cd frontend
npm install
npm run dev        # http://127.0.0.1:5173
```

默认请求 `http://localhost:8080/api`（见 `.env.development`），CORS 已在后端 `SecurityConfig` 允许 5173。

## 约定

- **响应结构**：`{ code, message, data, timestamp }`，`code === 0` 视为成功，`data` 会被拦截器直接抛出。
- **分页**：`{ records, total, page, size }`。
- **JWT**：`auth` store 持久化到 `localStorage.growthtrace.token`，请求拦截器自动加 `Authorization: Bearer <token>`。
- **401**：拦截器自动清理 token 并跳转 `/login?redirect=...`。
- **路由守卫**：除 `meta.public` 外均需登录；已登录访问 `/login` 会被重定向到 Dashboard。
- **命名语义**：全局使用 growth / target / journal / diagnosis / execution；严禁 career / position / job / matching。

## 下一步（Step 6）

按优先级顺序细化：auth → onboarding + profile → journal → target → diagnosis → execution → dashboard。
