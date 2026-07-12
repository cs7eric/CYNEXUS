# Cynexus Setup Findings

## Guide Requirements

- Root project: `cynexus`
- Guide originally says group ID `com.cynexus`, but user explicitly asked to keep existing Maven group ID `cn.cccs7`.
- Version: `1.0.0-SNAPSHOT`
- Java: 21
- Java package root: `cn.cccs7.cynexus`
- Build: Maven
- Architecture: DDD + Maven multi-module + microservice-ready structure.
- First services only: `cynexus-ai`, `cynexus-market`, `cynexus-common`.
- `cynexus-ai-starter` must run on port `18081`.
- `cynexus-market-starter` must run on port `18082`.
- Both services should use Nacos for registration, discovery, and config center.
- `spring.config.import` must be used for Nacos config; do not use old bootstrap style.
- AI service should use Feign clients and call market through the market API/Feign contract later.

## Module Structure

Required common modules:

- `cynexus-common-core`
- `cynexus-common-web`
- `cynexus-common-security`
- `cynexus-common-db`
- `cynexus-common-cloud`

Required AI modules:

- `cynexus-ai-api`
- `cynexus-ai-common`
- `cynexus-ai-domain`
- `cynexus-ai-infra`
- `cynexus-ai-application`
- `cynexus-ai-application-controller`
- `cynexus-ai-starter`

Required market modules:

- `cynexus-market-api`
- `cynexus-market-common`
- `cynexus-market-domain`
- `cynexus-market-infra`
- `cynexus-market-application`
- `cynexus-market-application-controller`
- `cynexus-market-starter`

## History-Derived Background

- Nacos and OpenFeign should be first-phase infrastructure, not postponed.
- `cynexus-common-cloud` is the right place for Nacos Discovery, Nacos Config, OpenFeign, and LoadBalancer dependencies.
- `cynexus-market-api` should hold DTOs and Feign contracts used by `cynexus-ai-infra`.
- AI business config belongs in PostgreSQL later; Nacos is for infrastructure config and service discovery.
- First Docker Compose should include only PostgreSQL, Redis, and Nacos.

## Local Repo Findings

- Existing root `pom.xml` currently uses `cn.cccs7:cynexus:1.0-SNAPSHOT`; it must be replaced.
- `guide/PROJECT_BRIEF.md` exists; the user-provided path had an extra space before `.md`.
- Current git status contains already-added files from the user/repo initialization; do not revert them.

## Version Choices

- Spring Boot: `4.0.7`
- Spring AI BOM: `2.0.0`
- Spring Cloud BOM: `2025.1.0`
- Spring Cloud Alibaba BOM: `2025.1.0.0`
- MyBatis-Plus Spring Boot 4 starter: `3.5.15`
- JobRunr Spring Boot 4 starter: `8.7.0`

## User Decisions

- Maven `groupId`: `cn.cccs7`
- Java package root: `cn.cccs7.cynexus`

## Layer/Object Convention Findings

- Domain modules must not contain Mapper, PO, Feign, Redis, Spring AI, or MVC references.
- Domain services should be interfaces in `domain/service` with implementations in `domain/service/impl`.
- Domain business objects should use BO suffix under `domain/entity`; stable domain values belong under `domain/valueobject`.
- ORM belongs in infra under `basic/entity`, `basic/mapper`, `basic/service`, and `basic/service/impl`.
- Domain repository interfaces belong in domain; infra repository implementations call basic services/mappers and convert PO to BO.
- Controllers should return `Result<DTO>` or `Result<List<DTO>>` and should not expose raw Map.
- Current code now follows those checked boundaries for common-core, AI domain, Market domain, and controller return contracts.

## MapStruct Convention

- Common-core owns `cn.cccs7.cynexus.common.core.mapper.BaseMapper<S, T>`.
- `BaseMapper` contains `toTarget(S source)`, `toSource(T target)`, `toTargetList(List<S> sourceList)`, and `toSourceList(List<T> targetList)`.
- MyBatis mapper inheritance remains separate as `cn.cccs7.cynexus.common.db.mapper.BaseMapperX`.
- API output models should use `dto` package types; do not create parallel `response` package records unless the convention changes again.

## Local Nacos

- Local Nacos runs from `deploy/local/docker-compose.yml`.
- For local development, services default to the Nacos public namespace: `NACOS_NAMESPACE` default is empty.
- `cynexus-ai` and `cynexus-market` explicitly set `spring.cloud.nacos.discovery.register-enabled=true`.
- If a custom namespace is needed later, use the Nacos namespace ID in `NACOS_NAMESPACE`, not the display name.

## Local PostgreSQL

- DB dependencies are centralized in `cynexus-common-db`: JDBC, PostgreSQL driver, and MyBatis-Plus Boot 4 starter.
- Service datasource config lives in each starter `application.yml`.
- AI datasource default: `jdbc:postgresql://127.0.0.1:25432/postgres`.
- Market datasource default: `jdbc:postgresql://127.0.0.1:25432/postgres`.
- Runtime defaults match the existing Docker Postgres exposed on host port `25432` with user `postgres`.
- Service infra modules own PO/Mapper/BasicService/repository implementations; AppService should stay above that layer.
- The manually started local Postgres container is `62852359c804` / `postgres`, image `postgres:18`, exposed as `127.0.0.1:25432->5432`.
- Its actual database files are in Docker anonymous volume `f9b03225196ab4753a432dc502f3f74454ef4134a0973af24671c8038b5d9397`, mounted at `/var/lib/postgresql`.
- The container uses `PGDATA=/var/lib/postgresql/18/docker`; compose must mount the existing volume at `/var/lib/postgresql`, not `/var/lib/postgresql/data`.
- The original `--volume=E:\dev-repo\postgresql\data;E:\dev-repo\Git\var\lib\postgresql\18\docker` bind argument was parsed strangely by Docker on Windows and is not the primary Postgres data location.
- Project-owned local Postgres should run separately on `127.0.0.1:25433` and use the bind mount `deploy/local/postgres/data:/var/lib/postgresql`.
- For the Postgres 18 image used here, `PGDATA=/var/lib/postgresql/18/docker`, so the local bind mount will contain the `18/docker` data directory underneath `deploy/local/postgres/data`.
- After import, old container `postgres` on `25432` and project container `cynexus-postgres` on `25433` both contain 29 user base tables with 27068 total rows in database `postgres`.
- User wants the project-owned default database to be `cynexus`; configs should default to `jdbc:postgresql://127.0.0.1:25433/cynexus`.
- Project container `cynexus-postgres` now contains both `postgres` and `cynexus` databases. The copied `cynexus` database has 29 user base tables and 27068 total rows, matching the project container's previous `postgres` database.

## Local Redis

- Local Redis runs from `deploy/local/docker-compose.yml` and is exposed on host `127.0.0.1:6379`.
- The compose service uses password `C020611.`.
- AI and Market starter configs now use `spring.data.redis.*` defaults matching the local Redis service.
- Shared Nacos config also carries the same Redis defaults for config-center driven runs.

