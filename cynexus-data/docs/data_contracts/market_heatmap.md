# Market Heatmap Contract

`build_market_heatmap` produces a compact PostgreSQL JSONB snapshot for Java/UI heatmap reads.

## Source

The first production path reads `cynexus_appl_market.tdx_industry_quote_snapshot`, which is populated by `sync_tdx_industry_quotes`.

## Output Table

`cynexus_appl_market.mkt_heatmap_snapshot`

- `snapshot_date`: trading/snapshot date.
- `kind`: currently `industries`.
- `source`: serving-source label, default `tdx_industry_quote_snapshot`.
- `payload`: UI-ready heatmap JSON.
- `item_count`: number of top-level heatmap items.

The payload keeps legacy-compatible top-level fields such as `items`, `totalItems`, `itemsTotal`, `topN`, `hiddenNoQuote`, and per-industry fields such as `sectorCode`, `fullCode`, `changePercent`, `amount`, and `children`.
