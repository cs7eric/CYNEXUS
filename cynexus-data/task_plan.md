# cynexus-data Legacy Migration Plan

## Goal

Migrate scoped data-production capabilities from `F:\dev-repo\mp4-to-word-new` into `cynexus-data` as a Java-triggerable Python Job Executor.

## Current Delivery Boundary

This pass completes the executable Job Executor foundation and dry-run migration skeleton. Detailed production parity for the old market factor calculations remains tracked as `planned_after_skeleton` in the inventory.

## Phase 0 - Inventory

Status: complete

- Reviewed migration boundary, legacy market adapters, repositories, scheduler runtime files, stock services, backend models, scripts, and backend design docs.
- Created capability inventory: `docs/architecture/legacy_migration_inventory.md`.
- Created per-file audit trail: `docs/architecture/legacy_file_inventory.md`.

## Phase 1 - Job Executor Foundation

Status: complete

- Added connections, hooks, runtime context, job contracts, registry, job runner, job store, checkpoints, locks, and trigger API.
- Added PostgreSQL DDL for Python-owned job records.
- Added `JobRegistry/default_registry()` facade while preserving `JOB_REGISTRY`.

## Phase 2 - Storage Foundation

Status: complete

- Added ClickHouse, DuckDB, and Postgres clients/readers/writers with Hook-based connection injection.
- Kept existing 1m parquet/TDX ingestion CLIs importable and their help checks passing.

## Phase 3 - Sources Foundation

Status: complete

- Added source client/parser/adapter modules for EastMoney, TDX, Tencent, Sina, THS, and AKShare fallback.
- Source code remains separated from storage/runtime imports.

## Phase 4 - Pipelines And Jobs

Status: complete

- Added dry-run pipelines and jobs for kline daily/minute/aggregate/backfill/repair, stock metadata, trade calendar, fund flow, margin, MSI, quality, and transcription.
- Added shared CLI entrypoint helper so each registered job can run with `python -m cynexus_data.jobs.<job_key> --dry-run`.

## Phase 5 - Regression

Status: complete

- Compile, unit tests, health check, dry-run jobs, trigger API, ingest CLI help, and boundary audits passed.

## Phase 6 - Production Parity Backlog

Status: in_progress