## Local Application Compose

- AI starter runs on port `18081`; Market starter runs on port `18082`.
- Application containers must use compose service names: `nacos:8848`, `postgres:5432`, and `redis:6379`.
- Host-side application access remains `127.0.0.1:18081` for AI and `127.0.0.1:18082` for Market.
- Starter jars need Spring Boot `repackage`; without it the generated jars are thin Maven jars and cannot be run with `java -jar`.
- Existing IntelliJ-launched Java processes occupied `18081` and `18082`; stopping those processes allowed Docker Compose to bind the same host ports.
- `cynexus-ai` and `cynexus-market` both register successfully to Nacos from containers and report actuator health `UP`.

## Service Schema And BIGINT ID Migration

- Current authoritative project database is `cynexus` in compose container `cynexus-postgres` on host port `25433`.
- Before schema/ID migration, only Postgres, Redis, and Nacos are running; AI and Market application containers are stopped.
- Business tables currently live in schema `app`.
- `app` contains 21 business tables with about 27027 rows.
- All service-owned UUID columns are in `app`; their `id` columns default to `gen_random_uuid()`.
- `public` contains JobRunr infrastructure tables plus old imported `self_selected_groups` and `self_selected_items`; the old public self-selected tables currently have 0 rows.
- `public` should be preserved as-is for now; JobRunr schema/data is third-party owned and should not be converted in this migration.
- Target schemas are `cynexus_appl_ai` and `cynexus_appl_market`.
- AI-owned tables: `ai_providers`, `ai_usage_bindings`, `application_analysis_configs`, `application_analysis_daily_snapshots`, `application_analysis_result_current`, `application_analysis_result_history`, `application_analysis_targets`.
- Market-owned tables: `market_limit_daily_snapshots`, `market_limit_daily_stocks`, `market_overview_snapshots`, `market_pulse_capture_batches`, `market_pulse_sector_daily_snapshots`, `scheduler_job_categories`, `scheduler_job_category_mappings`, `scheduler_job_run_history`, `scheduler_job_statuses`, `scheduler_jobs`, `sector_fund_flow_capture_batches`, `sector_fund_flow_daily_snapshots`, `self_selected_list_items`, `self_selected_lists`.
- Existing update triggers call `app.set_updated_at()`, so the function can remain in `app` even after tables move schemas.
- PostgreSQL `currentSchema` affects every query issued through the shared datasource, including JobRunr auto-configuration.
- JobRunr tables remain in `public`, so if JobRunr SQL storage auto-config is active while a service datasource uses `currentSchema=cynexus_appl_*`, startup fails looking for `jobrunr_migrations` in the service schema.
- AI and Market currently have JobRunr server/dashboard/scheduler disabled, so the starter configs also exclude JobRunr auto-configuration until a dedicated JobRunr schema/datasource decision is made.

## Core Service

- `cynexus-core` follows the same Maven service layout as AI and Market: api, common, domain, infra, application/application-controller, and starter.
- Core starter uses service name `cynexus-core` and local port `18080`.
- Core datasource default is `jdbc:postgresql://.../cynexus?currentSchema=cynexus_appl_core`.
- Core local compose service depends on Postgres, Redis, and Nacos, and AI/Market now wait for Core to be healthy.
- Scheduler ownership moved from Market schema to Core schema.
- `cynexus_appl_core` owns five scheduler tables: `scheduler_job_categories`, `scheduler_job_category_mappings`, `scheduler_job_run_history`, `scheduler_job_statuses`, and `scheduler_jobs`.
- After the move, `cynexus_appl_market` contains 9 business tables and no `scheduler_%` tables.

## CRUD Completion Findings

- AI application CRUD endpoint base path: `/api/ai/config`.
- Market application CRUD endpoint base path: `/api/market/data`.
- `MarketOverviewBasicServiceImpl` previously returned mock data and is now backed by `MarketOverviewMapper`.
- `MarketOverviewMapper.xml` was the only missing mapper XML in the checked AI/Market/Core PO set.
- A clean rebuild exposed old record-style accessors in Market assemblers; those were changed to Lombok getter calls.
- Core scheduler PO converters warned about unmapped audit fields until scheduler BO/DTO classes gained `createdAt`, `updatedAt`, and `deletedAt`.

## Local Docker Rerun

- The supported command for local application reruns is `.\deploy\local\up-latest.ps1`.
- `up-latest.ps1` runs `mvn clean package -DskipTests` before rebuilding/recreating `core`, `market`, and `ai`.
- `Dockerfile.app` now copies the local starter jar into the image, so the image content is tied to the Maven package output from the current workspace.
- The `.cmd` wrapper exists for Windows shell convenience: `deploy\local\up-latest.cmd`.
- If a completely uncached Docker rebuild is needed, use `.\deploy\local\up-latest.ps1 -NoCache`.

## Controller Dependency Rule

- Controllers should depend on application services, not domain services.
- Core now follows `Controller -> AppService -> DomainService -> Repository`.
- Current controller audit found no direct `DomainService` dependency in AI, Market, or Core controllers.

## Gateway Service

- `cynexus-gateway` is a real Spring Cloud Gateway microservice, not an external reverse proxy.
- Local gateway port is `127.0.0.1:18000`.
- Gateway uses Nacos discovery and Spring Cloud LoadBalancer routes:
  - `/api/core/**` -> `lb://cynexus-core`
  - `/api/ai/**` -> `lb://cynexus-ai`
  - `/api/market/**` -> `lb://cynexus-market`
- Spring Cloud Gateway 5 uses the config prefix `spring.cloud.gateway.server.webflux`.
- Local Docker rerun default now includes `gateway` in `deploy/local/up-latest.ps1`.
- `market_overview_snapshots` database columns differ from the previous placeholder PO; the table is now represented by its real columns, including money-flow fields, breadth fields, manual override fields, JSON `extra`, and audit fields.

## Collector Migration Inventory

