# Stock Basic Data Contract

Primary table: `cynexus_appl_market.sec_stock_basic` in PostgreSQL.

This table is the Python-owned stock universe used by data jobs. Java may still own UI-facing stock configuration; this table exists to let Python jobs resolve active stock codes without hard-coded lists.

Fields:

- `code`: six-digit stock code.
- `name`: display name.
- `market`: `SH`, `SZ`, or `BJ`.
- `board`: `main_sh`, `main_sz`, `chinext`, `star`, `bse`, or `unknown`.
- `is_st`: derived from name.
- `limit_up_threshold`: 0.05, 0.10, 0.20, or 0.30.
- `is_active`: active universe flag.
- `source`: source provider, currently `akshare`.
- `industry`: optional industry name from metadata enrichment providers such as EastMoney.
- `total_market_cap`: optional total market capitalization.
- `float_market_cap`: optional float/circulating market capitalization.
- `cap_style`: `large`, `mid`, `small`, `micro`, or null.
- `sector_index_symbol`, `sector_index_name`, `industry_query`: optional industry-index mapping metadata.

Consumers:

- `sync_adjusted_kline_daily` can load active codes from this table when `codes` is omitted in non-dry-run mode.
- `sync_stock_basic providers=eastmoney` can use active codes from this table to enrich existing universe rows.

## Sector Mapping

Tables:

- `cynexus_appl_market.sec_sector`
- `cynexus_appl_market.sec_stock_sector`

`sec_sector` stores normalized industry/concept/style buckets. `sec_stock_sector` stores stock membership and relation metadata.

Sector identity:

- `sector_id`: `<category_raw>:<topic_id-or-name>`.
- `category_raw`: eltdx topic category.
- `category_label`: `industries`, `concepts`, `styles`, or `unknown_<category_raw>`.

Mappings are replaced per stock by `sync_stock_sectors`, so stale topics for that stock are removed when a stock is refreshed.

## THS Industry Constituents

Tables:

- `cynexus_appl_market.ths_industry_constituent_capture_batch`
- `cynexus_appl_market.ths_industry_constituent_snapshot`

The snapshot table stores q.10jqka industry membership plus the 14-column quote fields exposed by the source page.

Identity and replace rule:

- Snapshot rows are keyed operationally by `snapshot_date`, `source`, `industry_code`, and `stock_code`.
- The pipeline replaces only the requested `industry_code` values for the requested snapshot date/source.
- `stock_code` is normalized to six digits so values such as `3030` become `003030`.

Large historical quote/time-series details still belong in ClickHouse. This table is a lightweight PG membership/snapshot surface for jobs and UI summaries.

## Trade Calendar

`cynexus_appl_market.sec_trade_calendar`

- `trade_date`: calendar date.
- `is_trading_day`: whether the date is an A-share trading day.
- `reason`: `weekday`, `weekend`, known holiday name, `extra_holiday`, or `extra_workday`.
- `source`: calendar source, default `local_rules`.
