# Market Pulse Pipeline

## Job

`build_market_pulse`

## Current Production Path

1. Read the migrated sector fund-flow snapshot for `trade_date`.
2. Normalize rows into market-pulse sector snapshots.
3. Rank sectors by `change_pct` descending.
4. Persist the active market-pulse snapshot for the date.
5. Return strong-sector and capital-flow payloads for Java/UI callers.

## Parameters

- `trade_date`: target date.
- `create_table`: apply `sql/postgres/20260628_create_market_pulse_tables.sql`.
- `rows`: optional JSON list for dry-run/manual repair.

## Boundary

This job produces data. It does not assemble Flask API responses, fetch leading-stock detail drawers, or call old frontend-specific services.