- Old project path is `F:\dev-repo\mp4-to-word-new`.
- New collector placeholder path is `E:\project-repo\cynexus\cynexus-collector`; it currently has no files.
- Old project Git status was clean during inspection.
- Old `.env` exists but was intentionally not read.
- Old backend entrypoint is Flask: `app.py` -> `backend.app_factory.create_app()` -> `backend.bootstrap.register_blueprints()`.
- Old bootstrap registers stock chart, F10, scheduler, self-selected, AI provider, transcription, public, system, and MP4 history blueprints.
- Old bootstrap can auto-start many scheduler jobs during Flask startup, so collector must not import old `app.py` or bootstrap wholesale.
- Old `backend/runner.py` contains local dev behavior that can kill a process occupying port `5000`; this must not be used in Docker/service runtime.
- Old `backend/config/database.py` uses lazy SQLAlchemy engine/session creation from `DATABASE_URL`.
- Old `backend/config/settings.py` binds many runtime paths to the old repository root, including `reference`, `models`, `uploads`, `outputs`, `runtime`, and scheduler directories.
- Old backend APIs are large: `backend/api/stock_chart.py` is about 149 KB and `backend/api/scheduler.py` is about 60 KB.
- Old project contains large files that must not be copied into Git or Docker build context: Whisper model artifacts under `models`, DuckDB under `reference/stock/duckdb`, large `hsjday.zip` daily archives, and many media files under `uploads`.
- Recommended service name is `cynexus-collector`, local port `18083`, Gateway route `/api/collector/**`, and Nacos service name `cynexus-collector`.
- Core should own job scheduling metadata/history and call Collector for Python-only execution work.
- Collector should own AkShare/THS/ELTDX quirks, TDX `.day`/hsjday handling, local DuckDB/day-file readers, Whisper transcription, and media processing.
- Java services should keep owning AI config, application analysis tables, scheduler metadata/history, market overview/pulse/limit data, sector fund-flow data, and self-selected data.
- Detailed inventory was written to `docs/collector-migration-inventory.md`.

## DuckDB To ClickHouse Migration Foundation

- The old DuckDB database is `F:\dev-repo\mp4-to-word-new\reference\stock\duckdb\market_data.duckdb`.
- The DuckDB database contains schema `main` with 24 base tables.
- Largest DuckDB tables are `daily_raw`, `daily_qfq`, and `daily_hfq`, each around 28.5 million rows.
- Local ClickHouse should start as standalone infrastructure first; DuckDB data migration should be a later explicit phase.
- ClickHouse local Docker service uses image `clickhouse/clickhouse-server:25.8`.
- Local ClickHouse ports are HTTP `127.0.0.1:28123` and native `127.0.0.1:29000`.
- Local ClickHouse database/user are both `cynexus`.
- Local ClickHouse data/log bind mounts are `deploy/local/clickhouse/data` and `deploy/local/clickhouse/logs`.

## DuckDB PostgreSQL Target Tables

- User requested selected DuckDB tables to be created first in PostgreSQL under schema `cynexus_appl_market`.
- MSI table name mapping:
  - `market_sentiment_index_daily` -> `msi_index_daily`
  - `limit_emotion_summary_daily` -> `msi_limit_emotion_daily`
  - `ma_count_daily` -> `msi_ma_count_daily`
  - `profit_effect_daily` -> `msi_profit_effect_daily`
  - `risk_appetite_daily` -> `msi_risk_appetite_daily`
  - `style_risk_appetite_daily` -> `msi_style_risk_daily`
  - `turnover_activity_daily` -> `msi_turnover_activity_daily`
  - `volatility_sentiment_daily` -> `msi_volatility_daily`
  - `market_pulse_sector_breadth_daily` -> `msi_sector_breadth_daily`
- General market table name mapping:
  - `market_overview_daily` -> `mkt_overview_daily`
  - `market_pulse_sector_daily` -> `mkt_sector_pulse_daily`
  - `ths_industry_fund_flow_daily` -> `mkt_ths_industry_fund_flow_daily`
  - `index_returns_daily` -> `mkt_index_return_daily`
- Security master-data table name mapping:
  - `stock_universe` -> `sec_stock_universe`
  - `corp_events` -> `sec_corp_event`
- PostgreSQL target tables intentionally use project-standard `id BIGINT NOT NULL PRIMARY KEY` with no default. DuckDB natural keys are partial unique indexes filtered by `deleted_at IS NULL`.
- `mkt_index_return_daily` uses `current_close` and `current_trade_date` instead of DuckDB's `current` / `current_date` names to avoid PostgreSQL keyword ambiguity and improve clarity.
- Local backup before DDL: `deploy/local/postgres-backups/cynexus-before-duckdb-target-tables-20260626-213927.sql`.
- DDL migration file: `deploy/local/postgres/migrations/20260626_create_market_duckdb_target_tables.sql`.
- DDL was applied successfully and re-run successfully for idempotency.

## Remaining DuckDB Tables Migrated To ClickHouse

- Tables not mapped to PostgreSQL were migrated to ClickHouse:
  - `daily_raw`
  - `daily_qfq`
  - `daily_hfq`
  - `adj_factors`
  - `index_daily_raw`
  - `ingest_state`
  - `intraday_bars`
  - `quotes`
  - `schema_meta`
- User explicitly named `daily_raw`, `daily_qfq`, `adj_factors`, and `index_daily_raw`; inspection showed `daily_hfq`, `ingest_state`, `intraday_bars`, `quotes`, and `schema_meta` were also not mapped to PostgreSQL.
- ClickHouse keeps the original DuckDB table names for these detail/time-series/state tables.
- Long-history daily tables use `MergeTree`, yearly partitions via `toYear(trade_date)`, and `ORDER BY (code, trade_date)`.
- Initial monthly partition design failed for `daily_raw` because a single full-history insert touched more than ClickHouse's default `max_partitions_per_insert_block`.
- ClickHouse data/log bind mounts on Windows failed during MergeTree atomic rename; switching to Docker named volumes fixed inserts.
- Migration path is DuckDB `COPY ... FORMAT PARQUET` -> `docker cp` -> `clickhouse-client INSERT ... FORMAT Parquet`.
- Row-count reconciliation succeeded for all 9 tables:
  - `daily_raw`: 28,516,739
  - `daily_qfq`: 28,482,626
  - `daily_hfq`: 28,482,612
  - `index_daily_raw`: 5,012
  - `ingest_state`: 12,037
  - `schema_meta`: 2
  - empty tables: `adj_factors`, `intraday_bars`, `quotes`

## Market Pulse Python Data Backfill Findings

- User reports current Java Market Pulse data is stale/incomplete: funds tide / turnover trend only reaches 2026-05-27 in UI, limit emotion shows 2026-06-25 at 21:54:15 instead of latest trading day 2026-06-26, style/industry/concept heatmaps are empty, and three-index intraday charts have no points.
- Initial search indicates old Python project stores relevant persisted data under `F:\dev-repo\mp4-to-word-new\reference`, including `market-overview`, `market-pulse`, `stock/cache/intraday`, and THS/heatmap-related directories. Precise route-to-file mapping is still being traced.
- Existing Java endpoint `/api/market/pulse/**` currently reads PG tables in `cynexus_appl_market`: `market_overview_snapshots`, `market_limit_daily_snapshots`, `market_limit_daily_stocks`, `mkt_index_return_daily`, `mkt_style_sector_snapshot`, and `mkt_heatmap_snapshot`.
- Principle to implement: for non-trading days, display the previous available trading day data.

## Market Pulse Backfill Open Questions

- Need confirm whether old Python has 2026-06-26 limit emotion JSON. If not, latest available raw snapshot may truly be 2026-06-25, and refresh/source collection must be fixed separately.
- Need confirm whether three-index intraday files exist for 2026-06-26 in old `reference/stock/cache/intraday`; if only stock intraday files exist, Java must read ClickHouse minute/index data or another old API source.

