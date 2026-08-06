# Findings

- User reports the screenshot clearly contains execution times `10:41:17`, `10:08:56`, and `10:08:31`, but the review UI shows only a date selector and `--:--:--`.
- The repository already contains extensive unrelated/uncommitted changes; preserve them and scope edits to this fix.
- `PortfolioImportProcessingDomainService` previously returned null when the AI used the `tradeTime` alias without a calendar date, so the visible rightmost time was discarded during normalization.
- Time-only values are now combined with the import task's `queuedAt` date in Asia/Shanghai; the review UI also repairs persisted time-only normalized values using that task date.
- Prompt v6 explicitly requires retaining row times for a screenshot titled “当日成交”, and Release migration routing now applies it through the Agent migration bundle.
- Added an idempotent market backfill for legacy candidates whose raw payload still contains a time-only `tradedAt`/`tradeTime`/`executionTime`/`time`; the current database reported 0 rows needing that repair.
