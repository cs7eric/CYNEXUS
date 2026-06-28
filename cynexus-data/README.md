# cynexus-data

Python data jobs for Cynexus.

## Layout

```text
cynexus-data/
  src/cynexus_data/  # Python package code
  tests/             # unit and integration tests
  scripts/           # operator scripts
  docs/              # architecture and data design notes
  sql/               # ClickHouse DDL and queries
  var/               # local logs and resume state, ignored by git
```

## Local TDX 1m K -> ClickHouse

This POC reads local TongDaXin minute K files through `pytdx.reader` and writes normalized 1 minute bars into ClickHouse table `cynexus.ch_dwd_kline_1m`.

Supported local file formats:

- `.lc1` with `TdxLCMinBarReader`
- `.1` with `TdxMinBarReader`

Install dependencies:

```powershell
cd E:\project-repo\cynexus\cynexus-data
python -m pip install -e .
```

Create the ClickHouse table:

```powershell
Get-Content -Raw sql\clickhouse\20260627_create_ch_dwd_kline_1m.sql |
  docker exec -i cynexus-clickhouse clickhouse-client --user cynexus --password $env:CLICKHOUSE_PASSWORD --database cynexus --multiquery
```

Dry run one file:

```powershell
python -m cynexus_data.jobs.ingest_tdx_1m_to_clickhouse `
  --file C:\new_tdx\vipdoc\sz\minline\sz000001.lc1 `
  --dry-run
```

Ingest one file:

```powershell
python -m cynexus_data.jobs.ingest_tdx_1m_to_clickhouse `
  --file C:\new_tdx\vipdoc\sz\minline\sz000001.lc1 `
  --create-table
```

Scan a TDX root and ingest discovered 1m files:

```powershell
python -m cynexus_data.jobs.ingest_tdx_1m_to_clickhouse `
  --tdx-root C:\new_tdx `
  --create-table `
  --limit-files 20
```

Rolling backfill when TDX overwrites local minute files:

```powershell
# 1. Download the latest 100 day 1m data in TongDaXin.
python -m cynexus_data.jobs.ingest_tdx_1m_to_clickhouse `
  --tdx-root "D:\Program File\tdx" `
  --create-table

# 2. In TongDaXin, switch/download the previous 100 day window.
# 3. Run the same ingest command again.
# 4. Repeat until ClickHouse coverage is enough.
```

The local TDX files can be overwritten between runs. ClickHouse keeps previously ingested bars. The table uses `ReplacingMergeTree(ingested_at)` with `(market, symbol, period, trade_time, source)` as the identity, so overlapping imports are deduplicated by the latest ingest after ClickHouse merges. Use `FINAL` in verification queries when exact de-duplicated counts are required.

Check current coverage:

```powershell
Get-Content -Raw sql\clickhouse\queries\ch_dwd_kline_1m_coverage.sql |
  docker exec -i cynexus-clickhouse clickhouse-client --user cynexus --password $env:CLICKHOUSE_PASSWORD --database cynexus
```

Default ClickHouse connection matches local compose:

- host: `127.0.0.1`
- http port: `28123`
- database/user: `cynexus`

Override with CLI flags or `CLICKHOUSE_*` environment variables.

## Per-symbol parquet 1m -> ClickHouse

The parquet backfill expects files named like `000001.SZ.parquet`.

Dry run:

```powershell
python -m cynexus_data.jobs.ingest_parquet_1m_to_clickhouse `
  --dir F:\data\stock_1min `
  --dry-run
```

Ingest with resume state:

```powershell
python -m cynexus_data.jobs.ingest_parquet_1m_to_clickhouse `
  --dir F:\data\stock_1min `
  --create-table `
  --workers 8 `
  --batch-rows 100000
```

Completed files are recorded in `var\parquet_1m_ingest_state.jsonl`, so rerunning the same command continues from unfinished files.

By default, parquet import only writes rows with `trade_date >= 2010-01-01`.
Override with `--start-date yyyy-mm-dd`, or pass `--start-date ""` to disable the lower bound.

## Job Executor

Run a local health check:

```powershell
python -m cynexus_data.jobs.health_check
```

Run a dry-run daily K job:

```powershell
python -m cynexus_data.jobs.sync_kline_daily --dry-run --trade-date 2026-06-28
```

Run the internal trigger API after installing dependencies:

```powershell
python -m cynexus_data.interfaces.trigger_api.app --host 127.0.0.1 --port 18083
```