## 2026-06-28 Market Pulse Backfill Additional Findings

- Old limit-emotion daily raw file exists for `2026-06-26`: `F:\dev-repo\mp4-to-word-new\reference\market-limit\daily\2026-06-26.json`, with `marketStatus=closed`, `updateTime=2026/6/26 15:30:00`, source `eltdx.list_by_category(6)+universe`, and `stockCount=5282`.
- Java `MarketPulseQueryAppService.getLimitEmotion()` returns `market_limit_daily_snapshots.extra` directly when it contains key `limitUp`, so the PG import should store an old-Python-compatible payload in `extra` instead of relying only on summary columns.
- Java `MarketPulseQueryAppService.getHeatmap()` returns `mkt_heatmap_snapshot.payload` directly when it is a JSON object, so heatmap imports should write frontend-compatible payloads to that JSONB column.
- Old sector universe files under `F:\dev-repo\mp4-to-word-new\reference\stock-universe\sectors` were updated on `2026-06-26 17:16:05`, including `sectors_industries_0.json`, `sectors_concepts_2.json`, and `sectors_styles_4.json`.
- No `index-*-2026-06-26.json` files were found under the old intraday cache directory in the first check, so three-index intraday needs either an alternate source/import table or a latest-available-date fallback.

## Market Pulse Backfill Resolution Findings

- `market_limit_daily_snapshots` now has a 2026-06-26 row with limit_up=57, limit_down=35, touched=96, broken=39, stock_count=5282, and `extra` contains the UI-compatible limit-emotion payload.
- `mkt_style_sector_snapshot` now has 29 rows for 2026-06-26, matching the old Python style-sector whitelist.
- `mkt_heatmap_snapshot` now has 2026-06-26 payloads for `industries` (90 items from THS industry constituents snapshots) and `concepts` (top 200 from stock-universe concepts plus ClickHouse daily quotes).
- Three-index minute data source was not the old Python cache for 2026-06-26; the usable latest source was local TDX `.lc1` files under `D:\Program File\tdx\vipdoc\{sh,sz}\minline`. Those were inserted into ClickHouse `ch_dwd_kline_1m` for `sh000001`, `sz399001`, and `sz399006`.
- `index_daily_raw` only had `sh000001`, `sh000300`, and `sh000852`; `sz399001` and `sz399006` daily rows were backfilled from local TDX `.day` files with `unit_scale=10` so Java can return previousClose for all three index cards.
- Java `/api/market/pulse/index-kline/batch` now reads ClickHouse minute bars and falls back from a non-trading requested date to the latest available minute trading date.

## Market Pulse Layering Repair Findings

- Current Java Market Pulse query chain violates the project layering guide: `MarketPulseQueryRepository` exposes `Map<String,Object>`, `MarketPulseQueryRepositoryImpl` uses `JdbcTemplate` with inline PostgreSQL SQL, `MarketPulseQueryAppService` builds nested maps, and `MarketPulseQueryController` returns raw maps instead of `Result<T>`.
- Existing table-layer PO classes already cover `market_overview_snapshots`, `market_limit_daily_snapshots`, `market_limit_daily_stocks`, `mkt_style_sector_snapshot`, and `mkt_heatmap_snapshot`.
- `mkt_index_return_daily` does not have a Java table-layer class yet, so it needs PO/BO/mapper XML support before Market Pulse index-return endpoints can be typed.
- Frontend Market Pulse page currently imports page-specific API wrappers and types from global `src/lib/api.ts`; this violates the requested page-module organization.
- Follow-up correction: replacing raw maps with `Result<JsonNode>` is still non-compliant because the API contract remains dynamic and bypasses DTO/Request/converter conventions.
- Market Pulse JSONB columns can be parsed in application code, but the parsed result must become typed DTOs before returning from AppService/Controller.
- Heatmap persisted payload uses `changePercent` and `stockCount`; the Java DTO maps these to frontend-compatible `change_pct` and `count`.
- Market Pulse `IndustryHeatmap` reuses `HeatmapSectorNode` from `industry-application`, so the backend heatmap DTO must preserve sector-node fields such as `changePercent`, `stockCount`, `conceptTags`, and `children`; otherwise frontend code calls `sector.children.filter(...)` on `undefined`.

## Industry Application API Migration Findings

- Frontend page route `/stock-overview/industry-application` resolves to `F:\dev-repo\mp4-to-word-new\frontend\src\views\industry-application\index.tsx`.
- The page currently imports industry application API helpers from global `F:\dev-repo\mp4-to-word-new\frontend\src\lib\api.ts`, which violates the migration guide rule for Java API clients.
- The old global API segment calls Python paths under `/api/stock-chart/industry-application/**`: `targets`, `target-codes`, `kline`, `refresh`, `results/{targetId}`, and `overview`.
- The page also calls `fetchMarketHeatmap`, which now routes through the Java Market Pulse API base and may be reusable instead of duplicating heatmap logic.
- Frontend industry application types are currently page-local under `views/industry-application/lib/types.ts`; new service should either import those stable view types temporarily or define service-level models and keep component-facing normalization in the service layer.
- Java Industry Application migration needs a real application boundary in addition to infra/repository: controller returns `Result<IndustryApplicationDTO.*>`, AppService orchestrates refresh/read/write, and frontend calls move to `src/services/market/industry-application.ts`.
- Old Python result storage was local JSON; Java migration stores target config and refreshed K-line result snapshots in PostgreSQL tables `mkt_industry_application_config`, `mkt_industry_application_target`, and `mkt_industry_application_result`.
- The industry application overview heatmap uses the existing Market Pulse heatmap endpoint. `styles` was empty because Market Pulse heatmap reads `mkt_heatmap_snapshot`, while style-sector data lives in `mkt_style_sector_snapshot`; the Java query app service now adapts style-sector rows into `MarketPulseHeatmapDTO` for `kind=styles`.
- TDX local day-file reads for `sh880301` returned latest trading date `2026-06-26`; on the current date `2026-06-29` this satisfies the non-trading-day display rule by serving latest available trading data.

## Industry Application Constituents Drawer Repair Findings

- The right drawer was empty because the fund-flow rows returned by Java had `code: null`; the drawer only fetches constituents by THS industry code through `/api/market/pulse/industries/{code}/constituents`.
- PostgreSQL `cynexus_appl_market.ths_industry_constituent_snapshot` and `ths_industry_constituent_capture_batch` were empty before repair.
- The old project already had usable persisted source data under `F:\dev-repo\mp4-to-word-new\reference\ths-industry`: `industry_list.json` plus 90 files under `constituents\*.json`.
- `industry_list.json` provides `nameToCode`, allowing existing `sector_fund_flow_daily_snapshots.sector_name` values to be repaired to THS `881xxx` codes.
- Import source files contain 90 successful industry snapshots and 4,663 constituent rows, fetched on `2026-06-26`.
- After import, `/api/market/pulse/industries/881121/constituents` returns 100 rows for `半导体`; `/api/market/pulse/industries/881270/constituents` returns 62 rows for `元件`.
- `SectorHeatmap` still had a drill-down import from global `src/lib/api.ts`, which would route that entry point back to the old Python API. It now imports `fetchIndustryConstituentsByCode` from `src/services/market/pulse.ts`.

