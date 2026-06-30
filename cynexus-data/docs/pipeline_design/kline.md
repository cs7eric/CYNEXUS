# Kline Pipeline Design

## Minute Sync

`sync_kline_minute` supports TDX local 1m files.

- `dry_run=true`: discover files only, no ClickHouse connection.
- `dry_run=false`: read TDX local files and write normalized 1m rows through `ClickHouseKlineSink`.
- Optional params: `tdx_root`, `limit_files`, `batch_size`, `create_table`.
- Provider fallback params: `providers`, `codes` / `symbols`, `period`, `limit`.
- When `providers` excludes `tdx` and `codes` are provided, the pipeline fetches minute bars through `sources/provider_fallback.py` and writes normalized rows to `ch_dwd_kline_1m`.
- Network provider attempts are returned in `provider_results` with `source`, `dataQuality`, and attempt metadata.

## Daily Sync

`sync_kline_daily` supports TDX local `.day` files.

- `dry_run=true`: discover files only, no ClickHouse connection.
- `dry_run=false`: parse local `.day` files and write normalized rows into `daily_raw`.
- Optional params: `day_root`, `tdx_root`, `market` / `markets`, `codes` / `symbols`, `start_date`, `end_date`, `trade_date`, `limit_files`, `unit_scale`, `batch_size`, `create_table`, `target_table`, `replace_existing`, `flush_rows`.
- Provider fallback params: `providers`, `adjust`, `limit`.
- When `providers` excludes `tdx` and `codes` are provided, the pipeline fetches daily bars through `sources/provider_fallback.py` and writes normalized rows to `daily_raw`.
- Network provider attempts are returned in `provider_results` with `source`, `dataQuality`, and attempt metadata.
- Resume params: `resume`, `checkpoint_prefix`. When `resume=true`, completed TDX files or network provider code/date/provider batches are skipped through `PipelineContext.checkpoints`.
- Maintenance rebuilds should write a staging table through `target_table`, use `replace_existing=false` only for known-empty staging tables, and set `flush_rows` to batch many `.day` files into large inserts. This avoids thousands of small ClickHouse parts during historical rebuilds.
- `flush_rows` cannot be combined with `resume=true`, because a batched insert no longer has unambiguous per-file checkpoint semantics.

`backfill_history` delegates to this daily sync path for `dataset=kline_daily`, `dataset=daily_raw`, or default `dataset=kline`.

`sync_tdx_hsjday` is the upstream preparation job for refreshing the local `.day` root from TDX `hsjday.zip`.

## Daily Read Compatibility

`storage/clickhouse/readers.py` migrates the legacy daily K-line read shape without importing the old backend:

- `read_daily_kline_bars(client, code, adjust, start, end, limit)` reads `daily_raw`, `daily_qfq`, or `daily_hfq`.
- `limit` follows the old behavior: take the latest N rows, then return them ascending by `trade_date`.
- `get_latest_daily_trade_date`, `has_adjusted_daily`, and `list_codes_with_adjusted_daily` support lightweight coverage checks.

## Adjusted Daily Sync

`sync_adjusted_kline_daily` pulls adjusted daily K-line data from `eltdx`.

- `dry_run=true`: validate the request shape only, no eltdx or ClickHouse access.
- `dry_run=false`: fetch adjusted K-line and write to `daily_qfq` and/or `daily_hfq`.
- Required for non-dry-run: `codes` / `symbols`.
- Optional params: `adjust` / `adjusts`, `start_date`, `end_date`, `trade_date`, `limit_codes`, `page_size`, `max_pages`, `batch_size`, `create_table`.
- Resume params: `resume`, `checkpoint_prefix`. When `resume=true`, completed `adjust + code + date range` batches are skipped through `PipelineContext.checkpoints`.

`adjust=both` expands to `qfq,hfq`.

## TDX Intraday Detail Sync

`sync_tdx_intraday_detail` pulls eltdx auction points and historical 1m timeshare detail into ClickHouse.

- `dry_run=true`: validate the request shape only, no eltdx or ClickHouse access.
- `dry_run=false`: fetch selected datasets and write to `ch_ods_stock_auction_point` and/or `ch_ods_stock_timeshare_1m`.
- Default datasets: `auction,timeshare`.
- Optional params: `datasets` / `dataset`, `trade_date`, `codes` / `symbols`, `target_type`, `limit_codes`, `timeout_seconds`, `batch_size`, `create_table`.
- Resume params: `resume`, `checkpoint_prefix`. When `resume=true`, completed `dataset + code + trade_date` batches are skipped through `PipelineContext.checkpoints`.
- When `codes` are omitted in non-dry-run mode, active codes are read from `sec_stock_basic`.

The pipeline intentionally does not migrate old auction AI analysis or UI cache assembly. Those remain outside the Python data executor boundary.

## Period Aggregation

`aggregate_kline_periods` derives higher periods from `period=1` rows.

- Default periods: `5,15,30,60`.
- `dry_run=true`: return plan without ClickHouse access.
- `dry_run=true, estimate=true`: read ClickHouse counts only.
- `dry_run=false`: insert aggregated rows back into the kline fact table with `source='aggregate_1m'`.

Supported filters:

- `trade_date`
- `start_date`
- `end_date`
- `symbols` / `codes`
- `table`

## Quality Check

`quality_check` supports read-only OHLCV validation.

- `dry_run=true`: return planned checks only.
- `dry_run=true, estimate=true`: count invalid rows in ClickHouse.
- `dry_run=false`: run the same read-only checks for operational execution.
- Daily raw source sampling uses `checks=daily_source_sample`. It samples ClickHouse daily rows with `sample_every` (default `100`), calls configured daily providers with unadjusted bars (`adjust=none`), and compares OHLC values against `price_tolerance_pct` / `price_tolerance_abs`.
- Useful params: `daily_table`, `trade_date`, `sample_every`, `sample_limit`, `sample_markets`, `sample_scope`, `source_providers`, `source_limit`.
- `sample_scope=stock` limits the sample to A-share stock code ranges. Use it when validating stock UI/MSI inputs so bonds, funds, ETFs, and indices with different TDX price scales do not mask stock-data quality.
