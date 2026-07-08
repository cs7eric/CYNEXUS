# A Stock Data Integration Result

Status: source/query integration complete; persistence DDL, repositories, and reusable dataset sync job added on 2026-07-07.

## Implementation Matrix

| Area | Status | Notes |
| --- | --- | --- |
| EastMoney throttled HTTP helper | complete | `sources/eastmoney/http.py`; session reuse, min interval, jitter, retries, proxy-disconnect fallback. |
| EastMoney reports/datacenter/news/hot/limit/fund-flow | complete | `sources/eastmoney/a_stock.py`; exposed through `/internal/a-stock/eastmoney/*`. |
| THS EPS/hot/northbound/limit/hot-list | complete | `sources/ths/a_stock.py`; exposed through `/internal/a-stock/ths/*`. |
| CNInfo announcements/IRM | complete | `sources/cninfo/client.py`; dynamic orgId lookup and IRM query-string rule. |
| Sina financial/options | complete | `sources/sina/a_stock.py`; financial reports, option codes, T quote, Greeks/IV. |
| Baidu K-line MA | complete | `sources/baidu/client.py`; live smoke passed after endpoint correction. |
| iwencai report search | complete, live skipped | `sources/iwencai/client.py`; live requires `IWENCAI_API_KEY`. |
| mootdx quote/detail helpers | complete | `sources/tdx/mootdx_market.py`; live order-book smoke returned 46 fields. |
| Internal routes | complete | `interfaces/trigger_api/a_stock_routes.py`; thin route layer only. |
| Persistence DDL | complete | PostgreSQL and ClickHouse DDL added under `cynexus-data/sql/*/20260707_create_a_stock_data_tables.sql`. |
| Persistence repositories | complete | `storage/postgres/a_stock.py` and `storage/clickhouse/a_stock.py`. |
| Dataset sync job | complete | `sync_a_stock_dataset` registered in job registry, jobs.yaml, and pyproject console scripts. |
| Knowledge-base docs | complete | Two published KB docs inserted into `kb_document`; source markdown lives under `docs/knowledge-base/market-data/`. |
| Unit/import tests | complete | 5 unit tests passed. |
| Live smoke | complete with recorded failures | Latest report: `var/reports/a-stock-data-smoke/20260706-233539`. |

## Verification

- `python -m compileall -q src scripts`: passed.
- `PYTHONPATH=src python -m pytest -q tests/unit/test_sources/test_a_stock_data_sources.py tests/unit/test_pipelines/test_a_stock_persistence_pipeline.py`: 9 passed.
- FastAPI app import/route registration check: 37 `/internal/a-stock/*` paths found.
- Live smoke `scripts/a_stock_data_live_smoke.py` latest run:
  - ok: 25
  - failed: 5
  - skipped: 1
- Local PostgreSQL DDL application: passed after backup `deploy/local/postgres-backups/cynexus-before-a-stock-persistence-20260707-011519.sql`.
- Local ClickHouse DDL application: first attempt failed due nullable sorting key in `ch_ods_stock_transaction`; fixed and re-run passed.
- Live write smoke:
  - `sync_a_stock_dataset --dataset cninfo_announcements --code 000001 --page-size 1`: wrote 1 PG row.
  - `sync_a_stock_dataset --dataset tdx_order_book --code 600519`: wrote 1 ClickHouse row.
- Knowledge-base publish:
  - `A Stock 数据落库表设计`: document id `332571765114957824`, status `published`.
  - `Cynexus Data A 股能力全量地图`: document id `332571765354033152`, status `published`.

## Latest Live Smoke Details

Report path: `cynexus-data/var/reports/a-stock-data-smoke/20260706-233539`.

