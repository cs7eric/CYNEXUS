# Findings And Decisions

## Requirements

- Add a personal holdings and return-tracking capability below Stock Overview.
- Support one uploaded trading-record screenshot per asynchronous AI parsing task.
- Track positions and investment returns.
- Display buy and sell points on price charts.
- Support post-trade and periodic review workflows.
- Re-evaluate the feature as a product user and against comparable products.
- Implement the owning business capability in `cynexus-market` with proper DDD layering.
- Persist application data in `cynexus_appl_market` following table conventions.
- Put frontend API clients under domain-specific `cynexus-ui/src/services` folders, not `lib/api.ts`.
- Add complete UI pages, routes, components, and workflow states consistent with the project style.
- Extend frameworks only when repository capability is insufficient and justify any POC or dependency.
- Later implementation must use feature-sized commits with completion status and self-test results.
- The portfolio feature must be designed as a new bounded context and must not be placed inside Finance Capture, Stock Review, or existing Stock Overview page internals.

## Research Findings

- The working tree contains substantial user-owned changes across market, AI, data, UI, migrations, and version files. New work must be isolated and commits must include only task-owned paths/hunks.
- CodeGraph CLI is installed; CodeGraph MCP and FastCtx MCP are not exposed in this session.
- The CodeGraph index contains 2,682 files and 46,079 nodes, with two pending file changes requiring synchronization before impact analysis.
- No closer `AGENTS.md` exists under `cynexus-market`; root guidance and `docs/agent/backend-ddd.md` govern market backend design.
- Existing durable market documentation currently contains only the ETF redesign note, so this feature needs its own BRD/architecture artifacts rather than being appended to an unrelated design.
- DDD rules require transaction normalization, return accounting, position state transitions, prompt resolution, parsing, and validation in the market/AI domain layers; Application Services may only orchestrate and map DTOs.
- Database migrations belong under `deploy/local/postgres/migrations`, while durable product/architecture design belongs under `docs` or `design`; market migrations must be included in the Release market migration bundle.
- Every AI parser capability must align capability code, DB prompt key/version, usage binding, execution log identity, runtime route, provider UI metadata, fallback prompt, structured validation, and observability.
- Autonomous Agent exposure is outside this MVP. Portfolio mutations remain user-driven; raw images and import candidates must never be model-callable through the autonomous Agent.
- The UI is a restrained, dense A-share research workbench. The new route must use `WorkspaceShell`, semantic tokens, domain services under `src/services`, and synchronized route/sidebar/breadcrumb labels.
- Each data block must distinguish loading, error, empty, stale/unavailable, partial, saving, and success states. Market direction uses red-up/green-down with non-color cues.
- Recharts through the shared chart primitives is preferred for ordinary charts; existing on-demand ECharts is justified for dense date categories, dual axes, or data zoom and must be disposed correctly.
- Page structure should prioritize context/freshness, primary action, filters, decision-relevant position/return data, then detail. Avoid card walls, nested cards, decorative gradients, and modal-first workflows.
- Stock chart querying already follows Controller -> AppService -> `StockChartQueryDomainService` -> repository, with a composite repository sourcing Postgres, ClickHouse, local TDX, and HTTP F10 data. Trade markers should consume this existing read contract rather than duplicate K-line ingestion.
- `MarketOverview` already has market DTO/domain/repository/controller layers and both current/history Agent tool handlers, providing the closest bounded market-read pattern.
- The active frontend Stock Overview entry is `cynexus-ui/src/views/stock-overview/index.tsx`; a new personal portfolio entry should be placed directly beneath it in market navigation as requested, while remaining a separate route/workflow.
- Initial CodeGraph searches found no `FileUpload`, `accounting_receipt_parse`, or `CurrentUser` symbols, so exact upload/security naming must be discovered through related symbols and scoped source inspection.
- Existing Finance Agent transaction reads demonstrate the desired owner-scoped, bounded read adapter style; portfolio overview/history reads should follow that pattern if Agent exposure is accepted.
- `FinanceAiCaptureController/AppService/DomainService` already supports multipart input, `Idempotency-Key`, AI parser ports, candidate mapping, persisted capture history, and optional posting. It is the closest import workflow reference, but portfolio imports should require an explicit preview confirmation because errors change positions and P&L.
- Gateway identity is signed using trusted principal and tenant headers. AI has an `AgentIdentityResolver`; the market feature needs an equivalent common/request identity boundary or a shared resolver, never a caller-supplied owner override.
- `StockChartQueryDomainService` exposes daily K-lines and many adjacent stock reads, but the interface has 125 affected symbols. Portfolio annotations should compose its stable `klines` read rather than broaden or couple the shared interface unnecessarily.
- Stock Overview contains 12 files with an overview page plus a dedicated `market-pulse` sub-workbench. Market API clients are already grouped under `cynexus-ui/src/services/market` with 14 focused files, confirming the new client belongs there under a portfolio-specific name/folder.
- The new page should be a separate feature folder and route rather than expanding the already broad `stock-overview/index.tsx`; navigation ordering can still place it immediately below Stock Overview.
- The `klines` domain method normalizes period/adjustment/source and selects daily or minute repositories. This is sufficient for server-side/combined trade-marker timelines without a new market-data framework.
- Finance capture already implements parse, confirm, retry, archive, idempotency hash, and persisted item workflows. These lifecycle concepts should be adapted to an investment import aggregate.
- CodeGraph found no existing Java use of Apache POI, Commons CSV, EasyExcel, OpenCSV, or Tika. Import implementation may require a minimal deterministic CSV/XLSX parsing dependency after confirming POMs, with AI limited to mapping/repair rather than primary arithmetic.
- Cynexus AI already exposes synchronous structured vision execution through `/api/ai/executions/vision`, `ScenarioExecutionDomainService.prepareVisionExecution`, `AiImageInputBO`, and `AiCompletionGateway.completeVision`.
- A market-owned durable worker can call the synchronous vision capability asynchronously after upload. Upload latency is then bounded, while task lifecycle/retry/recovery remains in the new market bounded context.
- Media Transcription delegates task ownership to the Python executor and is not an appropriate aggregate to reuse. Its task-ref/status API confirms the UI polling contract, but portfolio import should persist its own states and artifacts in `cynexus_appl_market`.
- The existing receipt client base64-encodes images and retries a synchronous vision request twice, but performs weak manual JSON parsing. The new portfolio parser must use a dedicated capability, strict output schema, bounded image size/type/dimensions, domain normalization, confidence/warning fields, and persistent execution metadata.
- The market migration release script explicitly lists market SQL files, so the portfolio migration must be added to that bundle in implementation order.
- Market migrations use `cynexus_appl_market`, bigint IDs, `created_at/updated_at/deleted_at` conventions, JSONB `extra/warnings`, partial unique indexes for active records, and explicit CHECK constraints. New personal ledger tables should preserve these conventions while treating confirmed trades as immutable business facts.
- Existing market AI task code demonstrates useful persistence, claim, retry, and recovery patterns, but does not by itself decide which runner this feature should enable.
- Market declares JobRunr but explicitly disables its scheduler/background server/dashboard and contains no indexed production enqueue API usage. Spring scheduling, JobRunr, Kafka, and new workflow engines remain unselected until the implementation POC proves what existing runtime abstraction can satisfy the required behavior.
- Root Maven and UI package versions currently agree at `1.1.398-SNAPSHOT`; implementation must preserve synchronization after functional commits.
- CodeGraph and repository searches found no current Kafka, RabbitMQ, RocketMQ, Spring AMQP, or Spring Cloud Stream integration; RabbitMQ is a new shared infrastructure capability, not a reuse of an existing broker.
- The user selected RabbitMQ after comparing RabbitMQ, RocketMQ, and Kafka for future Cynexus messaging needs.
- RabbitMQ should own work dispatch and domain-event delivery, while PostgreSQL remains the source of truth for portfolio task state; reliable cross-system publication requires Transactional Outbox and consumer idempotency.
- RabbitMQ local infrastructure is now running from `deploy/local/docker-compose.yml` as `cynexus-rabbitmq` on `cynexus-dev_default`, with persisted bind storage at `deploy/local/rabbitmq/data`, AMQP port `5672`, management port `15672`, and image tag `rabbitmq:4.1-management-alpine`.
- The earlier recommendation to defer `cynexus-common-messaging` was too narrow for the approved multi-service RabbitMQ direction. Enterprise implementation requires a broker-neutral API module, a Rabbit transport module, and an Outbox/Inbox reliability module, while business bounded contexts own their own ports, tables, queues, and commands.
- Existing `cynexus-common` is a Maven parent with focused modules and no RabbitMQ integration; adding a new parent under it follows the repository's current module convention.

