# pipelines/fund_flow

Fund-flow pipelines.

Use this folder for stock-level and sector-level fund-flow synchronization and ranking builds.

- `sync_sector_flow.py` writes THS industry/concept snapshots to PostgreSQL.
- `sync_stock_flow.py` writes Tencent qt per-stock active buy/sell snapshots to PostgreSQL.

Provider clients remain under `sources/`; this package owns orchestration and persistence only.