## Stock Overview Application Analysis Migration Findings

- Frontend route `/stock-overview/application-analysis` resolves to `F:\dev-repo\mp4-to-word-new\frontend\src\views\application-analysis\index.tsx`.
- The page currently imports application-analysis helpers from global `F:\dev-repo\mp4-to-word-new\frontend\src\lib\api.ts`.
- Old Python application-analysis endpoints are `GET/PUT /api/stock-chart/application-analysis/targets`, `GET /api/stock-chart/application-analysis/results/{target_id}`, `POST /api/stock-chart/application-analysis/refresh`, `GET/POST /api/stock-chart/application-analysis/scheduler[/start|/stop]`, `POST /api/stock-chart/application-analysis`, `POST /api/stock-chart/application-analysis/recent30/refresh`, `GET /api/stock-chart/application-analysis/recent30/{target_id}`, `GET /api/stock-chart/application-analysis/recent30/{target_id}/full`, and `GET /api/stock-chart/application-analysis/recent30/{target_id}/{date}`.
- Java-owned backing tables already exist in `cynexus_appl_ai`: `application_analysis_configs`, `application_analysis_targets`, `application_analysis_result_current`, `application_analysis_result_history`, and `application_analysis_daily_snapshots`.
- Local PG verification before migration showed: targets=4, current=4, history=1128, daily=22, configs=1.
- The page also calls old stock-chart K-line API through `fetchStockKlines`; current Java Market service only has special-purpose industry and market-pulse K-line endpoints, not a general stock-chart K-line contract. This remains out of Phase 24 unless a general Java stock-chart K-line endpoint is added.

## Application Analysis Stock Chart Runtime Findings

- ClickHouse `ch_dwd_kline_1m` is populated for application-analysis stock symbols, including `SH 600021` from `2010-01-04` through `2026-06-26` with 964,000 rows.
- Therefore application-analysis intraday/timeshare and minute K should not return stable empty DTOs; they can be served from Java Market through ClickHouse.
- `stock_fund_flow_snapshot` exists in `cynexus_appl_market` but currently has 0 rows, so true Tencent/EM per-stock active fund-flow is not yet available in PG.
- Old project reference only had a few stale `reference/stock-universe/qt_fund_flow/*.json` files dated `2026-06-07`, not enough for a proper complete stock fund-flow backfill.
- Until `stock_fund_flow_snapshot` is collected/imported, the Java stock-chart fund-flow endpoint uses local TDX daily bars as an explicit fallback for amount/change/history only. Active net amount and buy/sell ratio remain null so UI does not confuse derived daily data with real fund-flow.
- Jackson field aliasing on Lombok DTOs needed getter-level `@JsonProperty` for `effective_adjust` and `requested_adjust`; field-level annotation alone still emitted camelCase in this project runtime.
- Application-analysis B/S annotation data in the old project is file-backed under `reference/stock/data/annotations/*.json`. Java currently has no `stock_chart_annotation` table or repository contract, so the frontend service keeps localStorage as a non-Python temporary path rather than writing a new file-backed Java API.
- Auction data in the old project is fetched through `eltdx` and scheduler/cache code. There is no current Java-owned auction snapshot table, so the Java endpoint returns a typed empty/not-wired payload instead of pretending to serve real persisted data.

## Core Scheduler Runtime And Python Executor Findings

- `cynexus-data` exposes the trigger API on `127.0.0.1:18083`; after starting it locally, `/internal/health` returns `{"ok": true, "service": "cynexus-data"}`.
- With `cynexus-data` running, Gateway/Core manual trigger works: `POST /api/core/scheduler/management/jobs/test_scheduler_demo/trigger` returns `status=success` and a Python `run_id`.
- Python `cynexus-data` currently has registered jobs such as `sync_tdx_intraday_detail`, `build_market_overview`, `build_market_pulse`, `build_msi`, and `trigger_probe`; it does not have registered jobs named `application_analysis`, `auction_ai_analysis`, or `daily_eod_incremental`.
- The old scheduler metadata describes `daily_eod_incremental` as deprecated and split into independent jobs such as initial backfill, QFQ reconciliation, limit emotion, market overview daily, and turnover activity.
- `auction_ai_analysis` is an AI analysis job, but the data-sync part for auction facts can use the existing Python job `sync_tdx_intraday_detail` with `datasets=["auction"]`.
- Existing Core manual trigger records run history as `triggerType="manual"` and has no automatic runner consuming `run_time`, `cron`, `every_minutes`, or `intraday_windows`.
- Generic MyBatis entity updates on `scheduler_job_statuses` can fail because the `schedule` column is PostgreSQL `jsonb` while the Java entity serializes it as text; runtime state changes should use a narrow SQL update for running timestamps instead of `updateById`.

## Scheduler Chain Progress And History Findings

- Old Python `market_sentiment_chain_scheduler.py` treated `market_sentiment_chain_refresh` as an orchestrated chain, not a single MSI rebuild. It ran steps in order: `tdx_hsjday_download`, `initial_backfill_refresh`, `qfq_reconciliation_refresh`, `limit_emotion_refresh`, `risk_appetite_refresh`, `ma_count_refresh`, `volatility_sentiment_refresh`, `style_risk_appetite_refresh`, `profit_effect_refresh`, `market_overview_daily`, `turnover_activity_refresh`, `ths_industry_fund_flow_daily`, `sector_breadth_refresh`, and `market_sentiment_index_refresh`.
- Old Python chain status updated `lastStep`, `lastCompletedSteps`, `lastStepResults`, `lastTargetsProcessed`, and `lastMessage` during the run, while history used `begin_run` with `processing` then `update_run` to final `success`/`failed`.
- The Java scheduler frontend already reads `config.lastStep`, `config.lastCompletedSteps`, `config.lastStepResults`, `live.lastMessage`, and history `message/error`; the backend just was not populating those fields from Python executor results.
- Current Java mapping incorrectly points `market_sentiment_chain_refresh` to `rebuild_msi`, which only rebuilds the final MSI index over dates and does not execute the full old chain.
- Current Java `recordTriggerResult` only writes status/duration/error and a generic remark (`Triggered by Java ...`), so history rows have no row counts, run id, pipeline, or chain summary for the UI to display.

## Scheduler Progress JSON Field Findings

- `scheduler_jobs` is the static job definition table and should not own mutable runtime progress.
- Current runtime progress belongs in `scheduler_job_statuses.progress`, while completed run summaries belong in `scheduler_job_run_history.result_summary`.
- `extra` remains useful for backwards compatibility and legacy fields, but should no longer be the canonical UI progress contract.
- Both new columns are PostgreSQL `jsonb`; Java PO fields need `JsonbTypeHandler` and `autoResultMap = true` like existing `task_job_run.metadata`.
- The API should expose a stable progress envelope (`schemaVersion`, `type`, `status`, `percent`, `currentStep`, `completedSteps`, `stepResults`, `message`, `metrics`, `payload`, `updatedAt`) so job-specific details can live under `payload` without forcing page code to parse arbitrary top-level JSON.

