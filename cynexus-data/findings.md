# cynexus-data Migration Findings

## Boundary Findings

- `cynexus-data` is a Python Job Executor, not a Flask backend rewrite.
- Java/Core owns scheduler configuration, UI APIs, permissions, AI provider orchestration, polish, summary, and ask/Q&A.
- Python owns execution of data jobs, storage writes, data quality, repair/backfill, and transcript production.
- FastAPI is used only as an internal Trigger API, not as a replacement UI API.

## Legacy Project Findings

- Source project: `F:\dev-repo\mp4-to-word-new`.
- Source `.env` and `.env.local` files were intentionally not read.
- `backend/models` is in the audit because old scheduler and market repositories depend on ORM concepts.
- Per-file inventory reviewed 618 non-runtime, non-dependency files and classified each as `migrate`, `split`, `rewrite`, `skip`, or `review`.
- Old project contains large out-of-scope surfaces: Flask UI/API, React frontend, AI provider/prompt/polish/summary/ask flows, runtime uploads/outputs, logs, tools, and local model artifacts.
- The legacy transcription service combines transcription, polish, summary, metadata, and ask. Only media/audio to transcript is in scope.

## Migration Defaults

- Trigger API framework: FastAPI.
- Python job state tables: Python-owned Postgres tables under the configured Python schema.
- Do not import `backend.*` from new code.
- Do not copy old Flask app/bootstrap/runtime JSON behavior.
- Source modules must not write databases or manage job/checkpoint state.
- Domain modules must remain pure and avoid env, filesystem, network, and database access.

## Current Implementation Findings

- Job Executor foundation is executable: health check, registry, runner, locks, checkpoints, and in-memory job state work without external services.
- Storage foundation is present for ClickHouse, DuckDB, and Postgres; live external storage checks require configured services and credentials.
- Provider modules are split into hook/client/parser/adapter layers, but detailed legacy provider parity is staged behind `planned_after_skeleton` rows in the inventory.
- Pipelines are dry-run capable and return structured metadata through `JobResult`.
- Registered jobs can now run through both the Trigger API and CLI module entrypoints.
- Existing 1m parquet/TDX ingestion CLI entrypoints still compile and expose help.

## Production Logic Findings

