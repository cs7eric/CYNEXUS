# table_design

Table design documentation.

Use this folder to document ClickHouse fact tables, Postgres serving tables, partition keys, order keys, and retention policy.

## ClickHouse Intraday Detail

`sql/clickhouse/20260628_create_intraday_detail.sql` creates:

- `ch_ods_stock_auction_point`: eltdx auction point facts, partitioned by `toYYYYMM(trade_date)`, ordered by `trade_date, market, symbol, trade_time, phase, source`.
- `ch_ods_stock_timeshare_1m`: eltdx historical 1m timeshare facts, partitioned by `toYYYYMM(trade_date)`, ordered by `trade_date, market, symbol, trade_time, source`.

Both tables use `ReplacingMergeTree(ingested_at)` so repeated syncs can converge on the latest provider row without issuing large deletes.

## PostgreSQL Market Overview

`sql/postgres/20260628_create_market_overview_tables.sql` creates:

- `market_overview_snapshot`: daily lightweight breadth/fund-flow fields with source-aware merge rules.
- `market_overview_detail_snapshot`: one compact JSONB technical/detail payload per `trade_date, benchmark_code, source_table`.

The detail table stores UI-serving summaries only. Daily/index OHLCV facts remain in ClickHouse and are read by `build_market_overview_detail` when the detail payload is rebuilt.

## PostgreSQL Index Returns

`sql/postgres/20260628_create_index_return_tables.sql` creates:

- `mkt_index_return_daily`: compact broad-index window returns keyed by `trade_date, index_code, window_days`.

Raw index OHLCV rows remain in ClickHouse `index_daily_raw`; this PostgreSQL table is only the Java/UI serving cache.

## PostgreSQL TDX Industry Quotes

`sql/postgres/20260628_create_tdx_industry_quote_tables.sql` creates:

- `tdx_industry_quote_batch`: quote capture batch metadata.
- `tdx_industry_quote_snapshot`: active TDX 56 industry quote snapshots keyed by `snapshot_date, code6, source`.

The snapshot table is a lightweight UI-serving surface for industry heatmaps and rankings.

`sql/postgres/20260628_create_market_heatmap_tables.sql` creates:

- `market_heatmap_snapshot`: compact JSONB heatmap snapshots keyed by `snapshot_date, kind, source`.

`sql/postgres/20260628_create_sector_quote_detail_tables.sql` creates:

- `tdx_sector_quote_detail_snapshot`: compact JSONB TDX theme-market detail snapshots keyed by `snapshot_date, code6, source`.

`sql/postgres/20260628_create_stock_turnover_tables.sql` creates:

- `stock_turnover_series_snapshot`: compact per-stock turnover-rate JSONB series keyed by `symbol, target_type, period, adjust, source`.
