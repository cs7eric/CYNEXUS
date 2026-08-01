# 个人持仓与收益追踪技术方案

## 1. 方案状态

- 状态：MVP 技术方案校正版，待产品范围确认
- 当前阶段：设计，不实施、不启用运行时组件
- 核心约束：业务归属 `cynexus-market`，视觉模型调用复用 `cynexus-ai` 治理能力
- 未决项：异步任务的实际执行适配器，需要在实现前完成小型 POC 后选择

本方案只服务于以下闭环：

```text
截图上传 -> 异步解析 -> 人工校对 -> 显式确认
        -> 交易账本 -> 持仓/盈亏 -> 买卖点 -> 复盘
```

多组合、完整现金账本、TWR/XIRR、Agent 工具和通用任务平台均不属于首期。

## 2. 业务与技术边界

### 2.1 `cynexus-market` 拥有的内容

- 解析任务的业务生命周期。
- 截图证据与用户归属关系。
- AI 候选交易、人工修改、警告和确认状态。
- 已确认交易账本及更正关系。
- 持仓、成本、已实现/未实现盈亏计算。
- 买卖点查询和复盘生命周期。
- 对外 Market API 和所有 portfolio 数据表。

### 2.2 `cynexus-ai` 只提供的能力

- 根据稳定 capability 和 prompt 调用支持 vision 的模型。
- Provider/model 路由、Prompt 管理、结构化输出约束、执行日志和用量记录。
- 返回模型结果或明确失败，不拥有交易任务和持仓事实。

建议 capability code 为 `portfolio_trade_screenshot_parse`。优先通过现有通用 vision execution 契约接入；只有现有契约不能表达图片和结构化结果时，才在 AI 服务增加最小适配能力。

### 2.3 不复用的业务模型

- 不把任务放入 Finance Capture。
- 不把复盘放入现有 Stock Review。
- 不把截图任务放入 Media Transcription。
- 不把新页面代码放入 Stock Overview 页面目录。

可以复用的只有底层能力和实现模式，例如身份解析、multipart 校验、AI 路由、行情查询、ID 生成、审计字段和 UI primitives。

## 3. DDD 分层

`cynexus-market` 内新增 feature-first 的 `portfolio` 包，不建立横跨整个 market 的通用“task”抽象。

```text
cynexus-market-api
  portfolio DTO / request / response contracts

cynexus-market-application
  portfolio controllers
  portfolio application services
  DTO assemblers

cynexus-market-domain
  portfolio entities / value objects
  portfolio policies / domain services
  portfolio repositories / gateway ports

cynexus-market-infra
  portfolio persistence adapters
  screenshot asset adapter
  AI vision adapter
  existing market-data adapter

cynexus-market-starter
  portfolio bean/runtime assembly only when implementation begins
```

应用服务只处理身份、事务编排和 DTO 映射。下列规则必须属于 domain：

- 任务状态迁移。
- AI 结果解析和候选标准化。
- 必填字段、卖超和疑似重复校验。
- 确认幂等和更正规则。
- 移动加权成本和盈亏计算。
- 复盘状态与结果快照。

## 4. 核心领域模型

### 4.1 `PortfolioImportTask`

职责：管理一张截图从上传到确认的完整生命周期。

```text
UPLOADED -> PROCESSING -> READY_FOR_REVIEW -> CONFIRMED
    |            |               |
    |            +-> FAILED      +-> CANCELLED
    +---------------------------> CANCELLED
FAILED -> PROCESSING (显式重试)
```

约束：

- 一个任务只有一张截图。
- `CONFIRMED` 和 `CANCELLED` 是终态。
- 只有 `READY_FOR_REVIEW` 可以确认。
- 任务版本用于防止多个页面同时覆盖校对结果。
- 解析失败与业务警告分开：失败代表没有可校对结果，警告代表结果存在但需要人工判断。

### 4.2 `PortfolioImportCandidate`

职责：保存 AI 提取值、用户修正值、字段置信度和验证结果。

- 原始值与最终候选值分开保存，便于审计。
- 排除某行不会删除原始识别结果。
- 确认时以当前未排除的候选值生成交易。
- 候选不是交易事实，不能参与持仓计算。

