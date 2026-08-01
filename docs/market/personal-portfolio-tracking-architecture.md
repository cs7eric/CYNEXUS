# 个人持仓与收益追踪技术方案

## 1. 方案状态

- 状态：MVP 技术方案；RabbitMQ 异步方案已确认，待实现
- 当前阶段：设计，不实施、不启用运行时组件
- 核心约束：业务归属 `cynexus-market`，视觉模型调用复用 `cynexus-ai` 治理能力
- 消息选型：RabbitMQ 承载截图解析 command；PostgreSQL task/outbox 承载业务状态和可靠发布

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
cynexus-common-messaging
  broker-neutral envelope and transport API
  RabbitMQ transport/configuration
  Outbox relay and Inbox idempotency SPI

cynexus-market-api
  portfolio DTO / request / response contracts

cynexus-market-application
  portfolio controllers
  portfolio application services
  portfolio RabbitMQ message listener
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
  RabbitMQ publisher / topology adapter
  portfolio outbox persistence

cynexus-market-starter
  portfolio bean/runtime assembly and common messaging wiring
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
PENDING_DISPATCH -> QUEUED -> PROCESSING -> READY_FOR_REVIEW -> CONFIRMED
        |              ^          |        |              |
        |              |          |        +-> FAILED     +-> CANCELLED
        |              +-- RETRY_WAITING <-+
        +-------------------------------------------> CANCELLED
FAILED -> PENDING_DISPATCH (用户显式重试并创建新 command)
```

约束：

- 一个任务只有一张截图。
- `PENDING_DISPATCH` 表示 outbox 尚未得到 broker confirm；`QUEUED` 表示 command 已可靠进入 RabbitMQ。
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
- `status`, `version`, `attempt_count`, `queued_at`
- `capability`, `prompt_version`, `provider_code`, `model_code`
- `started_at`, `completed_at`, `confirmed_at`
- `last_error_code`, `last_error_message`
- `idempotency_key`, `created_at`, `updated_at`, `deleted_at`

索引覆盖 owner + status + created_at。任务状态是 UI 和业务恢复的事实来源；RabbitMQ delivery 状态不能替代它。

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

### 5.6 `portfolio_outbox_event`

可靠发布 `portfolio.import.parse-requested.v1` command：

- `id`, `message_id`, `aggregate_type`, `aggregate_id`
- `message_type`, `event_name`, `schema_version`, `routing_key`
- `payload`, `headers`, `status`
- `attempt_count`, `available_at`, `claimed_at`, `claimed_by`
- `published_at`, `last_error_code`, `last_error_message`
- `created_at`, `updated_at`

任务和 outbox 在同一个 PostgreSQL 事务内写入。`message_id` 唯一，待发布索引覆盖 `status + available_at`。完整平台规则见 `docs/messaging/rabbitmq-architecture.md`。

首期不增加 account、cash event、performance snapshot、trade cycle 或通用 inbox 表。Portfolio consumer 使用任务状态机完成幂等；没有天然状态机的未来消费者再按需增加 inbox。

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

## 7. RabbitMQ 异步解析方案

### 7.1 消息边界

RabbitMQ 只负责把截图解析 command 可靠交给 `cynexus-market` consumer：

```text
Upload API
  -> transaction: asset + task(PENDING_DISPATCH) + outbox
  -> 202 taskId
  -> outbox relay publishes command
  -> Market consumer calls cynexus-ai
  -> candidates + READY_FOR_REVIEW
