# sources/akshare

AkShare source adapter.

Use this folder for AkShare wrapper calls and adapters. Keep AkShare-specific field names inside this layer and emit project dataset records outward.

- Stock basic fallback lives in `stock_basic.py`.
- Market overview capture uses `stock_zh_a_spot_em()` and `stock_market_fund_flow()`.
- Margin capture uses `stock_margin_sse()`, `stock_margin_szse()`, `stock_margin_detail_sse()`,
  `stock_margin_detail_szse()`, and `stock_margin_account_info()`.

The source client imports `akshare` lazily so dry-runs and unit tests do not require the optional package.
