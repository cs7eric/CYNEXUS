# Stock Basic Pipeline Design

`sync_stock_basic` syncs the Python-owned stock universe into PostgreSQL.

## Provider

Current production-enabled provider:

- `akshare.stock_info_a_code_name`
- `eastmoney.api/qt/stock/get` for explicit-code metadata enrichment.

## Behavior

- `dry_run=true`: return plan only; no provider or PostgreSQL access.
- `dry_run=false`: fetch provider data, normalize board/ST/limit threshold fields, and upsert `cynexus_data.sec_stock_basic`.
- `providers=eastmoney`: fetch industry, total market cap, float market cap, and cap style for explicit `codes`, or for active codes already present in `sec_stock_basic`.

Optional params:

- `providers`: `akshare`, `eastmoney`, or both.
- `codes` / `symbols`: explicit code list for EastMoney metadata enrichment.
- `create_table`: apply table DDL before upsert.
- `limit`: test/smoke limit.

## Downstream Usage

`sync_adjusted_kline_daily` uses this table as its default active code source when no explicit `codes` are provided. EastMoney enrichment also uses this table as the default active code source when `codes` are omitted.

## Sector Mapping

`sync_stock_sectors` syncs stock to industry/concept/style mappings from eltdx topics.

Behavior:

- `dry_run=true`: return plan only; no eltdx or PostgreSQL access.
- `dry_run=false`: fetch topics per stock, derive an industry from topic reasons when possible, and replace that stock's sector mappings in PostgreSQL.

Optional params:

- `codes` / `symbols`: explicit stock list.
- `limit_codes`: smoke-test limit.
- `create_table`: apply sector mapping DDL before write.
- `pool_size`, `timeout`: eltdx client options.

If `codes` is omitted in non-dry-run mode, the pipeline reads active codes from `sec_stock_basic`.

## THS Industry Constituents

`sync_ths_industry_constituents` syncs q.10jqka industry constituent snapshots into PostgreSQL.

Behavior:

- `dry_run=true`: return plan only; no THS or PostgreSQL access.
- `dry_run=false` with `history_path`: import legacy per-industry JSON payloads such as `881103.json`.
- `dry_run=false` without `history_path`: fetch explicit `industry_codes` from THS, parse all pages, dedupe by six-digit stock code, and replace those industry snapshots.

Optional params:

- `snapshot_date` / `trade_date`: snapshot date used for storage.
- `industry_codes` / `codes`: comma-separated THS industry codes. Required for live fetch.
- `history_path`: file, directory of `{industry_code}.json`, or dated subdirectory.
- `create_table`: apply THS constituent DDL before write.
- `max_pages`, `page_sleep`: crawler controls.

## Trade Calendar

`sync_trade_calendar` generates A-share trading calendar rows from local 2024-2027 rules, weekend checks, and optional request-level overrides.

Optional params:

- `start_date` / `end_date`: inclusive calendar range.
- `extra_holidays`: comma-separated dates forced to non-trading days.
- `extra_workdays`: comma-separated dates forced to trading days.
- `create_table`: apply stock metadata DDL before writing.

Core calendar helpers stay pure and do not read files/env. Runtime override files should be parsed by an operator layer and passed into the job as params.
