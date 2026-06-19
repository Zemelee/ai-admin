# 实习管理 AI 智能体系统（MVP）


> 当前版本：**MVP-v1.0**（核心骨架）

---

## 1. 目录结构（Monorepo）

```
ai-admin/
├── backend/             # Spring Boot 3.x + Java 21
├── frontend/            # Vue3 + TS + Element Plus（pnpm）
├── docs/                # API 导出 / 部署说明
├── plans/               # 架构方案 + 二期 backlog
├── docker-compose.yml   # MySQL 8 + MinIO 一键启动
├── pnpm-workspace.yaml
└── .gitignore
```

## 2. 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + TS + Vite + Element Plus + Pinia + Vue Router + Axios + ECharts |
| 后端 | Spring Boot 3.x + Java 21 + MyBatis-Plus + Sa-Token + MinIO Client + Knife4j |
| 存储 | MySQL 8（业务数据） + MinIO（图片附件） |
| AI | GLM-4-Air-250414（通过封装好的 [`backend/.../ai/GlmClient.java`](backend/src/main/java/com/zr/aiadmin/ai/GlmClient.java:1) 调用） |
| 包管理 | 前端 **pnpm**，后端 **Maven** |

## 3. 快速启动

### 3.1 启动基础设施（MySQL + MinIO）

```bash
docker compose up -d
```

- MySQL：`localhost:3306`，库名 `ai_admin`，账号 `root/ai_admin_2026`
- MinIO 控制台：`http://localhost:9001`，账号 `minioadmin/minioadmin`，bucket `ai-admin`

### 3.2 启动后端

```bash
cd backend
./mvnw spring-boot:run
# 访问 Knife4j: http://localhost:8080/doc.html
```

### 3.3 启动前端

```bash
cd frontend
pnpm install
pnpm dev
# 访问: http://localhost:5173
```

## 4. 角色与默认账号（种子数据）

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 监管者 supervisor | `admin` | `123456` |
| 实习指导教师 teacher | `teacher01` | `123456` |
| 实习学生 student | `student01` | `123456` |
| 企业指导人员 mentor | `mentor01` | `123456` |

## 5. MVP 功能范围

✅ 已涵盖：
- 4 类角色登录 + 基础工作台
- 4 个核心表单：请假 / 实习日志 / 实习周记 / 单位变更
- 简单 AI 政策问答（GLM 直连）
- 三色预警引擎（5 条核心规则）
- 图片附件上传（MinIO）

⏳ 二期：详见 [`plans/phase2-backlog.md`](plans/phase2-backlog.md:1)

## 6. 详细架构

参见 [`plans/mvp-architecture.md`](plans/mvp-architecture.md:1)。