## Technical Decisions

| Decision | Rationale |
|----------|-----------|
| Pending repository discovery | Architectural choices will be made only after inspecting existing contracts and callers. |
| Design the import as an auditable staged workflow | AI output is untrusted and brokerage files vary; users need preview, correction, validation, duplicate detection, and explicit commit before ledger effects. |
| Reuse only low-level technical patterns from finance/media | The portfolio aggregate, lifecycle, APIs, tables, UI, and product concepts must be newly designed under `cynexus-market`; existing features may inform multipart/idempotency/async infrastructure only. |
| Superseded: deterministic file parser first | The original request sounded like file-record import; the user clarified MVP input is a single screenshot, so CSV/XLSX parsing is deferred and not part of the proposed first release. |
| Replace file parser scope with screenshot vision parsing | The clarified input is one screenshot, so CSV/XLSX dependencies are out of MVP scope; use a vision-capable model with structured output and domain validation. |
| Market owns async task; AI owns model execution | This preserves the new `cynexus-market` business aggregate while reusing the dedicated AI provider/prompt/routing runtime and avoiding synchronous upload requests. |
| One implicit personal ledger, no portfolio management | The requested value is import, tracking, markers, and review; account/portfolio administration is separate product scope. |
| No complete account-level return percentage in MVP | Trade screenshots do not provide reliable starting capital, deposits, withdrawals, dividends, or cash balance. Show realized/unrealized/total P&L and only rates with explicit denominators. |
| Superseded: async framework selection deferred to POC | The user approved RabbitMQ as the messaging platform after the alternatives were analyzed. |
| RabbitMQ for portfolio parsing and reusable business messaging | It matches asynchronous work, routing, ACK/retry/DLQ, and moderate-throughput domain events. Kafka is deferred for future high-throughput replayable streams; RocketMQ has no current project-specific advantage. |
| Market consumer owns portfolio processing | `cynexus-market` consumes the command, loads the owner-scoped task/image, calls `cynexus-ai`, validates candidates, and persists task state; AI must not consume portfolio commands or access market tables. |

## Issues Encountered

| Issue | Resolution |
|-------|------------|
| Requested first-class MCP inspection tools are unavailable | Use CodeGraph CLI first, then scoped local file reads as the documented fallback. |

## Resources

- `AGENTS.md`
- `.agents/skills/cynexus-backend/SKILL.md`
- `.agents/skills/cynexus-frontend/SKILL.md`
- `.agents/skills/cynexus-ai/SKILL.md`
- `.agents/skills/cynexus-agent-tools/SKILL.md`

## Visual/Browser Findings

- No browser or visual inspection performed yet.