- Kline period aggregation can be implemented safely as an insert-select from `period=1` rows using `toStartOfInterval`, `argMin(open, trade_time)`, `argMax(close, trade_time)`, and sum/max/min aggregations.
- Reusing `ch_dwd_kline_1m` for derived periods is acceptable for the current table design because `period` is part of the order key and derived rows use `source='aggregate_1m'`.
- Default job dry-runs should not require ClickHouse; read-only estimation is opt-in through `estimate=true`.
- TDX local minute sync can share the existing POC writer, but it should remain pipeline-owned and Hook-configured instead of shelling into the old CLI.
- TDX `.day` backfill should support both a full TDX installation root (`vipdoc/<market>/lday`) and an extracted `hsjday` root (`<market>/lday` or `hsjday/<market>/lday`).
- Daily raw writes should keep file lineage columns so later unit-scale repair and source reconciliation can trace back to a specific `.day` file.
- `sync_tdx_hsjday` should default to `var/tdx/hsjday` instead of a real TDX install path, because the job replaces the target root after validation.
- A downloaded hsjday zip can be validated without network access by using `skip_download=true` and passing `zip_path`; this is important because TDX may present anti-bot challenge pages.
- Adjusted daily K-line migration can be isolated behind an optional `eltdx` dependency: dry-run and tests do not require the package, while non-dry-run raises a clear dependency error if it is missing.
- The adjusted K-line job intentionally requires explicit `codes` for non-dry-run until a production stock universe repository is migrated.
- Stock basic is now Python-owned in `cynexus_data.sec_stock_basic`; this is intentionally execution metadata for Python jobs, not Java UI stock configuration.
- `sync_adjusted_kline_daily` can now use `sec_stock_basic` as its active code source, so all-code adjusted sync no longer requires hand-passed symbols after `sync_stock_basic` has run.
- Stock sector mappings are migrated as normalized PostgreSQL tables instead of old JSON snapshots. This keeps Python job consumers queryable while avoiding the old runtime JSON persistence pattern.
- `sync_stock_sectors` replaces mappings per stock, which is safer for retries and avoids stale topic membership for refreshed stocks.
- THS industry fund-flow is light enough for Python-owned PostgreSQL snapshots. The current migration keeps batch metadata plus per-sector daily rows in PG; larger stock-level intraday fund-flow should still go to ClickHouse in a later pass.
- Legacy THS live fetch depends on `hexin-v` generation through optional `akshare + py-mini-racer`; the pipeline also supports `history_path` JSON import so historical backfill does not require live crawling.
- MSI composite can be migrated before all factor producers: the formula layer is pure and treats missing components as neutral `50.0`, while `require_full=true` preserves the legacy backfill guard for production schedules.
- Sector breadth is the first migrated MSI factor producer because it depends directly on the newly migrated THS industry fund-flow snapshot and has a deterministic formula: `advance_pct = advancing / total`, `score = advance_pct * 100`.
- Transcription migration should keep the old media preparation behavior but remove hardcoded model paths. The new pipeline accepts an injectable transcriber and persists only transcript text/metadata, leaving polish, summary, metadata generation, and ask outside `cynexus-data`.
- Market pulse can be derived from the migrated sector fund-flow snapshot without re-crawling AkShare/THS. This keeps raw sector fund-flow, MSI sector breadth, and market pulse UI snapshots traceable to the same daily input.
- Market overview's important legacy behavior is field-level merging, not a specific Flask API response. The migrated snapshot keeps `eltdx` responsible for market breadth/amount fields and `akshare/manual` responsible for fund-flow fields.
- MA count production reads adjusted daily K-line rows from ClickHouse and persists compact factor state in PostgreSQL. `msi_ma_count_daily` feeds both MSI price-strength/breadth values and the profit-effect producer.
- Profit effect is deterministic once `up_5d_pct` and `new_low_60d_pct` exist: `0.60 * up_5d_pct + 0.40 * (100 - new_low_60d_pct)`. This makes it safe to support both cached PG input and explicit repair parameters.
- Legacy turnover, risk-appetite, and style-risk producers use historical percentile scores for MSI normalization. The migrated `PercentileRepository` now supports allowlisted PostgreSQL percentile scoring; non-dry-run producers use it when no explicit repair `score` is supplied and fall back to neutral `50` when history is unavailable.
- Legacy volatility sentiment can be migrated as a pure realized-volatility calculation because the score is self-contained: lower current realized volatility percentile maps to higher sentiment.
- Legacy limit emotion has two layers: snapshot/streak generation and daily summary scoring. The new migration implements the daily summary and a CH snapshot reader with board-based thresholds; full ST-aware streak parity remains a later hardening item because it depends on stock metadata and richer history logic.
- Existing deployments that already created `msi_ma_count_daily` need additive DDL. The MSI SQL now includes `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` for `price_strength_score` and `breadth_score`.
- Legacy trading-calendar logic is local-rule based, not provider-backed. The migrated core keeps those rules pure and accepts overrides as function/pipeline inputs instead of reading old backend config paths.
- `sync_trade_calendar` now persists generated rows to `sec_trade_calendar`, which gives Java/API/UI a lightweight PG calendar table without coupling Python to Java scheduler tables.
- Legacy `market_data_provider.py` mixed fallback, cache reads, provider calls, and UI-facing metadata. The new migration keeps fallback in `sources/provider_fallback.py` and persistence in pipelines, preserving source/domain boundaries.
- Kline provider fallback is production-enabled for explicit `codes` and non-TDX providers. TDX local remains the default bulk path because local file discovery and batching are materially different from network provider calls.
- Provider-neutral symbol conversion belongs in `core/market_symbol.py`; domain conversion and source fallback both depend on core instead of depending on each other.
- Kline resume should stay pipeline-owned, not source-owned. The migrated implementation keys checkpoints by concrete TDX file or provider code/date/provider batch for daily raw, and by `adjust + code + date range` for adjusted daily data.
- Limit emotion threshold parity depends on stock metadata, not only code prefixes. The migrated producer now prefers `sec_stock_basic.limit_up_threshold` / `is_st` and only falls back to code rules when metadata is unavailable.
- Historical limit streak parity can stay in pure domain code: ClickHouse supplies recent OHLC/pre-close rows, while the same ST-aware threshold logic computes previous/current streaks and distribution fields.
- TDX hsjday live download needs an optional browser-backed path because `data.tdx.com.cn` can return a small JavaScript challenge page to non-browser clients. The migrated source keeps `urlopen` as the fast path, detects challenge bytes before replacing the final zip, and uses optional Playwright Range downloads only for `download_method=playwright` or `download_method=auto` fallback.
- Legacy stock fund-flow UI used Tencent `qt.gtimg.cn` quote rows plus `s_pk` disk ratios, not the old `ff_` endpoint. The migrated production job stores lightweight per-stock active buy/sell snapshots in PostgreSQL, while raw intraday transaction/order-book style flow still belongs in ClickHouse when a reliable source is added.
- AkShare market overview capture should stay source-owned and lazy-imported. The migrated job uses AkShare for spot amount/breadth and market fund-flow, then relies on the existing market-overview merge rules so AkShare can fill fund-flow fields without clobbering eltdx market breadth fields when both sources are present.
- eltdx market overview capture is isolated as an optional TDX source helper. The job summarizes A-share quotes into market amount and breadth fields, leaving AkShare/manual fund-flow fields untouched through the source-specific overwrite rules.
- THS industry constituents live on `q.10jqka.com.cn`, not the existing `data.10jqka.com.cn` THS base URL used for fund-flow. The source client therefore uses an explicit q-domain URL for constituent pages while retaining the same `hexin-v` generation boundary.
- Legacy THS constituent JSON is per industry code and may contain numeric stock codes, so the migrated parser/domain both normalize `代码` to six digits and the domain layer dedupes by stock code before persistence.
- THS constituent PG replacement must be scoped by requested industry code. Replacing the whole snapshot date would make partial industry refreshes unsafe, so the repository soft-deletes only requested `industry_code` rows for the date/source.
- Legacy EastMoney adapter mixes kline, stock meta, and market breadth. The current highest-value remaining source migration is EastMoney market breadth from `push2.eastmoney.com/api/qt/clist/get`, because it can fill market-overview breadth fields through the existing `build_market_overview` snapshot pipeline without adding a new storage surface.
- EastMoney clist rows expose change percent (`f3`), volume (`f5`), and amount (`f6`) directly enough for market overview. The migrated source summarizes those fields in source code and lets the existing market-overview repository own persistence/merge behavior.
- EastMoney stock metadata is explicit-code enrichment, not a full stock-universe replacement. The migrated path adds nullable `sec_stock_basic` columns for industry and market-cap fields, uses `providers=eastmoney` with explicit `codes` or existing active PG codes, and keeps source fetching separate from repository writes.
- Tencent adapter review after EastMoney shows daily/minute K-line fallback is already production-wired through `sources/provider_fallback.py`. The remaining useful Tencent adapter gap is `qt.gtimg.cn` batch snapshot breadth/quote capture, which can later feed market overview or market pulse from active `sec_stock_basic` codes without reintroducing old UI cache behavior.
- Tencent qt batch snapshots are light enough to summarize directly for market overview. The migrated path accepts explicit `codes` or reads active codes from `sec_stock_basic`, keeps batch fetching in the source client, and persists only through the existing market-overview repository.
- Sina is minute-K-line-only in the legacy adapter. The production path already flows through `fetch_minute_kline_with_fallback`; the remaining parity gap was parser tolerance and volume-ratio metadata rather than a new pipeline or storage table.
- Legacy mootdx is best treated as an explicitly requested optional fallback, not a default provider. The migrated source keeps the dependency optional, supports daily and minute K-lines, rejects adjusted minute requests just like the legacy adapter, and lets provider fallback handle ordering/error metadata.
- Legacy eltdx is now partially production-migrated across several focused modules: adjusted daily K-line, market overview quote breadth, stock sector topics, minute/history K-line normalization, auction point capture, and historical 1m timeshare detail capture.
- eltdx auction/timeshare detail belongs in ClickHouse rather than PostgreSQL because it is high-frequency fact data. The migrated `sync_tdx_intraday_detail` job writes `ch_ods_stock_auction_point` and `ch_ods_stock_timeshare_1m`; old auction UI summary, JSON cache assembly, and AI auction analysis remain out of scope.
- Legacy market-overview technical helpers are partly pure calculation and partly UI/detail assembly. Window metrics, regime buckets, and support/resistance levels live safely in `domain/market/market_technical.py`; executable detail persistence now reads ClickHouse daily bars and writes only UI-ready summaries.
- Style rotation, industry strength, breadth sentiment, and similar-scenario backtests have the same boundary: the calculation is pure and lives in `domain/market/market_technical.py`; live provider/cache loaders remain pipeline concerns.
- Market overview detail persistence can be implemented as a compact PostgreSQL JSONB serving snapshot without violating the ClickHouse/PG split. `build_market_overview_detail` reads daily/index OHLCV windows from ClickHouse, uses `market_overview_snapshot` for breadth sentiment, and stores only the final UI-ready payload in `market_overview_detail_snapshot`.
- Legacy `kline_repo.py` daily reads are storage-read helpers, not pipeline jobs. The migrated `read_daily_kline_bars` preserves timestamp, ascending order, latest-N behavior, and adjusted-factor payload shape while keeping all ClickHouse access behind injected clients.
- Legacy `index_repo.py` mixes ClickHouse grouped index reads, return formulas, and PostgreSQL cache writes. The migrated boundary splits those into pure `domain/market/index_returns.py`, `storage/clickhouse/readers.py`, `storage/postgres/index_returns.py`, and the executable `build_index_returns` job.
- Legacy `f10/index_codes.py` has useful static TDX industry/concept index code data, but its override-file reads do not belong in domain. The migrated `domain/market/index_codes.py` keeps the catalog and pure lookup helpers while accepting caller-supplied extra items for future override support.
- Legacy `tdx_industry_service.py` is a useful data producer when stripped of heatmap assembly: eltdx resolves TDX 56 industry full codes and quote snapshots; the migrated source normalizes those rows and `sync_tdx_industry_quotes` persists compact PostgreSQL serving snapshots.
- Legacy `market_heatmap_service.py` mixes useful heatmap payload assembly with Flask-era hotpath fetchers, local JSON quote/share caches, stock-universe file reads, and UI-specific response shaping. The migrated production boundary builds a compact industry heatmap JSONB snapshot from persisted `tdx_industry_quote_snapshot` rows, while leaving old runtime caches and route behavior out of scope.
- Legacy `sector_quote_service.py` is useful as an eltdx `f10.theme_market` data producer, but its seed lookup and Tencent fallback were tied to old stock-universe JSON and UI hotpaths. The migrated boundary uses the TDX 56 industry seed catalog already in domain, captures selected `theme_market` sections, and stores compact PostgreSQL JSONB detail snapshots.
- Legacy per-stock turnover was intentionally moved out of K-line JSON files into a separate JSON cache. In `cynexus-data` the same separation maps cleanly to PostgreSQL `stock_turnover_series_snapshot`: ClickHouse remains the OHLCV fact source, eltdx supplies share profile metadata, and PG stores only the compact UI-serving turnover series.
- Legacy `_pg_writer.py` was only a non-fatal bridge from JSON market-overview payloads into PostgreSQL. The new pipeline writes directly through `MarketOverviewRepository`, so no separate helper belongs in the executor.
- Legacy stock chart config, search, self-selected lists, workspaces, and annotations are application/UI/user state. They should stay with Java/Core/UI rather than becoming Python data jobs.
- Legacy `_limit_pg_writer.py` and `market_limit_pg_repo.py` have one scoped production responsibility beyond MSI summary scoring: extracting limit-up, limit-down, and broken-board stock lists from the computed payload for fast PG reads. In `cynexus-data`, the replacement is direct `build_limit_emotion` persistence into `market_limit_stock_snapshot`; old Flask-era non-fatal wrapper behavior and route reads are skipped.
- Legacy `analysis_data_reader.py` is an AI feature-summary reader facade over old services and runtime caches, not a data producer. Legacy `sample_data_service.py` only generates synthetic UI fallback K-lines. Both are out of the Python executor migration scope.
- The former `planned_after_skeleton` count was inflated by stale per-file inventory rows: package `__init__.py`, old script wrappers, and repositories whose capabilities already exist as jobs/pipelines/tables. After reconciliation, the backlog count is zero; remaining risk is live integration validation, not missing per-file migration.
- Legacy `style_sector_service.py` has a scoped production responsibility: compute 29 style-sector equal-weight change percentages from category `4` stock-sector constituents and Tencent qt quotes. The migrated boundary stores compact `market_style_sector_snapshot` rows and leaves old JSON cache/UI service behavior out of scope.
- Legacy `stock_universe_service.py` is covered by PostgreSQL stock-basic and stock-sector jobs/tables. The old daily JSON/sectors cache is intentionally not restored.
- Legacy `etf_poc_service.py` is an ETF POC/UI aggregation path, not part of the current stock-data executor scope. It should be reintroduced only as a separate formal ETF pipeline if ETF coverage becomes a requirement.
- Current PG alignment uses two schemas: `cynexus_appl_market` for market/serving/data-production tables and `cynexus_appl_core` for Python job runtime and transcription tables.
- User-approved old Java-facing tables should be reused instead of shadowed: limit stock rows write `market_limit_daily_stocks`; overview/pulse/sector fund-flow write their existing plural old tables.
- New market serving tables that do not already exist in Java are named with `mkt_` rather than `market_`: `mkt_heatmap_snapshot`, `mkt_overview_detail_snapshot`, and `mkt_style_sector_snapshot`.
- `msi_turnover_activity_daily` now carries `sh_amount`, `sz_amount`, and `sample_count`; the dry-run payload uses `shAmount`, `szAmount`, and `sampleCount`.
