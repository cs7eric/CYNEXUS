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
- Legacy turnover, risk-appetite, and style-risk producers use historical percentile scores for MSI normalization. The new migration stores production raw values and accepts explicit `score` for repair/backfill; it uses neutral `50` when the percentile store is not yet present.
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
