# Progress Log

## Session: 2026-08-01

### Phase 8: Portfolio Domain Foundation

- **Status:** complete
- Actions taken:
  - Added the independent `portfolio` domain package under `cynexus-market`.
  - Implemented import-task lifecycle transitions with terminal-state and explicit-retry rules.
  - Implemented moving weighted average cost, realized/unrealized P&L, sell-over-position validation, and replacement-aware trade filtering.
  - Added focused tests for state transitions, idempotent processing claims, retry behavior, and ledger calculations.
- Verification:
  - `mvn -pl cynexus-market/cynexus-market-domain -am test` passed with 47 tests.
- Commit:
  - `feat(market): add portfolio domain ledger foundation`

### Phase 8: Portfolio Persistence Schema

- **Status:** complete
- Actions taken:
  - Added idempotent `cynexus_appl_market` tables for screenshot assets, import tasks, candidates, confirmed trades, reviews, and portfolio outbox events.
  - Added owner/status/time indexes and database checks for task states, candidate confidence, trade values, review ranges, and outbox lifecycle.
  - Registered the migration in the Release market migration bundle.
- Verification:
  - Applied the migration inside a PostgreSQL transaction and rolled it back successfully with `ON_ERROR_STOP=1`.
- Commit:
  - `feat(market): add portfolio persistence schema`

### Phase 8: Portfolio Upload Task And Outbox Entry

- **Status:** complete
- Actions taken:
  - Added Portfolio upload and task query API under `/api/market/portfolio/imports`.
  - Added owner-scoped MyBatis persistence adapters for assets and import tasks.
  - Added transactional outbox entry creation for `portfolio.import.parse-requested.v1`.
  - Added server-side image type/size validation and idempotency handling.
- Verification:
  - `mvn -pl cynexus-market/cynexus-market-application/cynexus-market-application-controller,cynexus-market/cynexus-market-infra -am test` passed with 50 domain tests and 6 application tests.
- Commit:
  - `feat(market): add portfolio upload task and outbox entry`

### Phase 1: Repository And Product Discovery

- **Status:** complete
- **Started:** 2026-08-01
- Actions taken:
  - Loaded the applicable planning, backend, frontend, AI, and Agent-tool workflows.
  - Confirmed CodeGraph CLI availability and absence of FastCtx/CodeGraph MCP tools.
  - Inspected worktree status and identified extensive unrelated user-owned changes.
  - Checked CodeGraph index status and located applicable market/documentation guidance.
  - Synchronized the CodeGraph index.
  - Read backend DDD, database placement, AI capability, Agent cache/tool, deploy, and complete frontend design/implementation guidance.
  - Incorporated user correction: new portfolio bounded context, one screenshot per asynchronous AI parse task, no placement inside existing business features.
  - Completed repository discovery and selected only low-level reusable infrastructure patterns.
- Files created/modified:
  - `.planning/2026-08-01-personal-portfolio-tracking/task_plan.md`
  - `.planning/2026-08-01-personal-portfolio-tracking/findings.md`
  - `.planning/2026-08-01-personal-portfolio-tracking/progress.md`

### Phase 2: BRD And Business Model

- **Status:** complete
- Actions taken:
  - Defined the standalone product, competitor framing, accounting rules, screenshot-first async workflow, reviews, non-goals, and acceptance criteria.
  - Corrected the original over-scoped draft: removed multiple portfolios, cash events, TWR/XIRR, benchmarks, broad asset support, and Agent exposure from MVP.
  - Made the return contract explicit: realized/unrealized/total P&L plus rates with valid denominators, without claiming complete account performance from trade screenshots alone.
- Files created/modified:
  - `docs/market/personal-portfolio-tracking-brd.md`

### Phase 3: Technical Architecture And Delivery Plan

- **Status:** complete
- Actions taken:
  - Replaced the over-scoped architecture with a minimal portfolio bounded context, five business tables, focused APIs, AI boundary, UI routes, tests, and feature-sized delivery commits.
  - Removed the premature Spring scheduling decision. The async business contract is fixed, while the concrete runner is deferred to a focused implementation POC.
- Files created/modified:
  - `docs/market/personal-portfolio-tracking-architecture.md`

### Phase 4: Review And Handoff

- **Status:** complete
- Actions taken:
  - Reconciled the BRD and architecture around the same screenshot-to-confirmation-to-review MVP.
  - Confirmed that no runtime, migration, backend, or frontend implementation has been started.
  - Checked the five task-owned documents and verified that scheduler, broad portfolio, full-return, and Agent concepts appear only as explicit exclusions or unselected options.

### Phase 5: RabbitMQ Architecture Decision

- **Status:** complete
- Actions taken:
  - Compared RabbitMQ, RocketMQ, and Kafka and recorded the user-approved RabbitMQ decision.
  - Confirmed no existing broker integration is present in the indexed repository or scoped dependency/configuration search.
  - Added the reusable Cynexus RabbitMQ architecture covering topology, versioned contracts, Transactional Outbox, consumer idempotency, manual ACK, tiered retry, DLQ, security, observability, Release/production deployment, and Kafka reevaluation criteria.
  - Replaced the portfolio async-runner placeholder with the concrete RabbitMQ command flow and resolved the publisher-confirm/early-consumer state race.
  - Fixed MVP defaults to A-share/CNY/long-only, 30-day screenshot retention, and sell-triggered review reminders with closed-position priority.
