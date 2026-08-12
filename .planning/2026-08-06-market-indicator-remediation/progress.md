# Progress: Market Indicator Remediation Proposal

## 2026-08-06

- Inspected the route, frontend components, API contract, Java query/cache path, Python event/transmission calculation, live API responses, and prior Market Indicator redesign documentation.
- Prepared an implementation-ready remediation proposal only. No source or runtime changes were made.
- Began implementation: separated the UI's full-market and limit-pool tabs, added capture scope metadata to the frontend/backend contract, clarified empty transmission states and signal units, corrected sector-event value rendering, preserved watchlist target types, prevented non-intraday benchmark leakage, and replaced hard-coded leader timing/activity evidence defaults.
- UI production build passed; Java market application compile passed; focused Python market/transmission tests passed (18 tests). Targeted ESLint still reports the pre-existing React hook/compiler findings in `market-indicator/index.tsx`; no new syntax/type errors were introduced.
- Rebuilt and recreated the Release `market` and `ui` services with `1.1.398-260806-RELEASE`; live API verification completed after startup. `/api/market/indicator/latest` exposes full-market scope/coverage metadata, and `/api/market/indicator/transmission` exposes runtime cadence/benchmark/scope metadata. The current snapshot has no transmission episodes, correctly represented as an empty confirmation result rather than inferred sector causality.
- After product review found the first UI layer too close to the old snapshot page, replaced the top work area with a visible market-context/quality gate and a master-detail sector transmission workbench. Confirmed Episodes now expose leader, response coverage, median lag, ex-leader return and benchmark evidence; when Episodes are absent, the page shows explicitly labelled cross-sectional observations and lists the missing evidence instead of presenting them as transmission. Auto-refresh now follows the backend effective cadence (minimum 15 seconds) rather than polling the full-market payload every five seconds. UI build passed and the Release UI image was rebuilt/recreated; live bundle verification confirmed the new workbench strings.
- Diagnosed the apparently empty ranking: calculations had produced 4,585 persisted Episode captures for 2026-08-06 (110 non-emerging), but the Java query treated an empty latest Redis array as authoritative and never fell back to persisted intraday facts. Changed transmission lookup to prefer the full-market bundle, fall back to PostgreSQL when the latest capture is empty, and rank/deduplicate persisted Episodes by sector/direction with confirmed stages first. The live API now returns 100 ranked Episodes for the date, including 42 confirmed rows; leader evidence is parsed from persisted payloads in the UI.

## 2026-08-06 Phase 7 proposal

- Audited reusable 1m index and stock-bar paths, sentiment/limit-emotion history endpoints, market-indicator event/industry tables, and existing bounded Agent exposure.
- Confirmed the core product gap is lifecycle and time alignment: current records are capture snapshots, daily limit ladder data has no intraday seal timestamp, and day-level sentiment cannot be treated as minute context.
- Added the implementation-ready event-anchored radar/replay design to `task_plan.md`, including definitions, `asOf` rules, first-board semantics, score provenance, API/persistence boundaries, AI safety requirements, and acceptance tests.
- No source code, database migration, prompt, or runtime changes were made in this phase.

## 2026-08-06 Phase 8 implementation

- Added the event-anchored `replay/timeline` API and frontend 1m replay panel, including event/industry facts, coverage status, and a pending state when 1m data lags the requested trade date.
- Fixed replay minute filtering to use ClickHouse's local `time` field before epoch timestamps; the latter is encoded with a UTC interpretation and previously removed every Shanghai-session point.
- Rebuilt market/AI/UI artifacts with test-source compilation skipped because of the pre-existing `RedisAgentRateLimitConcurrencyTest` type mismatch, recreated Release services, and verified the gateway endpoint returns `dataStatus=ready` with 237 index points for 2026-08-06. A future uncovered date returns `dataStatus=pending` with an explicit coverage message.

## 2026-08-07 Phase 9 implementation

- Added bounded radar/replay query APIs for market state, chronological events, limit ladder, replay summary, Episode detail, and evidence explanation. The endpoints reuse persisted indicator captures, transmission Episodes/outcomes, and ClickHouse 1m benchmark bars; future dates remain `pending` instead of fabricating minute evidence.
- Extended `MarketResearchFeignClient` and the bounded `market.indicator` Agent tool with `radar_state`, `radar_events`, `limit_ladder`, `replay_summary`, and `replay_episode` operations. The tool remains read-only and returns source/freshness metadata.
- Added a post-market scoreboard and Episode detail panel to the review workspace. Users can select an Episode, inspect coverage/lag/benchmark/outcome context, and request a clearly labelled deterministic explanation. The button does not claim a model call; the true AI capability remains an explicit exclusion until prompt/provider/binding/logging are wired.
- Verification: market controller compile, AI infra compile with test-source skip, UI typecheck/Vite production build, Release market/ai/ui image rebuild and recreation, live endpoint checks for 2026-08-06 (`ready`, 237 1m index points, ladder 76 items, replay summary/Episode/explain 200), and uncovered 2026-08-07 (`pending`, latest data date 2026-08-06).

## 2026-08-07 Phase 10 completion

- Completed the AI capability contract for `market_indicator_replay_explanation`: DB prompt, usage binding, runtime domain/controller route, execution logging/fallback path, and the AI Provider UI capability catalog now use the same capability and prompt key.
- Release AI deployment completed successfully after applying the Agent migration and rebuilding `cynexus-ai:1.1.398-260806-RELEASE`; `cynexus-release-ai` is healthy and the endpoint is reachable.
- Real provider verification was performed against the current local provider configuration. The configured providers are unavailable for this environment (unsupported model, missing subscription/group, insufficient balance, or missing API key), so the request returns a bounded `AI_PROVIDER_ERROR` and the UI falls back to the deterministic replay explanation endpoint.
- Deterministic fallback was verified with a real 2026-08-06 Episode (`mi_episode_c29537e81a973757c68d89ff`), returning `status=deterministic_fallback`, evidence references, drivers, counter-evidence, and failure conditions without fabricating model output.
- Frontend production build passed after registering the new AI capability. Existing font-resolution and large-chunk warnings remain non-blocking.
