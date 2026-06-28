# cynexus-data Migration Progress

## 2026-06-28

- Started implementation from `migration.md`.
- Created persistent migration planning files inside `cynexus-data` to keep this workflow independent from the repo root Java/ClickHouse plan.
- Created capability inventory in `docs/architecture/legacy_migration_inventory.md`.
- Created per-file legacy audit trail in `docs/architecture/legacy_file_inventory.md`; reviewed 618 non-runtime, non-dependency legacy files without reading `.env`.
- Added job executor design/data-contract docs:
  - `docs/architecture/job_executor.md`
  - `docs/data_contracts/job.md`
  - `docs/pipeline_design/transcription.md`
- Added PostgreSQL DDL for Python-owned job run, checkpoint, lock, and transcript tables:
  - `sql/postgres/20260628_create_job_executor_tables.sql`
- Added connections, hooks, runtime, storage, source, pipeline, job, and Trigger API foundation under `src/cynexus_data`.
- Added source modules for EastMoney, TDX, Tencent, Sina, THS, and AKShare fallback.
- Added dry-run jobs for kline, metadata, fund flow, margin, MSI, quality, repair/backfill, and transcription.
- Added `config/jobs.yaml` as the Java-trigger job contract catalog.
- Added shared job CLI helper and wired registered jobs to `python -m cynexus_data.jobs.<job_key> --dry-run`.
- Installed project dependencies from `requirements.txt` in the current Python environment so FastAPI Trigger API validation could run.
- Removed local password-style defaults from `.env.example`, `README.md`, `connections.yaml`, and `settings.py`; connection secrets now come from env.

## Validation Results

- `python -m compileall -q src`: passed.
- `pytest -q`: passed, 9 tests.
- `python -m cynexus_data.jobs.health_check`: passed.
- `python -m cynexus_data.jobs.sync_kline_daily --dry-run --trade-date 2026-06-28`: passed.
- `python -m cynexus_data.jobs.sync_kline_minute --dry-run`: passed.
- `python -m cynexus_data.jobs.aggregate_kline_periods --dry-run`: passed.
- `python -m cynexus_data.jobs.backfill_history --dry-run --start-date 2024-01-01 --end-date 2024-01-05`: passed.
- `python -m cynexus_data.jobs.repair_missing_data --dry-run --dataset kline`: passed.
- `python -m cynexus_data.jobs.sync_metadata --dry-run`: passed.
- `python -m cynexus_data.jobs.sync_trade_calendar --dry-run`: passed.
- `python -m cynexus_data.jobs.sync_stock_basic --dry-run`: passed.
- `python -m cynexus_data.jobs.sync_fund_flow --dry-run`: passed.
- `python -m cynexus_data.jobs.sync_margin --dry-run`: passed.
- `python -m cynexus_data.jobs.build_msi --dry-run`: passed.
- `python -m cynexus_data.jobs.rebuild_msi --dry-run`: passed.
- `python -m cynexus_data.jobs.quality_check --dry-run`: passed.
- `python -m cynexus_data.jobs.transcribe_media --dry-run --input-uri file:///tmp/demo.mp4`: passed.
- Trigger API TestClient:
  - `GET /internal/health`: 200.
  - `GET /internal/jobs`: 200.
  - `POST /internal/jobs/health_check/trigger`: 200.
  - `POST /internal/jobs/sync_kline_daily/trigger`: 200.
- Existing 1m ingestion checks:
  - `python -m cynexus_data.jobs.ingest_parquet_1m_to_clickhouse --help`: passed.
  - `python -m cynexus_data.jobs.ingest_tdx_1m_to_clickhouse --help`: passed.
- Boundary audits:
  - No `import backend` / `from backend` matches in `src` or `tests`.
  - No domain code imports network/database/env/hook dependencies.
  - No source code imports storage/runtime or storage hooks; only README boundary examples mention ClickHouse/Postgres.
  - No local password-style defaults matched the credential scan.

## Errors And Fixes

- `New-Item -LiteralPath` was unsupported in this PowerShell host; directory creation was retried with `New-Item -Path`.
- Per-file inventory generation with recursive `Path.rglob()` timed out; replaced with `rg --files` and explicit excludes.
- PowerShell heredoc redirection syntax failed; replaced with Python subprocess invocation.
- Several registered job modules initially had no `python -m` output; added `main_for_job()` and module-level `main()` wrappers.

## 2026-06-28 Production Logic Pass

