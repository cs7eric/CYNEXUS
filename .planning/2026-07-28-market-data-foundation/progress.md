# Progress

## 2026-07-28

- Restored task context from the prior Market Indicator work.
- Confirmed the worktree already contains substantial Market Indicator changes; these must be preserved.
- Planning files created for the new market-data foundation task.
- Found recurring scheduler root cause: `market_indicator_refresh` was absent from `SchedulerDataJobKeyResolver`, so Core filtered it out before due-job execution.
- Added an 800-code tiered candidate pool (500 hot + 300 deterministic rotation), persisted candidate-pool records, and changed the intended interval to 3 minutes.
- Added normalized `a_stock_financial_snapshot`, `a_stock_daily_screening_fact`, and stock profile extension columns, with Sina financial normalization writing both legacy JSON and normalized rows.
- Added Release market migration application and an idempotent schedule update migration.
- Rebuilt the shared data-job executor so the candidate-pool code is active in Docker; verified a manual refresh stored 800 candidates and 799 fetched snapshots.
- Fixed market-indicator snapshot primary-key collisions by using a deterministic capture/code ID with an idempotent upsert; added a focused regression test.
- Verified Release images `1.1.316-260728-RELEASE`, all Release containers, scheduler status/history, market API output, screening tables, and synchronized version `1.1.316-SNAPSHOT`.