## TDX Hsjday Var-Root Alignment Findings

- The old design document `F:\dev-repo\mp4-to-word-new\design\backend\tdx-hsjday-download-pipeline.md` defines the important flow as download zip, extract to a dated `hsjday-{date}` directory, verify, then replace the live `hsjday` directory.
- The old literal paths under `reference` should not be carried forward; the new Python project should map them under `E:\project-repo\cynexus\cynexus-data\var`.
- New canonical paths are:
  - zip: `cynexus-data\var\stock\download\{date}\hsjday.zip`
  - temporary extract: `cynexus-data\var\stock\download\{date}\hsjday_extract_tmp`
  - dated verified target: `cynexus-data\var\tdx\day\hsjday-{date}`
  - live target consumed by downstream daily sync: `cynexus-data\var\tdx\day\hsjday`
- The earlier new-project default wrote live files to `cynexus-data\var\tdx\hsjday`, while downstream sync could read a different root unless the chain passed an explicit `day_root`; this caused `tdx_hsjday_download` to report files but `initial_backfill_refresh` to read/write 0 rows.
- The current downloaded local data was moved from `var\tdx\hsjday` to `var\tdx\day\hsjday`; it contains 12,246 `.day` files, about 0.87 GB.
- TDX hsjday packages can include non-stock `.day` files such as `sz200b07.day`; local discovery must skip files that do not contain a 6-digit symbol instead of failing the whole chain.

## MSI Historical Percentile Repair Findings

- Old MSI semantics were mixed by design: some factor tables stored raw values, while the serving/MSI path converted those raw values into historical percentile scores.
- The migrated Python code had drifted for `price_strength`, `breadth`, `profit_effect`, and `limit_emotion` by feeding raw formula values into UI/MSI score fields.
- `msi_ma_count_daily` has both raw fields (`new_high_252d_pct`, `breadth_raw`) and score fields, so the producer can write percentile scores while the MSI assembler can still recompute from raw rows for resilience.
- `msi_profit_effect_daily.score` is the old raw formula value; final MSI and Java profit-effect display should prefer the percentile component score from `msi_index_daily.profit_effect_score`.
- `limit_emotion` must percentile-rank `limit_up_down_ratio`, inverse-rank `break_board_rate`, percentile-rank `yesterday_limit_up_avg_return`, then percentile-rank the weighted composite raw score.
- `market_sentiment_chain_refresh` must default the final `market_sentiment_index_refresh` step to `force=true` and `require_full=true`; otherwise the chain can finish upstream factor refresh but return a cached or partial MSI composite.
- The 2026-06-30 local MSI rerun showed `msi_sector_breadth_daily` currently has only 14 rows (`2026-06-09` through `2026-06-29`), so `require_full=true` can only safely rewrite those 14 MSI dates; older `msi_index_daily` rows remain unchanged until sector breadth history is backfilled.
- Direct ClickHouse rerun of recent `build_limit_emotion` hit `ATTEMPT_TO_READ_AFTER_EOF` from the limit-emotion OHLCV reader. For the rerun, existing PostgreSQL raw factor rows were used to batch recompute percentile score columns in `msi_ma_count_daily` and `msi_limit_emotion_daily`, then final MSI was rebuilt from PostgreSQL factors.

## QFQ Reconciliation Runtime Findings

- `sync_adjusted_daily` defaults to both `qfq` and `hfq` when no `adjust` is provided. In the market sentiment chain this made the `qfq_reconciliation_refresh` step plan about `active_code_count * 2` items even though the chain step is semantically QFQ-only.
- The old adjusted daily write path called `ClickHouseAdjustedDailyKlineSink.replace_daily` per stock. With key-scope replacement, every stock produces a synchronous ClickHouse mutation, which is expensive during full-market refresh.
- The chain resolves a single `target_date` from ClickHouse `daily_raw` and passes it to child steps as `trade_date/date/snapshot_date`, so QFQ reconciliation in this chain is a one-day refresh. Date-scope replacement is therefore appropriate and avoids per-code mutation pressure.
- The optimized shape is: fetch all QFQ frames for the target day, concatenate them, delete the target date range once from `daily_qfq`, then insert the combined frame in batches.
- `replace_scope="keys"` remains the default for direct adjusted-daily pipeline use, preserving existing behavior outside the chain.

## THS Industry Constituents Job Fallback Findings

- New system scheduler jobs already exist for `ths_industry_fund_flow_daily` and `ths_industry_constituents_daily` / `weekly`, but `sync_ths_industry_constituents` previously failed in live mode unless Core passed `industry_codes` or `codes`.
- The old Python constituents scheduler did not require external codes; it derived the 90 THS industry codes from `industry_list.json` / live industry-list fallback before refreshing constituents.
- The user accepted using previously stored industry/code data when the live list cannot be fetched, because THS industry codes are stable enough for this job.
- Current local `ths_industry_constituent_snapshot` has snapshot date `2026-06-26`, 4,663 rows, and 90 distinct industry codes.
- Before repair, latest local industry fund-flow rows for 2026-06-29, 2026-06-30, and 2026-07-01 each had 90 rows with 90 missing `sector_code` values.
- Historical industry fund-flow rows contain a full 90-row `sector_name -> sector_code` mapping, so the safe repair path is to update only alive `scope='industry'` rows where `sector_code` is null/empty and the name matches a historical mapped row.

## F10 Query Chain Migration Findings

- User confirmed the target architecture is `cynexus-ui -> cynexus-market -> cynexus-data`; UI must not call old Python F10 routes directly.
- `cynexus-ui/src/components/stock-detail-dialog.tsx` still imports all F10 data from `@/services/legacy/f10`, while stock K-line/meta/sector data already comes from `@/services/market/stock-chart`.
- The current dialog actually uses these F10 scopes:
  - `valuation`
  - `business-composition`
  - `finance-report`
  - `announcements`
  - `news`
  - `roadshows`
  - `company-news`
- The legacy Python F10 surface also includes `profit-forecast`, and it should be migrated with the same chain for completeness even if the dialog does not render it yet.
- Old Python F10 is a cached source-facing adapter layer over `eltdx`; cache lives under `reference/stock/cache/f10/<scope>/...`, but the user explicitly wants the new path to be service-to-service, not UI-to-Python.
- `cynexus-data` already has a single FastAPI internal app at `src/cynexus_data/interfaces/trigger_api/app.py` with `/internal/*` endpoints. There is no existing internal query router beyond job-trigger/status APIs.
- `cynexus-core` already demonstrates the preferred new-project cross-service pattern for Python calls: a typed domain client backed by a small infra HTTP client (`HttpDataJobExecutorClient`), not a frontend proxy or ad hoc controller shortcut.
- `cynexus-market` stock-chart query stack already uses the right layering for this page family:
  - controller: `StockChartQueryController`
  - app service: `StockChartQueryAppService`
  - domain repository: `StockChartQueryRepository`
  - infra repo/client: `StockChartQueryRepositoryImpl`