- Implemented grouped pure OHLCV aggregation in `domain/stock/aggregate_kline.py`.
- Implemented ClickHouse kline period count and insert helpers in `storage/clickhouse/readers.py`.
- Upgraded `pipelines/kline/aggregate_periods.py` from a skeleton to an executable ClickHouse aggregation pipeline.
- Upgraded `pipelines/kline/sync_minute.py` from file discovery to optional TDX local 1m ingestion through `ClickHouseKlineSink`.
- Added `daily_raw` ClickHouse DDL and a TDX local `.day` source reader.
- Upgraded `pipelines/kline/sync_daily.py` from a skeleton to optional TDX local `.day` ingestion through `ClickHouseDailyKlineSink`.
- Updated `pipelines/kline/backfill.py` to delegate daily kline backfills to the daily sync pipeline.
- Replaced the custom `sync_kline_daily` CLI parser with the shared job CLI parser so new params are passed through.
- Added OHLCV quality rules in `quality/checks.py`.
- Upgraded `pipelines/quality/run_quality_check.py` to support read-only ClickHouse anomaly counts.
- Added Kline contract and pipeline docs:
  - `docs/data_contracts/kline.md`
  - `docs/pipeline_design/kline.md`
- Added unit tests for grouped aggregation, ClickHouse query builders, aggregate pipeline orchestration, TDX minute dry-run, and OHLCV quality checks.

## 2026-06-28 Production Logic Validation

- `python -m compileall -q src`: passed.
- `pytest tests/unit -q`: passed, 19 tests.
- `python -m cynexus_data.jobs.aggregate_kline_periods --dry-run --periods 5,30 --trade-date 2026-06-26`: passed.
- `python -m cynexus_data.jobs.quality_check --dry-run --checks kline_ohlcv --trade-date 2026-06-26`: passed.
- `python -m cynexus_data.jobs.sync_kline_minute --dry-run --limit-files 1`: passed.
- `python -m cynexus_data.jobs.sync_kline_daily --dry-run --market sz --limit-files 1`: passed.
- `python -m cynexus_data.jobs.backfill_history --dry-run --dataset kline_daily --market sz --limit-files 1 --start-date 2026-01-01 --end-date 2026-06-26`: passed.

## 2026-06-28 TDX Hsjday Migration Pass

- Added `sources/tdx/hsjday.py` for hsjday zip download, freshness inspection, safe extraction, extracted-root verification, and rollback-capable target replacement.
- Added `pipelines/kline/sync_tdx_hsjday.py`.
- Added `jobs/sync_tdx_hsjday.py` and registered it in `jobs/registry.py`.
- Added `sync_tdx_hsjday` to `config/jobs.yaml`.
- Added `docs/pipeline_design/tdx_hsjday.md`.
- Added unit tests for hsjday zip verification/extraction and skip-download pipeline replacement.

## 2026-06-28 TDX Hsjday Validation

- `python -m compileall -q src`: passed.
- `pytest tests/unit -q`: passed, 23 tests.
- `python -m cynexus_data.jobs.sync_tdx_hsjday --dry-run --date 2026-06-26`: passed.
- Trigger API:
  - `GET /internal/jobs/sync_tdx_hsjday`: 200.
  - `POST /internal/jobs/sync_tdx_hsjday/trigger`: 200.

## 2026-06-28 Adjusted Daily Kline Migration Pass

- Added ClickHouse DDL for `daily_qfq` and `daily_hfq`.
- Added `sources/tdx/adjusted.py` for eltdx adjusted daily K-line normalization.
- Added `ClickHouseAdjustedDailyKlineSink`.
- Added `pipelines/kline/sync_adjusted_daily.py`.
- Added `jobs/sync_adjusted_kline_daily.py` and registered it in `jobs/registry.py`.
- Added `sync_adjusted_kline_daily` to `config/jobs.yaml`.
- Updated Kline data contract and pipeline docs for adjusted daily tables.
- Added unit tests for adjusted source normalization and pipeline orchestration.

## 2026-06-28 Adjusted Daily Kline Validation

- `python -m compileall -q src`: passed.
- `pytest tests/unit -q`: passed, 27 tests.
- `python -m cynexus_data.jobs.sync_adjusted_kline_daily --dry-run --codes 000001 --adjust qfq`: passed.
- Trigger API:
  - `GET /internal/jobs/sync_adjusted_kline_daily`: 200.
  - `POST /internal/jobs/sync_adjusted_kline_daily/trigger`: 200.

## 2026-06-28 Stock Basic Migration Pass

