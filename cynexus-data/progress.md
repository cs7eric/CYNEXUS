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

## 2026-06-28 Tencent Realtime Snapshot Migration Pass

- Reviewed legacy `backend/adapters/market/tencent.py` and current Tencent source/client/parser/provider-fallback implementation.
- Confirmed Tencent daily/minute K-line fallback and stock fund-flow snapshot paths are already production-wired.
- Added `parse_qt_realtime_snapshot_text` and `summarize_qt_realtime_snapshots` for `qt.gtimg.cn` quote rows.
- Added `TencentClient.fetch_realtime_snapshots_batch` and `fetch_market_overview`.
- Wired `build_market_overview source=tencent` to fetch live breadth from explicit `codes` or active `sec_stock_basic` codes.
- Updated market-overview overwrite rules, job config, source docs, pipeline/data contract docs, and Tencent inventory status.

## 2026-06-28 Tencent Realtime Snapshot Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_tencent_fund_flow.py tests/unit/test_pipelines/test_market_overview_pipeline.py tests/unit/test_domain/test_market_overview.py -q`: passed, 9 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_market_overview --dry-run --source tencent --trade-date 2026-06-26 --codes 600000,000001`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 108 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Sina Minute Kline Hardening Pass

- Reviewed legacy `backend/adapters/market/sina.py` and current Sina source/provider-fallback implementation.
- Confirmed Sina minute K-line is already production-wired through `fetch_minute_kline_with_fallback`.
- Hardened `parse_sina_minute_rows` to skip malformed rows, accept second/minute timestamps, and emit `volume_ratio` parity metadata.
- Updated Sina source docs and legacy inventory/file inventory status.

## 2026-06-28 Sina Minute Kline Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_market_parsers.py tests/unit/test_sources/test_provider_fallback.py tests/unit/test_pipelines/test_kline_sync_minute.py -q`: passed, 9 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 108 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Mootdx Optional Fallback Migration Pass

- Reviewed legacy `backend/adapters/market/mootdx_adapter.py` and current provider fallback implementation.
- Added `sources/tdx/mootdx.py` with optional dependency guard, frequency mapping, daily/minute K-line normalization, volume-ratio metadata, and adjusted-minute rejection.
- Wired explicit `providers=mootdx` into daily and minute provider fallback without changing default provider ordering.
- Updated TDX source docs and legacy inventory/file inventory status.

## 2026-06-28 Mootdx Optional Fallback Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_tdx_mootdx.py tests/unit/test_sources/test_provider_fallback.py -q`: passed, 6 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 112 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 eltdx Inventory Reconciliation

- Reconciled top-level `backend/adapters/market/eltdx_adapter.py` inventory with already migrated production paths.
- Marked eltdx as `partial_production` because adjusted daily K-line, market overview breadth, stock topics, and minute/history K-line support already exist in focused modules.
- Recorded remaining eltdx-specific gap as auction/timeshare detail capture.

## 2026-06-28 eltdx Intraday Detail Migration Pass

- Reviewed legacy `backend/adapters/market/eltdx_adapter.py` auction and history-timeshare functions plus old `auction_service.py` / `kline_service.py` boundaries.
- Added `sources/tdx/intraday.py` with optional eltdx client creation, auction point normalization, historical timeshare normalization, response row extraction, timestamp tolerance, and volume-ratio metadata.
- Added `TdxHook.get_intraday_detail_client`.
- Added ClickHouse DDL for `ch_ods_stock_auction_point` and `ch_ods_stock_timeshare_1m`.
- Added `ClickHouseIntradayDetailSink` with JSON serialization for provider raw payload lineage.
- Added executable `pipelines/kline/sync_intraday_detail.py` and registered `jobs/sync_tdx_intraday_detail.py`.
- Updated job config, console script, TDX source docs, K-line pipeline/data contracts, table design docs, and legacy inventory/file inventory.
- Kept old auction AI analysis, UI phase summary/cache assembly, and JSON runtime cache out of scope.

## 2026-06-28 eltdx Intraday Detail Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_tdx_intraday.py tests/unit/test_pipelines/test_tdx_intraday_detail_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 9 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_tdx_intraday_detail --dry-run --trade-date 2026-06-26 --codes 600000 --datasets auction,timeshare`: passed through `JobRunner`.

