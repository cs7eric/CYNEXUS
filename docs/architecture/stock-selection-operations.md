# Stock Selection Operations

## Current serving boundary

`/api/market/screener` remains a real-time, end-of-day assembly of ClickHouse
`daily_raw` and PostgreSQL securities, fund-flow, industry, concept, and style
relations. The durable daily table is a replay/quality projection and is not
yet the serving read path. Do not describe it as a completed serving-path
migration.

## Daily projection

The Data Executor job `materialize_stock_screening_facts` runs at 17:55 on
trading days. It reads the prior-complete A-share daily bars from `daily_raw`,
then atomically replaces the same bounded date range in
`a_stock_daily_screening_fact`.

- Default: materialize the latest available daily trade date.
- Backfill: provide `start_date` and `end_date`; both are inclusive.
- Checkpoint: `a_stock.stock_screening_fact.watermark` records the completed
  date range, source count, stored count, and coverage percentage.
- Failure: a source/stored-count mismatch makes the job fail so the Scheduler
  status is actionable; it is not silently downgraded to a warning.

The projection includes OHLC, prior close, change, amplitude, volume, and
amount. It intentionally does not claim to materialize fund flow, industry
membership, turnover/volume-ratio snapshots, or financial coverage.

## Quality checks

Run the bounded check through the Data Executor:

```text
quality_check
  checks=screening_fact_coverage
  trade_date=YYYY-MM-DD
```

It compares deduplicated A-share `daily_raw` rows with the PostgreSQL fact
projection. A 100% result only confirms the daily-price projection. It does
not prove complete coverage for PE or any other financial field.

For 2026-08-14, the projection validated at 5,541 / 5,541 rows (100%). The
serving Screener reports 5,514 daily rows because its query additionally
applies its own securities/master-data eligibility boundary. These are
separate, explicitly named coverage measures.

## Incident response

1. Check the Scheduler job `stock_screening_fact_materialization` and its Data
   Executor JobResult.
2. Inspect the checkpoint and run `screening_fact_coverage` for the affected
   date.
3. If the source is present but coverage is not equal, rerun the same bounded
   date after fixing the cause. The job replaces only that date range.
4. If `daily_raw` itself is late or incomplete, repair the daily-bar ingestion
   first; do not accept a projection retry as a data-source repair.
5. Keep the Screener UI quality banner truthful: it reports current serving
   inputs, not just the projection status.
