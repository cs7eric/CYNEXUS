# Findings

## Initial Context

- Existing `cynexus_appl_market.sec_stock_basic` is the stock master table with about 5,530 active rows.
- Existing `a_stock_financial_statement` has only 48 rows and is insufficient for market-wide screening.
- ClickHouse `daily_raw` already contains about 23.9 million daily rows from 2010-01-04 through 2026-07-28, with OHLC, amount, volume, market, and source.
- Existing `market_indicator_refresh` is configured for 10-minute full-market collection during trading windows, but current run status/history is empty and today's snapshots show only 09:28 and 09:42 records.

## Decisions To Validate

- Use a tiered candidate pool for intraday snapshots; retain full-market metadata and historical daily facts separately.
- Avoid claiming a real-time active pool unless the pool-selection inputs are persisted and observable.
- Prefer Postgres normalized screening tables for current stock profile/financial snapshots and ClickHouse for large historical daily trading facts, with query/API integration kept explicit.

## Scheduler Root Cause

- The scheduler status is enabled, but `cynexus-core` filters due jobs through `SchedulerExecutionCapabilityPort.supports`.
- `SchedulerDataJobKeyResolver` did not contain `market_indicator_refresh`, so the job was never considered executable by the runtime even though the database row had `extra.data_job_key`.
- The existing one-shot snapshot was therefore manual/triggered outside the recurring runtime; `last_run_at` and run history remained empty.
- Release startup applies only the Agent migration bundle, so market scheduler/data migrations are not automatically applied by the Release entrypoint.

## Runtime Evidence

- At 2026-07-28 20:51 China time, `market_indicator_refresh` had `is_enabled=true`, status enabled, `total_runs=0`, and no run history.
- Current snapshots were only 09:28 (19 rows) and 09:42 (5,518 rows).
- Core scheduler tick is 30 seconds; the configured market interval was 10 minutes, but it could not run until the resolver mapping was present.
