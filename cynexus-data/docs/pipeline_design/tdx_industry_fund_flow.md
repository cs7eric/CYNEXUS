# TDX Industry Fund Flow Pipeline

`sync_tdx_industry_fund_flow`

## Flow

1. Resolve the TDX 56 industry catalog and representative seed stocks.
2. Call `eltdx.f10.theme_market(seed, 200742)` for `main_capital_flow`.
3. Choose the best row for the requested trading date:
   - exact day first,
   - otherwise latest previous row,
   - otherwise latest available row,
   - otherwise mark empty/error.
4. Persist one PostgreSQL serving row per TDX 56 industry for that day.

## Boundary

- Python owns source fetching and PostgreSQL writes.
- Java/UI read through the internal query API.
- Raw 30-day source history is not stored as-is; the serving table keeps one daily selected snapshot row per TDX 56 industry.
