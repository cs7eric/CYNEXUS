# CYNEXUS

<p align="center">
  <strong>Personal Intelligence, Connected.</strong><br />
  一个把知识、市场、财务与 AI 工作流放进同一处的个人智能工作空间。
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ef5b25?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/React-19-149eca?style=flat-square&logo=react&logoColor=white" alt="React 19" />
  <img src="https://img.shields.io/badge/Python-3.12-3776AB?style=flat-square&logo=python&logoColor=white" alt="Python" />
  <img src="https://img.shields.io/badge/License-Private-30343b?style=flat-square" alt="Private" />
</p>

> [!IMPORTANT]
> CYNEXUS 是私有项目。生产式 Release 运行时由 Docker 管理；不要直接启动源码树中的 Java 或 Python 后端进程。

## 一个工作台，而不是一堆工具

CYNEXUS 将日常信息处理组织为可追踪的工作流：输入进入系统，AI 与规则协作处理，结果回到原始业务页面等待确认。它适用于研究、资产跟踪、知识沉淀和自动化运营等长期、重复且需要保留判断过程的工作。

| 领域 | 能力 |
| --- | --- |
| **AI Workspace** | 多 Provider 路由、提示词治理、结构化输出、Agent 能力与用量追踪 |
| **Market Intelligence** | 行情与指标、因子研究、事件回放、选股、策略回测、交易复盘 |
| **Portfolio & Finance** | 交易截图识别、候选交易人工复核、账本与财务 AI 录入 |
| **Knowledge & Media** | 知识库、文档处理、媒体转写与内容复用 |
| **Operations** | 任务调度、数据采集、数据质量、API 工作流历史与失败诊断 |

## 体验流

```text
截图 / 文档 / 指令 / 数据源
            |
            v
   API workflow / Scheduler / Data job
            |
            v
 AI capability + Domain policy + Provider routing
            |
            v
  结构化结果 -> 人工复核 -> 业务页面确认
            |
            v
      可追溯的知识、数据与决策记录
```

工作流历史展示每次处理尝试、重试与审核事件；原始上传图片可在 API Workflows 页面按需查看。自动审核失败不会覆盖已经可人工复核的业务结果。

## Architecture

```text
                         React 19 + Vite + Tailwind
                                      |
                                  Gateway :18000
                                      |
     +----------------+---------------+----------------+----------------+
     |                |               |                |                |
 Core :18080      AI :18081      Market :18082   Knowledgebase     WeChat
     |                |               |                |                |
     +----------------+---------------+----------------+----------------+
                                      |
       PostgreSQL  |  Redis  |  ClickHouse  |  Nacos  |  MinIO
                                      |
                        Python Data Executor
```

Java 服务遵循 DDD 分层：`api -> application -> domain -> infra -> starter`。领域规则属于 `domain`；应用层负责用例编排与事务边界；基础设施实现持久化和外部系统适配。

## Repository Map

| 路径 | 说明 |
| --- | --- |
| `cynexus-ui` | React 19、TypeScript、Vite 8 与 Tailwind v4 前端 |
| `cynexus-core` | 工作流、调度、媒体、财务与核心平台能力 |
| `cynexus-ai` | AI Provider、Prompt、结构化输出、Agent 与使用量治理 |
| `cynexus-market` | 市场数据查询、因子研究、投资组合与交易复盘 |
| `cynexus-knowledgebase` | 知识库服务 |
| `cynexus-wechat` | 微信接入能力 |
| `cynexus-gateway` | 网关与外部入口 |
| `cynexus-data` | Python 数据执行器、采集源、管道、存储与质量检查 |
| `deploy` | Docker Release、Nacos 配置与 PostgreSQL 迁移 |
| `docs` | 架构与运行文档 |

## Quick Start

### Prerequisites

- Docker Desktop with Docker Compose
- Java 21 and Maven
- Node.js 22+ with pnpm
- Python 3.12+ (仅在维护 `cynexus-data` 时需要)

### Run the Release stack

Release 使用 Docker 镜像，默认端口如下：

| Service | Port |
| --- | ---: |
| UI | `5173` |
| Gateway | `18000` |
| Core | `18080` |
| AI | `18081` |
| Market | `18082` |

```powershell
# 使用当前已构建的镜像启动
.\deploy\release\start-release.ps1

# 构建 Maven artifact 与 Docker 镜像后启动
.\deploy\release\start-release.ps1 -Build

# 仅部署有改动的服务
.\deploy\release\start-release.ps1 -Build -Changed
```

启动后访问 [http://localhost:5173](http://localhost:5173)。停止 Release：

```powershell
.\deploy\release\stop-release.ps1
```

### Frontend development

前端可单独以 HMR 模式运行，并代理到已启动的 Release 后端。使用 `5174`，避免占用 Release UI 端口：

```powershell
Set-Location cynexus-ui
pnpm install
pnpm dev -- --port 5174
```

## Build & Verify

```powershell
# Java: 编译全部 Maven 模块
mvn -DskipTests compile

# 前端: 类型检查与生产构建
Set-Location cynexus-ui
pnpm build

# Python: 执行数据项目测试
Set-Location ..\cynexus-data
pytest
```

执行功能变更时，请优先运行受影响模块的窄范围测试。Java、前端和数据层变更的具体约束见各目录内的 `AGENTS.md`。

## Development Principles

- **可追溯优先**：工作流、重试、审核和业务确认分别记录，不以最后一次错误覆盖当前真实状态。
- **AI 受治理**：Provider、Prompt、能力注册与使用量绑定必须一起交付；写操作走预览、明确确认与审计。
- **领域边界清晰**：业务规则不放入 controller、application service 或基础设施层。
- **Release 可复现**：后端、迁移和共享运行时变更必须通过版本化 Docker Release 部署验证。
- **数据职责单一**：Python 遵循 `job -> runtime -> pipeline -> source/domain/storage -> connection/hook`。

## Documentation

- [Release runtime](deploy/release/README.md)
- [API workflow contract](docs/agent/api-workflows.md)
- [Agent guide](AGENTS.md)
- [Frontend guide](cynexus-ui/FRONTEND_UI_IMPLEMENTATION_GUIDE.md)

---

<p align="center">Built for deliberate work, not just more automation.</p>