- Files created/modified:
  - `docs/messaging/rabbitmq-architecture.md`
  - `docs/market/personal-portfolio-tracking-architecture.md`
  - `docs/market/personal-portfolio-tracking-brd.md`

### Phase 6: RabbitMQ Shared Local Infrastructure

- **Status:** complete
- Actions taken:
  - Added `rabbitmq:4.1-management-alpine` beside Redis in `deploy/local/docker-compose.yml`.
  - Configured the shared `cynexus` account, persisted bind storage, localhost-only AMQP/management ports, and a `rabbitmq-diagnostics -q ping` health check.
  - Ran Compose validation and started only the RabbitMQ service; existing Release applications and shared stores were not restarted.
- Verified container health, `cynexus-dev_default` network aliases, AMQP diagnostics, management API authentication, and RabbitMQ server version `4.1.8`.
- Added `deploy/local/rabbitmq/data/` to `.gitignore` after confirming the bind-mounted broker state is runtime data and must remain local.
- Files created/modified:
  - `deploy/local/docker-compose.yml`
  - `deploy/local/README.md`
  - `.gitignore`

### Phase 7: Common Messaging Foundation

- **Status:** in_progress
- Actions taken:
  - Re-evaluated the module boundary after the user requested enterprise-level reusable MQ implementation.
  - Chose `cynexus-common-messaging` with `api`, `rabbit`, and `outbox` submodules.
  - Kept portfolio commands, task state, routing keys, migrations, and business listeners in `cynexus-market`; common modules own only transport contracts and reliability mechanisms.
  - Updated the RabbitMQ and portfolio architecture documents and delivery plan to reflect the common modules.
- Files created/modified:
  - `docs/messaging/rabbitmq-architecture.md`
  - `docs/market/personal-portfolio-tracking-architecture.md`
  - `.planning/2026-08-01-personal-portfolio-tracking/task_plan.md`
  - `.planning/2026-08-01-personal-portfolio-tracking/findings.md`
  - `.planning/2026-08-01-personal-portfolio-tracking/progress.md`

## Test Results

| Test | Input | Expected | Actual | Status |
|------|-------|----------|--------|--------|
| CodeGraph CLI availability | `codegraph --help` | CLI command list | Command succeeded | pass |
| Corrected document structure | BRD, architecture, and planning files | Files readable with expected Markdown sections | 5 files readable; BRD 19 and architecture 36 headings | pass |
| Scope exclusion scan | scheduler/framework, multi-portfolio, TWR/XIRR, Agent terms | Terms occur only as exclusions or unselected choices | Confirmed by scoped `rg` output | pass |
| RabbitMQ architecture completeness | topology, envelope, outbox, ACK, retry, DLQ, security, observability, deployment | Required sections and portfolio routing contract present | Required-term scan passed | pass |
| Stale async decision scan | old POC/unselected markers | No obsolete decision remains | No stale decision markers found | pass |
| Markdown diff hygiene | task-owned design files | No whitespace errors | `git diff --check` passed | pass |
| Local RabbitMQ Compose validation | `docker compose -f deploy/local/docker-compose.yml config --quiet` | Compose resolves without errors | Command succeeded | pass |
| RabbitMQ container health | `docker compose ... up -d rabbitmq`; `docker compose ... ps rabbitmq` | Container healthy without touching app stack | `cynexus-rabbitmq` healthy | pass |
| RabbitMQ broker diagnostics | `docker exec cynexus-rabbitmq rabbitmq-diagnostics -q ping` | Broker responds | `Ping succeeded` | pass |
| RabbitMQ management API | authenticated request to `127.0.0.1:15672/api/overview` | Management endpoint accessible | HTTP 200; version 4.1.8 | pass |
| Shared network membership | `docker inspect cynexus-rabbitmq` | Release-shared network available | `cynexus-dev_default`, aliases `rabbitmq`/`cynexus-rabbitmq` | pass |

## Error Log

| Timestamp | Error | Attempt | Resolution |
|-----------|-------|---------|------------|
| 2026-08-01 | FastCtx and CodeGraph MCP tools unavailable | 1 | Use CodeGraph CLI and scoped PowerShell reads. |
| 2026-08-01 | PowerShell count command string escaping failed | 1 | Removed the backtick escape sequence and reran the read-only count. |
| 2026-08-01 | CodeGraph symbol `StockChartQueryDomainServiceImpl.query` not found | 1 | Used interface impact analysis and exact method names instead. |
| 2026-08-01 | `rg` received an unexpanded Windows filename glob | 1 | Used `rg <pattern> docs/market -g <glob>` instead. |
| 2026-08-01 | Validation repeated an unexpanded Windows filename glob | 2 | Re-ran with a directory plus `-g` and confirmed no stale decisions. |

## 5-Question Reboot Check

| Question | Answer |
|----------|--------|
| Where am I? | Product, portfolio architecture, and RabbitMQ messaging design are complete; implementation has not started |
| Where am I going? | Feature-sized RabbitMQ infrastructure, outbox, portfolio backend, AI, and UI implementation commits |
| What's the goal? | Design a complete personal portfolio and return-tracking workflow for Cynexus |
| What have I learned? | The worktree is heavily modified and CodeGraph CLI is available |
| What have I done? | Completed the BRD, portfolio architecture, reusable RabbitMQ architecture, and delivery sequence |
