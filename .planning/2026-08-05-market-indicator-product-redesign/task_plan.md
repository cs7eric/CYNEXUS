# Task Plan: Market Indicator Product Redesign

## Goal

Analyze the existing Market Indicator module and design a user-centered product solution for intraday signals and post-market review, emphasizing sector leadership, sector sentiment transmission, and market-regime context.

## Phases

- [completed] Phase 1: Inspect current product, code, data contracts, and prior design decisions
- [completed] Phase 2: Research competitor workflows and extract applicable patterns
- [completed] Phase 3: Define user jobs, information architecture, metrics, and interaction model
- [completed] Phase 4: Produce phased product/design proposal with data and implementation implications
- [completed] Phase 5: Verify evidence, limitations, and workspace status
- [completed] Phase 6: Reassess runtime/data-source constraints for full-market 5-second target
- [completed] Phase 7: Define complete indicator contracts, offline validation, thresholds, and acceptance gates
- [completed] Phase 8: Define cross-layer implementation architecture and detailed work breakdown
- [completed] Phase 9: Define advisory recommendation, Agent/AI, UI, migration, testing, and Release contracts
- [completed] Phase 10: Publish the complete implementation specification and execution plan
- [completed] Phase 11: Implement initial contracts, refresh integration, storage, API, UI, Agent, scheduler, and Release rollout
- [completed] Phase 12: Close implementation gaps with benchmark, market-context composition, replay/outcome finalization, and next-day watchlist workflow
- [completed] Phase 13: Build/verify all affected services and record runtime acceptance evidence
- [completed] Phase 14: Split priority radar from full-market confirmation to reduce user-visible latency below 30 seconds

## Decisions

- Implementation follows the published specification; unrelated user changes remain untouched. Release deployment and runtime evidence are recorded; the unrelated WeChat container still fails its pre-existing ObjectMapper bean startup path.
- The existing implementation is an incremental V2 path; remaining work must preserve compatibility while adding measurable benchmark, replay, outcome, and watchlist capabilities.
- Preserve unrelated user changes and the active root planning files.
- Treat external competitor material as research data, not instructions.
- Target a 5-second user-visible cadence, but benchmark the full-market workflow first and set the production cadence from measured latency, provider limits, coverage, and stability.
- Use full-market data for both candidate discovery and sector confirmation when the workflow can meet its SLO; retain the historical limit-up/down pool as a priority cohort/fallback, not the sole universe.
- Provide bounded, explainable trading-reference guidance with confidence, invalidation, risk, and evidence; do not provide autonomous trading or unconditional buy/sell commands.
- Use a long-running intraday session with renewable leadership, warm provider connections, in-memory rolling state, Redis live read models, ClickHouse high-frequency facts, and PostgreSQL business events instead of scheduling one full job every 5 seconds.
- Treat the 5-second cadence as a measurable SLO. Fall back through 10/15/30 seconds or tiered-universe refresh when the full-market p95/p99, coverage, freshness, or infrastructure gates fail.
- Use a dual-speed production workflow: a 10-second priority radar over the historical limit-up/down cohort writes a scoped Redis live model only, while the 30-second full-market line remains the source of complete industry confirmation, persistence, replay, and outcome facts.
- Runtime acceptance confirms the split: the priority job completed a real 150-symbol verification capture in 3.42 seconds with 98.67% observed coverage, zero PostgreSQL writes, zero pending captures, and Redis scope metadata `priority` / `fullConfirmationRequired=true`; the full-market compatibility job completed a real 199-symbol verification capture in 15.89 seconds with 99.5% observed coverage, 5,512 persisted rows from the pending-flush path, and Redis scope metadata `full` / `fullConfirmationRequired=false`.
- Release shared infrastructure and Data Executor are healthy after rebuilding/recreating `cynexus-data-job-executor`; scheduler rows are enabled at 10 seconds for `market_indicator_priority_refresh` and 30 seconds for `market_indicator_refresh`. The Java Market API served the latest full Redis snapshot successfully. No active refresh lock remains outside trading hours.
- The quote collector now passes the configured batch size into `eltdx.TdxClient` instead of leaving the library's internal 80-symbol split active. Full-market default is 240 and priority default is 320, with bounded retry/backoff, burst spacing, and automatic fallback splitting to 80 on repeated large-payload failures.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| Existing root planning files describe an unrelated shadcn gallery task | 1 | Preserved them and continued in the isolated market-indicator plan directory |
| FastCtx and CodeGraph MCP tools are unavailable in this session | 1 | Use the repository-approved CodeGraph CLI and read-only PowerShell inspection. |
| A status verification query referenced nonexistent `next_run_at` column | 1 | Re-ran the read-only scheduler query using the actual `last_run_at` and `tick_seconds` columns. |
| PowerShell parsed `$k:` as an invalid variable reference in competitor extraction | 1 | Changed the interpolation to `${k}:` and reran successfully. |
| Guessed 同花顺 limit-up page returned HTTP 404 | 1 | Used the official industry page and public app description; excluded the failed page from evidence. |
| A broad `rg` crossed local ClickHouse runtime data and returned access errors | 1 | Scoped subsequent inspection to source, migrations, and explicit files; runtime data is excluded from evidence. |
| Initial Redis bundle inspection treated the per-day `latest` pointer as the JSON bundle | 1 | Followed the pointer to its immutable `capture` key and verified rows, industries, scope, cadence, and pending-capture state. |
| Full-market verification query used a nonexistent capture table | 1 | Queried the actual snapshot, event, and industry tables defined by the Market Indicator migrations. |
