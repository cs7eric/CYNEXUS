# Market Limit Pipeline

`build_limit_emotion`

Migrates the scoped data-production part of legacy `_limit_pg_writer.py` and `market_limit_pg_repo.py`.

## Flow

1. Read daily OHLCV rows from ClickHouse, or use supplied dry-run rows.
2. Build ST-aware limit-up, limit-down, broken-board, and streak flags in pure domain code.
3. Calculate the MSI limit-emotion summary.
4. Persist the summary to `msi_limit_emotion_daily`.
5. Extract stock-level limit snapshots and replace same-date, same-source rows in `market_limit_stock_snapshot`.

## Boundary

- No Flask route or JSON runtime cache is migrated.
- No raw OHLCV is stored in PostgreSQL.
- Dry-run returns the extracted stock rows but does not touch ClickHouse or PostgreSQL.
