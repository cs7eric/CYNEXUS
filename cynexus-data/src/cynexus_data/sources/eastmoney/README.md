# sources/eastmoney

EastMoney source adapter.

Use this folder for EastMoney client calls, response parsing, schema definitions, and project adapters. Connectivity should come from `hooks/sources/eastmoney.py` when implemented.

Implemented surfaces:

- `api/qt/stock/kline/get` raw K-line fetch used by provider fallback.
- `api/qt/clist/get` A-share market breadth capture used by `build_market_overview source=eastmoney`.
- `api/qt/stock/get` explicit-code stock metadata enrichment used by `sync_stock_basic providers=eastmoney`.

Source modules return parsed payloads only. Pipelines decide whether and where to persist results.