### 4.3 `PortfolioTrade`

职责：表示用户确认的交易事实。

- 交易记录一旦生效不原地覆盖。
- 更正操作把原交易标记为被替代，并创建新交易记录。
- 交易保留来源任务、候选和确认幂等键。
- 同一 principal + tenant 内进行查询和计算。

### 4.4 `PortfolioLedgerDomainService`

职责：按成交时间和 ID 顺序应用有效交易，生成持仓和收益视图。

首期数据量属于个人账本规模，持仓可以从交易账本确定性重算，不需要先引入独立快照或事件流框架。若后续性能数据证明有必要，再增加可重建的 projection 表。

### 4.5 `PortfolioReview`

职责：保存一只股票、一个日期区间内的结构化复盘。

- 草稿可继续编辑。
- 完成时保存由系统计算的结果快照。
- 交易更正后显示“结果可能已变化”，由用户刷新快照，不静默改写已完成复盘。

## 5. 最小数据模型

所有表位于 `cynexus_appl_market`。遵循项目的 bigint ID、`created_at`、`updated_at`、`deleted_at`、JSONB、CHECK 约束和活动记录部分索引规范。

### 5.1 `portfolio_import_asset`

保存受 owner 约束的图片证据：

- `id`, `principal_key`, `tenant_key`
- `content_type`, `byte_size`, `width`, `height`, `content_sha256`
- `content_bytes` 或受端口管理的 `storage_key`
- `retention_until`, `purged_at`
- `created_at`, `deleted_at`

具体存储方式在实现 POC 时选择，domain 只依赖 `PortfolioImportAssetStorePort`。图片 bytes 不出现在列表 DTO 和普通日志中。

### 5.2 `portfolio_import_task`

- `id`, `principal_key`, `tenant_key`, `asset_id`
- `status`, `version`, `attempt_count`
- `capability`, `prompt_version`, `provider_code`, `model_code`
- `started_at`, `completed_at`, `confirmed_at`
- `last_error_code`, `last_error_message`
- `idempotency_key`, `created_at`, `updated_at`, `deleted_at`

索引覆盖 owner + status + created_at。异步执行所需的 lease/next-attempt 字段只在选定具体执行适配器后加入，不提前把调度实现写进业务模型。

### 5.3 `portfolio_import_candidate`

- `id`, `task_id`, `row_no`
- `raw_payload`：模型返回的该行原始 JSON
- `normalized_payload`：当前校对值
- `confidence`, `warnings`
- `review_status`：`PENDING` / `ACCEPTED` / `EXCLUDED`
- `version`, `created_at`, `updated_at`

`task_id + row_no` 在活动记录中唯一。候选 payload 在 domain 中经过严格 schema 映射，业务代码不直接依赖任意 JSON 字段。

### 5.4 `portfolio_trade`

- `id`, `principal_key`, `tenant_key`
- `source_task_id`, `source_candidate_id`
- `symbol`, `security_name`, `side`
- `traded_at`, `quantity`, `price`, `gross_amount`, `total_fee`
- `external_trade_no`, `status`
- `replaces_trade_id`, `confirmed_at`, `confirmed_by`
- `created_at`, `updated_at`, `deleted_at`

金额和数量使用明确精度的 PostgreSQL `numeric`，Java 使用 `BigDecimal`。约束保证数量和价格大于 0、方向枚举有效、更正关系不自引用。

### 5.5 `portfolio_review`

- `id`, `principal_key`, `tenant_key`
- `symbol`, `period_start`, `period_end`
- `thesis`, `plan`, `execution`, `lessons`, `next_action`
- `tags`, `rating`, `metrics_snapshot`
- `status`, `completed_at`
- `created_at`, `updated_at`, `deleted_at`

首期不增加 account、cash event、performance snapshot、trade cycle 等表。它们只有进入经确认的后续产品范围时才设计。

## 6. API 设计

所有 API 从可信请求上下文解析 principal + tenant，不接受 owner 入参。

### 6.1 导入任务

