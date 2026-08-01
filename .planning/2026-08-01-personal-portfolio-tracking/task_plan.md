# Task Plan: Personal Portfolio And Return Tracking

## Goal

Produce an evidence-based BRD and implementation design for a complete personal stock portfolio, return tracking, trade import, AI parsing, chart annotation, and review workflow that fits Cynexus architecture and can be implemented safely in later phases.

## Current Phase

Phase 8

## Phases

### Phase 1: Repository And Product Discovery

- [x] Inspect existing market backend, database, AI, file upload, and UI capabilities
- [x] Inspect Stock Overview route, navigation, charting, and neighboring service patterns
- [x] Identify reusable contracts and architectural gaps
- [x] Capture findings and constraints
- **Status:** complete

### Phase 2: BRD And Business Model

- [x] Define users, problems, outcomes, scope, and non-goals
- [x] Define screenshot import, transaction, position, P&L, marker, and review rules
- [x] Define import and AI-assisted correction workflow
- [x] Define acceptance criteria and product metrics
- **Status:** complete

### Phase 3: Technical Architecture And Delivery Plan

- [x] Define DDD ownership and cross-module dependencies
- [x] Design the minimal PostgreSQL schema for `cynexus_appl_market`
- [x] Design APIs, AI boundary, asynchronous behavior contract, and UI structure
- [x] Define security, idempotency, testing, migration, and release strategy
- [x] Define feature-sized commit sequence and verification gates
- **Status:** complete

### Phase 4: Review And Handoff

- [x] Validate corrected scope against repository conventions and prior discovery
- [x] Check working tree for unintended output
- [x] Prepare BRD/design artifacts and unresolved product decisions for handoff
- **Status:** complete

### Phase 5: RabbitMQ Architecture Decision

- [x] Compare RabbitMQ, RocketMQ, and Kafka against known Cynexus workloads
- [x] Confirm the repository currently has no RabbitMQ/Kafka/RocketMQ integration
- [x] Define reusable RabbitMQ topology, message contracts, delivery semantics, and ownership
- [x] Replace the portfolio async-runner placeholder with the approved RabbitMQ design
- [x] Define Outbox/Inbox, retry/DLQ, security, observability, deployment, and test gates
- [x] Validate and prepare the design update for an independent commit
- **Status:** complete

### Phase 6: RabbitMQ Shared Local Infrastructure

- [x] Add pinned RabbitMQ management image beside Redis in local Docker Compose
- [x] Configure credentials, persisted data, AMQP/management ports, and health check
- [x] Validate Compose configuration and start only RabbitMQ
- [x] Verify health, diagnostics, management API, and shared Docker network membership
- [x] Commit the infrastructure feature with self-test results
- **Status:** complete

### Phase 7: Common Messaging Foundation

- [x] Add `cynexus-common-messaging` parent and `api`, `rabbit`, `outbox` submodules
- [x] Implement broker-neutral message envelope, destination, transport and error contracts
- [x] Implement Rabbit publisher-confirm transport, connection properties and message boundaries
- [x] Implement Outbox relay and Inbox idempotency SPI without owning business tables
- [x] Compile and test focused modules
- [x] Commit the enterprise messaging foundation with self-test results
- **Status:** complete

### Phase 8: Portfolio Implementation

- [ ] Add portfolio persistence migration
- [x] Add portfolio domain model and deterministic ledger rules
- [ ] Add RabbitMQ Outbox publisher and portfolio parse consumer
- [ ] Add AI vision capability and structured candidate validation
- [ ] Add confirmation, ledger, returns, chart markers, reviews, and APIs
- [ ] Add UI routes, services, pages, and complete workflow
- [ ] Verify Release rebuild, migrations, backend/frontend tests, and version sync
- **Status:** in_progress

## Key Questions

Resolved defaults:

1. MVP is A-share, CNY, and long-only.
2. Screenshot evidence defaults to 30-day retention with early user deletion.
3. Every sell creates a review reminder; a fully closed position receives higher priority.
4. Release uses a single RabbitMQ node; production guidance uses a three-node cluster with quorum queues.

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Separate discovery/design from implementation | The user explicitly requested BRD and technical design first; the feature crosses market, AI, database, UI, and Agent boundaries. |
| Keep task records under `.planning/2026-08-01-personal-portfolio-tracking` | Repository guidance reserves `.planning` for task planning and the worktree already has unrelated plans and code changes. |
| Create a new portfolio bounded context | The user clarified that no existing product design should contain this feature; existing Finance/Stock Review logic is not the owning model. |
| Accept one screenshot per asynchronous parse task | The user clarified the import contract; upload returns a task reference, processing happens asynchronously, and confirmed rows alone affect the ledger. |
| Keep a single implicit personal ledger in MVP | Multiple portfolios and account administration do not contribute to the requested screenshot-to-return workflow. |
| Limit return claims to P&L and rates with valid denominators | Trade screenshots alone do not establish cash balance or external capital flows, so a complete account return would be misleading. |
| Superseded: keep the async runner unselected until a focused POC | The user subsequently selected RabbitMQ as the shared business messaging platform. |
| Use RabbitMQ for business commands and domain events | Known Cynexus needs are reliable tasks, routing, acknowledgements, retries, and dead letters; Kafka remains a future data-stream option only when replay/high-throughput requirements are proven. |
| Add `cynexus-common-messaging` now | RabbitMQ is a cross-service platform capability; common contracts, publisher confirms, retry, tracing, and Outbox/Inbox reliability must not be reimplemented per bounded context. |
| Split common messaging into `api`, `rabbit`, and `outbox` | Keeps broker-neutral contracts independent from Spring AMQP and keeps business-owned table schemas outside the platform module. |

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| FastCtx and CodeGraph MCP tools are unavailable in this session | 1 | Use the required CodeGraph CLI and scoped PowerShell reads; do not initialize or alter the existing index without checking status. |
| PowerShell line-count command conflicted with JavaScript template escaping | 1 | Reissued the read-only command without PowerShell backtick escapes. |
| CodeGraph lookup for `StockChartQueryDomainServiceImpl.query` did not match a symbol | 1 | Used interface impact output, which revealed the actual methods such as `klines`; subsequent queries will use exact indexed method names. |
| PowerShell did not expand an `rg` filename glob | 1 | Re-ran `rg` with a directory argument and `-g` include pattern. |
| A scoped validation command repeated an unexpanded Windows glob | 2 | Used the already documented `rg <pattern> <directory> -g <glob>` form and confirmed no stale decision markers. |

## Notes

- Treat all pre-existing worktree changes as user-owned.
- Do not start deprecated Dev or local source runtimes.
- No portfolio business-code edits yet; shared RabbitMQ infrastructure is complete and common messaging foundation is the active implementation phase.