- `StockChartDTO` / `StockChartBO` currently have no F10 DTO/BO types, so the migration needs explicit typed contracts instead of reusing `Map`/`JsonNode`.
- Attempting the planning skill catch-up script at `C:\Users\Cs7er\.claude\skills\planning-with-files\scripts\session-catchup.py` failed because that path does not exist in this environment. The actual installed script is under `C:\Users\Cs7er\.agents\skills\planning-with-files\scripts\session-catchup.py`.

## Application Analysis Information Tab Findings

- The required Python -> Java path already exists for stock information: `cynexus-data` exposes typed internal F10 endpoints, and Market exposes typed public endpoints under `/api/market/stock-chart/f10/*`.
- Existing public endpoints cover announcements, stock news, company research/news, and roadshows through `StockChartQueryController -> StockChartQueryAppService -> StockChartQueryRepository -> HttpStockChartF10DataClient`.
- The page should consume this existing public contract through a dedicated Market service and must not reach Python directly or place request code in `src/lib/api.ts`.

## Viewable Information Feed Findings

- TDX F10 announcements include a PDF URL, but its news and company-research lists expose only metadata (`recId` / `docHash`). Its documented `CWServ.tdxf10_gg_idreq` detail call returned a success wrapper with only `[0]` for live news and research identifiers, so it cannot provide a reliable body or canonical link.
- The Python A-stock persistence pipeline already owns better sources: CNInfo announcements persist PDF URLs, EastMoney search news persists article URLs and summaries, EastMoney ReportAPI persists report identifiers, and CNInfo IRM persists question/answer text.
- The verified EastMoney stock-report detail convention is `https://data.eastmoney.com/report/zw_stock.jshtml?infocode=<report_id>`; a live `AP202605041821953310` request returned HTTP 200.
- `600415` currently has persisted CNInfo announcements and EastMoney reports but no persisted EastMoney news, confirming that watchlist warmup needs to include the message datasets.

## A Stock Data Source Integration Findings

- The requested `a-stock-data.md` capability set fits best as source/query integrations first; new persisted datasets should not be scheduled until each has a table contract and owner.
- EastMoney source calls now share a rate-limited helper with session reuse, jitter, retries, and proxy-disconnect fallback. This keeps the anti-ban behavior out of individual endpoint functions.
- Live smoke showed EastMoney reportapi, datacenter, search/news, global news, limit pools, hot concept, and concept blocks reachable from this environment.
- EastMoney `push2`, `push2his`, and `emappdata` endpoints were intermittently reset by upstream/proxy during smoke. This matches the skill's warning that EastMoney has IP/network-level risk; these failures should be treated as live-source reachability failures, not parser failures.
- Baidu K-line MA requires `https://finance.pae.baidu.com/selfselect/getstockquotation` with `Accept: application/vnd.finance-web.v1+json`, `Origin`, and `Referer`; the earlier `vapi/v1/getquotation` probe returned 403.
- THS EPS forecast, hot reason, northbound, limit-up pool, and hot list were all reachable in live smoke.
- CNInfo announcements and IRM were reachable; announcement orgId lookup must use the official `szse_stock.json` mapping before falling back to synthetic orgIds.
- Sina financial reports and ETF option contract-month discovery were reachable.
- iwencai integration is implemented but live smoke is skipped without `IWENCAI_API_KEY`.
- Optional mootdx quote/order-book helper returned a 46-field quote payload for `600519` in live smoke.

## A Stock Data Persistence Findings

- The A-stock persistence pass keeps the existing `cynexus-data` layering intact:
  - provider-specific fetching stays under `sources/*`;
  - row shaping is pure in `domain/stock/a_stock_persistence.py`;
  - orchestration and anti-scraping sleep live in `pipelines/a_stock/sync_a_stock_dataset.py`;
  - PostgreSQL and ClickHouse writes are isolated in `storage/postgres/a_stock.py` and `storage/clickhouse/a_stock.py`;
  - executor access is through `jobs/sync_a_stock_dataset.py`.
- PostgreSQL is the right target for compact UI/query snapshots: reports, announcements, news, IRM, daily fund-flow, industry ranking, dragon-tiger, lockup, block trades, holder counts, dividends, hot ranks/concepts/reasons, limit pools, option contracts, and concept membership.
- ClickHouse is the right target for minute/detail/high-frequency facts: EastMoney minute fund-flow, mootdx transaction/order-book snapshots, Baidu K-line MA, THS northbound minutes, and Sina option quote/Greeks snapshots.
- `sec_stock_basic` should be extended for EastMoney stock info rather than creating a duplicate stock-basic table.
- EastMoney 120-day fund-flow gets its own `a_stock_fund_flow_daily` table to preserve EastMoney source semantics instead of mixing it into the existing Tencent `stock_fund_flow_snapshot`.
- EastMoney industry ranking gets `a_stock_industry_rank_snapshot`, while existing THS/TDX sector/industry fund-flow tables remain separate source-specific contracts.
- Margin remains on the existing margin pipeline/tables; EastMoney margin route is a query/source capability and should not create a parallel primary table unless a future explicit source comparison view is needed.
- The local PostgreSQL DDL application succeeded after backup `deploy/local/postgres-backups/cynexus-before-a-stock-persistence-20260707-011519.sql`; 19 `a_stock_%` tables exist.
- ClickHouse rejects nullable columns in MergeTree sorting keys unless `allow_nullable_key` is enabled. `ch_ods_stock_transaction` was fixed to order by non-null `source, code, trade_date, trade_time`.
- `sync_a_stock_dataset` dry-run intentionally skips live provider calls unless `fetch_live=true`; this avoids accidental large crawls from scheduler testing.
- CLI `payload` and `payloads` parameters must be JSON-decoded when passed as strings; this is now covered by a regression test.
- Local Python ClickHouse smoke needs `CLICKHOUSE_PASSWORD=C020611.` in the environment unless the runtime uses the project connection registry.
- Knowledge-base docs were inserted into `cynexus_appl_knowledge_base.kb_document` as published documents:
  - `332571765114957824`: `A Stock 数据落库表设计`
  - `332571765354033152`: `Cynexus Data A 股能力全量地图`

## Settings Data Source Health Check Findings