- Fill in detailed calculations currently marked `planned_after_skeleton`: market factor repositories, MSI factor formulas, market overview, market pulse, limit emotion, sector breadth, detailed fund-flow ranks, TDX hsjday safety flow, and final persisted transcription implementation.
- Add integration tests that require live ClickHouse/Postgres/network credentials.
- Completed in this pass: Kline period aggregation execution path, TDX local minute sync execution path, TDX local daily `.day` ingestion/backfill path, TDX hsjday zip prepare/verify/replace job, eltdx adjusted daily qfq/hfq sync path, stock basic universe sync, stock sector mapping sync, and Kline OHLCV read-only quality checks.
- Completed in this pass: THS industry fund-flow snapshot migration with local JSON import, optional live THS fetch path, PostgreSQL snapshot tables, docs, unit tests, dry-run job validation, and boundary scans.
- Completed in this pass: MSI composite builder/rebuilder, `msi_index_daily`, sector-breadth factor builder from industry fund-flow snapshots, docs, unit tests, dry-run job validation, and boundary scans.
- Completed in this pass: transcript-only media transcription execution path, PostgreSQL persistence, injectable transcriber boundary, docs, unit tests, dry-run job validation, and boundary scans.
- Completed in this pass: market pulse sector snapshot builder from migrated sector fund-flow rows, PostgreSQL capture/snapshot tables, docs, unit tests, dry-run job validation, and boundary scans.
- Completed in this pass: market overview daily snapshot table, source-specific field merge rules, build job, docs, unit tests, dry-run job validation, and boundary scans.
- Completed in this pass: MA count and profit-effect MSI producers, ClickHouse daily adjusted input reader, PostgreSQL factor tables, docs, unit tests, dry-run job validation, full unit regression, and boundary scans.
- Completed in this pass: remaining MSI factor producers for turnover activity, volatility sentiment, risk appetite, style risk, and limit emotion, including pure formulas, CH readers, PG tables, pipelines, jobs, docs, unit tests, CLI dry-runs, full unit regression, and boundary scans.
- Completed in this pass: trading-calendar pure helpers, PostgreSQL calendar table/repository, executable sync pipeline, docs, unit tests, and CLI dry-run validation.
- Completed in this pass: source-layer provider fallback facade, daily/minute kline network fallback paths, normalized CH frame conversion, docs, unit tests, and CLI dry-run validation.
- Completed in this pass: Kline daily and adjusted-daily resume checkpoints, checkpoint skip/write metadata, docs, and unit tests.
- Completed in this pass: ST-aware limit emotion snapshot generation using stock metadata, ClickHouse OHLC fact reads, docs, and unit tests.
- Completed in this pass: historical limit streak parity for limit emotion snapshots, including previous/current streaks, promotion/broken-streak flags, streak summary payload, docs, and unit tests.
- Completed in this pass: TDX hsjday optional Playwright anti-bot fallback, download method job params, docs, unit tests, dry-run validation, full unit regression, and boundary scans.
- Completed in this pass: Tencent qt stock fund-flow snapshot migration, PostgreSQL stock snapshot tables, executable `sync_stock_fund_flow` job, docs, unit tests, dry-run validation, full unit regression, and boundary scans.
- Completed in this pass: AkShare market-overview live capture for spot breadth/amount and market fund-flow, hook wiring, docs, unit tests, dry-run validation, full unit regression, and boundary scans.
- Completed in this pass: eltdx market-overview live capture for A-share amount/breadth, TDX hook wiring, docs, unit tests, dry-run validation, full unit regression, and boundary scans.
- Completed in this pass: THS q.10jqka industry constituent production path with parser/client, stock-domain normalization, PostgreSQL snapshot tables, history import, live explicit-code sync, job registration, docs, unit tests, dry-run validation, full unit regression, and boundary scans.
- Completed in this pass: EastMoney clist market-breadth live capture wired into `build_market_overview`, docs, unit tests, dry-run validation, full unit regression, and boundary scans.
- Completed in this pass: EastMoney explicit-code stock metadata enrichment for `sync_stock_basic`, additive stock-basic PG columns, docs, unit tests, dry-run validation, full unit regression, and boundary scans.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| `New-Item -LiteralPath` unsupported in this PowerShell host | Create new migration directories | Retried with `New-Item -Path`. |
| Detailed inventory generation timed out | Used Python `Path.rglob()` over old repo | Replaced with `rg --files` input and explicit excludes. |
| PowerShell heredoc syntax failed | Tried piping `$files` into `python - <<'PY'` | Replaced with Python subprocess calling `rg --files`. |
| Some job modules exited with no output under `python -m` | Batch dry-run validation | Added shared `main_for_job()` and wired CLI main functions into all registered pipeline jobs. |
| `ModuleNotFoundError: cynexus_data` during pytest collection | Ran pytest in a shell without `src` on `PYTHONPATH` | Retried validation with `$env:PYTHONPATH='src'`; targeted and full unit tests passed. |
| `health_check.py: error: unrecognized arguments: --dry-run` | Ran health check with shared pipeline-job CLI flag | Re-ran as `python -m cynexus_data.jobs.health_check`; health check passed. |
| `session-catchup.py` not found under `.claude` | Ran planning-with-files catchup helper on this Windows host | Recovered context from project `task_plan.md`, `progress.md`, and `findings.md`; recorded missing helper and continued. |
| Documentation/test patch context mismatch | First combined patch expected different `storage/postgres/README.md` wording | Split into smaller patches using actual file text; docs and tests landed. |
