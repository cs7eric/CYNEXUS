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
