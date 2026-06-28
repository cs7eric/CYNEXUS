# Index Returns Pipeline

## Job

`build_index_returns`

## Purpose

Migrate the scoped data-production part of the legacy `index_repo.py`: broad-index window return calculation from ClickHouse index K-line facts, plus lightweight PostgreSQL persistence for Java/UI reads.

## Inputs

- ClickHouse `index_daily_raw`.
- Optional request `rows_by_code` for dry-run, repair, or tests.
- Optional `targets` JSON array for custom index targets.

## Parameters

- `trade_date`: as-of date. Defaults to today.
- `window_days` / `days`: return window. Capped by the pure domain helper to the legacy-safe range.
- `source_table`: ClickHouse source table, default `index_daily_raw`.
- `lookback_days`: rows requested per index, default `window_days + 1`.
- `create_table`: apply PostgreSQL DDL.
- `targets`: JSON array with `name`, `code`, `fullCode`, `exchange`.
- `rows_by_code`: JSON object keyed by full index code.

## Boundary

- Domain code only calculates return payloads.
- ClickHouse readers only read grouped index K-line facts.
- PostgreSQL stores only the compact serving rows in `mkt_index_return_daily`.
- Old Flask routes and UI cache reads are not migrated.
