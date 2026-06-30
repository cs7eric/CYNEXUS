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

## 2026-06-28 PostgreSQL Schema Alignment Pass

- Updated Python/PostgreSQL connection design for split schemas:
  - `postgres_market` defaults to `cynexus_appl_market`.
  - `postgres_core` defaults to `cynexus_appl_core`.
  - runtime job state uses core; market/serving repositories use market.
- Added Python-side bigint id generation and wired it into affected repositories.
- Aligned table targets with the user-approved mapping:
  - reused existing `market_overview_snapshots`, `market_pulse_capture_batches`, `market_pulse_sector_daily_snapshots`, `sector_fund_flow_capture_batches`, `sector_fund_flow_daily_snapshots`;
  - changed new `market_*` serving tables to `mkt_heatmap_snapshot`, `mkt_overview_detail_snapshot`, and `mkt_style_sector_snapshot`;
  - changed limit stock detail persistence from the parallel `market_limit_stock_snapshot` proposal to existing `market_limit_daily_stocks`.
- Added `sh_amount`, `sz_amount`, and `sample_count` to `msi_turnover_activity_daily` DDL and verified the calculation/ClickHouse reader/repository path writes them.
- Converted new PostgreSQL tables to explicit `id bigint PRIMARY KEY` plus natural unique indexes where needed.
- Applied all 16 `sql/postgres/*.sql` files successfully to local PG at `127.0.0.1:25433/cynexus`.
- Live PG verification confirmed target market/core tables exist and `msi_turnover_activity_daily` has `sh_amount`, `sz_amount`, and `sample_count`.