- The health-check path follows the requested data flow: `cynexus-ui -> cynexus-core -> cynexus-data`; the UI service lives under `cynexus-ui/src/services/core/data-source-health.ts`, not global `src/lib/api.ts`.
- `cynexus-data` now exposes `GET /internal/data-sources/health`, which returns a source-level envelope with `summary` and `items`; each item has connectivity, authenticity status, latency, sample count, checked time, error message, structured checks, and raw payload metadata.
- The quick check intentionally skips slow/live TCP probes for AkShare and mootdx while still checking Python package availability for pytdx, eltdx, and mootdx. Full check enables the mootdx TCP quote probe.
- The local quick-check source inventory is currently 24 targets: PostgreSQL Market/Core, ClickHouse, DuckDB, TDX local filesystem, pytdx/eltdx/mootdx runtime packages, EastMoney Push2/Push2His/Datacenter/Report/Search/HotRank, Tencent, Sina, THS data/basic, exchange margin, Baidu, CNInfo, iwencai, AkShare, and mootdx TCP.
- Core persists health runs in `cynexus_appl_core.data_source_health_check_run` and per-source rows in `cynexus_appl_core.data_source_health_check_item`. Both use project-standard `BIGINT` application-generated IDs, audit columns, soft delete column, JSONB payload columns, and alive unique indexes.
- MyBatis-Plus maps the field `successRate24h` to `success_rate24h` by default, so `DataSourceHealthCheckItemPO` needs an explicit `@TableField("success_rate_24h")` annotation to match the table convention.
- Live source checks are inherently unstable; in the latest local smoke, EastMoney Push2His failed once with a remote disconnect while Sina root returned HTTP 404 and was marked degraded rather than healthy. These are stored as health results, not hidden as build failures.
- Frontend TypeScript verification remains blocked by unrelated existing errors in market pulse, industry application chart, market sentiment sparkline, and stock-overview market-pulse; the health page/service files did not appear in the TypeScript diagnostics.
## 2026-07-11 Application Analysis Stock Fund Flow

- `stock_fund_flow_snapshot` originally contained only three symbols for one date; `a_stock_fund_flow_daily` contained 120-day history for only two symbols.
- The enabled `a_stock_code_daily` job did request EastMoney 120-day flow for six self-selected symbols, but `ignore_errors=true` hid six `RemoteDisconnected` failures while the overall job was recorded as success.
- Tencent `qt.gtimg.cn` remained reachable and returned current active buy/sell, order-disk ratios, turnover, amount, price, and change for all six self-selected stocks.
- Tencent quote field index 30 carries the source quote timestamp and must determine `trade_date`; the job run date is not reliable on weekends or holidays.
- `600415` is the screenshot target: its 2026-06-26 TDX daily close was 9.76 and amount was 710,339,648, matching the reported empty-state screenshot.
- Tencent outer/inside volume fields are lots (100 shares). Active net amount in wan must be `(outer_lots - inside_lots) * price * 100 / 10000`; the initial parser omitted the lot-to-share multiplier and understated active net amount by 100x.

## 2026-07-11 EastMoney Fund Flow Resilience

- AkShare 1.18.46 uses the same `push2his.eastmoney.com/api/qt/stock/fflow/daykline/get` endpoint with `klt=101`, a fixed `ut`, and a timestamp; AkShare does not provide a separate transport path.
- The current local/container egress receives `RemoteDisconnected` from Push2His even with complete parameters, browser headers, proxy bypass, HTTP/HTTPS, and known numbered hosts.
- `push2delay.eastmoney.com` is reachable and returns full main/super/large/medium/small net fields, but exactly one latest row regardless of `lmt`, `beg`, or `end`.
- The safe collection model is Push2His for an explicit history backfill through a working fixed egress, Push2Delay for daily incremental collection, and Tencent for active-flow/quote supplements.
- A shared session is required for effective rate limiting. Creating an EastMoney client per stock reset the prior throttle timer and defeated anti-crawl pacing.
- The user-provided browser cURL proved Push2His can return 120 rows from this machine. The minimum successful session state was `qgqp_b_id` plus `nid18`; either cookie alone failed.
- Push2His uses JSONP when `cb` is present, so the collector must parse both JSON and JSONP. Browser-equivalent mobile request headers and those two cookies are read from environment variables only.
- Repeated verification requests eventually triggered temporary connection resets again, confirming that valid cookies do not remove rate limits; the circuit breaker and low-frequency scheduler remain necessary.
- A persistent Playwright profile can obtain the two required cookies without manual browser export. Keeping the profile and cookie cache under the mounted `cynexus-data/var` allows reuse across Data Executor recreation.
- Running a six-symbol history batch immediately after one successful 120-row request triggered throttling. History recovery must remain one missing symbol per low-frequency scheduled run; cookie refresh is not a substitute for request pacing.
- The verified runtime uses Debian `/usr/bin/chromium` rather than downloading Playwright Chromium during Docker build, which avoids Docker Desktop BuildKit stalls while preserving Playwright's persistent-context behavior.

## 2026-07-12 Viewable Information Feed

- Sina's server-rendered stock news page returns 40 dated article rows with canonical URLs for `600415`; it is suitable as a persisted, viewable fallback beside EastMoney search news.

## 2026-07-12 Information Content And AI Analysis Design

- The persisted Market information tables only contain list metadata and links today; no table owns canonical extracted content, content hashes, fetch status, or AI-output provenance.
- The AI service already has a model-routing and structured-completion foundation, so information analysis should be a distinct AI scenario/persistence contract rather than being folded into Application Analysis daily snapshots.
- `MediaTranscriptionAiAppService` already demonstrates prompt/version resolution, provider fallback, output parsing, and execution logging. The information-analysis service should reuse those mechanics but persist its own domain result and demand evidence chunk IDs.
- There is no Market-to-AI Feign contract today. A typed contract can be added in `cynexus-ai-api` without creating a Maven cycle because AI application already depends only on `cynexus-market-api`, not the Market application module.
- The current Data image has `requests` but no robust PDF/HTML extraction dependency. The planned extractor needs a focused dependency such as PyMuPDF for text PDFs and `trafilatura` for HTML; OCR should be an explicit later fallback rather than silently claiming scanned PDFs are readable.
- Market's existing information repository is JDBC-backed and already resolves the canonical source URL server-side. The new content operation can safely reuse this lookup before asking Data to fetch, so the browser never supplies a fetch URL.
- The shared `SnowflakeIdGenerator` is available in Market infrastructure for the new persisted cache rows. Data's existing F10 internal API and Market's `HttpStockChartF10DataClient` provide the exact transport/auth pattern for the new typed content-extraction client.
- The first implementation keeps extraction synchronous per explicit user action, but persists both success and failure states. This gives immediate reader feedback, a retry path, and a natural cache boundary before introducing a background worker.
- Sina's declared `ISO-8859-1` response charset is unreliable: its recent article pages contain valid UTF-8. Extraction now tries strict UTF-8 before GB18030 fallback and removes the standard Sina promotion/footer marker before hashing or AI analysis.
- `ScenarioExecutionAppService` had two existing generic-path limitations: it attempted to query a missing model-config table and `AgentExecutor.execute` returned synthetic content. The stock-information no-tool path now resolves a configured provider binding and invokes the actual adapter; its successful live result used the existing DeepSeek application-analysis binding.
