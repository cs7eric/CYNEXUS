# Stock Turnover Pipeline

`sync_stock_turnover`

Migrates the useful data-production part of legacy `backend/services/stock/f10/turnover.py` and `backend/services/stock/turnover_repo.py`.

## Flow

1. Resolve codes from explicit `codes` or active `sec_stock_basic` rows.
2. Read daily K-line bars from ClickHouse.
3. Read share profile from optional eltdx, unless `shares_by_code` is supplied.
4. Build turnover-rate entries in pure domain code.
5. Store the full series in PostgreSQL as a JSONB serving snapshot.

## Boundary

- No old filesystem JSON cache.
- No stock-chart Flask route.
- No mutation of K-line source files.
- Dry-run supports supplied `bars_by_code` and `shares_by_code` and does not touch ClickHouse, eltdx, or PostgreSQL.
