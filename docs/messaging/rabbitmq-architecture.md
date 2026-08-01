# Cynexus RabbitMQ 消息架构

## 1. 决策状态

- 状态：已选型，待实现
- 决策：RabbitMQ 作为 Cynexus 业务命令和领域事件的消息平台
- 首个落地场景：Personal Portfolio 交易截图异步 AI 解析
- 不替代：PostgreSQL 业务事实、任务状态、审计记录和用户确认事务

RabbitMQ 解决可靠任务投递、消费确认、路由、重试和死信。Kafka 仅在未来出现高吞吐、长期事件保留、任意时间回放、CDC 或流式计算需求时评估；RocketMQ 当前不采用。

## 2. 适用范围

适合进入 RabbitMQ 的消息：

- 长耗时、可重试的后台命令，例如截图解析、文档索引和媒体处理。
- 一个业务事实需要通知多个独立消费者，例如交易确认后触发提醒和审计集成。
- 不要求当前 HTTP 请求内完成的跨服务工作。

不应进入 RabbitMQ：

- 用户确认交易以及同事务内必须完成的持仓/盈亏计算。
- 普通同步查询。
- 大体积图片、文件、Base64、Prompt 全文或模型原始响应。
- 需要长期保留和任意回放的高频行情流；此类需求应重新评估 Kafka。

## 3. 核心语义

- 交付语义：at-least-once。
- 正确性来源：消费者幂等，而不是假设消息只到达一次。
- 消息顺序：只在明确的 aggregate 内要求；首期 portfolio 任务通过任务状态机串行化，不依赖全局顺序。
- RabbitMQ 是传输系统，不是业务事实源。
- 每个消费者拥有独立队列；多个业务能力不得共享同一队列抢占同一事件。

## 4. 逻辑拓扑

### 4.1 Virtual host

- 本地/Release：独立 vhost `/cynexus`。
- 生产：环境隔离，例如 `/cynexus-prod`、`/cynexus-staging`。
- 禁止服务使用 RabbitMQ 默认 `guest` 账户远程访问。

### 4.2 Exchanges

| Exchange | 类型 | 用途 |
| --- | --- | --- |
| `cynexus.command` | topic, durable | 点对点业务命令 |
| `cynexus.event` | topic, durable | 可被多个消费者订阅的领域事件 |
| `cynexus.retry` | topic, durable | 分级延迟重试 |
| `cynexus.dlx` | topic, durable | 超限或不可恢复消息 |

不使用 auto-delete exchange/queue。生产业务队列使用 durable quorum queue；单节点 Release 环境仍可运行 quorum queue，但不提供节点级高可用。

### 4.3 Routing key

统一格式：

```text
<bounded-context>.<aggregate-or-capability>.<action>.v<major>
```

示例：

```text
portfolio.import.parse-requested.v1
portfolio.import.review-ready.v1
portfolio.trade.confirmed.v1
knowledge.document.index-requested.v1
media.transcription.requested.v1
market.data.refreshed.v1
```

路由 key 的 major version 只在不兼容变更时升级。兼容字段新增保持原版本。

### 4.4 Queue naming

统一格式：

```text
cynexus.<consumer-service>.<purpose>.v<major>
```

Portfolio 首期：

```text
cynexus.market.portfolio-import-parse.v1
cynexus.market.portfolio-import-parse.retry-30s.v1
cynexus.market.portfolio-import-parse.retry-2m.v1
cynexus.market.portfolio-import-parse.retry-10m.v1
cynexus.market.portfolio-import-parse.dlq.v1
```

## 5. 消息契约

消息使用 UTF-8 JSON，不使用 Java 原生序列化。公共 envelope：

```json
{
  "messageId": "snowflake-id",
  "messageType": "COMMAND",
  "name": "portfolio.import.parse-requested",
  "schemaVersion": 1,
  "producer": "cynexus-market",
  "aggregateType": "PortfolioImportTask",
  "aggregateId": "task-id",
  "occurredAt": "2026-08-01T10:00:00Z",
  "traceId": "trace-id",
  "correlationId": "upload-request-id",
  "causationId": null,
  "payload": {
    "taskId": "task-id"
  }
}
```