- Added PostgreSQL DDL for `cynexus_data.sec_stock_basic`.
- Added `domain/stock/stock_meta.py` for market, board, ST, and limit threshold rules.
- Added `sources/akshare/stock_basic.py` for AkShare stock code/name normalization.
- Added `storage/postgres/stock_basic.py` repository for stock basic upsert and active code lookup.
- Upgraded `pipelines/stock_basic/sync_stock_basic.py` from skeleton to AkShare -> PostgreSQL upsert.
- Updated `sync_adjusted_kline_daily` so non-dry-run can load active codes from `sec_stock_basic` when `codes` is omitted.
- Added stock basic data contract and pipeline docs.
- Added unit tests for stock metadata rules, AkShare normalization, stock basic pipeline, and adjusted daily code lookup from PostgreSQL.

## 2026-06-28 Stock Basic Validation

- `python -m compileall -q src`: passed.
- `pytest tests/unit -q`: passed, 32 tests.
- `python -m cynexus_data.jobs.sync_stock_basic --dry-run`: passed.
- `python -m cynexus_data.jobs.sync_adjusted_kline_daily --dry-run --adjust qfq`: passed.
- Trigger API:
  - `GET /internal/jobs/sync_stock_basic`: 200.
  - `POST /internal/jobs/sync_stock_basic/trigger`: 200.

## 2026-06-28 Stock Sector Migration Pass

- Added PostgreSQL DDL for `sec_sector` and `sec_stock_sector`.
- Added `domain/stock/sector_meta.py` for industry extraction, category labels, sector ids, and mapping normalization.
- Added `sources/tdx/stock_topics.py` for eltdx stock topics normalization.
- Added `storage/postgres/stock_sector.py`.
- Added `pipelines/stock_basic/sync_stock_sectors.py`.
- Added `jobs/sync_stock_sectors.py` and registered it.
- Added `sync_stock_sectors` to `config/jobs.yaml`.
- Updated stock basic data contract and pipeline docs with sector mapping.
- Added unit tests for industry extraction, topic mapping, source mapping, and pipeline orchestration.

## 2026-06-28 Stock Sector Validation

- `python -m compileall -q src`: passed.
- `pytest tests/unit -q`: passed, 37 tests.
- `python -m cynexus_data.jobs.sync_stock_sectors --dry-run --codes 000001`: passed.
- Trigger API:
  - `GET /internal/jobs/sync_stock_sectors`: 200.
  - `POST /internal/jobs/sync_stock_sectors/trigger`: 200.

## 2026-06-28 THS Sector Fund Flow Migration Pass

- Reviewed legacy THS fund-flow adapter, service, repository, Alembic DDL, and backfill script.
- Added normalized sector fund-flow domain rules in `domain/market/fund_flow.py`.
- Upgraded THS parser/client to parse industry fund-flow HTML and optionally perform live `hexin-v` THS requests.
- Added PostgreSQL DDL and repository for sector fund-flow capture batches and daily snapshots.
- Upgraded `pipelines/fund_flow/sync_sector_flow.py` from dry-run skeleton to local JSON import/live THS sync orchestration.
- Updated `sync_fund_flow` job contract params in `config/jobs.yaml`.
- Added fund-flow data contract and pipeline docs.
- Added unit tests for THS parser, fund-flow domain normalization, and JSON-import pipeline orchestration.

## 2026-06-28 THS Sector Fund Flow Validation

- `python -m compileall -q src`: passed.
- First targeted pytest attempt failed at collection because this shell did not have `src` on `PYTHONPATH`; retried with `$env:PYTHONPATH='src'`.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_market_parsers.py tests/unit/test_domain/test_fund_flow.py tests/unit/test_pipelines/test_fund_flow_pipeline.py -q`: passed, 8 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_fund_flow --dry-run --trade-date 2026-06-26`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 42 tests.
- Boundary audits:
  - No `import backend` / `from backend` matches in `src` or `tests`.
  - Domain scan only matched README boundary text.
  - Source scan only matched README boundary text.

## 2026-06-28 MSI Composite Migration Pass

- Reviewed legacy MSI 9-factor design, market sentiment pipeline docs, repository, and backfill script.
- Added pure MSI composite domain rules in `domain/market/msi.py`: weights, missing-score neutral handling, percentile helper, sector-breadth score, and level mapping.
- Added PostgreSQL DDL and repository support for `msi_index_daily`.
- Upgraded `pipelines/msi/build_msi.py` from a dry-run stub to an executable composite builder with component params, optional cache read, `force`, `require_full`, and `create_table`.
- Upgraded `pipelines/msi/rebuild_msi.py` to orchestrate date ranges through `build_msi`.
- Added MSI data contract and pipeline docs.
- Added unit tests for composite scoring, helpers, build dry-run, require-full skip, and rebuild date planning.