## 2026-06-28 Market Overview Technical Helper Migration Pass

- Reviewed legacy `backend/services/stock/market_overview/windows.py`, `support_resistance.py`, and `regime.py`.
- Added `domain/market/market_technical.py` with pure moving average, window metric, volatility, regime bucket, and support/resistance level helpers.
- Kept these helpers free of storage, source, hook, env, filesystem, and network imports.
- Updated domain README, market-overview pipeline/data-contract docs, top-level inventory, and per-file inventory.
- Left UI summary assembly and persisted detail pipeline staged until Java/UI needs a stable serving contract.

## 2026-06-28 Market Overview Technical Helper Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_market_technical.py -q`: passed, 4 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 123 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are existing lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Market Overview Detail Helper Migration Pass

- Reviewed legacy `market_overview/style_rotation.py`, `industry_strength.py`, `sentiment.py`, and `similar_scenarios.py`.
- Extended `domain/market/market_technical.py` with pure helpers for:
  - style relative-return rows and dominant style selection;
  - industry/index relative strength;
  - breadth sentiment component scoring and rolling breadth state;
  - similar-scenario matching and forward-return summaries.
- Kept legacy provider calls, JSON cache reads, industry-index resolution, and detail persistence out of domain.
- Updated market-overview docs and legacy inventory/file inventory.

## 2026-06-28 Market Overview Detail Helper Validation

- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_market_technical.py -q`: passed, 8 tests.
- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 127 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are existing lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 MSI Percentile Repository Migration Pass

- Reviewed legacy `backend/repositories/market/percentile_helper.py`.
- Added `storage/postgres/percentile.py` with allowlisted table/column resolution, `percentile_score`, and `enrich_history_scores`.
- Wired non-dry-run `build_turnover_activity`, `build_risk_appetite`, and `build_style_risk` to use PostgreSQL historical percentiles when no explicit `score` is supplied.
- Kept dry-run behavior storage-free and preserved neutral fallback when historical rows or storage access are unavailable.
- Updated MSI docs, findings, task plan, and legacy inventory.

## 2026-06-28 MSI Percentile Repository Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_storage/test_postgres_percentile.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py tests/unit/test_domain/test_msi_factors.py -q`: passed, 12 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 131 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are existing lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Market Overview Detail Snapshot Migration Pass

- Added `read_daily_bars_by_code` ClickHouse reader for compact per-code daily OHLCV windows.
- Extended market overview PostgreSQL DDL with `market_overview_detail_snapshot`.
- Extended `MarketOverviewRepository` with `list_snapshots`, `get_detail`, and `upsert_detail`.
- Added executable `pipelines/market_overview/build_market_overview_detail.py`:
  - dry-run uses supplied rows or deterministic synthetic rows;
  - non-dry-run reads benchmark/style index bars from ClickHouse;
  - breadth sentiment comes from supplied rows or `market_overview_snapshot`;
  - final UI-ready detail payload is persisted as JSONB in PostgreSQL.
- Added and registered `jobs/build_market_overview_detail.py`, job config, console script, docs, tests, and inventory updates.

## 2026-06-28 Market Overview Detail Snapshot Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_pipelines/test_market_overview_pipeline.py tests/unit/test_storage/test_postgres_market_overview.py tests/unit/test_jobs_registry.py -q`: passed, 11 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_market_overview_detail --dry-run --trade-date 2026-06-26`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 135 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are existing lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Kline Reader Compatibility Migration Pass

- Reviewed legacy `backend/repositories/market/kline_repo.py`.
- Added `read_daily_kline_bars` to `storage/clickhouse/readers.py` with legacy-compatible payload fields: Beijing 09:30 epoch-ms `timestamp`, `trade_date`, OHLCV, amount, and adjusted `adj_factor`.
- Added lightweight helpers:
  - `get_latest_daily_trade_date`
  - `has_adjusted_daily`
  - `list_codes_with_adjusted_daily`
- Added unit tests with fake ClickHouse clients for SQL shape, latest-N ordering, adjusted/raw payloads, and helper queries.
- Updated K-line data contract/pipeline docs and inventory rows.

