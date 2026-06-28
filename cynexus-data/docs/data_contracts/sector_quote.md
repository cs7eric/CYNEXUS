# Sector Quote Detail Contract

`sync_tdx_sector_quote_details` persists lightweight TDX sector/theme-market detail snapshots for Java/UI reads.

## Source

`sources/tdx/sector_quote.py` wraps optional `eltdx` `f10.theme_market` calls for TDX 56 industry seeds.

Supported datasets:

- `period_stats`: ReqId `200741`.
- `main_capital_flow`: ReqId `200742`.
- `related`: ReqId `200743`.
- `constituents`: ReqId `200744`.
- `main_control`: ReqId `200745`.

## Output Table

`cynexus_appl_market.tdx_sector_quote_detail_snapshot`

- `snapshot_date`: snapshot/trading date.
- `code6`: TDX industry code, such as `880471`.
- `name`: industry name.
- `seed_code`: representative stock used by eltdx.
- `payload`: JSONB detail payload grouped by section.
- `section_counts`: row counts per section.

Old Flask response assembly, stock-universe JSON lookup, and Tencent fallback hotpath are not part of this table.
