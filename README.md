# CYNEXUS

<div align="center">

**个人投资研究工作台**

把市场信号、研究过程、交易记录和 AI 协作，放进一条可以回看的工作流。

`发现`  ·  `研究`  ·  `确认`  ·  `复盘`

</div>

---

## 它解决什么问题

投资研究通常散落在行情软件、聊天记录、截图、表格和笔记里。CYNEXUS 把这些输入放在同一套工作流中：数据负责提供事实，AI 负责整理和提取，用户负责判断与确认，系统负责留下过程。

它适合希望长期积累研究资产的个人用户，而不是追求“自动替我下决定”的黑盒工具。

## 从一个信号，到一条记录

```text
市场数据 / 资讯 / 交易截图 / 研究文档
                    │
                    ▼
         观察 · 筛选 · 分析 · 提取
                    │
                    ▼
             候选结果与证据
                    │
                    ▼
              人工确认与入账
                    │
                    ▼
              复盘 · 校正 · 再研究
```

例如上传一张交易截图，CYNEXUS 会先识别出候选交易，再进行可选的 AI 复核，最后回到投资组合页面等待你确认。识别失败、重试和复核事件都保留在 API Workflows 中，原图也能按记录查看。

## 四个工作区

### 发现

市场概览、情绪、资金、ETF、融资融券、期货持仓、涨停板和资讯流，帮助你知道今天发生了什么。

### 研究

Screener、行业与概念、因子研究、事件回放和策略回测，帮助你把“感觉”变成可验证的假设。

### 记录

交易截图导入、财务 AI Capture、账本、知识库和媒体处理，把外部输入变成自己的结构化资产。

### 复盘

交易复盘、持仓详情、工作流历史和数据质量检查，让你能回到当时的输入、过程和结果。

## AI 在哪里发挥作用

AI 是工作流中的一个受治理环节，不是产品的全部。

- **提取**：从截图、文档和媒体中生成结构化候选结果。
- **研究**：辅助整理市场事件、因子、资讯和策略证据。
- **编排**：通过 Cynexus Agent 查询受控的数据能力。
- **治理**：统一管理 Provider、模型、Prompt、能力和用量。

会写入业务数据的操作都经过预览、明确确认和审计；AI 复核失败不会抹掉已经可以人工确认的识别结果。

## 系统结构

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

| 模块 | 负责什么 |
| --- | --- |
| `cynexus-ui` | React 工作台和所有用户交互 |
| `cynexus-core` | API 工作流、调度、媒体与财务基础能力 |
| `cynexus-ai` | Provider、Prompt、结构化输出和 Agent 治理 |
| `cynexus-market` | 市场数据、投资组合、交易复盘与研究能力 |
| `cynexus-knowledgebase` | 知识库与研究材料 |
| `cynexus-data` | Python 数据采集、管道、存储和质量检查 |
| `deploy` | Docker Release、配置和数据库迁移 |

Java 服务采用 `api → application → domain → infra → starter` 分层，业务规则集中在领域层。

## 开始运行

### Release

后端和共享基础设施通过 Docker 管理。需要 Docker Desktop、Java 21、Maven、Node.js 22+ 和 pnpm。

```powershell
# 启动已构建的 Release
.\deploy\release\start-release.ps1

# 构建并启动
.\deploy\release\start-release.ps1 -Build

# 只部署当前改动涉及的服务
.\deploy\release\start-release.ps1 -Build -Changed
```

打开 [http://localhost:5173](http://localhost:5173)。停止 Release：

```powershell
.\deploy\release\stop-release.ps1
```

### 前端 HMR

```powershell
Set-Location cynexus-ui
pnpm install
pnpm dev -- --port 5174
```

HMR 前端使用 `5174`，代理到已运行的 Release 后端。

## 开发约定

- 先阅读目标目录最近的 `AGENTS.md`。
- 功能改动运行受影响模块的窄范围测试。
- 后端、迁移和共享运行时改动必须部署到 Release 验证。
- AI 能力需要同时完成路由、Prompt、Provider、用量绑定和可观测性。
- Agent 写操作必须经过预览、确认和审计。

## 文档入口

- [Release 运行说明](deploy/release/README.md)
- [API Workflow 合约](docs/agent/api-workflows.md)
- [贡献与仓库约定](AGENTS.md)
- [前端实现指南](cynexus-ui/FRONTEND_UI_IMPLEMENTATION_GUIDE.md)

---

<div align="center"><sub>CYNEXUS · 把每一次判断，变成下一次研究的起点。</sub></div>