## 2026-06-28 PostgreSQL Schema Alignment Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest tests/unit/test_storage -q`: passed, 23 tests.
- `$env:PYTHONPATH='src'; pytest tests/unit -q`: passed, 184 tests.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.health_check`: passed.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_turnover_activity --dry-run ...`: passed and returned `shAmount`, `szAmount`, `sampleCount`.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_limit_emotion --dry-run ...`: passed and produced stock rows targeting `market_limit_daily_stocks` persistence in non-dry-run mode.
- `$env:PYTHONPATH='src'; python -m cynexus_data.jobs.build_market_heatmap --dry-run ...`: passed.
- Boundary scans found no remaining `cynexus_data.<table>` schema docs, no old `market_limit_stock_snapshot`, and no stale singular `sector_fund_flow_*` / `market_pulse_*` table references in active docs/code/tests/sql.

## 2026-06-29 ClickHouse Daily Writer Schema Compatibility Pass

- Investigated `ProgrammingError: Unrecognized column 'market' in table daily_raw`.
- Confirmed the live ClickHouse daily tables are narrower than the newer DDL and already contain large migrated datasets, so rebuilding/dropping them is not an acceptable fix.
- Updated `ClickHouseDailyKlineSink` and `ClickHouseAdjustedDailyKlineSink` to describe the target table once, project inserts to real table columns, and validate required daily fact columns.
- Preserved wide-table support: when a future table has metadata columns such as `market`, `symbol`, `tdx_code`, `source_file`, or `adjust`, the writer keeps those columns if the source frame provides them.

## 2026-06-29 ClickHouse Daily Writer Schema Compatibility Validation

- `python -m compileall -q src`: passed.
- `$env:PYTHONPATH='src'; pytest -q tests/unit/test_storage/test_clickhouse_writers.py tests/unit/test_pipelines/test_kline_sync_daily.py tests/unit/test_pipelines/test_kline_sync_adjusted_daily.py`: passed, 10 tests.

## 2026-06-29 Scheduler Chain Timeout / Progress Bridge Fix

- Investigated `Data job executor request failed: request timed out` on `market_sentiment_chain_refresh`.
- Root cause: Java called the long-running Python chain synchronously, while UI progress reads `cynexus_appl_core.scheduler_job_statuses`; Python progress was only in `task_job_run.metadata`.
- Updated Python trigger API / `JobRunner` so `market_sentiment_chain_refresh` creates a running `task_job_run` immediately and executes the chain in a FastAPI background task.
- Updated Java scheduler management path so the trigger returns `processing` immediately, then polls `/internal/job-runs/{run_id}` and writes chain progress into `scheduler_job_statuses.progress`.
- Hardened Java polling condition: any non-terminal Python run with a `run_id` is polled, preventing `running` from being recorded as a failed final result.
- Added default chain `target_date` resolution from ClickHouse `index_daily_raw` latest `sh000001` trade date, so non-trading-day/manual triggers use the latest available trading day (`2026-06-26` in local verification).
- Validation:
  - `python -m compileall -q src`: passed.
  - `mvn -pl cynexus-core/cynexus-core-starter -am -DskipTests package`: passed after stopping the local core process that held the jar.
  - Dry-run trigger via Java returned in 0.08s and completed 14/14 steps with `scheduler_job_statuses.progress.percent=100`.
  - Real trigger via Java returned in 0.11s and is running in background with `is_running=true`, `currentStep=initial_backfill_refresh`, and `percent=7.14`.

## 2026-06-29 MSI/QFQ Freshness and Idempotency Retrospective

- Investigated user-reported MSI data freshness and QFQ parity problems:
  - MSI default target date had been inferred from `index_daily_raw.sh000001`, which lagged at `2026-06-26`.
  - `style_risk` used `daily_qfq.000300/000852`; `daily_qfq.000300` lagged at `2026-06-15`.
  - `risk_appetite` still used `daily_qfq.000300/511010/511090`; all three lagged at `2026-06-15`.
  - `daily_raw` had same-code same-date duplicates and bad scaled rows such as `000852` close `0.501/0.504` beside normal `859-860` values.
- Root causes captured:
  - Migration compared function names but missed old-code behavior contracts, especially DELETE-before-INSERT and single-day QFQ anchor-date fetch.
  - Code DDL was trusted without verifying live ClickHouse physical DDL; live `daily_raw/daily_qfq/daily_hfq` are still `MergeTree`, not `ReplacingMergeTree`.
  - Factor producers lacked a freshness matrix per input table/code, allowing stale qfq input to be written as current-date factor output.
  - Reader-side grouping was used as emergency protection before the write path was made idempotent.
- Fixes implemented:
  - `market_sentiment_chain_refresh` now defaults target date from ClickHouse `daily_raw` latest date instead of `index_daily_raw`.
  - `build_style_risk` defaults to `daily_raw` for `000300/000852` and persists `source=clickhouse.daily_raw`.
  - `build_risk_appetite` now defaults to `daily_raw` for `000300/511010/511090` and persists `source=clickhouse.daily_raw`.
  - `read_close_series`, limit-emotion OHLC readers, and limit-emotion history readers group duplicate `code/trade_date` rows and use the high close as a defensive guard against low-scale dirty rows.
  - `build_limit_emotion` automatically resolves the previous trading date from the same ClickHouse source table when `prev_trade_date` is not passed.
  - `build_limit_emotion` filters OHLC rows to active `sec_stock_basic` codes when metadata exists, preventing non-stock instruments from entering limit/streak counts.
  - `sync_adjusted_kline_daily` keeps eltdx as the qfq/hfq source of truth, adds the old single-date `get_adjusted_kline(anchor_date, count=10)` fast path, and supports `workers` for parallel fetches.
  - `ClickHouseDailyKlineSink.insert_daily` and `ClickHouseAdjustedDailyKlineSink.replace_daily` now delete affected `code/date` ranges before insert, preserving old-code idempotent overwrite semantics even on legacy `MergeTree` tables.
- Local repair results for `2026-06-29`:
  - `sync_fund_flow`: wrote 90 THS industry fund-flow snapshot rows.
  - `build_sector_breadth`: wrote 90-row sector breadth, `score=45.56`.
  - `build_limit_emotion`: wrote active-stock limit emotion, `yesterday_limit_up_count=58`, `yesterday_limit_up_avg_return=1.2418`.
  - `sync_adjusted_kline_daily adjust=qfq workers=32`: wrote 5204 qfq rows for 2026-06-29.
  - `build_ma_count`: rewrote qfq-derived MA sample with `total_eligible=5204`.
  - `build_profit_effect`: rewrote `score=39.53`.
  - `build_risk_appetite`: rewrote from `daily_raw`, with `hs300_base_date=2026-06-01`, `spread_weighted=1.8909`, `source=clickhouse.daily_raw`.
  - `build_msi`: rewrote final MSI with `component_count=9`, `composite_score=44.71`, `level=weak`.
- Validation:
  - `python -m compileall -q src`: passed.
  - `$env:PYTHONPATH='E:\project-repo\cynexus\cynexus-data\src'; pytest -q tests/unit/test_storage/test_clickhouse_writers.py tests/unit/test_pipelines/test_kline_sync_daily.py tests/unit/test_pipelines/test_kline_sync_adjusted_daily.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py tests/unit/test_storage/test_clickhouse_readers.py`: passed, 23 tests.
- Follow-up risk:
  - Existing duplicate/dirty `daily_raw` rows remain in the historical fact table. Readers are now defensive and future writes are idempotent, but a separate cleanup/rebuild plan is still needed if downstream analytics must scan raw history without these guards.

## 2026-06-30 ClickHouse Fact Write Audit And Root-Cause Hardening

- Audited active Python write paths:
  - ClickHouse writers: `ClickHouseKlineSink`, `ClickHouseDailyKlineSink`, `ClickHouseAdjustedDailyKlineSink`, `ClickHouseIntradayDetailSink`, minute aggregation insert-select.
  - Legacy-style direct ingest CLIs: `ingest_tdx_1m_to_clickhouse.py`, `ingest_parquet_1m_to_clickhouse.py`.
  - PostgreSQL repositories: confirmed most serving/factor writes use natural-key `ON CONFLICT` or scoped soft-delete/replace patterns, so the turnover inflation class is concentrated in ClickHouse fact tables.
- Verified live ClickHouse state:
  - `daily_raw` has `10,756,728` duplicate `code + trade_date` keys.
  - `daily_qfq` currently has `0` duplicate keys.
  - `daily_hfq` has `193` duplicate keys.
  - `daily_raw.999999/399001` on 2026-06-29 each had two rows; summing both explains the old `70355` 亿 turnover, while max-per-index gives about `35177` 亿.
  - Recent-window scale scan showed 2026-06-29 duplicates with `max(close)/min(close) > 100`, for example `000852` had both `0.501` and `859.789`.
  - A full-table scale scan exceeded the local ClickHouse memory limit, so future audits should be date-windowed or use a staged materialized quality table.
- Reviewed legacy code before finalizing the fix:
  - `scripts/initial_backfill.py` used `ALTER TABLE daily_raw DELETE ... mutations_sync=1` before insert.
  - `scripts/tdx_parser.py` documented/detected TDX unit-scale anomalies.
  - `scripts/validate_daily_raw.py` had a post-backfill unit-scale anomaly check.
- Fixes implemented:
  - Daily raw/qfq/hfq writer now deduplicates input frames by `code + trade_date` before delete/insert, keeping the highest-close row for observed low-scale duplicate anomalies.
  - 1m writer now deduplicates input frames by `(market, symbol, period, trade_time, source)` and supports `replace_existing` deletion by symbol/date/source before insert.
  - TDX/parquet 1m ingest CLIs expose `--replace-existing`; `sync_kline_minute` exposes `replace_existing`.
  - Intraday auction/timeshare writer now deduplicates by natural key and deletes same symbol/date/source rows before insert.
  - Minute-period aggregation now deletes existing `source='aggregate_1m'` target rows for the requested period/date/symbol scope before inserting regenerated bars.
  - Daily ClickHouse readers now group duplicate daily rows before returning chart/MSI inputs, preventing historical dirty rows from leaking into calculations while cleanup is pending.
  - `quality_check` supports `daily_duplicate_keys` and `daily_scale_anomaly`.
- Validation:
  - `python -m compileall -q src`: passed.
  - Targeted regression passed: `31 passed`.
  - Full Python unit regression passed: `199 passed`.
  - Live `quality_check --dry-run --estimate --checks daily_duplicate_keys,daily_scale_anomaly --trade-date 2026-06-29` passed with `CLICKHOUSE_PASSWORD` set and reported `duplicate_keys=9460`, `anomaly_keys=81` for `daily_raw`.
- Error encountered:
  - First live `quality_check` attempt failed with ClickHouse authentication because the shell did not include `CLICKHOUSE_PASSWORD`; reran with the local ClickHouse password set successfully.
- Remaining data task:
  - Historical `daily_raw` and `daily_hfq` still need a cleanup/rebuild plan. The code now prevents the same class of future writes and protects current readers, but it has not physically rewritten the existing large ClickHouse fact tables.

## 2026-06-30 Market Pulse Serving-Date Repair

- Investigated Market Pulse mixed/stale data after the Python-to-Java API migration:
  - PG serving tables were split by freshness: overview, heatmap, style-sector, and limit snapshots had `2026-06-29`, while `mkt_index_return_daily` still stopped at `2026-06-26`.
  - ClickHouse `ch_dwd_kline_1m` had only `2026-06-26` rows for the three index symbols, so the Java API correctly fell back to old minute bars even when overview displayed `2026-06-29`.
  - `daily_raw` held newer broad-index daily rows, but index codes were stored as TDX/raw codes (`999999`, `000300`, `000852`) rather than Java-facing `sh000001`, `sh000300`, `sh000852`.
- Fixes implemented:
  - `read_index_daily_bars` now maps Java-facing broad-index codes onto the available `daily_raw` storage codes and groups duplicate same-date rows by max close.
  - `build_index_returns --source-table daily_raw --trade-date 2026-06-29` rewrote Java-facing index returns for `sh000001`, `sh000300`, and `sh000852`.
  - Tencent source support was extended with `appstock/app/minute/query`, parsing index minute timeshare points into derived 1m OHLC bars.
  - `sync_kline_minute --codes sh000001,sz399001,sz399006 --providers tencent --replace-existing` wrote 726 index 1m rows for `2026-06-29`.
  - TDX local `.day` parser price restoration was corrected from `/1000` to `/100`, matching the live TDX day file format and preventing future low-scale raw-price writes.
  - Limit emotion now reads `daily_raw` with a temporary 10x price-scale guard for existing low-scale rows, while avoiding `daily_raw.amount` because one old ClickHouse part currently raises a checksum mismatch on that column.
- Local repair results:
  - `market_overview`: API returns `tradingDate=2026-06-29`, `totalAmount=35171.36`, `rising=2384`, `falling=2697`, `limitUp=101`, `limitDown=54`.
  - `index-kline/batch`: API returns `date=2026-06-29` and 242 points per index for `000001`, `399001`, `399006`.
  - `limit-emotion`: API returns `tradeDate=2026-06-29`, `limitUp=101`, `limitDown=54`, `touched=177`, `broken=76`, `maxHeight=2`, `promotion=8.77`, and stock prices in normal scale, e.g. `000777 latestPrice=18.48`.
  - Frontend Vite proxy (`localhost:5173/api/market/pulse/...`) returns the same repaired Java payloads.
- Validation:
  - `python -m compileall -q src`: passed.
  - Targeted regression passed: `25 passed`.
- Remaining data task:
  - ClickHouse reported `CHECKSUM_DOESNT_MATCH` while reading `/var/lib/clickhouse/.../daily_raw/.../amount.bin` from an old part. Current Market Pulse readers avoid that damaged column where possible, but the damaged part should be cleaned by a dedicated rebuild/partition repair plan rather than hidden inside UI logic.

## 2026-06-30 ClickHouse daily_raw Cleanup/Rebuild

- Started dedicated cleanup/rebuild after user approved "清理重建".
- Re-read migration plan/findings/progress before destructive maintenance.
- Verified live ClickHouse `daily_raw` DDL is the narrow legacy `MergeTree` shape with `PARTITION BY toYear(trade_date)` and `ORDER BY (code, trade_date)`.
- Located the corrupted physical part reported by ClickHouse:
  - table: `cynexus.daily_raw`
  - part: `2007_786_54179_239`
  - partition: `2007`
  - active: `1`
  - rows: `249114`
  - path: `/var/lib/clickhouse/store/631/6319661d-8a9d-4c19-9c7e-e5576b33f64b/2007_786_54179_239/`
- Active partition metadata shows `daily_raw` still contains `1990-2009` data. This conflicts with the later rule that data before 2010 should not be imported, so the cleanup plan should rebuild/keep only `trade_date >= 2010-01-01`.
- Added rebuild-safe daily sync parameters:
  - `target_table` lets `sync_kline_daily` write a staging table such as `daily_raw_rebuild`.
  - `replace_existing=false` disables per-file delete mutations when inserting into a known-empty rebuild table.
  - Normal daily sync keeps the previous default `replace_existing=true`.
- Validation after the parameter change:
  - `python -m compileall -q src`: passed.
  - `$env:PYTHONPATH='src'; pytest -q tests/unit/test_pipelines/test_kline_sync_daily.py tests/unit/test_storage/test_clickhouse_writers.py`: passed, 9 tests.
- Created staging table `daily_raw_rebuild` with the live narrow `daily_raw` schema.
- Dry-run over `E:\project-repo\cynexus\cynexus-data\var\tdx\day\hsjday` discovered `12243` TDX `.day` files for markets `sh,sz,bj` with `start_date=2010-01-01`.
- First background import launch failed because PowerShell expanded `$env:` variables before passing the command to the child process; no rows were written. Restarted with escaped `$env:` assignments.
- Started local Python import into `daily_raw_rebuild` with `replace_existing=false`; initial monitor showed `897491` rows, `700` codes, date range `2010-01-04` to `2026-06-29`.
- Stopped the first staging import after `total_parts` grew to `17506`; root cause was one insert per `.day` file. Dropped the staging table instead of trying to repair that part layout.
- Added `flush_rows` batching to `sync_kline_daily`; `flush_rows=1000000` batches many TDX files into large inserts and avoids small-part explosion.
- Validation after batching change:
  - `python -m compileall -q src`: passed.
  - `$env:PYTHONPATH='src'; pytest -q tests/unit/test_pipelines/test_kline_sync_daily.py tests/unit/test_storage/test_clickhouse_writers.py`: passed, 10 tests.
- Rebuilt narrow-schema `daily_raw_rebuild` with `flush_rows=1000000`; final staging state was `23719531` rows, `11664` codes, date range `2010-01-04` to `2026-06-29`, and `86` active parts.
- Rejected the narrow-schema staging table because it still had `486291` duplicate `code + trade_date` keys. The duplicates are not only dirty data: the narrow schema drops market identity, so valid `SH` and `SZ` instruments can collide on the same six-digit code, for example `SH 000852` index versus `SZ 000852` stock.
- Changed daily raw writer deduplication to use `market + symbol + trade_date` when the source frame carries market identity; legacy/narrow adjusted tables still fall back to `code + trade_date`.
- Updated daily raw read paths for the rebuilt wide table:
  - turnover activity uses `tdx_code='sh999999'` and `tdx_code='sz399001'`;
  - raw index close-series reads resolve broad-index codes such as `000300` and `000852` to explicit `tdx_code`;
  - limit-emotion OHLC/history reads partition by `tdx_code`, return `market/tdxCode`, filter against stock metadata market, and no longer apply the old temporary `daily_raw * 10` price scale workaround.
  - quality checks use `market + symbol + trade_date` for `daily_raw` and `daily_raw_rebuild`.
- Rebuilt wide-schema `daily_raw_rebuild` from `E:\project-repo\cynexus\cynexus-data\var\tdx\day\hsjday` with:
  - `start_date=2010-01-01`
  - `target_table=daily_raw_rebuild`
  - `replace_existing=false`
  - `flush_rows=1000000`
  - `batch_size=1000000`
- Wide staging validation passed:
  - `23719531` rows.
  - `11856` instruments by `tdx_code`.
  - date range `2010-01-04` to `2026-06-29`.
  - `84` active parts.
  - duplicate `(market, symbol, trade_date)` keys: `0`.
  - pre-2010 rows: `0`.
  - scaled duplicate anomaly keys: `0`.
  - `2026-06-29` `sh999999 + sz399001` amount: `35177.53851904` 亿.
- Swapped tables with ClickHouse `RENAME TABLE`: old `daily_raw` -> `daily_raw_dirty_backup_20260630`, `daily_raw_rebuild` -> `daily_raw`.
- Verified new production `daily_raw` DDL is the wide `ReplacingMergeTree(ingested_at)` schema ordered by `(market, symbol, trade_date, source)`.
- Deleted `daily_raw_dirty_backup_20260630` after validation to remove the corrupt/dirty old data and reclaim ClickHouse storage.
- Final live state:
  - only `daily_raw` remains among `daily_raw%` tables.
  - active rows: `23719531`.
  - active size: `797.85 MiB`.
  - inactive `daily_raw%` parts: none reported.
  - duplicate `(market, symbol, trade_date)` keys: `0`.
  - `SH 000852` and `SZ 000852` both exist independently on `2026-06-29`.
- Validation:
  - `python -m compileall -q src`: passed.
  - Targeted regression: `30 passed`.
  - Full unit regression: `$env:PYTHONPATH='src'; pytest -q tests/unit`: passed, `202 passed`.

## 2026-06-30 Daily Raw Source Sampling Validation

- Added `quality_check` support for `checks=daily_source_sample`.
- The check samples ClickHouse daily rows using `sample_every` (default `100`), calls daily source providers with `adjust=none`, and compares OHLC fields using `price_tolerance_pct` and `price_tolerance_abs`.
- Added sampling filters:
  - `sample_limit` for smoke runs.
  - `sample_markets` for market-scoped validation.
  - `sample_scope=stock` for A-share stock-only validation, excluding bonds/funds/ETFs/indices from stock-quality checks.
- Added provider retry behavior for date-aware validation: if one provider returns rows but not the requested trade date, the check tries the next provider before reporting a miss.
- Live smoke findings:
  - `sample_scope=all` over SH/SZ exposed non-stock unit-scale mismatches, for example convertible bonds and funds where TDX local `.day` price units differ from ordinary stock units.
  - BJ samples did not validate through the current Tencent/EastMoney fallback for the target date, so BJ needs a separate provider/scale pass before all-market validation is meaningful.
- Live stock validation result:
  - Command: `quality_check --dry-run --estimate --checks daily_source_sample --daily-table daily_raw --trade-date 2026-06-29 --sample-every 100 --sample-markets sh,sz --sample-scope stock --source-providers tencent,eastmoney --source-limit 20`.
  - Sampled rows: `52`.
  - Checked rows: `52`.
  - OHLC mismatches: `0`.
  - Missing source rows: `0`.
  - Fetch errors: `0`.
- Validation:
  - `python -m compileall -q src`: passed.
  - Targeted regression: `15 passed`.
  - Full unit regression: `$env:PYTHONPATH='src'; pytest -q tests/unit`: passed, `204 passed`.