规则：

- `messageId` 全局唯一，重试保持相同 ID，用户重新发起任务使用新 ID。
- `aggregateId` 用于日志、幂等和局部顺序，不作为授权凭证。
- owner、principal、图片地址和密钥默认不进入消息；消费者从自己的可信业务记录加载。
- 消息 schema 需要版本化测试；消费者必须忽略未知的兼容字段。
- 消息和 headers 设置大小上限，首期建议 64 KiB；超限 payload 应存储后仅传引用 ID。

## 6. 生产端可靠性：Transactional Outbox

PostgreSQL 写入和 RabbitMQ 发布不能组成单一原子事务，禁止使用“先写数据库、再直接 publish”作为唯一可靠路径。

业务事务同时写入 aggregate 和该服务自有 outbox：

```text
BEGIN
  INSERT/UPDATE business aggregate
  INSERT messaging_outbox
COMMIT
```

每个服务在自己的应用 schema 内拥有 outbox，不建立跨服务共享表。Portfolio 首期使用 `cynexus_appl_market.portfolio_outbox_event`。

建议字段：

- `id`, `message_id`, `aggregate_type`, `aggregate_id`
- `message_type`, `event_name`, `schema_version`, `routing_key`
- `payload`, `headers`
- `status`: `PENDING` / `PUBLISHING` / `PUBLISHED` / `FAILED`
- `attempt_count`, `available_at`, `claimed_at`, `claimed_by`
- `published_at`, `last_error_code`, `last_error_message`
- `created_at`, `updated_at`

发布流程：

1. 事务提交后优先立即唤醒 relay，降低延迟。
2. relay 使用 `FOR UPDATE SKIP LOCKED` 批量 claim `PENDING` 记录。
3. 使用 RabbitMQ publisher confirm 和 mandatory routing。
4. broker confirm 后标记 `PUBLISHED`。
5. nack、unroutable 或连接失败时释放 claim，并以有上限的退避重试。
6. 服务启动和低频 reconciliation 必须重新扫描未发布记录。

这里的 relay 只负责数据库与 RabbitMQ 的可靠交接，不执行 AI 任务，也不是 PostgreSQL 业务任务队列。首期可由 Spring `TaskScheduler` 驱动快速唤醒和恢复扫描；未来若平台引入 CDC，可替换 relay adapter，而不改变业务事务和消息契约。

## 7. 消费端可靠性

### 7.1 ACK 边界

使用 manual ACK：

1. 校验 envelope 和 schema version。
2. 执行业务状态 claim 或 inbox 幂等检查。
3. 完成外部调用和数据库事务。
4. 数据库提交成功后 ACK。

数据库提交前不能 ACK。无法识别的 schema、缺少必填字段或违反大小限制的消息直接进入 DLQ，不无限 requeue。

### 7.2 幂等策略

消费者必须选择一种可测试的幂等方式：

- 聚合状态机/条件更新，例如 `{PENDING_DISPATCH, QUEUED, RETRY_WAITING} -> PROCESSING`，适合 portfolio parse command。
- 业务唯一键，例如来源 task + candidate row。
- consumer inbox，以 `(consumer_name, message_id)` 唯一约束，适合没有天然状态机的事件消费者。

平台不承诺 exactly-once。重复消息、消费者崩溃和 ACK 丢失均按正常情况设计。

### 7.3 并发和背压

- Portfolio AI consumer 初始 concurrency `2`，prefetch `1`。
- 并发、prefetch 和单用户活跃任务数均通过配置控制。
- 视觉模型限流必须在 consumer 调用端生效，不能依靠 RabbitMQ 吞吐自然限流。
- 队列积压超过告警阈值时停止扩大入口流量，并向 UI 返回可理解的排队状态。

## 8. 重试与死信

