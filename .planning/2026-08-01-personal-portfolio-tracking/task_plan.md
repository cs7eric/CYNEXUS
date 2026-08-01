# Task Plan: Personal Portfolio And Return Tracking

## Goal

Produce an evidence-based BRD and implementation design for a complete personal stock portfolio, return tracking, trade import, AI parsing, chart annotation, and review workflow that fits Cynexus architecture and can be implemented safely in later phases.

## Current Phase

Phase 4

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

## Key Questions

1. Does the user accept the MVP restriction to A-share, CNY, long-only trades?
2. Should screenshot evidence default to a 30-day retention period or remain until user deletion?
3. Should every sell create a review reminder, or should closed positions receive the primary reminder?
4. Which existing background execution abstraction satisfies persistence, recovery, and single-processing guarantees in the implementation POC?

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Separate discovery/design from implementation | The user explicitly requested BRD and technical design first; the feature crosses market, AI, database, UI, and Agent boundaries. |
| Keep task records under `.planning/2026-08-01-personal-portfolio-tracking` | Repository guidance reserves `.planning` for task planning and the worktree already has unrelated plans and code changes. |
| Create a new portfolio bounded context | The user clarified that no existing product design should contain this feature; existing Finance/Stock Review logic is not the owning model. |
| Accept one screenshot per asynchronous parse task | The user clarified the import contract; upload returns a task reference, processing happens asynchronously, and confirmed rows alone affect the ledger. |
| Keep a single implicit personal ledger in MVP | Multiple portfolios and account administration do not contribute to the requested screenshot-to-return workflow. |
| Limit return claims to P&L and rates with valid denominators | Trade screenshots alone do not establish cash balance or external capital flows, so a complete account return would be misleading. |
| Keep the async runner unselected until a focused POC | Asynchronous behavior is required, but enabling Spring scheduling, JobRunr, or another task framework is an implementation choice rather than product scope. |

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| FastCtx and CodeGraph MCP tools are unavailable in this session | 1 | Use the required CodeGraph CLI and scoped PowerShell reads; do not initialize or alter the existing index without checking status. |
| PowerShell line-count command conflicted with JavaScript template escaping | 1 | Reissued the read-only command without PowerShell backtick escapes. |
| CodeGraph lookup for `StockChartQueryDomainServiceImpl.query` did not match a symbol | 1 | Used interface impact output, which revealed the actual methods such as `klines`; subsequent queries will use exact indexed method names. |
| PowerShell did not expand an `rg` filename glob | 1 | Re-ran `rg` with a directory argument and `-g` include pattern. |

## Notes

- Treat all pre-existing worktree changes as user-owned.
- Do not start deprecated Dev or local source runtimes.
- No business-code edits during the BRD/design phase.
