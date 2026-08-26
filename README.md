# CYNEXUS

<div align="center">

## Your personal operating system for investing and research

把市场数据、交易记录、研究过程和 AI 协作放进一个可复盘的工作台。

`Research` &nbsp; `Decide` &nbsp; `Record` &nbsp; `Review`

</div>

---

## What is CYNEXUS?

CYNEXUS 是面向个人投资研究与信息管理的智能工作空间。

它不是聊天机器人，也不是单纯的行情看板。它把你每天面对的几类工作连成闭环：从市场信号和资讯中发现线索，用 AI 和数据工具完成研究，将交易和账务沉淀为可追溯记录，再通过复盘验证自己的判断。

```text
市场数据 / 资讯 / 截图 / 文档
              ↓
       观察、筛选、研究
              ↓
      AI 辅助分析与结构化提取
              ↓
   交易、账务、知识与策略记录
              ↓
         复盘、校正、再决策
```

## Three Workflows

### 01 / 看见机会

**市场监控、选股与研究**

- 市场概览、情绪、资金、ETF、融资融券和期货持仓
- 涨停板监控、指标采集、盘中事件与资讯流
- 自选股、行业与概念分析、股票复盘
- Screener、因子研究、事件回放与策略回测

> 目标不是更多数据，而是把值得关注的变化找出来，并保留判断依据。

### 02 / 把输入变成记录

**交易、财务与内容处理**

- 上传交易截图，AI 识别为待确认交易，再由用户审核入账
- 财务 AI Capture 将票据和截图转换为账本候选项
- 文档、视频与媒体处理，保留过程与结果历史
- 知识库将研究材料、笔记与结论集中管理

> AI 提供候选结果；确认操作始终留在业务页面，由用户完成。

### 03 / 让系统持续运转

**AI 治理、自动化与可追溯性**

- Provider、模型、Prompt 与用量统一管理
- Scheduler 和 Python Data Executor 负责定时采集、投影与质量检查
- API Workflows 记录每次异步处理、重试、审核与失败原因
- Cynexus Agent 可在受控边界内查询项目数据、编排可执行任务

> 每一条自动化都应该能回答：输入是什么、做了什么、结果在哪里、谁确认了它。

## Product Map

| 工作区 | 你可以做什么 |
| --- | --- |
| **Trade View** | 导入交易、人工确认候选交易、查看复盘与持仓表现 |
| **Cynexus Screener** | 基于市场数据筛选标的，研究因子并验证策略 |
| **Market & Information** | 跟踪市场状态、资讯、社交信号与数据采集任务 |
| **Finance** | 管理账本、交易与 AI 辅助录入 |
| **Knowledge Base** | 组织研究材料、笔记和可复用内容 |
| **AI & Agent** | 配置模型能力、提示词、用量与 Agent 工具 |
| **Settings** | 管理调度、数据源、Provider 和工作流运行记录 |

## Built for review, not blind automation

在 CYNEXUS 中，AI 处理并不等于自动落库。

例如，交易截图导入会经历以下过程：

```text
上传截图 → 视觉识别 → 候选交易 → AI 复核 → 人工确认 → 写入交易记录
```

其中 AI 复核是增强步骤，不会阻塞已经可供人工确认的识别结果。API Workflows 页面保留每次尝试和错误事件，原始截图也可按需查看，便于排查而不混淆当前业务状态。

## Architecture at a Glance

```text
                         React 19 / Vite / Tailwind
                                      │
                                Gateway :18000
                                      │
       ┌───────────────┬──────────────┼──────────────┬───────────────┐
       │               │              │              │               │
   Core :18080     AI :18081     Market :18082  Knowledge Base    WeChat
       │               │              │              │               │
       └───────────────┴──────────────┴──────────────┴───────────────┘
                                      │
          PostgreSQL · Redis · ClickHouse · Nacos · MinIO
                                      │
                           Python Data Executor
```

| Area | Stack | Responsibility |
| --- | --- | --- |
| `cynexus-ui` | React 19, TypeScript, Vite, Tailwind | 用户工作台 |
| `cynexus-core` | Java 21, Spring Boot | 工作流、调度、媒体、财务与平台能力 |
| `cynexus-ai` | Spring AI, Provider adapters | 模型路由、Prompt、结构化输出、Agent 治理 |
| `cynexus-market` | Java, ClickHouse, PostgreSQL | 市场数据、研究、组合与交易复盘 |
| `cynexus-data` | Python | 数据源、采集管道、存储与质量检查 |
| `deploy` | Docker Compose, PostgreSQL migrations | Release 运行时与部署资产 |

Java 服务按 `api → application → domain → infra → starter` 组织。业务规则放在 `domain`，应用层只负责编排与事务。

## Run Locally

### Release runtime

Release 是唯一允许运行的本地后端形态，使用 Docker 管理：

```powershell
# 启动已构建的 Release 服务
.\deploy\release\start-release.ps1

# 编译并构建 Release 镜像
.\deploy\release\start-release.ps1 -Build

# 仅构建和部署当前改动涉及的服务
.\deploy\release\start-release.ps1 -Build -Changed
```

| UI | Gateway | Core | AI | Market |
| ---: | ---: | ---: | ---: | ---: |
| `5173` | `18000` | `18080` | `18081` | `18082` |

访问 [http://localhost:5173](http://localhost:5173)。停止服务：

```powershell
.\deploy\release\stop-release.ps1
```

### Frontend only

前端可以通过 HMR 连接已有 Release 后端。请使用 `5174`，不要占用 Release UI 的 `5173`：

```powershell
Set-Location cynexus-ui
pnpm install
pnpm dev -- --port 5174
```

## Contributing Notes

- Java、前端、数据与部署目录各自包含 `AGENTS.md`，修改前先阅读最近的说明。
- 功能改动应运行受影响模块的测试；后端、迁移和共享运行时改动需要部署到 Release 验证。
- AI 能力必须同时完成运行时路由、Prompt、Provider、用量绑定与可观测性。
- 会修改业务数据的 Agent 能力必须经过预览、显式确认与审计。

## Documentation

- [Release runtime](deploy/release/README.md)
- [API workflow contract](docs/agent/api-workflows.md)
- [Contributor guide](AGENTS.md)
- [Frontend implementation guide](cynexus-ui/FRONTEND_UI_IMPLEMENTATION_GUIDE.md)

---

<div align="center"><sub>CYNEXUS · Make every decision easier to revisit.</sub></div>
