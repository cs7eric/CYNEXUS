# Market Heatmap Pipeline

`build_market_heatmap`

Migrates the scoped production part of legacy `market_heatmap_service.py`: build a UI-ready industry heatmap snapshot from already captured market facts.

## Flow

1. Accept explicit `rows` for dry-run/manual repair, or read `tdx_industry_quote_snapshot` by `snapshot_date`.
2. Normalize TDX industry quote rows in pure domain code.
3. Sort industries by amount and apply `top_n`.
4. Persist the final payload to `market_heatmap_snapshot`.

## Boundary

- No old Flask route, local JSON runtime cache, stock-universe hotpath cache, or UI rendering code is migrated.
- Source fetching remains in `sync_tdx_industry_quotes`.
- This pipeline only transforms persisted facts into a lightweight PG serving snapshot.
