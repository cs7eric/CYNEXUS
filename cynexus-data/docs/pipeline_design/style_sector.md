# Style Sector Pipeline

`build_style_sector`

Migrates the scoped data-production part of legacy `backend/services/stock/style_sector_service.py`.

## Flow

1. Read category `4` style-sector constituents from PostgreSQL stock-sector mappings.
2. Fetch Tencent qt realtime snapshots for the union of constituent stock codes.
3. Compute each style sector as an equal-weight average of constituent `change_pct`.
4. Replace same-date `mkt_style_sector_snapshot` rows for source `tencent.qt`.

## Boundary

- No old `reference/stock-universe/sectors/*.json` cache is used.
- No Flask/UI route behavior is migrated.
- Dry-run accepts supplied `sectors` and `quotes_by_code` JSON and does not access Postgres or Tencent.