错误分为三类：

| 类型 | 示例 | 行为 |
| --- | --- | --- |
| 瞬时错误 | AI timeout、429、临时 5xx、网络闪断 | 30 秒、2 分钟、10 分钟分级重试 |
| 永久业务错误 | 非交易截图、无法解码、字段结构永久非法 | 更新业务任务为 `FAILED`，ACK，不重试 |
| 非法消息 | schema 不支持、消息超限、必填 envelope 缺失 | 投递 DLQ，并告警 |

延迟队列使用 TTL + dead-letter routing，不依赖 RabbitMQ delayed-message 插件。消费者需要重试时，将原 `messageId` 和更新后的 attempt metadata 可靠发布到对应 retry queue，publisher confirm 成功后 ACK 原消息；重试 publish 失败则 NACK/requeue 原消息。

超过最大次数后：

- 将消息投递到业务 DLQ。
- 将对应任务更新为最终失败。
- UI 提供显式“重新解析”；重新解析创建新的 command message，不直接修改 DLQ 消息。
- 运维 redrive 必须记录操作者、原因和新 message ID。

## 9. Portfolio 消息流程

```text
Upload API
  -> transaction: asset + import_task(PENDING_DISPATCH) + outbox
  -> 202 taskId

Outbox relay
  -> cynexus.command
  -> portfolio.import.parse-requested.v1

Market portfolio consumer
  -> claim task: pending/queued/retry-waiting -> PROCESSING
  -> load task-owned image
  -> call cynexus-ai vision capability
  -> validate and persist candidates
  -> task READY_FOR_REVIEW
  -> ACK

UI
  -> GET task
  -> edit candidates
  -> explicit confirm transaction
  -> trade ledger + P&L update
```

Consumer 必须位于 `cynexus-market`。`cynexus-ai` 不消费 portfolio command、不读取 market 表，只提供受治理的视觉模型执行能力。

取消竞态：

- 消费前发现任务已 `CANCELLED`：直接 ACK。
- AI 调用期间用户取消：模型调用可能无法撤销，但保存结果前再次检查状态并丢弃结果。
- 任务已 `READY_FOR_REVIEW` 或 `CONFIRMED` 时收到重复消息：直接 ACK，不重复调用 AI。
- broker 可能在 outbox relay 写回 `QUEUED` 前交付消息；consumer 允许从 `PENDING_DISPATCH` claim，relay 只能条件更新仍为 `PENDING_DISPATCH` 的任务，禁止把 `PROCESSING` 回退为 `QUEUED`。

## 10. 公共模块和代码分层

RabbitMQ 会被多个业务使用，因此公共能力正式落入 `cynexus-common-messaging`，但不把业务模型塞进 common。

```text
cynexus-common
  cynexus-common-messaging
    cynexus-common-messaging-api
      MessageEnvelope / MessageDestination / MessageTransport
    cynexus-common-messaging-rabbit
      RabbitMessageTransport / connection / publisher-confirm / topology SPI
    cynexus-common-messaging-outbox
      OutboxRecord / OutboxStore / OutboxRelay / InboxStore SPI

cynexus-market
  portfolio domain/application
  portfolio outbox store and migration
  portfolio Rabbit topology definition
  portfolio command listener -> PortfolioImportWorkerAppService
```

职责边界：

- `api` 不依赖 Spring AMQP，提供版本化 envelope、destination、message type 和 broker-neutral transport port。
- `rabbit` 封装 RabbitTemplate、publisher confirm、mandatory return、header propagation、listener factory、连接属性和 topology registration。
- `outbox` 封装 claim lease、批量 relay、publish success/failure、退避和 inbox 幂等 SPI；不拥有任何业务表名。
- 每个 bounded context 自己提供 outbox/inbox store、事务表和业务 routing key。
- Domain 只依赖自己的 `PortfolioImportDispatchPort`，不依赖 common messaging 或 Rabbit 类型。
- Application listener 负责把通用 envelope 映射为业务 command，再调用 application service。
- Starter 负责启用模块、连接、队列、relay、health 和 metrics。