## 2026-06-28 MSI Composite Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_msi.py tests/unit/test_pipelines/test_msi_pipeline.py -q`: passed, 6 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_msi --dry-run --trade-date 2026-06-26 --components '{"vol":80,"turnover":60}'`: passed.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.rebuild_msi --dry-run --start-date 2026-06-24 --end-date 2026-06-26`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 48 tests.
- Boundary audits stayed clean; domain/source matches were README-only.

## 2026-06-28 Sector Breadth Migration Pass

- Reviewed legacy `sector_breadth_repo.py` and `backfill_sector_breadth.py`.
- Added `msi_sector_breadth_daily` DDL under the MSI table script.
- Added PostgreSQL repository methods to compute sector breadth from migrated `sector_fund_flow_daily_snapshot` rows and persist the MSI factor-8 snapshot.
- Added `pipelines/msi/build_sector_breadth.py`.
- Added and registered `jobs/build_sector_breadth.py`.
- Updated MSI docs and job contract catalog.
- Added sector-breadth pipeline unit test.

## 2026-06-28 Sector Breadth Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_pipelines/test_sector_breadth_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 3 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_sector_breadth --dry-run --trade-date 2026-06-26 --advancing 72 --declining 17 --flat 1 --total 90`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 49 tests.
- Boundary audits stayed clean; domain/source matches were README-only.

## 2026-06-28 Transcription Persistence Migration Pass

- Reviewed legacy `backend/services/transcription_service.py` and `transcribe.py`.
- Confirmed migration boundary remains transcript-only; polish, summary, ask, prompt/provider management stay out of scope.
- Added `storage/postgres/transcription.py` for `media_transcript` read/upsert and DDL application.
- Added media helpers for local path classification, optional ffmpeg video-to-WAV conversion, and duration probing.
- Added injectable transcriber loader for `module:attribute`, plus controlled `transcript_text` / `mock_transcript` path for tests/manual repair.
- Upgraded `pipelines/transcription/transcribe_media.py` from `NotImplementedError` to executable local media -> transcript -> PostgreSQL persistence.
- Updated transcription pipeline docs and `transcribe_media` job params.
- Added unit tests for media classification, dry-run boundary, persistence, and cache reads.

## 2026-06-28 Transcription Persistence Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_pipelines/test_transcription_pipeline.py -q`: passed, 4 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.transcribe_media --dry-run --input-uri file:///tmp/demo.mp4`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 53 tests.
- Boundary audits stayed clean; domain/source matches were README-only.

## 2026-06-28 Market Pulse Migration Pass

- Reviewed legacy market pulse repository, service, and Alembic DDL.
- Added pure market pulse domain rules in `domain/market/market_pulse.py`.
- Added PostgreSQL DDL and repository for `market_pulse_capture_batch` and `market_pulse_sector_daily_snapshot`.
- Added `pipelines/market_pulse/build_market_pulse.py`.
- Added and registered `jobs/build_market_pulse.py`.
- Updated job catalog plus market pulse data contract and pipeline docs.
- Added unit tests for strong-sector ranking, capital-flow split, dry-run pipeline, and registry coverage.

## 2026-06-28 Market Pulse Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_market_pulse.py tests/unit/test_pipelines/test_market_pulse_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 5 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_market_pulse --dry-run --trade-date 2026-06-26 --rows '[{"sector_name":"软件服务","change_pct":1.2,"net":6},{"sector_name":"银行","change_pct":-0.2,"net":-3}]'`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 56 tests.
- Boundary audits stayed clean; domain/source matches were README-only.

## 2026-06-28 Market Overview Migration Pass

- Reviewed legacy market overview PG repository, eltdx service, akshare service, manual fund-flow service, and backfill script.
- Added pure market overview field normalization and source-specific merge rules in `domain/market/market_overview.py`.
- Added PostgreSQL DDL and repository for `market_overview_snapshot`.
- Added `pipelines/market_overview/build_market_overview.py`.
- Added and registered `jobs/build_market_overview.py`.
- Updated job catalog plus market overview data contract and pipeline docs.
- Added unit tests for alias normalization, source overwrite rules, source merge, dry-run pipeline, and registry coverage.

## 2026-06-28 Market Overview Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_market_overview.py tests/unit/test_pipelines/test_market_overview_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 5 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_market_overview --dry-run --source eltdx --fields '{"tradingDate":"2026-06-26","totalAmount":1234.5,"risingCount":3000}'`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 59 tests.
- Boundary audits stayed clean; domain/source matches were README-only.

## 2026-06-28 MA Count And Profit Effect Migration Pass