```text
POST   /api/market/portfolio/imports
GET    /api/market/portfolio/imports
GET    /api/market/portfolio/imports/{taskId}
PUT    /api/market/portfolio/imports/{taskId}/candidates/{candidateId}
POST   /api/market/portfolio/imports/{taskId}/confirm
POST   /api/market/portfolio/imports/{taskId}/retry
POST   /api/market/portfolio/imports/{taskId}/cancel
GET    /api/market/portfolio/imports/{taskId}/image
```

- 上传使用 multipart 并通过 `Idempotency-Key` 防止重复创建。
- 上传成功返回 `202 Accepted`、任务 ID 和当前状态。
- 校对更新带 candidate version；确认带 task version 和 confirmation idempotency key。
- `GET task` 同时返回任务状态、候选、警告和允许的下一步操作。

### 6.2 持仓、交易和图表

```text
GET    /api/market/portfolio/overview
GET    /api/market/portfolio/positions
GET    /api/market/portfolio/trades
GET    /api/market/portfolio/symbols/{symbol}
POST   /api/market/portfolio/trades/{tradeId}/correct
```

`symbols/{symbol}` 返回持仓摘要、交易列表、与现有行情组合后的买卖点以及行情 freshness。K 线仍由现有行情查询能力提供，portfolio 不复制 K 线表。

### 6.3 复盘

```text
GET    /api/market/portfolio/reviews
POST   /api/market/portfolio/reviews
GET    /api/market/portfolio/reviews/{reviewId}
PUT    /api/market/portfolio/reviews/{reviewId}
POST   /api/market/portfolio/reviews/{reviewId}/complete
```

控制器按任务边界拆分，不创建一个包含全部逻辑的 `PortfolioController` 或 `PortfolioAppService`。

## 7. 异步解析方案

### 7.1 已确定的是行为，不是框架

业务已确定：上传不能等待模型；任务必须持久化；用户离开页面后仍可恢复；失败可重试；同一任务不能被重复确认。

当前没有确定启用 Spring scheduling，也没有确定启用 JobRunr。`@Scheduled + PostgreSQL queue` 只是候选实现，不是产品 scope，也不是已经批准的技术决定。

### 7.2 实现前 POC

POC 只回答四个问题：

1. 仓库现有 AI/后台执行模式能否接收任务 ID，并在请求返回后执行 vision call。
2. 服务重启后，处于 `UPLOADED` 或 `PROCESSING` 的任务如何恢复。
3. 多实例下如何保证同一任务同一时间只处理一次。
4. 超时、重试和最终失败如何留下可查询状态。

候选路径按优先级评估：

1. 复用仓库已有且正在运行的后台执行抽象，只为 portfolio 增加 adapter。
2. 若没有可复用抽象，采用 market 自有的持久任务 claim adapter；再评估触发方式。
3. JobRunr、Kafka 或新工作流框架只有在前两项无法满足恢复和并发要求时才进入提案。

进程内 `CompletableFuture` 或纯内存队列不能作为生产方案，因为服务重启会丢失任务。

### 7.3 稳定端口

无论选择何种 runner，业务层只依赖：

- `PortfolioImportDispatchPort`：请求异步处理某个任务 ID。
- `PortfolioScreenshotParsePort`：提交图片并返回结构化模型结果。
- `PortfolioImportTaskRepository`：加载任务并执行状态迁移。

因此异步实现选择不会渗透到 controller、会计规则或 UI API。

## 8. AI 解析契约

`cynexus-ai` 的 prompt/schema 至少约束：

- 只提取图片中明确可见的事实。
- 支持一张图多行交易。
- 缺失字段为 `null`，不得补全或推测。
- `side` 只允许 `BUY` / `SELL`。
- 数值返回标准十进制字符串，禁止货币符号混入。
- 每个字段返回 confidence 和可定位的 evidence text。
- 顶层包含 `isTradeScreenshot`、`candidates`、`warnings`。

Market domain 再执行：

- JSON schema 和字段白名单校验。
- A 股证券代码匹配。
- 日期、数量、价格和费用规范化。
- 金额交叉校验和疑似重复提示。
- 候选数量、字符串长度和错误信息上限。

AI 输出不包含任何持仓、成本或盈亏计算。

## 9. 前端结构

建议文件边界：

```text
cynexus-ui/src/services/market/portfolio/
  imports.ts
  portfolio.ts
  reviews.ts
  types.ts

cynexus-ui/src/views/market/personal-portfolio/
  overview/
  import-review/
  position-detail/
  review-editor/
  components/
  hooks/
  lib/
```

