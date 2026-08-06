# sources/tdx

TongDaXin source adapter.

Current code:

- `local_minute.py`: reads local TDX minute files and normalizes them for the 1m ClickHouse writer.
- `local_day.py` and `parser.py`: read local `.day` files.
- `hsjday.py`: downloads/verifies/extracts TDX daily history zips.
- `adjusted.py`: optional `eltdx` adjusted daily K-line client helpers.
- `market_overview.py`: optional `eltdx` A-share amount/breadth live capture.
- `intraday.py`: optional `eltdx` auction point and historical 1m timeshare normalization.
- `industry_quotes.py`: optional `eltdx` TDX 56 industry index quote snapshot source.
- `indicator.py`: optional `eltdx 1.2.x` full-market quote snapshot adapter used by the live indicator monitor.
- `sector_quote.py`: optional `eltdx` `f10.theme_market` parser/client helpers for TDX sector detail snapshots.
- `stock_profile.py`: optional `eltdx` share-profile helper for stock turnover calculations.
- `mootdx.py`: optional `mootdx` K-line fallback adapter used only when explicitly requested by provider fallback.
- `easy_tdx.py`: optional easy_tdx standard/MAC adapter for online 1m and adjusted K-line, historical timeshare, stock fund-flow, and board queries.

Source modules fetch and parse only; persistence and job/checkpoint state belong in pipelines/runtime.

Online usage:

- `sync_kline_minute` keeps local TDX files as the bulk path and uses `easy_tdx` when the requested code has no local 1m file.
- `sync_kline_daily` can use `easy_tdx` for direct qfq/hfq daily bars through the provider fallback.
- `sync_tdx_intraday_detail --provider easy_tdx` uses easy_tdx for historical timeshare. Auction data continues to use the existing eltdx auction adapter; the default remains eltdx for compatibility.
- Internal routes under `/internal/a-stock/tdx/*` expose easy_tdx fund-flow and board data.

Market indicators use bounded full-market quote snapshots first. Depth and historical
trade calls remain candidate enrichment APIs because eltdx exposes them per symbol.
The package is research-only; do not use this collector as a commercial data feed.
