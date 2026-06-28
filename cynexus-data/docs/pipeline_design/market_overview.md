# Market Overview Pipeline

## Job

`build_market_overview`

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

## Parameters

- `trade_date`: optional date if not included in `fields`.
- `source`: `eltdx`, `akshare`, `eastmoney`, `manual`, or another source tag.
- `create_table`: apply `sql/postgres/20260628_create_market_overview_tables.sql`.
- `fetch_live`: force live source capture. Currently supported for `source=akshare`, `source=eltdx`, and `source=eastmoney`.
- `eltdx_timeout` / `timeout_seconds`: optional eltdx connection timeout.
- `max_pages` / `page_size` / `page_sleep`: optional EastMoney clist crawler controls.
- `fields`: JSON object with overview fields.

When `source=akshare`, `source=eltdx`, or `source=eastmoney` and no overview `fields` are provided, live capture is enabled automatically.