## 2026-06-28 Kline Reader Compatibility Validation

- `$env:PYTHONPATH='src'; pytest tests/unit/test_storage/test_clickhouse_readers.py -q`: passed, 4 tests.
- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 139 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are existing lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Index Returns Migration Pass

- Reviewed legacy `backend/repositories/market/index_repo.py`.
- Added pure `domain/market/index_returns.py` for default broad-index targets, daily return rows, and cumulative window return payloads.
- Added ClickHouse readers for grouped `index_daily_raw` reads:
  - `read_index_daily_bars`
  - `latest_index_trade_date`
  - `has_index_kline`
- Added PostgreSQL DDL/repository for `mkt_index_return_daily`.
- Added executable `pipelines/market/build_index_returns.py` and `jobs/build_index_returns.py`.
- Registered job in registry/config/console scripts.
- Added docs under `docs/data_contracts/index_returns.md` and `docs/pipeline_design/index_returns.md`.
- Updated task plan, findings, jobs/storage/domain docs, and legacy inventory.

## 2026-06-28 Index Returns Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_index_returns.py tests/unit/test_storage/test_clickhouse_readers.py tests/unit/test_storage/test_postgres_index_returns.py tests/unit/test_pipelines/test_index_returns_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 15 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_index_returns --dry-run --trade-date 2026-06-26 --window-days 5`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 148 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are existing lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Index Code Catalog Migration Pass

- Reviewed legacy `backend/services/stock/f10/index_codes.py`.
- Added `domain/market/index_codes.py` with:
  - TDX `sh8803XX` industry index catalog;
  - TDX `sh8804XX` concept index catalog;
  - pure normalization, kind inference, all-code listing, and lookup helpers;
  - caller-supplied `extra_items` support instead of domain-level filesystem override reads.
- Added unit tests and data-contract docs.
- Updated domain README, findings, task plan, and legacy file inventory.

## 2026-06-28 Index Code Catalog Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_index_codes.py tests/unit/test_domain/test_index_returns.py -q`: passed, 6 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 151 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are existing lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 TDX Industry Quote Snapshot Migration Pass

- Reviewed legacy `backend/services/stock/f10/tdx_industry_codes.py`, `tdx_industry_seeds.py`, and `tdx_industry_service.py`.
- Extended `domain/market/index_codes.py` with TDX 56 industry catalog and representative seed stocks.
- Added `sources/tdx/industry_quotes.py` for eltdx full-code resolution, quote normalization, and heatmap-compatible payload assembly.
- Added PostgreSQL DDL/repository for:
  - `tdx_industry_quote_batch`
  - `tdx_industry_quote_snapshot`
- Added executable `pipelines/market/sync_tdx_industry_quotes.py` and `jobs/sync_tdx_industry_quotes.py`.
- Registered job in registry/config/console scripts.
- Added docs under `docs/data_contracts/tdx_industry_quotes.md` and `docs/pipeline_design/tdx_industry_quotes.md`.
- Updated TDX source docs, PG/table docs, findings, task plan, and legacy inventory.

## 2026-06-28 TDX Industry Quote Snapshot Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_index_codes.py tests/unit/test_sources/test_tdx_industry_quotes.py tests/unit/test_storage/test_postgres_tdx_industry_quotes.py tests/unit/test_pipelines/test_tdx_industry_quotes_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 13 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_tdx_industry_quotes --dry-run --snapshot-date 2026-06-26`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 159 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are existing lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Market Heatmap Snapshot Migration Pass

- Reviewed legacy `backend/services/stock/market_heatmap_service.py` and `sector_quote_service.py` boundaries.
- Added pure `domain/market/market_heatmap.py` to normalize persisted industry quote rows into a legacy-compatible heatmap payload.
- Added PostgreSQL DDL/repository for `market_heatmap_snapshot`.
- Added executable `pipelines/market/build_market_heatmap.py` and `jobs/build_market_heatmap.py`.
- Registered the job in registry/config/console scripts.
- Added docs under `docs/data_contracts/market_heatmap.md` and `docs/pipeline_design/market_heatmap.md`.
- Kept old Flask response wiring, local JSON runtime caches, stock-universe hotpath cache, and theme-market UI service out of scope.

