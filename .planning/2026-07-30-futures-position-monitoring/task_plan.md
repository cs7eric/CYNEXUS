# Task Plan: Futures Long/Short Position Monitoring Implementation

## Goal
Implement the approved daily exchange-member futures long/short position monitor end to end, with ingestion in `cynexus-data`, query semantics/API/UI in the market bounded context, and Release-stack verification.

## Current Phase
Phase 10: Release Verification

## Phases

### Phase 1: Requirements & Discovery
- [x] Inspect existing market service, data ingestion, schema, UI, routing, scheduler, and version conventions
- [x] Identify AkShare and exchange-data integration constraints
- [x] Capture product and competitor-oriented workflow requirements
- **Status:** complete

### Phase 2: Planning & Structure
- [x] Define MVP and follow-up scope
- [x] Define domain model, persistence, APIs, jobs, UI, and observability design
- [x] Define technical route, milestones, commits, testing, and release verification
- [x] Incorporate review corrections for rank-type modeling, canonical provider selection, futures trade dates, watchlist workflow, POC gates, API identity, and per-feature version sync
- **Status:** complete

### Phase 3: Implementation
- [x] User approved implementation after plan review
- **Status:** complete

### Phase 4: Testing & Verification
- [x] Run focused tests after each feature commit
- [x] Run Maven full compile and UI production build after UI integration
- **Status:** complete

### Phase 5: Delivery
- [x] Deliver scoped commits with completed scope and self-test results
- **Status:** complete

### Phase 6: POC Contract & Source Boundary
- [x] Run focused tests and document per-exchange POC status
- **Status:** complete

### Phase 7: Data Storage & Sync Job
- [x] Add PostgreSQL migrations, capture batches, canonical replacement, and scheduler-backed sync job
- **Status:** complete

### Phase 8: Java Query API
- [x] Add market domain query semantics, MyBatis infrastructure, DTO mapping, and REST endpoints
- **Status:** complete

### Phase 9: UI Workflow
- [x] Add Market navigation, page, ranking/detail/history workflow, freshness/coverage states, and futures self-selected behavior
- **Status:** complete

### Phase 10: Release Verification
- [x] Synchronize versions and verify Maven/UI versions
- [x] Run Maven full compile and UI production build
- [x] Rebuild `market`/`ui` Release images and verify the empty-data API workflow
- [ ] Verify a live non-empty sync through the shared Data Executor image
- **Status:** blocked by the shared Data Executor image predating this feature

## Key Questions
1. MVP definition: daily/EOD exchange member position rankings, not personal positions or intraday member positions.
2. POC candidates: CFFEX, SHFE, CZCE, DCE, GFEX, and INE; only exchanges meeting reliability gates enter MVP.
3. Instrument semantics: both contract and variety scopes are supported and never merged; DCE variety-level data is labeled explicitly.
4. Monitoring MVP includes saved filters and pinned futures instruments; directional signals, server alerts, and notifications are follow-up scope.
5. Delayed, missing, corrected, fallback, and partial data are represented through capture batches, selected-provider provenance, coherent trade dates, coverage, and warnings.

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| Analysis only in this turn | User explicitly requested a full analysis before authorizing implementation |
| Place planning artifacts in `.planning/2026-07-30-futures-position-monitoring/` | Follows repository guidance for session planning and avoids root scratch files |
| Model independent `VOLUME`, `LONG`, and `SHORT` rankings | Exchange reports contain separate member lists; a shared rank number does not imply the same member |
| Persist one canonical provider fact set per exchange/date | Prevents double counting while preserving fallback provenance in capture batches |
| Compute Top-N summaries on query for MVP | Daily rank volume is small; avoid a derived table until performance evidence requires materialization |
| Reuse self-selected lists with `future_instrument` | Covers contract and variety monitoring without creating a second watchlist subsystem |
| Require 20-day multi-exchange POC gates before implementation | The installed wrapper showed upstream fragility; production support must be evidence-based |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| FastCtx MCP tools were not exposed in the available tool catalog | 1 | Use read-only PowerShell inspection as fallback; note the limitation in findings |
| Compact Python live-probe command had mismatched parentheses | 1 | Log the failure and switch to separate small probes if needed |
| Planning-file patch context mismatch | 1 | Re-read the file and apply a narrower patch |

## Notes
- Preserve all pre-existing worktree changes; do not edit overlapping feature files during analysis.
- Version bump, commits, runtime restart, and release verification belong to implementation phases after approval.