- Reviewed legacy MA count/profit-effect repositories and backfill scripts.
- Added pure `domain/market/ma_count.py` formulas for MA20/MA60 participation, 5-day up ratio, 60-day new lows, 252-day new highs, breadth raw score, and profit-effect score.
- Added PostgreSQL DDL and repository methods for `msi_ma_count_daily` and `msi_profit_effect_daily`.
- Added ClickHouse reader `read_ma_count_input_rows` over adjusted daily K-line data.
- Added `pipelines/msi/build_ma_count.py` and `pipelines/msi/build_profit_effect.py`.
- Added and registered `jobs/build_ma_count.py` and `jobs/build_profit_effect.py`.
- Updated MSI docs and job catalog.
- Added unit tests for the pure formulas and dry-run pipeline behavior.

## 2026-06-28 MA Count And Profit Effect Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_ma_count.py tests/unit/test_pipelines/test_msi_factor_pipeline.py tests/unit/test_jobs_registry.py -q`: passed.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_profit_effect --dry-run --trade-date 2026-06-26 --up-5d-pct 60 --new-low-60d-pct 10`: passed.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_ma_count --dry-run --trade-date 2026-06-26 --rows '[{"code":"000001","close":12,"ma20":10,"ma60":9,"close_1d_ago":11,"close_5d_ago":10,"close_59d_ago":13,"min_60d":12,"close_251d_ago":8,"max_252d":12}]'`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 63 tests.
- Boundary audits stayed clean; domain/source matches were README-only.

## 2026-06-28 Remaining MSI Factor Producers Migration Pass

- Reviewed legacy `risk_appetite_repo.py`, `style_risk_appetite_repo.py`, `turnover_activity_repo.py`, `volatility_sentiment_repo.py`, `limit_repo.py`, `percentile_helper.py`, and related backfill references.
- Added pure `domain/market/msi_factors.py` calculations for turnover activity, volatility sentiment, risk appetite, style risk, and limit emotion.
- Extended ClickHouse readers for close series, market amount rows, and daily limit-emotion snapshots.
- Extended PostgreSQL MSI DDL and repository writes for:
  - `msi_turnover_activity_daily`
  - `msi_volatility_daily`
  - `msi_risk_appetite_daily`
  - `msi_style_risk_daily`
  - `msi_limit_emotion_daily`
- Fixed MA Count composite linkage by adding `price_strength_score` and `breadth_score` to domain output, DDL, and repository write/read paths.
- Added pipelines and jobs:
  - `build_turnover_activity`
  - `build_volatility_sentiment`
  - `build_risk_appetite`
  - `build_style_risk`
  - `build_limit_emotion`
- Registered new jobs, updated `config/jobs.yaml`, job README, MSI pipeline docs, MSI data contracts, and pipeline-local README.
- Added unit tests for pure factor formulas and dry-run pipeline orchestration.

## 2026-06-28 Remaining MSI Factor Producers Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_msi_factors.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 7 tests.
- CLI dry-runs passed through `JobRunner` for:
  - `python -m cynexus_data.jobs.build_turnover_activity --dry-run`
  - `python -m cynexus_data.jobs.build_volatility_sentiment --dry-run`
  - `python -m cynexus_data.jobs.build_risk_appetite --dry-run`
  - `python -m cynexus_data.jobs.build_style_risk --dry-run`
  - `python -m cynexus_data.jobs.build_limit_emotion --dry-run`
- `python -m cynexus_data.jobs.health_check`: passed. The `health_check` entrypoint uses its own CLI and does not accept the shared `--dry-run` flag.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 68 tests.
- Boundary audits:
  - No `import backend` / `from backend` matches in `src` or `tests`.
  - Domain/source scan only matched README boundary text, source clients, and literal lineage strings such as `clickhouse.daily_qfq`.

## 2026-06-28 Trading Calendar Migration Pass

- Reviewed legacy `backend/services/stock/trading_calendar.py` and the new `core/calendar.py` placeholder.
- Migrated local A-share trading calendar rules for 2024-2027 into pure `core/calendar.py`.
- Added pure helpers: `is_trading_day`, `previous_trading_day`, `next_trading_day`, `generate_trade_calendar`, and `is_trade_time`.
- Added PostgreSQL table `sec_trade_calendar` to the stock metadata DDL and `TradeCalendarRepository`.
- Upgraded `pipelines/stock_basic/sync_trade_calendar.py` from a dry-run skeleton to executable calendar generation and PG persistence.
- Updated job params, core docs, stock-basic data contract, and stock-basic pipeline docs.
- Added unit tests for calendar rules, override behavior, trade time, and dry-run/write pipeline orchestration.

