# Trade Review Timeshare Storage Plan

## Goal

Produce a complete, implementation-ready design for selectively persisting and caching raw intraday timeshare series used by the trade-review workbench, without rebuilding timeshare from local TDX 1m K data and without duplicating non-chart metrics.

## Scope

- ClickHouse facts and selection-reason metadata.
- Redis compressed cache with seven-day TTL.
- After-close collection and repair jobs.
- Java query/API composition and fallback behavior.
- Trade-review chart and industry-constituent consumers.
- Data health, historical as-of semantics, rollout, and verification.

## Phases

- [x] Restore the existing trade-review plan and verify live ClickHouse schemas.
- [x] Inspect current intraday source, pipeline, scheduler/job, Redis, Java API, and UI consumption paths.
- [x] Finalize storage scope, field contracts, deduplication, completeness, and retention rules.
- [x] Design the after-close selection/collection/write/cache workflow and repair behavior.
- [x] Design Java APIs, Redis read-through behavior, UI integration, and degradation states.
- [x] Produce phased implementation order, migrations, tests, deployment, observability, and capacity estimates.
- [x] Optimize the live archive run by batching ClickHouse cleanup and avoiding repeated cached targets.

## Confirmed Decisions

- Do not rebuild timeshare from `ch_dwd_kline_1m`.
- Persist all daily limit-up and limit-down stocks.
- For every THS industry, additionally persist non-limit stocks ranked top 15 and bottom 10 by close change percentage.
- Persist raw stock/ETF timeshare; retain index and TDX/THS industry context in the existing 1m K fact and prewarm only its normalized Redis payload, rather than duplicating it into the timeshare table.
- Deduplicate a security series across industries; store selection reasons separately.
- Always include same-day portfolio trade symbols and explicit review/manual targets in addition to the market-wide selection policy.
- Redis TTL is seven days and should use a compact binary representation.
- Timeshare chart contract excludes `turnover_rate` and `volume_ratio`.
- Historical replay must never consume future data.
- The live archive path must not query `ch_dwd_kline_1m` per target; raw TDX timeshare is sufficient for this cache path.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| FastCtx and CodeGraph MCP tools were not available | 1 | Used CodeGraph CLI first, then focused `rg`/file reads as required by repository guidance. |
| `cynexus-market/AGENTS.md` does not exist | 1 | Continue with repository-level guide, backend DDD guide, and closest module/package conventions discovered through CodeGraph. |
| An overly broad `rg` search entered `deploy/local/postgres-backups` and produced truncated/noisy output | 1 | Exclude backup directories and use focused schema files or live read-only database metadata queries for subsequent inspection. |
| Direct Data Executor probe used `/tdx/timeshare` and returned HTTP 404 | 1 | Use the mounted API prefix `/internal/a-stock/tdx/timeshare` used by the UI service. |
| eltdx recent-history date-specific probe returned zero points; no-date probe used an incorrect weekend date label | 1 | Do not depend on the recent-history protocol for v1. Use fixed-date raw timeshare plus minute-K amount enrichment; track recent-history support as a later source-adapter fix. |
| Initial PowerShell multi-code probe ended with an empty pipe parser error | 1 | Materialized the `foreach` results into an array before formatting; the corrected probe completed. |
| Read-only Redis INFO/CONFIG probe returned `NOAUTH` | 1 | Capacity plan will use the measured workload estimate; implementation verification must query Redis through the configured shared credentials without printing them. |