| Probe | Status | Count | Reason |
| --- | --- | ---: | --- |
| EastMoney reports | ok | 100 |  |
| EastMoney industry reports | ok | 100 |  |
| EastMoney stock info | failed |  | connection reset by upstream/proxy during live run; same endpoint passed in the first run. |
| EastMoney concept blocks | ok | 29 |  |
| EastMoney fund-flow minute | failed |  | `push2.eastmoney.com` connection reset/upstream proxy disconnect. |
| EastMoney fund-flow 120d | failed |  | `push2his.eastmoney.com` connection reset/upstream proxy disconnect. |
| EastMoney industry rank | failed |  | `push2.eastmoney.com` connection reset/upstream proxy disconnect. |
| EastMoney daily dragon tiger | ok | 112 |  |
| EastMoney single-stock dragon tiger | ok | 3 |  |
| EastMoney lockup | ok | 2 |  |
| EastMoney margin | ok | 5 |  |
| EastMoney block trade | ok | 5 |  |
| EastMoney holders | ok | 1 |  |
| EastMoney dividends | ok | 5 |  |
| EastMoney stock news | ok | 0 | endpoint reachable; returned no article rows for the probe/IP. |
| EastMoney global news | ok | 5 |  |
| EastMoney limit pool | ok | 60 |  |
| EastMoney hot rank | failed |  | `emappdata.eastmoney.com` connection reset/upstream proxy disconnect. |
| EastMoney hot concept | ok | 6 |  |
| Baidu K-line MA | ok | 2001 |  |
| THS EPS forecast | ok | 3 |  |
| THS hot reason | ok | 73 |  |
| THS northbound | ok | 262 |  |
| THS limit-up pool | ok | 60 |  |
| THS hot list | ok | 100 |  |
| CNInfo announcements | ok | 5 |  |
| CNInfo IRM | ok | 5 |  |
| Sina financial report | ok | 2 |  |
| Sina option codes | ok | 4 |  |
| iwencai search | skipped |  | `IWENCAI_API_KEY` not set. |
| mootdx order book | ok | 46 |  |

## Persistence Matrix

| Dataset family | Target |
| --- | --- |
| EastMoney stock info | PostgreSQL `sec_stock_basic` extended fields |
| Reports, iwencai, THS EPS | PostgreSQL `a_stock_research_report` |
| Sina financial statements | PostgreSQL `a_stock_financial_statement` |
| CNInfo announcements | PostgreSQL `a_stock_announcement` |
| EastMoney news/global news | PostgreSQL `a_stock_news` |
| CNInfo IRM | PostgreSQL `a_stock_investor_interaction` |
| EastMoney 120d daily fund-flow | PostgreSQL `a_stock_fund_flow_daily` |
| EastMoney minute fund-flow | ClickHouse `ch_ods_stock_fund_flow_minute` |
| EastMoney industry rank | PostgreSQL `a_stock_industry_rank_snapshot` |
| Dragon tiger | PostgreSQL `a_stock_dragon_tiger_daily`, `a_stock_dragon_tiger_seat` |
| Lockup/block/holders/dividends | PostgreSQL corresponding `a_stock_*` event tables |
| Hot rank/concepts/reasons/limit pools | PostgreSQL corresponding `a_stock_*_snapshot` tables |
| Sina option contracts | PostgreSQL `a_stock_option_contract_snapshot` |
| Sina option quote/Greeks | ClickHouse `ch_ods_option_quote_snapshot` |
| Baidu K-line MA | ClickHouse `ch_ods_baidu_kline_ma` |
| THS northbound minute flow | ClickHouse `ch_ods_northbound_minute` |
| mootdx transactions/order-book | ClickHouse `ch_ods_stock_transaction`, `ch_ods_stock_order_book_snapshot` |

## Scheduler Guidance

- EastMoney datacenter/reportapi: serial, 2s request interval, daily or after close.
- EastMoney push2/push2his/emappdata: serial, 2-5s interval, small stock pools only; record upstream resets.
- THS: 1s interval; hot/reason/limit data 15-30 minutes intraday, northbound 1-5 minutes if needed.
- CNInfo: 1.5s interval; daily or low-frequency incremental.
- Sina: 0.5s interval; financials daily, options 30-60 seconds only for strategy/UI needs.
- Baidu: 1s interval; daily after close.
- iwencai: 3s+ interval; requires API key and quota-aware batching.
- mootdx: 0.2s interval; small concurrency only.