## 2026-06-28 Trading Calendar Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_core/test_calendar.py tests/unit/test_pipelines/test_trade_calendar_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 7 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_trade_calendar --dry-run --start-date 2026-01-01 --end-date 2026-01-05`: passed through `JobRunner`, 5 rows generated, 1 trading day.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 73 tests.
- Boundary audits:
  - No `import backend` / `from backend` matches in `src` or `tests`.
  - Domain/core/source scan only matched README boundary text, source-client network imports, and literal lineage strings.

## 2026-06-28 Provider Fallback Migration Pass

- Reviewed legacy `backend/services/stock/market_data_provider.py`.
- Added `sources/provider_fallback.py` to resolve provider order from `config/sources.yaml`, attempt providers in order, capture failures/empty results, and return `source`, `stale`, `dataQuality`, and attempt metadata.
- Added symbol helpers for market-prefix normalization and EastMoney `secid` conversion.
- Added `domain/stock/kline_records.py` to convert provider kline rows into ClickHouse daily/minute DataFrames without storage writes inside the source layer.
- Upgraded `sync_kline_daily`:
  - TDX local file ingestion remains the primary local path.
  - When providers exclude `tdx` and `codes` are passed, daily bars are fetched through provider fallback and written to `daily_raw`.
  - Dry-run reports `network_provider_plan`.
- Upgraded `sync_kline_minute` similarly for non-TDX minute providers.
- Added provider fallback architecture docs and updated kline pipeline docs.
- Added unit tests for fallback attempts, symbol helpers, daily network persistence path, and updated minute dry-run behavior.

## 2026-06-28 Provider Fallback Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_provider_fallback.py tests/unit/test_pipelines/test_kline_sync_daily.py tests/unit/test_pipelines/test_kline_sync_minute.py -q`: passed, 5 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_kline_daily --dry-run --providers eastmoney,tencent --codes 600000 --trade-date 2026-06-26`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_kline_minute --dry-run --providers sina,eastmoney --codes 600000 --period 1`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 76 tests.
- Boundary audits:
  - No `import backend` / `from backend` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook imports; matches are lineage strings such as `clickhouse.daily_qfq`.

## 2026-06-28 Kline Resume Checkpoint Migration Pass

- Added optional `resume=true` support to `sync_kline_daily`.
  - Completed TDX local file batches are skipped by checkpoint key.
  - Completed network provider code/date/provider batches are skipped by checkpoint key.
  - Successful batches write checkpoint payloads with status, row counts, source, and date range.
- Added optional `resume=true` support to `sync_adjusted_kline_daily`.
  - Completed `adjust + code + date range` batches are skipped.
  - Successful adjusted batches write checkpoint payloads with status and row counts.
- Updated job params and Kline pipeline docs.
- Added unit tests for daily local resume skip and adjusted daily resume skip.

## 2026-06-28 Kline Resume Checkpoint Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_pipelines/test_kline_sync_daily.py tests/unit/test_pipelines/test_kline_sync_adjusted_daily.py -q`: passed, 7 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 78 tests.
- Boundary audits:
  - No `import backend` / `from backend` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook imports; matches are lineage strings only.

## 2026-06-28 Limit Emotion ST-Aware Snapshot Migration Pass

- Reviewed current limit-emotion implementation and stock metadata repository.
- Added pure `build_limit_snapshot_rows` in `domain/market/msi_factors.py`.
  - Uses `sec_stock_basic.limit_up_threshold` and `is_st` metadata when present.
  - Falls back to code-prefix threshold rules when metadata is missing.
  - Produces limit-up/down, touched, broken-board, change percent, and threshold lineage fields.
- Added ClickHouse reader `read_limit_emotion_ohlcv_rows` so ClickHouse only supplies OHLC/pre-close facts.
- Added `StockBasicRepository.list_limit_meta`.
- Updated `build_limit_emotion` non-dry-run path to load stock meta, build ST-aware snapshots, then persist summary.
- Updated MSI docs and data contract.
- Added unit tests for ST threshold behavior and pipeline metadata usage.

## 2026-06-28 Limit Emotion ST-Aware Snapshot Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_msi_factors.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py -q`: passed, 7 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 80 tests.
- Boundary audits:
  - No `import backend` / `from backend` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook imports; matches are lineage strings only.

## 2026-06-28 Limit Emotion Historical Streak Migration Pass

- Reviewed legacy `limit_repo.py` streak calculation and distribution output.
- Extended pure `build_limit_snapshot_rows` with optional `history_rows`.
  - Computes `previousLimitUpStreak`.
  - Computes current `limitUpStreak`.
  - Computes `isPromoted` and `isBrokenStreak`.
