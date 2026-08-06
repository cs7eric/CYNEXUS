# Findings

- 当前数据库中的 `margin_sync_daily` 每个交易日 17:30 执行；2026-07-30 执行成功但 rows_read/rows_written 均为 0。
- 当前数据库中的 `margin_stock_history_backfill` 每个工作日 20:45 执行，昨天运行约 105 秒写入 5580 行。
- 当前数据源在 2026-07-30 20:31 的最新两融日期仍为 2026-07-29。
- `sync_margin.py` 的空结果路径会返回成功，未产生 skipped 状态或重试语义。
- `SchedulerDataJobKeyResolver` 已将历史 Job 映射到 `sync_margin`，调度配置通过 scheduler job 的 `extra.schedule` 保存。
