# Market Overview Pipeline

## Job

`build_market_overview`

`build_market_overview_detail`

## Current Production Path

This migration provides the PostgreSQL snapshot and merge behavior from the legacy repository. It accepts normalized or legacy camelCase fields, applies source-specific overwrite rules, and writes one daily row.

`source=akshare` can now fetch live source fields directly:

- `stock_zh_a_spot_em()` supplies total amount, total volume, rising/falling/flat counts, limit-up/down counts, and stock count.
- `stock_market_fund_flow()` supplies main/super-large/large/medium/small net inflow and ratios.

`source=eltdx` can fetch live source fields directly through the optional `eltdx` runtime:

- `get_a_share_codes_all()` supplies the A-share universe.
- `get_quote()` supplies amount, last price, and pre-close values.
- The job summarizes total amount, rising/falling/flat counts, limit-up/down counts, and stock count.

`source=eastmoney` can fetch live market breadth directly through `push2.eastmoney.com`:

- `api/qt/clist/get` supplies A-share quote rows page by page.
- The job summarizes total amount, total volume, rising/falling/flat counts, limit-up/down counts, and stock count.
- This path is a lightweight HTTP fallback for market breadth when AkShare or eltdx are unavailable.

`source=tencent` can fetch live market breadth from `qt.gtimg.cn` batch quote snapshots:

- Explicit `codes` / `symbols` are used when supplied.
- If codes are omitted, the job reads active codes from `sec_stock_basic`.
- The job summarizes total amount, total volume, rising/falling/flat counts, limit-up/down counts, and stock count.

## Parameters

- `trade_date`: optional date if not included in `fields`.
- `source`: `eltdx`, `akshare`, `eastmoney`, `tencent`, `manual`, or another source tag.
- `create_table`: apply `sql/postgres/20260628_create_market_overview_tables.sql`.
- `fetch_live`: force live source capture. Currently supported for `source=akshare`, `source=eltdx`, `source=eastmoney`, and `source=tencent`.
- `eltdx_timeout` / `timeout_seconds`: optional eltdx connection timeout.
- `max_pages` / `page_size` / `page_sleep`: optional EastMoney clist crawler controls.
- `codes` / `symbols`: optional Tencent quote universe.
- `batch_size` / `batch_sleep`: optional Tencent batch quote controls.
- `fields`: JSON object with overview fields.

When `source=akshare`, `source=eltdx`, `source=eastmoney`, or `source=tencent` and no overview `fields` are provided, live capture is enabled automatically.

## Technical Detail Helpers

`domain/market/market_technical.py` migrates part of the legacy market-overview technical detail logic as pure functions:

- `latest_window_metrics(bars)`: return/range/volatility/volume-ratio metrics over 5/10/15/20/30/60/100/120/250-day windows.
- `build_support_resistance(bars, ...)`: support and resistance levels from moving averages, rolling highs/lows, gaps, swing points, and volume nodes.
- `infer_regime_bucket(...)`: pure regime bucket classifier.
- `build_style_overview(...)`: style relative strength over 5/20/60-day windows.
- `build_industry_strength_overview(...)`: industry/index relative strength against a benchmark.
- `build_sentiment_overview(...)`: breadth and limit-up/limit-down based sentiment scoring from caller-supplied rows.
- `build_similar_scenario_backtest(...)`: historical similarity matching and forward-return summary from caller-supplied bar/breadth/style inputs.

`build_market_overview_detail` is the executable detail pipeline for those helpers:

- Dry-run mode uses caller-supplied rows, or deterministic synthetic rows when none are supplied.
- Non-dry-run reads compact daily bars from ClickHouse with `read_daily_bars_by_code`.
- Breadth/sentiment rows come from `market_overview_snapshots` unless explicitly supplied.
- The pipeline writes one UI-ready JSON payload into PostgreSQL `mkt_overview_detail_snapshot`; it does not copy daily bar facts into PostgreSQL.