## 2026-06-28 Market Heatmap Snapshot Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_market_heatmap.py tests/unit/test_storage/test_postgres_market_heatmap.py tests/unit/test_pipelines/test_market_heatmap_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 7 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_market_heatmap --dry-run --snapshot-date 2026-06-26 --top-n 1`: passed through `JobRunner`.

## 2026-06-28 TDX Sector Quote Detail Migration Pass

- Reviewed legacy `backend/services/stock/sector_quote_service.py`.
- Added `sources/tdx/sector_quote.py` for optional eltdx `f10.theme_market` ReqId parsing:
  - `200741` period stats;
  - `200742` main capital flow;
  - `200743` related sectors;
  - `200744` constituents;
  - `200745` main control.
- Added pure `domain/market/sector_quote.py` for JSONB payload wrapping and section-count metadata.
- Added PostgreSQL DDL/repository for `tdx_sector_quote_detail_snapshot`.
- Added executable `pipelines/market/sync_tdx_sector_quote_details.py` and `jobs/sync_tdx_sector_quote_details.py`.
- Registered job in registry/config/console scripts.
- Added docs under `docs/data_contracts/sector_quote.md` and `docs/pipeline_design/tdx_sector_quote_details.md`.
- Kept old Flask API, stock-universe JSON seed lookup, and Tencent constituent fallback out of scope.

## 2026-06-28 TDX Sector Quote Detail Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_sources/test_tdx_sector_quote.py tests/unit/test_domain/test_sector_quote.py tests/unit/test_storage/test_postgres_sector_quote.py tests/unit/test_pipelines/test_tdx_sector_quote_details_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 10 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_tdx_sector_quote_details --dry-run --snapshot-date 2026-06-26 --codes 880471 --datasets period_stats,constituents`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 172 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are existing lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Stock Turnover Series Migration Pass

- Reviewed legacy `backend/services/stock/f10/turnover.py`, `turnover_repo.py`, and eltdx turnover formula.
- Added pure `domain/stock/turnover.py` for per-stock turnover series calculation.
- Added `sources/tdx/stock_profile.py` for optional eltdx circulating/total share lookup.
- Added PostgreSQL DDL/repository for `stock_turnover_series_snapshot`.
- Added executable `pipelines/stock_basic/sync_stock_turnover.py` and `jobs/sync_stock_turnover.py`.
- Registered job in registry/config/console scripts.
- Added docs under `docs/data_contracts/stock_turnover.md` and `docs/pipeline_design/stock_turnover.md`.
- Replaced old filesystem JSON turnover cache with PostgreSQL JSONB serving snapshots; K-line facts stay in ClickHouse.

## 2026-06-28 Stock Turnover Series Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_stock_turnover.py tests/unit/test_sources/test_tdx_stock_profile.py tests/unit/test_storage/test_postgres_stock_turnover.py tests/unit/test_pipelines/test_stock_turnover_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 8 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.sync_stock_turnover --dry-run --codes 600000 --limit-codes 1`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 178 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Scope Reconciliation Pass

- Reviewed legacy `backend/services/stock/_pg_writer.py`; confirmed market overview PG writes are now direct in `MarketOverviewRepository.upsert_fields` and `build_market_overview`.
- Reviewed `config_service.py`, `search_service.py`, `self_selected_service.py`, and `workspace_service.py`.
- Marked UI/config/user-state files as out of Python executor scope in `legacy_file_inventory.md`.
- Kept `_limit_pg_writer.py` staged because existing MSI limit emotion covers summary scoring, but old stock-detail PG snapshot parity still needs a deliberate decision.
- Validation after reconciliation: `python -m compileall -q src` passed; `$env:PYTHONPATH='src'; pytest tests/unit -q` passed, 178 tests.

## 2026-06-28 Market Limit Stock Snapshot Migration Pass

