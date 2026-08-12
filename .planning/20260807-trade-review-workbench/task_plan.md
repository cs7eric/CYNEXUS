# Trade Review Workbench

## Goal

Implement the post-market trade-review route and full-screen replay panel for A-share and ETF trading days. The workbench must synchronize stock, industry, index, volume, VWAP, portfolio trades, observations, reviews, and Market Indicator events while preserving historical as-of semantics.

## Phases

- [x] Inspect existing portfolio, chart, market-indicator, scheduler, and data-health contracts.
- [x] Add bounded market/portfolio read models and persistence for review anchors/observations.
- [x] Implement API endpoints and local TDX 1m load trigger integration.
- [x] Implement the trade-review route, data checks, and replay panel.
- [x] Add focused tests, build checks, version verification, and Release deployment.
- [x] Expand day-level replay with dated ladder pools, prior-day limit-up intraday cards, and day-to-minute drill-down.
- [x] Repair historical industry/index overlays and verify synchronized 60-day playback.
- [x] Render distinct day-level ladder pools, make day-to-minute replay start from 09:30, repair ClickHouse-backed industry/index history, run focused frontend checks, and rebuild the Release UI.
- [x] Design the real day-level market-state data contract, collection jobs, storage, as-of semantics, and phased backfill plan to replace the current mock pools.
- [x] Reconcile the daily-data plan with the required cursor-accurate intraday state model and replace the frontend-composition proposal with a single backend Trade Review Market State aggregation API.

## Constraints

- Preserve unrelated dirty worktree changes, especially Market Indicator replay work.
- Do not expose this UI-oriented, potentially large interactive replay aggregation as an autonomous Agent tool; the existing bounded Market Indicator tool remains the Agent-facing capability.
- Historical context must be as-of the replay date and must not use future data.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| PowerShell parsed `-Dsurefire.failIfNoSpecifiedTests=false` as a lifecycle phase | 1 | Re-run Maven with PowerShell stop-parsing (`--%`). |
| Combined callback plumbing patch no longer matched `index.tsx` after concurrent edits | 1 | Re-read the exact call/signature blocks and apply smaller scoped patches. |
| Standard Release UI image build could not fetch the Docker Hub nginx token | 1 | Reused the existing versioned UI image as the local base, copied the newly built `dist`, retagged the same Release version, and force-recreated only the UI service. |
| Final `rg` verification pattern was parsed as an unclosed regex | 1 | Re-ran the verification with separate fixed-string searches for each symbol and label. |