```

用户校对、确认、交易写入和持仓/盈亏计算不进入 MQ，仍在同步数据库事务中完成。

### 7.2 拓扑

- Exchange：`cynexus.command`，topic、durable。
- Routing key：`portfolio.import.parse-requested.v1`。
- Main queue：`cynexus.market.portfolio-import-parse.v1`。
- Retry queues：30 秒、2 分钟、10 分钟三级 TTL + dead-letter routing。
- DLQ：`cynexus.market.portfolio-import-parse.dlq.v1`。
- 生产使用 quorum queue；首期 consumer concurrency `2`、prefetch `1`，均可配置。

消息只携带 `messageId`、schema/trace 元数据和 `taskId`。不携带图片、Base64、Prompt、候选值或 owner；consumer 从 owner-scoped 任务记录加载图片。

### 7.3 Transactional Outbox

上传事务同时写 `portfolio_import_task` 和 `portfolio_outbox_event`，避免数据库成功但消息丢失。

Outbox relay 使用 `FOR UPDATE SKIP LOCKED` claim 待发布记录，通过 publisher confirm + mandatory routing 确认 broker 接收后才标记 `PUBLISHED`。事务提交后立即唤醒 relay；低频恢复扫描负责 broker 中断和应用重启后的补发。

恢复扫描可以使用 Spring `TaskScheduler`，但它只转发 outbox，不消费 portfolio 业务任务、不调用 AI，也不构成 PostgreSQL 任务队列。未来如果引入 CDC，可替换 relay adapter，不改变业务事务和消息契约。

### 7.4 Consumer 与幂等

`PortfolioImportMessageListener` 是 inbound adapter，只解析 envelope 并调用 `PortfolioImportWorkerAppService`。AppService 编排 task claim、图片加载、AI port 调用和结果保存；状态规则和候选校验属于 domain。

Consumer 使用 manual ACK：

1. 校验 message schema 和大小。
2. 条件更新 `{PENDING_DISPATCH, QUEUED, RETRY_WAITING} -> PROCESSING`；终态或重复状态直接 ACK。
3. 加载当前任务图片并调用 `PortfolioScreenshotParsePort`。
4. 在事务中保存候选并更新 `READY_FOR_REVIEW`。
5. 数据库提交成功后 ACK。

系统采用 at-least-once。重复 delivery、ACK 丢失或 consumer 重启可能重复到达，但任务状态机保证不会重复创建候选或交易。AI 调用后、结果提交前崩溃可能导致再次调用模型，这是允许的成本，不影响账本正确性。

RabbitMQ 可能在 relay 写回 `QUEUED` 前交付消息，因此 consumer 允许从 `PENDING_DISPATCH` claim；relay 在 publisher confirm 后只能把仍为 `PENDING_DISPATCH` 的任务条件更新为 `QUEUED`，不能把已经 `PROCESSING` 的任务回退。

### 7.5 重试与失败

- AI timeout、429、临时 5xx 和网络错误进入 30 秒/2 分钟/10 分钟重试。
- 非交易截图、图片无法解码和结构永久非法直接更新任务 `FAILED` 并 ACK。
- 消息 schema 不支持、envelope 缺失或超限进入 DLQ 并告警。
- 重试超限后任务最终失败；用户显式重试会创建新的 outbox command 和 message ID。
- 用户取消后 consumer 直接 ACK；AI 调用期间取消时允许调用完成，但保存结果前再次检查并丢弃结果。

### 7.6 稳定端口

业务层依赖：

- `PortfolioImportDispatchPort`：在业务事务内登记待发布 command。
- `PortfolioScreenshotParsePort`：提交图片并返回结构化模型结果。
- `PortfolioImportTaskRepository`：加载任务并执行状态迁移。
- Outbox store、Rabbit topology 和 listener adapter 由 `cynexus-market-infra` 提供，可靠 relay/transport 由 `cynexus-common-messaging` 提供。

RabbitMQ client、listener container、exchange/queue declaration 和 publisher confirm 均属于 infra/starter wiring。平台级约束见 `docs/messaging/rabbitmq-architecture.md`。

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
- task + outbox 原子写入、publisher confirm、重复 delivery 和 manual ACK 边界。
- RabbitMQ 中断、应用重启、三级 retry 和 DLQ/re-drive。

### 11.3 UI

- 上传到确认的完整状态流。
- 页面刷新后恢复任务。
- 候选编辑、排除、警告和冲突。
- 持仓空/缺行情/stale 状态。
- 买卖点交互和复盘草稿恢复。
- 桌面与移动端无溢出、遮挡和布局跳动。

## 12. 分阶段实现与提交

得到产品确认后再进入实现。每个提交只包含该子功能及对应测试，并在提交说明或进度记录中写明完成状态和自测结果。

1. `docs(messaging): define RabbitMQ architecture`
   - common messaging module boundary and broker conventions。
2. `feat(messaging): add common messaging foundation`
   - `api`、`rabbit`、`outbox` 子模块、契约、publisher-confirm transport、relay/inbox SPI。
3. `feat(messaging): add RabbitMQ release infrastructure`
   - RabbitMQ、vhost/user/policy、health、配置和 Release smoke。
4. `docs(market): define personal portfolio MVP`
   - BRD、技术方案、确认后的产品口径。
5. `feat(market): add portfolio import persistence and outbox`
   - migration、task/candidate domain、repository、图片校验、outbox 原子写入。
6. `feat(ai): add portfolio screenshot parsing capability`
   - prompt、binding、vision adapter、结构化解析测试。
7. `feat(market): add RabbitMQ portfolio parse consumer`
   - topology、publisher confirm、consumer、retry/DLQ、校对和恢复测试。
8. `feat(market): complete portfolio import confirmation`
   - 候选校对、确认、幂等和更正入口。
9. `feat(market): add portfolio ledger and returns`
   - trade ledger、成本/盈亏、更正、overview API。
10. `feat(market): add trade markers and reviews`
   - 标的详情、买卖点、review domain/API。
11. `feat(ui): add personal portfolio workflow`
   - services、route/sidebar、四个页面和 UI 测试。
12. `chore(release): verify personal portfolio release`
   - 版本同步、migration bundle、编译、lint/build、Release workflow 验证。

RabbitMQ 基础设施、Outbox 和 portfolio consumer 分开提交。每个阶段记录完成状态和实际自测结果，不把 broker、业务表和完整 UI 堆入同一提交。

## 13. 首期技术完成定义

- 新 portfolio 包的依赖方向符合 DDD 分层。
- 数据只落在 `cynexus_appl_market`，AI prompt/binding 仍遵循 AI schema 治理。
- 上传、异步状态、校对、确认、持仓收益、买卖点和复盘端到端可用。
- 未确认 AI 结果永远不能进入持仓计算。
- RabbitMQ、Outbox、幂等 consumer、三级重试和 DLQ 的失败恢复场景实际通过。
- Spring scheduler 如被使用，仅用于 Outbox relay 的恢复扫描，不承担 AI 业务任务执行。
- 后端 focused tests 与 compile、前端 lint/build、migration 和 Release workflow 均实际通过。
- Maven 与 UI 版本保持同步。