路由：

```text
/personal-portfolio
/personal-portfolio/imports/:taskId
/personal-portfolio/stocks/:symbol
/personal-portfolio/reviews/:reviewId
```

交互要求：

- Overview 显示待确认任务数量，任务页通过有限轮询或既有实时能力刷新状态。
- 离开任务页后不依赖浏览器保存处理状态。
- 候选表格支持逐行排除、字段错误、冲突警告、保存中和版本冲突状态。
- 图表 marker 与 K 线日期/价格口径一致；红涨绿跌之外使用方向图标和文字。
- 每个页面具备 loading、empty、error、processing、stale 和 success 状态。
- 使用项目的 `WorkspaceShell`、语义 token、shadcn/Radix 和 Lucide，不新增独立设计系统。

## 10. 一致性与安全

- 确认操作在一个数据库事务内锁定任务、校验版本、写入交易并转为 `CONFIRMED`。
- 同一确认幂等键重复请求返回同一结果。
- 持仓查询只读取有效交易，不读取候选表。
- 更正交易以原记录 + 替代记录表达，并触发确定性重算。
- 图片类型由服务端解码验证，不只信任 MIME 和扩展名。
- AI 请求只携带当前任务图片，日志不记录 base64、bytes 或完整 OCR 内容。
- owner 条件必须进入 repository 查询，不只在 controller 校验。

## 11. 测试策略

### 11.1 Domain

- 任务状态机、终态和显式重试。
- AI 空值、非法枚举、超限和格式错误。
- 买入、部分卖出、清仓、多次交易、费用和同时间排序。
- 卖超拒绝、更正后重算、重复确认幂等。
- 复盘草稿、完成和结果快照。

### 11.2 API / Persistence

- multipart 图片校验和 owner 隔离。
- candidate 乐观锁、确认事务和重复请求。
- AI 超时/失败后的可恢复状态。
- 数据库约束、部分索引和 migration 幂等。

### 11.3 UI

- 上传到确认的完整状态流。
- 页面刷新后恢复任务。
- 候选编辑、排除、警告和冲突。
- 持仓空/缺行情/stale 状态。
- 买卖点交互和复盘草稿恢复。
- 桌面与移动端无溢出、遮挡和布局跳动。

## 12. 分阶段实现与提交

得到产品确认后再进入实现。每个提交只包含该子功能及对应测试，并在提交说明或进度记录中写明完成状态和自测结果。

1. `docs(market): define personal portfolio MVP`
   - BRD、技术方案、确认后的产品口径。
2. `feat(market): add portfolio import persistence`
   - migration、task/candidate domain、repository、图片校验。
3. `feat(ai): add portfolio screenshot parsing capability`
   - prompt、binding、vision adapter、结构化解析测试。
4. `feat(market): complete portfolio import confirmation`
   - 异步 adapter、校对、确认、幂等和错误恢复。
5. `feat(market): add portfolio ledger and returns`
   - trade ledger、成本/盈亏、更正、overview API。
6. `feat(market): add trade markers and reviews`
   - 标的详情、买卖点、review domain/API。
7. `feat(ui): add personal portfolio workflow`
   - services、route/sidebar、四个页面和 UI 测试。
8. `chore(release): verify personal portfolio release`
   - 版本同步、migration bundle、编译、lint/build、Release workflow 验证。

异步 POC 的结论必须在第 2 个功能提交前记录；如需新框架，先补 ADR 并说明为什么现有能力不足，不在功能提交中顺手启用。

## 13. 首期技术完成定义

- 新 portfolio 包的依赖方向符合 DDD 分层。
- 数据只落在 `cynexus_appl_market`，AI prompt/binding 仍遵循 AI schema 治理。
- 上传、异步状态、校对、确认、持仓收益、买卖点和复盘端到端可用。
- 未确认 AI 结果永远不能进入持仓计算。
- 没有为了该功能启用未经评审的 scheduler/background server。
- 后端 focused tests 与 compile、前端 lint/build、migration 和 Release workflow 均实际通过。
- Maven 与 UI 版本保持同步。