- Added pure `summarize_limit_streaks` to produce max height, leaders, distribution, promoted levels, and broken-streak rows.
- Added ClickHouse reader `read_limit_emotion_history_ohlcv_rows` for bounded historical OHLC/pre-close windows.
- Updated `build_limit_emotion` production path to read historical rows using `streak_lookback_days` and include streak summary in the returned payload.
- Updated MSI docs, data contract, and job params.
- Added unit tests for historical streak continuation/promotion and broken streak behavior.

## 2026-06-28 Limit Emotion Historical Streak Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_msi_factors.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py -q`: passed, 8 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 81 tests.
- Boundary audits:
  - No `import backend` / `from backend` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook imports; matches are lineage strings only.

## 2026-06-28 TDX Hsjday Playwright Fallback Migration Pass

- Reviewed legacy `scripts/download_tdx_hsjday.py` browser fallback behavior.
- Added `download_method=auto|urlopen|playwright` to `sources/tdx/hsjday.py`.
- Added `HsjdayDownloadResult` and challenge-specific error handling so partial challenge downloads do not replace the final zip.
- Added optional Playwright download path that visits the TDX home page, then downloads `hsjday.zip` through browser request Range chunks.
- Updated `sync_tdx_hsjday` to pass `download_method`, `playwright_headless`, `playwright_wait_ms`, `chunk_size_bytes`, and `timeout_seconds`.
- Updated hsjday pipeline docs, job parameter config, and legacy inventory status.
- Added unit coverage for JS challenge detection, auto fallback dispatch, and pipeline parameter forwarding.

## 2026-06-28 TDX Hsjday Playwright Fallback Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_tdx_hsjday.py tests/unit/test_pipelines/test_tdx_hsjday_pipeline.py -q`: passed, 5 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 84 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_tdx_hsjday --dry-run --date 2026-06-26 --download-method auto`: passed through `JobRunner`.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings and field names only.

## 2026-06-28 Tencent Stock Fund-Flow Migration Pass

- Reviewed legacy `backend/services/stock/f10/qt_fund_flow_service.py`.
- Added Tencent qt stock fund-flow parser for main quote rows and `s_pk` disk-ratio rows.
- Added `TencentClient.fetch_stock_fund_flow_batch`.
- Added pure `normalize_stock_fund_flow_snapshot` in `domain/market/fund_flow.py`.
- Added PostgreSQL DDL and `StockFundFlowRepository` for:
  - `stock_fund_flow_capture_batch`
  - `stock_fund_flow_snapshot`
- Replaced empty `pipelines/fund_flow/sync_stock_flow.py` with executable history import and live Tencent batch sync.
- Added and registered `jobs/sync_stock_fund_flow.py`.
- Updated job config, package console scripts, source docs, pipeline docs, and legacy inventory.
- Added unit tests for parser, domain normalization, history import pipeline, live-mock pipeline, and registry.

## 2026-06-28 Tencent Stock Fund-Flow Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_tencent_fund_flow.py tests/unit/test_domain/test_fund_flow.py tests/unit/test_pipelines/test_fund_flow_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 11 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_stock_fund_flow --dry-run --trade-date 2026-06-26 --codes 600000,000001`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 89 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings and field names only.

## 2026-06-28 AkShare Market Overview Capture Migration Pass

- Reviewed legacy `backend/services/stock/market_overview_akshare_service.py`.
- Added lazy AkShare market-overview capture in `sources/akshare/client.py`.
  - `stock_zh_a_spot_em()` summarizes total amount, volume, rising/falling/flat counts, limit-up/down counts, and stock count.
  - `stock_market_fund_flow()` summarizes main, super-large, large, medium, and small net inflow fields and ratios.
- Added `hooks/sources/akshare.py`, `akshare_default` connection config, and `HookBundle.akshare`.
- Upgraded `build_market_overview` so `source=akshare` with no explicit fields automatically fetches live source fields in non-dry-run mode.
- Kept dry-run free of AkShare import/network access.
- Updated market-overview docs, AkShare source docs, job params, findings, and inventory.
- Added unit tests for AkShare DataFrame summarization and pipeline live-source orchestration with mocked hooks/repository.

## 2026-06-28 AkShare Market Overview Capture Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_akshare_market_overview.py tests/unit/test_pipelines/test_market_overview_pipeline.py tests/unit/test_domain/test_market_overview.py -q`: passed, 5 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_market_overview --dry-run --source akshare --trade-date 2026-06-26`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 91 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings and field names only.