- Reviewed legacy `backend/services/stock/_limit_pg_writer.py` and `backend/repositories/market/market_limit_pg_repo.py`.
- Added pure `extract_limit_stock_snapshots` in `domain/market/msi_factors.py` for limit-up, limit-down, and broken-board stock rows.
- Added PostgreSQL DDL/repository for `market_limit_stock_snapshot`.
- Enhanced `pipelines/msi/build_limit_emotion.py` so non-dry-run saves both `msi_limit_emotion_daily` and compact stock-level serving rows.
- Added docs under `docs/data_contracts/market_limit.md` and `docs/pipeline_design/market_limit.md`.
- Updated storage/domain READMEs and reconciled legacy inventory rows for `_limit_pg_writer.py` and `market_limit_pg_repo.py`.

## 2026-06-28 Market Limit Stock Snapshot Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_msi_factors.py tests/unit/test_storage/test_postgres_market_limit.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py -q`: passed, 10 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 180 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_limit_emotion --dry-run --trade-date 2026-06-26 --rows ...`: passed through `JobRunner` and returned stock-level `stock_rows`.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.
- Remaining `planned_after_skeleton` rows: 64.

## 2026-06-28 UI/AI Reader Scope Reconciliation Pass

- Reviewed legacy `backend/services/stock/analysis_data_reader.py`; marked out of scope because it is an AI feature-summary reader over old services/cache paths, not a data-production job.
- Reviewed legacy `backend/services/stock/sample_data_service.py`; marked out of scope because it only creates synthetic UI fallback K-line samples.
- `python -m compileall -q src`: passed after reconciliation.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_msi_factors.py tests/unit/test_storage/test_postgres_market_limit.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py -q`: passed, 10 tests.
- Remaining `planned_after_skeleton` rows: 62.

## 2026-06-28 Backlog Inventory Correction Pass

- Recounted all remaining `planned_after_skeleton` rows after user questioned the inflated 60+ count.
- Confirmed the count was stale inventory, not 60+ missing production modules:
  - package/export rows;
  - old `scripts/backfill_*.py` wrappers now covered by jobs;
  - market repositories already covered by MSI/market-overview/market-pulse/fund-flow/kline jobs.
- Updated repository, service, and script rows to `partial_production`, `reviewed`, or `out_of_scope` with concrete new paths.
- Remaining `planned_after_skeleton` after correction: 3 (`etf_poc_service.py`, `stock_universe_service.py`, `style_sector_service.py`).

## 2026-06-28 Style Sector Snapshot Migration Pass

- Reviewed legacy `backend/services/stock/style_sector_service.py`.
- Added pure `domain/market/style_sector.py` for 29 style-sector allowlist and equal-weight change calculation.
- Extended `StockSectorRepository` with `list_sectors_by_category()` so pipelines can read category `4` style constituents from PostgreSQL instead of old JSON caches.
- Added PostgreSQL DDL/repository for `market_style_sector_snapshot`.
- Added executable `pipelines/market/build_style_sector.py` and `jobs/build_style_sector.py`.
- Registered `build_style_sector` in registry, jobs config, and console scripts.
- Added docs under `docs/data_contracts/style_sector.md` and `docs/pipeline_design/style_sector.md`.
- Reconciled `stock_universe_service.py` as covered by stock-basic/stock-sector PG jobs and `etf_poc_service.py` as out of current stock-data scope.

## 2026-06-28 Style Sector Snapshot Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_domain/test_style_sector.py tests/unit/test_storage/test_postgres_style_sector.py tests/unit/test_pipelines/test_style_sector_pipeline.py tests/unit/test_jobs_registry.py -q`: passed, 6 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_style_sector --dry-run --trade-date 2026-06-26 --sectors ... --quotes-by-code ...`: passed through `JobRunner`.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 184 tests.
- Boundary audits:
  - No `import backend` / `from backend` / `C020611` matches in `src` or `tests`.
  - Domain/core scan has no source/storage/hook/env/network imports; matches are lineage/source strings only.
  - Source scan has no storage writes or storage/hook imports added by this migration; the only runtime-context match is existing provider fallback infrastructure.

## 2026-06-28 Design Inventory Reconciliation Pass

- Added `docs/architecture/data_source_capability_matrix.md`.
- Marked legacy design rows for data-source matrix, stock data source, MSI pipeline, MSI factor contract, and transcription runtime flow as migrated to current docs.
- Remaining `planned_after_skeleton` rows: 0.