公共模块禁止提供 `MqUtil.send()`、动态执行任意 routing key、跨业务共享 queue 或基于 Java 原生序列化的通用接口。公共模块提供的是可靠性和传输能力，不提供 portfolio 类型。

## 11. 配置与安全

配置项至少包括：

```text
host / port / virtual-host
username / password or secret reference
TLS enabled / trust material
publisher-confirm-type=correlated
publisher-returns=true
consumer concurrency / prefetch
listener enabled
outbox relay enabled / batch-size / interval
retry delays / max attempts
message size and queue-lag limits
```

- 凭证由环境变量或现有 secret 机制提供，不写入仓库。
- 每个服务使用独立账号和最小 exchange/queue 权限。
- 生产启用 TLS、持久卷和网络访问限制。
- 日志只记录 message ID、task ID、routing key、attempt、状态和安全截断错误，不记录图片或完整 payload。

## 12. 可观测性

指标：

- publish 成功/失败、confirm latency、unroutable 数量。
- queue ready/unacked、oldest message age、consumer utilization。
- 消费成功/失败/重试/DLQ、处理耗时。
- outbox pending/oldest age/attempts。
- portfolio 排队时间、AI 调用时间和端到端完成时间。

Tracing 传播 `traceId`、`correlationId` 和 `causationId`。告警至少覆盖 broker 不健康、outbox 积压、queue lag、DLQ 新增和持续 AI 限流。

## 13. 部署方案

### 13.1 Release 环境

- 在版本化 Release stack 增加单节点 RabbitMQ 和持久卷。
- 使用固定兼容版本，不使用浮动 `latest`。
- 增加健康检查、vhost/user/permissions 初始化和端口配置。
- Release 启动脚本先验证 broker healthy，再启动启用 listener 的应用。
- 不启动 deprecated Dev 应用。

### 13.2 生产环境

- 推荐 3 节点 RabbitMQ 集群和 quorum queues。
- 节点跨故障域，持久卷独立。
- definitions、policies 和 permissions 进入版本化配置。
- 监控磁盘水位、内存水位、网络分区和证书到期。

## 14. 验证矩阵

实现至少验证：

- DB 提交成功但 broker 不可用：outbox 保留并在恢复后投递。
- publisher confirm 丢失：允许重复 publish，consumer 不重复处理。
- consumer 在 AI 调用前、调用后、DB 提交前和 ACK 前崩溃：任务可恢复且不产生重复交易。
- transient error 按 30 秒/2 分钟/10 分钟重试。
- permanent error 不重试，非法消息进入 DLQ。
- 重复 delivery、乱序 delivery、取消竞态和手工重新解析。
- owner 隔离、消息大小、敏感日志和不受支持 schema。
- broker 重启、应用重启和 Release stack 重启。
- Maven compile、focused tests、消息集成测试、版本同步和 Release smoke workflow。

## 15. 实施顺序

1. `docs(messaging): define RabbitMQ architecture`
2. `feat(messaging): add RabbitMQ release infrastructure`
3. `feat(messaging): add reliable outbox publishing`
4. `feat(market): add portfolio parse command consumer`
5. `test(messaging): verify retries idempotency and recovery`

每个提交记录完成状态和实际自测结果。基础设施、Outbox 和业务 consumer 分开提交，便于验证和回退。

## 16. Kafka 重新评估条件

出现以下任一明确需求时单独立项，不把 Kafka 伪装为 RabbitMQ 升级：

- 高频行情或遥测需要持续高吞吐分区流。
- 消费者需要按 offset 任意回放历史。
- 事件需要保留数天或更久并反复重算。
- PostgreSQL CDC、ClickHouse 流式摄取或流处理成为核心链路。
- 多个数据消费者需要互不影响地从同一历史日志建立状态。

RabbitMQ 继续承载业务 command/event；Kafka 承载 data stream，两者可以按职责并存。
