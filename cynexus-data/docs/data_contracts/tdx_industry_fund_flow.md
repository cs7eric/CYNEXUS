# TDX Industry Fund Flow Contract

`sync_tdx_industry_fund_flow` persists one daily serving snapshot row per TDX 56 industry for the `theme_market` `main_capital_flow` section (`ReqId 200742`).

## Source

- `sources/tdx/sector_quote.py`
- dataset: `main_capital_flow`
- source label: `eltdx.f10.theme_market.200742`

## Output Tables

- `cynexus_appl_market.tdx_industry_fund_flow_batch`
- `cynexus_appl_market.tdx_industry_fund_flow_snapshot`

Snapshot rows keep:

- `trade_date`: scheduler snapshot/trading date.
- `code6` / `full_code`: TDX 56 industry code.
- `tdx_name`: canonical TDX 56 industry name.
- `flow_sector_name`: name returned by `theme_market 200742`.
- `seed_code`: representative stock used for the query.
- `flow_trade_date`: actual row date returned by the source.
- `snapshot_status`: `ok | empty | error`.
- `selection_mode`: `exact | fallback_previous | latest_any | empty`.
- `name_match_type`: `exact | mismatch | empty`.
- `main_net`, `large_order_net`, `medium_order_net`, `small_order_net`.

The table is a compact UI-serving snapshot, not a raw intraday fact table.
