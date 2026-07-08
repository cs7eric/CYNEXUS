# A Stock Data Integration Process

## 2026-07-06

- Read `a-stock-data.md` and compared its missing/partial capabilities against current `cynexus-data`.
- Added Phase 34 to the root project plan.
- Created this integration plan/process/result documentation set under `cynexus-data/docs/a_stock_data_integration/`.
- Added provider-scoped source/query modules:
  - `sources/eastmoney/http.py` for rate-limited EastMoney requests with session reuse and retry.
  - `sources/eastmoney/a_stock.py` for reportapi, datacenter, push2/push2his, news, limit pools, hot rank, and concept blocks.
  - `sources/baidu/client.py` for Baidu K-line with MA fields.
  - `sources/iwencai/client.py` for API-keyed semantic report search and query2data.
  - `sources/cninfo/client.py` for announcements and IRM investor Q&A.
  - `sources/ths/a_stock.py` for EPS forecast, hot reason, northbound, limit-up pool, and hot list.
  - `sources/sina/a_stock.py` for financial statements and ETF option contract/quote/Greek data.
  - `sources/tdx/mootdx_market.py` for optional mootdx quote/order-book/transaction helpers.
- Added thin internal query routes in `interfaces/trigger_api/a_stock_routes.py` and registered them in the FastAPI app.
- Added unit tests in `tests/unit/test_sources/test_a_stock_data_sources.py`.
- Added live smoke script `scripts/a_stock_data_live_smoke.py`, writing JSON and Markdown reports under `var/reports/a-stock-data-smoke/<timestamp>/`.
- First live smoke run wrote `var/reports/a-stock-data-smoke/20260706-233403`: 26 ok, 4 failed, 1 skipped.
- Fixed the Baidu URL/headers to match `a-stock-data.md`; Baidu K-line MA then passed live with 2001 rows.
- Added EastMoney HTTP retry mounting and no-proxy fallback for proxy disconnect cases.
- Second live smoke run wrote `var/reports/a-stock-data-smoke/20260706-233539`: 25 ok, 5 failed, 1 skipped. Failures were EastMoney connection resets on some `push2`/`emappdata` endpoints and not parser/import failures.
- Verification passed:
  - `python -m compileall -q src scripts`
  - `PYTHONPATH=src python -m pytest -q tests/unit/test_sources/test_a_stock_data_sources.py`

## 2026-07-07

- Added Phase 35 to the root project plan for A-stock persistence and knowledge-base documentation.
- Added PostgreSQL DDL `sql/postgres/20260707_create_a_stock_data_tables.sql`.
  - Extended `sec_stock_basic` with EastMoney total shares, float shares, listing date, and EastMoney snapshot fields.
  - Added A-stock tables for research reports, financial statements, announcements, news, investor interaction, daily fund-flow, industry rank, dragon-tiger daily/seats, lockup, block trades, holder counts, dividends, hot ranks, hot concepts, hot reasons, limit pools, option contracts, and concept membership.
- Added ClickHouse DDL `sql/clickhouse/20260707_create_a_stock_data_tables.sql`.
  - Added minute/detail tables for EastMoney minute fund-flow, mootdx transactions, mootdx order-book snapshots, Baidu K-line MA, THS northbound minute flow, and Sina option quote/Greek snapshots.
- Added pure normalization in `domain/stock/a_stock_persistence.py`.
- Added storage repositories:
  - `storage/postgres/a_stock.py`
  - `storage/clickhouse/a_stock.py`
- Added pipeline/job wiring:
  - `pipelines/a_stock/sync_a_stock_dataset.py`
  - `jobs/sync_a_stock_dataset.py`
  - registry entry in `jobs/registry.py`
  - job config in `config/jobs.yaml`
  - console script `cynexus-sync-a-stock-dataset`
- Added tests in `tests/unit/test_pipelines/test_a_stock_persistence_pipeline.py`.
- Ran verification:
  - `python -m compileall -q src scripts`
  - `PYTHONPATH=src python -m pytest -q tests/unit/test_sources/test_a_stock_data_sources.py tests/unit/test_pipelines/test_a_stock_persistence_pipeline.py`
  - Result: 9 passed.
- Applied local PostgreSQL DDL after backup:
  - Backup: `deploy/local/postgres-backups/cynexus-before-a-stock-persistence-20260707-011519.sql`
  - PG table check: 19 `a_stock_%` tables exist and `sec_stock_basic` EastMoney fields exist.
- Applied local ClickHouse DDL.
  - First attempt failed because `ch_ods_stock_transaction` used nullable columns in the MergeTree sorting key.
  - Fixed the sorting key to `source, code, trade_date, trade_time`.
  - Re-run succeeded; 6 ClickHouse tables exist.
- Ran live write smoke:
  - `cninfo_announcements` for `000001` wrote 1 row to PostgreSQL `a_stock_announcement`.
  - `tdx_order_book` for `600519` wrote 1 row to ClickHouse `ch_ods_stock_order_book_snapshot`.
- Published two detailed knowledge-base documents to `cynexus_appl_knowledge_base.kb_document`:
  - `332571765114957824` / `A Stock 数据落库表设计`
  - `332571765354033152` / `Cynexus Data A 股能力全量地图`