## 2026-06-28 eltdx Market Overview Capture Migration Pass

- Reviewed legacy `backend/services/stock/market_overview_eltdx_service.py`.
- Added `sources/tdx/market_overview.py` with lazy optional `eltdx` client creation and pure quote summarization.
- Added `TdxHook.get_market_overview_client`.
- Upgraded `build_market_overview` so `source=eltdx` with no explicit overview fields automatically fetches live source fields in non-dry-run mode.
- Added `eltdx_timeout` / `timeout_seconds` job params.
- Updated market-overview docs, TDX source docs, findings, and inventory.
- Added unit tests for eltdx quote summarization and pipeline live-source orchestration with mocked hooks/repository.

## 2026-06-28 eltdx Market Overview Capture Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_tdx_market_overview.py tests/unit/test_pipelines/test_market_overview_pipeline.py tests/unit/test_domain/test_market_overview.py -q`: passed, 6 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_market_overview --dry-run --source eltdx --trade-date 2026-06-26`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 93 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings and field names only.

## 2026-06-28 THS Industry Constituents Migration Pass

- Reviewed legacy `backend/adapters/market/ths_industry_constituents_adapter.py` and `backend/services/stock/f10/ths_industry_constituents_service.py`.
- Added THS constituent HTML parsing and q.10jqka live client methods while keeping the existing data.10jqka fund-flow path intact.
- Added pure `domain.stock.ths_industry_constituents` normalization for six-digit stock codes, dedupe, and quote snapshot fields.
- Added PostgreSQL DDL and `ThsIndustryConstituentRepository` for:
  - `ths_industry_constituent_capture_batch`
  - `ths_industry_constituent_snapshot`
- Added executable `stock_basic.sync_ths_industry_constituents` pipeline with dry-run, legacy per-industry JSON import, and live explicit-code sync.
- Added and registered `jobs/sync_ths_industry_constituents.py`, job config, console script, source/storage/pipeline docs, and data contract notes.
- Updated legacy migration inventory status from dry-run foundation to partial production.
- Encountered missing planning `session-catchup.py` under `.claude`; restored context from project planning files instead.

## 2026-06-28 THS Industry Constituents Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_market_parsers.py tests/unit/test_domain/test_ths_industry_constituents.py tests/unit/test_pipelines/test_ths_industry_constituents_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 12 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_ths_industry_constituents --dry-run --snapshot-date 2026-06-26 --industry-codes 881103`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 99 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 EastMoney Market Breadth Migration Pass

- Reviewed legacy `backend/adapters/market/eastmoney.py` and current `sources/eastmoney`, `hooks/sources/eastmoney`, and market-overview pipeline/domain code.
- Added `EastMoneyClient.fetch_clist_page` and `fetch_market_breadth` for `push2.eastmoney.com/api/qt/clist/get`.
- Added pure source summarization for total amount, total volume, rising/falling/flat counts, limit-up/down counts, and stock count.
- Wired `build_market_overview source=eastmoney` to auto-fetch live breadth fields when no explicit fields are supplied.
- Updated market-overview source overwrite rules so EastMoney can refresh market breadth/amount fields.
- Updated job config, EastMoney source docs, market-overview design/data contract, and legacy inventory/file inventory.

## 2026-06-28 EastMoney Market Breadth Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_eastmoney_market_overview.py tests/unit/test_pipelines/test_market_overview_pipeline.py tests/unit/test_domain/test_market_overview.py -q`: passed, 8 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_market_overview --dry-run --source eastmoney --trade-date 2026-06-26`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 102 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 EastMoney Stock Meta Migration Pass

- Extended `EastMoneyClient` with `fetch_stock_meta_raw`, `fetch_stock_meta`, `parse_stock_meta`, and secid candidate generation.
- Extended pure stock metadata normalization with nullable industry, market-cap, cap-style, and optional sector-index fields.
- Added additive `sec_stock_basic` DDL columns and updated `StockBasicRepository.upsert_many` to preserve existing non-null enrichment fields when new provider rows omit them.
- Upgraded `sync_stock_basic` so `providers=eastmoney` enriches explicit `codes` or existing active codes from `sec_stock_basic`.
- Updated stock-basic job config, source docs, pipeline design, data contract, and EastMoney inventory status.

## 2026-06-28 EastMoney Stock Meta Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_eastmoney_market_overview.py tests/unit/test_domain/test_stock_meta.py tests/unit/test_pipelines/test_stock_basic_pipeline.py -q`: passed, 9 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_stock_basic --dry-run --providers eastmoney --codes 600000`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 106 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.
