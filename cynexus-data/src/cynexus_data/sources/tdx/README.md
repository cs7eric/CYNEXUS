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
- `sector_quote.py`: optional `eltdx` `f10.theme_market` parser/client helpers for TDX sector detail snapshots.
- `stock_profile.py`: optional `eltdx` share-profile helper for stock turnover calculations.
- `mootdx.py`: optional `mootdx` K-line fallback adapter used only when explicitly requested by provider fallback.

Source modules fetch and parse only; persistence and job/checkpoint state belong in pipelines/runtime.
