# Cynexus Initial Project Setup Plan

## Goal

Create the initial Cynexus Java 21 Maven multi-module, microservice-ready project structure described by `guide/TASKS.md` and `guide/PROJECT_BRIEF.md`.

The project must compile with:

```powershell
mvn clean package -DskipTests
```

## Phase 1 - Planning Files

Status: complete

- Capture guide requirements in persistent planning files.
- Note history-derived background only when needed.

## Phase 2 - Dependency and Toolchain Check

Status: complete

- Verify local Java and Maven availability.
- Select compatible Spring Boot 4.0.x, Spring AI 2.0.x, Spring Cloud 2025.1.x, Spring Cloud Alibaba 2025.1.0.0, MyBatis-Plus Spring Boot 4 starter, and JobRunr Spring Boot 4 starter versions.

## Phase 3 - Maven Structure

Status: complete

- Update root parent POM to `cn.cccs7:cynexus:1.0.0-SNAPSHOT`.
- Create aggregate modules:
  - `cynexus-common`
  - `cynexus-ai`
  - `cynexus-market`
- Create all requested child modules and nested application-controller modules.

## Phase 4 - Minimal Microservice Code

Status: complete

- Add starter classes:
  - `cn.cccs7.cynexus.ai.CynexusAiApplication`
  - `cn.cccs7.cynexus.market.CynexusMarketApplication`
- Enable Feign clients for AI.
- Add application.yml files with Nacos config import, service names, ports, datasource, Redis, and actuator exposure.
- Add market API DTOs and Feign client.
- Add market controller mock endpoints.
- Add AI infra tool/client adapter stubs that depend on market API.
- Add core/common helper classes only where needed for compilation.
- Add `docker-compose.core.yml` for PostgreSQL, Redis, and Nacos only.

## Phase 5 - Verification

Status: complete

- Run `mvn clean package -DskipTests`.
- Fix compile or dependency issues until the build succeeds, or record any external blocker.

## Explicit Non-Goals

Do not create: `cynexus-document`, `cynexus-strategy`, `cynexus-gateway`, `cynexus-admin`, `cynexus-oss`, `cynexus-user`, `cynexus-iam`, Jenkins config, Elasticsearch config, RabbitMQ config, Prometheus config, or Grafana config.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| `guide/PROJECT_BRIEF .md` not found | Read exact user-provided path | Real file is `guide/PROJECT_BRIEF.md`; read that file. |
| `rg --files` access denied from bundled rg.exe | Initial file listing | Use PowerShell `Get-ChildItem` fallback for local inspection. |
| `spring-ai-tools` dependency version missing | First Maven build | Removed unused artifact; Spring AI BOM remains in dependency management for later provider-specific starters. |

## Verification Results

- `mvn clean package -DskipTests` succeeded on 2026-06-26.
- Reactor built 23 projects successfully.
- Root directories checked; no forbidden first-phase modules were created.

## Phase 6 - Fill First-Phase Content From History

Status: complete

- Add common baseline content from history: result/page/exception/error/trace/json/base enum/web/db/cloud helpers.
- Add AI model/scenario/prompt/tool/execution concepts and service placeholders.
- Add AI adapter, factory, tool registry, secret resolver, observability placeholders.
- Add market overview/sector/stock feature DTOs, domain objects, app services, and controllers.
- Add PostgreSQL schema samples and Nacos config samples.
- Re-run Maven verification.

## Phase 6 Verification Results

- `mvn clean package -DskipTests` succeeded on 2026-06-26 after filling first-phase content.
- Java/YAML/SQL content files under common/AI/market/infrastructure now total 86 files.

## Phase 7 - Folder Convention Refactor

Status: complete

- Follow `guide/folder-convention.md`.
- Move common files into `result`, `exception`, `id`, `enums`, `util`, `constant`, `advice`, `filter`, `config`, and security/db/cloud subpackages.
- Move AI application services into `application.service`, add `command` and `assembler`.
- Move AI infra adapter/factory/registry into `infra.ai.*`, add persistence placeholders.
- Move market application services into `application.service`, add API request, domain repository, infra persistence/datafeed placeholders.
- Move root infrastructure files to `deploy/docker`, `deploy/nacos`, and `sql/*`.
- Re-run Maven verification.

## Phase 7 Verification Results

- `mvn clean package -DskipTests` succeeded after the folder-convention refactor.
- Removed empty legacy `infrastructure`, `infra/adapter`, and `infra/factory` directories.
- Root now uses `deploy/docker`, `deploy/nacos`, and `sql` for deployment/config/database assets.

## Phase 8 - Layer and Object Convention Audit

Status: complete

- Follow `guide/layer-and-object-convention.md`.
- Keep common-core free of MVC, MyBatis, Redis, Nacos, Feign, and Spring AI dependencies.
- Add explicit `Result`, `ResultCode`, layered exception classes, `BaseEnum`, `BasePO`, `BaseMapperX`, and meta fill handler.
- Convert domain service classes into interfaces plus `service/impl` implementations.
- Move AI domain business objects to `domain/entity/*BO` and value objects to `domain/valueobject`.
- Add AI ORM path: `basic/entity`, `basic/mapper`, `basic/service`, `basic/service/impl`, XML mapper, infra repository, and PO-to-BO converter.
- Use domain repository interfaces from application services instead of hard-coded model candidates.
- Normalize controllers to return `Result<DTO>` or `Result<List<DTO>>` instead of raw DTO/List/Map.
- Move Market overview through `Request -> Controller -> AppService -> BO -> Assembler -> DTO -> Result`.
- Normalize Market sector/stock endpoints to return DTO objects assembled from BOs.

## Phase 8 Verification Results

- `mvn clean package -DskipTests` succeeded after the layer/object convention audit.
- Searched domain modules for infra/mapper/PO/Feign/Redis/Spring AI/MVC references; no violations found.
- Searched common-core for MVC/MyBatis/Redis/Nacos/Feign/Spring AI references; no violations found.
- Searched AI/Market controllers for old response contracts and old AI domain package imports; no violations found.

## User Decisions

- Keep Maven `groupId` as `cn.cccs7`.
- Keep Java package root as `cn.cccs7.cynexus`.

## Phase 9 - Preserve Existing Local PostgreSQL In Compose

Status: complete

- Identify the running manual Postgres container and its actual data volume.
- Create a logical backup before changing compose.
- Add the existing Postgres 18 volume to `deploy/local/docker-compose.yml` as an external volume.
- Keep the original manual Postgres container and data intact.
- Validate compose syntax and document the safe switch procedure.
- Put the compose Postgres service behind the `postgres` profile so regular Nacos/Redis startup does not conflict with the still-running original Postgres container.

## Phase 10 - Create Project-Owned Local PostgreSQL

Status: complete

- Change local compose Postgres to use `./postgres/data:/var/lib/postgresql`, matching Redis-style bind-mounted local data.
- Use host port `25433` so the project Postgres can run beside the original `postgres` container on `25432`.
- Update project datasource defaults to `25433`.
- Start the project Postgres container and import data from the original container.
- Keep the original `postgres` container and its anonymous volume intact.
- Verify imported table counts and Maven build.

## Phase 11 - Rename Project Default Database To Cynexus

Status: complete

- Change local compose `POSTGRES_DB` from `postgres` to `cynexus`.
- Change AI, Market, and shared Nacos datasource defaults from database `postgres` to `cynexus`.
- Create the `cynexus` database in the already-running project Postgres.
- Copy existing imported data from database `postgres` to `cynexus`.
- Verify table and row counts against the copied source.
- Re-run Maven verification.

## Phase 12 - Run Applications From Local Docker Compose

Status: complete

- Add an application Dockerfile for Java 21 Spring Boot starter jars.
- Make AI and Market starter jars executable with Spring Boot repackage.
- Add `market` and `ai` services to `deploy/local/docker-compose.yml`.
- Use compose service names for container-to-container Nacos, PostgreSQL, and Redis connections.
- Keep host port mappings for AI `18081` and Market `18082`.
- Validate compose config, build images, start services, and check health endpoints.
- Stop the previous IntelliJ-launched Java processes that were occupying ports `18081` and `18082`, then let Docker Compose own those ports.

## Phase 13 - Split Service Schemas And Migrate IDs To BIGINT

Status: complete

- Treat the current `cynexus` PostgreSQL database as authoritative.
- Back up the project-owned database before migration.
- Move AI-owned tables into schema `cynexus_appl_ai`.
- Move Market-owned tables into schema `cynexus_appl_market`.
- Convert service-owned business primary keys from UUID to `BIGINT NOT NULL PRIMARY KEY`.
- Preserve existing rows and remap service-owned foreign-key style columns consistently.
- Leave third-party/public infrastructure tables alone unless explicitly required.
- Configure each service datasource with its own `currentSchema`.
- Rebuild/start compose applications and verify health.

## Phase 13 Verification Results

- Fresh backup was written before migration: `deploy/local/postgres-backups/cynexus-before-schema-bigint-20260626-153602.sql`.
- `cynexus_appl_ai` contains 7 AI-owned business tables.
- `cynexus_appl_market` contains 14 Market-owned business tables.
- All 21 service-owned `id` columns are `BIGINT NOT NULL` with primary keys and no database default.
- No UUID columns remain in `cynexus_appl_ai` or `cynexus_appl_market`.
- Existing business row counts were preserved after the schema move and ID remap.
- `docker compose -f deploy/local/docker-compose.yml config` succeeded.
- `mvn clean package -DskipTests` succeeded.
- `cynexus-ai` and `cynexus-market` both run from Docker Compose and report actuator health `UP`.

## Phase 14 - Add Core Service And Move Scheduler Schema

Status: complete

- Add `cynexus-core` as a Maven aggregate module following the same AI/Market module layout.
- Add minimal Core API/common/domain/infra/application-controller/starter code according to the guide folder and layer conventions.
- Register `cynexus-core` with Nacos and local Docker Compose.
- Configure Core datasource with `currentSchema=cynexus_appl_core` and Redis defaults.
- Back up the project-owned database before moving data.
- Move scheduler-related tables from `cynexus_appl_market` to `cynexus_appl_core`.
- Add service-specific Nacos config for Core.
- Build, start, and verify Core together with AI/Market.

## Phase 14 Verification Results

- `mvn clean package -DskipTests` succeeded across 31 Maven reactor projects.
- Fresh backup was written before scheduler schema migration: `deploy/local/postgres-backups/cynexus-before-core-scheduler-schema-20260626-162425.sql`.
- `cynexus_appl_core` contains all 5 scheduler tables.
- `cynexus_appl_market` contains 9 business tables and no `scheduler_%` tables.
- Local Docker Compose runs `cynexus-core`, `cynexus-ai`, and `cynexus-market` together.
- Health endpoints return `UP` for Core `18080`, AI `18081`, and Market `18082`.
- Core service info endpoint `http://127.0.0.1:18080/api/core/info` returns service name `cynexus-core` and schema `cynexus_appl_core`.

## Phase 15 - Complete Table CRUD And Lombok/MapStruct Cleanup

Status: complete

- Add application-layer DTO/BO converters for AI table DTOs with `INSTANCE = Mappers.getMapper(...)`.
- Add `AiConfigurationCrudAppService` and `AiConfigurationCrudController` for AI providers, usage bindings, and application analysis tables.
- Add application-layer DTO/BO converters for Market table DTOs with `INSTANCE = Mappers.getMapper(...)`.
- Add `MarketDataCrudAppService` and `MarketDataCrudController` for market overview snapshots, limit snapshots/stocks, pulse batches/snapshots, sector fund-flow snapshots, and self-selected lists/items.
- Change `MarketOverviewBasicService` from mock data to real MyBatis-Plus CRUD plus `findByTradeDate`.
- Add missing `MarketOverviewMapper.xml`.
- Replace remaining old record-style DTO accessors in AI/Market application code with Lombok getters.
- Add Core scheduler audit fields to BO/DTO classes so PO/BO/DTO table fields align.

## Phase 15 Verification Results

- `mvn compile -DskipTests` succeeded after the CRUD and converter additions.
- Converter audit found no `*Converter.java` without `INSTANCE = Mappers.getMapper(...)`.
- Mapper XML audit found no PO without a matching `*Mapper.xml`.
- No Java `record` declarations remain in AI, Market, or Core modules.
- Final `mvn clean package -DskipTests` succeeded across all 31 Maven reactor projects.

## Phase 16 - Add Microservice Gateway Entrypoint

Status: complete

- Add `cynexus-gateway` as a Maven microservice module with `cynexus-gateway-starter`.
- Use Spring Cloud Gateway WebFlux and Nacos service discovery.
- Route `/api/core/**`, `/api/ai/**`, and `/api/market/**` through service names with `lb://`.
- Register Gateway in local Docker Compose on `127.0.0.1:18000`.
- Include Gateway in `deploy/local/up-latest.ps1` so local reruns package and recreate the latest gateway image.
- Add a Nacos config sample for `cynexus-gateway.yml`.
- Align `market_overview_snapshots` PO/BO/DTO/converters with the real database table so the documented gateway Market API works.

## Phase 16 Verification Results

- `mvn clean package -DskipTests` succeeded across 33 Maven reactor projects after adding Gateway.
- `docker compose -f deploy/local/docker-compose.yml config` succeeded.
- `deploy/local/up-latest.ps1` rebuilt and recreated Core, Market, AI, and Gateway.
- Gateway container `cynexus-gateway` is healthy on `127.0.0.1:18000`.
- Verified through Gateway:
  - `GET /actuator/health` returns `UP`.
  - `GET /api/core/info` returns Core service info.
  - `GET /api/ai/config/providers` returns AI provider rows.
  - `GET /api/market/data/overview-snapshots` returns Market overview rows.

## Phase 17 - Collector Migration Inventory And Safe Bridge

Status: in_progress

- Inspect old project `F:\dev-repo\mp4-to-word-new` without modifying it.
- Identify Python-only capabilities that should become `cynexus-collector`.
- Identify Java-owned capabilities that should not be copied back into Python.
- Document large model/data/runtime directories that must become external mounts.
- Propose the first safe collector microservice contract through Nacos and Gateway.
- Do not migrate old code until the inventory boundary is clear.

## Phase 18 - DuckDB To ClickHouse Migration Foundation

Status: complete

- Add a local Docker Compose ClickHouse service.
- Persist ClickHouse data and logs under `deploy/local/clickhouse`.
- Keep the ClickHouse container independent from Java application startup for now.
- Validate ClickHouse starts and accepts queries.
- Do not migrate DuckDB table data in this phase.

## Phase 18 Verification Results

- `docker compose -f deploy/local/docker-compose.yml config` succeeded.
- `docker compose -f deploy/local/docker-compose.yml up -d clickhouse` pulled and started ClickHouse.
- `cynexus-clickhouse` is healthy.
- ClickHouse version is `25.8.24.21`.
- Database `cynexus` was created by container initialization.

## Phase 19 - Create PostgreSQL DuckDB Target Tables

Status: complete

- Create PostgreSQL target tables in schema `cynexus_appl_market` for the selected DuckDB market tables.
- Apply the requested name mapping:
  - MSI tables use `msi_*`.
  - General market result tables use `mkt_*`.
  - Security master data tables use `sec_*`.
- Follow current PostgreSQL table conventions:
  - `id BIGINT NOT NULL PRIMARY KEY`
  - no database default for `id`
  - application/migration generated distributed IDs
  - natural DuckDB keys represented as alive-only unique indexes
  - `extra jsonb`, `remark`, `created_at`, `updated_at`, and `deleted_at`
- Back up the local `cynexus` database before applying DDL.
- Validate table creation, id column shape, unique indexes, row counts, and idempotent rerun.
- Do not migrate DuckDB rows in this phase.

## Phase 19 Verification Results

- Fresh backup was written before DDL: `deploy/local/postgres-backups/cynexus-before-duckdb-target-tables-20260626-213927.sql`.
- Migration file added: `deploy/local/postgres/migrations/20260626_create_market_duckdb_target_tables.sql`.
- 15 target tables now exist in `cynexus_appl_market`.
- All 15 target `id` columns are `bigint NOT NULL` with no database default.
- All 15 target tables are currently empty.
- All requested natural-key unique indexes exist.
- Re-running the migration succeeded with only `already exists` notices.

## Phase 20 - Migrate Remaining DuckDB Tables To ClickHouse

Status: complete

- Identify DuckDB tables not mapped to PostgreSQL.
- Create ClickHouse tables for remaining market detail/time-series/state data.
- Keep DuckDB original table names in ClickHouse for simple migration and reconciliation.
- Use MergeTree tables with yearly partitions for long-history daily data.
- Use Parquet export/import from DuckDB to ClickHouse.
- Migrate and reconcile:
  - `daily_raw`
  - `daily_qfq`
  - `daily_hfq`
  - `adj_factors`
  - `index_daily_raw`
  - `ingest_state`
  - `intraday_bars`
  - `quotes`
  - `schema_meta`

## Phase 20 Verification Results

- ClickHouse DDL file added: `deploy/local/clickhouse/migrations/20260626_create_duckdb_market_tables.sql`.
- Migration script added: `deploy/local/clickhouse/scripts/migrate_duckdb_to_clickhouse.py`.
- ClickHouse data/log storage was changed from Windows bind mounts to Docker named volumes because MergeTree inserts failed on bind-mounted atomic renames.
- All 9 remaining DuckDB tables now exist in ClickHouse database `cynexus`.
- DuckDB and ClickHouse row counts match for all 9 migrated tables:
  - `daily_raw`: 28,516,739
  - `daily_qfq`: 28,482,626
  - `daily_hfq`: 28,482,612
  - `index_daily_raw`: 5,012
  - `ingest_state`: 12,037
  - `schema_meta`: 2
  - `adj_factors`: 0
  - `intraday_bars`: 0
  - `quotes`: 0
- Date ranges:
  - `daily_raw`, `daily_qfq`, `daily_hfq`: 1990-12-19 through 2026-06-26
  - `index_daily_raw`: 2018-03-21 through 2026-06-26

## Phase 21 - Market Pulse Python Data Backfill And Previous Trading Day Display

Status: complete

- Trace old Python Market Pulse, limit emotion, heatmap, and index intraday endpoints to their raw data files/interfaces.
- Compare old data dates with current Java/PG responses.
- Import available latest old data into `cynexus_appl_market` tables used by Java `/api/market/pulse/**`.
- Make Java Market Pulse reads use the latest available trading day, so non-trading days display the previous trading day.
- Verify Gateway endpoints return data for the latest available trading day.

## Phase 22 - Market Pulse Layering Repair

Status: complete

- Refactor Java Market Pulse query APIs to follow `infra/cynexus-layering-and-dataflow.md`.
- Move PostgreSQL SQL from Java strings into MyBatis Mapper XML.
- Replace repository/application/controller `Map<String,Object>` contracts with typed BO/DTO/`Result<T>` contracts.
- Keep ClickHouse access isolated behind an infra client with typed rows.
- Move Market Pulse frontend API wrappers out of global `src/lib/api.ts` into the page module.
- Verify Java compile and frontend import/type impact.

## Phase 22 Verification Results

- Market Pulse controller methods now return concrete `Result<MarketPulse*DTO>` contracts, with no `Result<JsonNode>`.
- Manual fund-flow save now accepts `MarketPulseManualFundFlowRequest`, not `JsonNode`.
- Market Pulse application service assembles typed DTOs from BOs and parses JSONB payload strings into typed DTOs only inside application logic.
- PostgreSQL reads stay behind `MarketPulseQueryMapper.xml` / BasicService / RepositoryImpl; no `JdbcTemplate` or inline PG SQL remains in the Market Pulse query path.
- `mvn -pl cynexus-market/cynexus-market-starter -am package -DskipTests` succeeded.
- Rebuilt/recreated the local `cynexus-market` container; it is healthy behind Gateway.
- Verified Gateway endpoints return business DTO fields, not Jackson node metadata:
  - `/api/market/pulse/market-overview-akshare`: `tradingDate=2026-06-26`
  - `/api/market/pulse/limit-emotion`: `tradeDate=2026-06-26`, `limitUp=57`, `limitDown=35`
  - `/api/market/pulse/style-sectors`: `count=29`
  - `/api/market/pulse/heatmap?kind=industries`: `count=90`, first `change_pct=2.6545`
  - `/api/market/pulse/heatmap?kind=concepts`: `count=200`, first `change_pct=-1.8299`
  - `/api/market/pulse/index-kline/batch`: `date=2026-06-26`, 3 index items, first item 240 points

## Phase 23 - Stock Overview Industry Application API Migration

Status: complete

- Follow `docs/migration-guides/python-to-java-api-migration-guide.md`.
- Migrate `F:\dev-repo\mp4-to-word-new\frontend\src\views\industry-application` API usage from old Python `/api/stock-chart/industry-application/**` to Java/Gateway.
- Review old Python route/service/store logic before implementation.
- Keep Java implementation layered: Controller -> AppService -> DomainService/Repository -> Infra BasicService/Mapper XML -> DB/ClickHouse.
- Use explicit Request/DTO/BO/PO contracts; no controller/app-service `Map` or `JsonNode` public contracts.
- Put frontend Java API client under `F:\dev-repo\mp4-to-word-new\frontend\src\services\market`, not page-local or global `src/lib/api.ts`.
- Preserve UI contract compatibility where practical, but move field normalization into the service layer.

## Phase 23 Verification Plan

- Java compile/package for impacted service modules: complete.
- Frontend type check or build for impacted frontend: attempted; blocked by existing unrelated TS errors in `calendar.tsx`, global `src/lib/api.ts`, `poc/sentiment-overlay`, self-selected, and stock-overview market-pulse files. The new `src/services/market/industry-application.ts` did not produce reported TS errors.
- Direct Java/Gateway endpoint smoke checks for targets, target-codes, kline/result, overview/heatmap: complete.
- Confirm the page imports the new `src/services/market/...` client and no longer imports industry-application functions from global `src/lib/api.ts`: complete.

## Phase 24 - Industry Application Constituents Drawer Data Repair

Status: complete

- Diagnose right-side constituents drawer after clicking Industry Application fund-flow / heatmap items.
- Import persisted THS industry constituent reference snapshots into `cynexus_appl_market.ths_industry_constituent_snapshot`.
- Repair `sector_fund_flow_daily_snapshots.sector_code` so fund-flow rows carry THS `881xxx` codes for drawer lookup.
- Move Industry Application heatmap drill-down constituent fetch from old global Python `src/lib/api.ts` to Java/Gateway service `src/services/market/pulse.ts`.
- Keep the shared drawer title stable when Java returns code as the data name.

## Phase 24 Verification Results

- Imported 90 THS industries and 4,663 constituent rows for snapshot date `2026-06-26`.
- Repaired 1,080 industry fund-flow rows; remaining missing `sector_code` rows: 0.
- Gateway `/api/market/industry-application/fund-flow?top=10` returns THS codes including `881270`, `881121`, `881129`, `881157`, and `881130`.
- Gateway `/api/market/pulse/industries/881270/constituents` returns `count=62`, `matched=62`, `snapshotDate=2026-06-26`.
- `python deploy/local/postgres/scripts/import_ths_industry_constituents.py --dry-run` succeeded.
- `python -m py_compile deploy/local/postgres/scripts/import_ths_industry_constituents.py` succeeded.
- Frontend `tsc -b --pretty false` still fails on existing unrelated files (`calendar.tsx`, global `src/lib/api.ts`, `poc/sentiment-overlay`, self-selected, stock-chart, stock-overview market-pulse); no Industry Application page/component errors remain.

## Phase 24 - Stock Overview Application Analysis API Migration

Status: complete

- Follow `docs/migration-guides/python-to-java-api-migration-guide.md`.
- Migrate `F:\dev-repo\mp4-to-word-new\frontend\src\views\application-analysis` application-analysis API calls from old Python `/api/stock-chart/application-analysis/**` endpoints to Java/Gateway.
- Keep frontend Java client under `frontend/src/services/ai/application-analysis.ts`, not `src/lib/api.ts`.
- Java implementation must follow Controller -> AppService -> Domain Repository -> Infra BasicService/Mapper XML -> DB.
- SQL belongs in MyBatis XML; public controller results must be explicit DTOs wrapped in `Result`.
- Support page contracts for targets, target save, current result, history metadata, scheduler status/control, trigger status, and recent30 snapshots.
- Scope note: the page also calls old stock-chart K-line APIs through `fetchStockKlines`; Java does not yet have a general stock K-line page API, so that dependency must be recorded as remaining work instead of silently hidden.

### Phase 24 Acceptance

- No application-analysis imports from frontend `src/lib/api.ts` remain in the page scope except unrelated stock-chart K-line types/helpers if explicitly documented.
- Java AI service compiles with the new typed DTO/request/controller/app-service path.
- Gateway/AI endpoint smoke checks return application-analysis target/result/recent30 data from `cynexus_appl_ai`.
- Frontend type check is attempted; any failures are classified as in-scope or pre-existing.

### Phase 24 Verification Results

- Java AI compile succeeded: `mvn -pl cynexus-ai/cynexus-ai-starter -am compile -DskipTests`.
- Java AI package succeeded: `mvn -pl cynexus-ai/cynexus-ai-starter -am package -DskipTests`.
- Started local `cynexus-ai` jar on `127.0.0.1:18081`; did not start Cynexus applications through Docker.
- Direct AI endpoint smoke:
  - `/api/ai/application-analysis/targets`: success=true, items=4, horizon.days=120.
  - `/api/ai/application-analysis/results/stock-600021`: success=true, `analysis_result` present, history=20.
  - `/api/ai/application-analysis/recent30/stock-000636/full?limit=5`: success=true, ok=true, items=2, firstDate=2026-06-20, `short_term_trend` present.
- Gateway smoke:
  - `/api/ai/application-analysis/targets`: success=true, items=4.
  - `/api/ai/application-analysis/refresh`: success=true, ok=false with explicit `Java application-analysis executor is not connected yet`.
  - `/api/ai/application-analysis/scheduler`: success=true, running=false, enabled=0, total=4, runs=4.
  - `/api/ai/application-analysis/recent30/refresh`: success=true, ok=false with explicit executor-not-connected status.
  - `POST /api/ai/application-analysis`: `Result.fail` with code `AI_APPLICATION_ANALYSIS_EXECUTOR_NOT_CONNECTED`.
- Frontend `tsc -b --pretty false` was attempted and still fails on existing unrelated files: `calendar.tsx`, global `src/lib/api.ts`, `poc/sentiment-overlay`, self-selected, stock-chart chart panel, and old stock-overview market-pulse notification usage. No new error was reported for `src/services/ai/application-analysis.ts` or `views/application-analysis`.
- Residual scope: `fetchStockKlines`, stock intraday, stock annotations, auction, technical indicator, and fund-flow subcomponents still call old stock-chart APIs. They are not application-analysis persistence APIs and need a separate Java stock-chart migration.

## Phase 25 - Application Analysis Stock Chart Runtime Repair

Status: complete

- Follow `docs/migration-guides/python-to-java-api-migration-guide.md`.
- Repair the remaining `/stock-overview/application-analysis` stock-chart runtime dependencies that made the page partially non-working after AI API migration.
- Add a typed Market stock-chart query path: Controller -> AppService -> Domain Repository -> Infra Repository -> TDX/ClickHouse/PG.
- Move intraday/timeshare and minute K queries to Java Market API backed by ClickHouse `ch_dwd_kline_1m`.
- Keep SQL templates under `cynexus-market-infra/src/main/resources/clickhouse/stock-chart`, not inline in Java.
- Keep public API contracts as typed DTOs; avoid `Map`/`JsonNode` on stock-chart controller results.
- Add a documented stock fund-flow fallback: if `stock_fund_flow_snapshot` is empty, derive amount/change history from local TDX daily bars while leaving true active fund-flow fields null and marking source as `java-tdx-local-daily-fallback`.

## Phase 25 Verification Results

- `mvn -pl cynexus-market/cynexus-market-starter -am compile -DskipTests` succeeded.
- `mvn -pl cynexus-market/cynexus-market-starter -am package -DskipTests` succeeded.
- Restarted local `cynexus-market` jar on `127.0.0.1:18082`; did not use Docker for Cynexus applications.
- Gateway `GET /api/market/stock-chart/intraday?target_type=stock&symbol=600021&trade_date=2026-06-26&periods=1m,5m,15m,30m` returns `source=clickhouse.ch_dwd_kline_1m`, `timeshare=241`, `1m=241`, `5m=50`.
- Gateway `GET /api/market/stock-chart/klines?target_type=stock&symbol=600021&period=1d` returns 520 daily bars through `2026-06-26`, latest close `15.04`.
- Gateway `GET /api/market/stock-chart/fund-flow?symbol=600021` returns `ok=true`, 30 fallback history rows, `tradeDate=2026-06-26`, and source `java-tdx-local-daily-fallback`.
- Search over the application-analysis page/service scope found no remaining `@/lib/api`, old `/api/stock-chart`, `localhost:5000`, or `VITE_API_BASE` references.
- Frontend `npx tsc -b --pretty false` was attempted; it still fails on pre-existing unrelated files (`calendar.tsx`, global `src/lib/api.ts`, `poc/sentiment-overlay`, self-selected, stock-chart chart panel, and old stock-overview market-pulse notification usage).

## Phase 26 - Core Scheduler Runtime And Python Executor Bridge

Status: complete

- Start the local `cynexus-data` FastAPI trigger API so Core can reach the Python job executor on port `18083`.
- Implement a Core scheduler runtime that automatically triggers enabled scheduler jobs when their persisted schedule is due.
- Support existing schedule JSON shapes: `run_time`, `cron`, `every_minutes`, `intraday_windows`, `workday_only`, `run_once_per_day`, and `run_once_per_week` where practical.
- Add missing scheduler-to-Python job mappings for `application_analysis`, `auction_ai_analysis`, and `daily_eod_incremental` if corresponding Python jobs exist.
- Preserve manual UI trigger behavior and verify it works through Gateway once the Python executor is running.
- Rebuild/restart affected local Java services and run targeted smoke checks.

### Phase 26 Verification Results

- Started local `cynexus-data` on `127.0.0.1:18083`; direct `/internal/health` and Gateway `/api/core/scheduler/data-jobs/health` both returned success.
- Rebuilt and restarted local Core with scheduling enabled.
- Added Core automatic scheduled triggering for enabled jobs with mapped Python executors; supported schedule shapes include `run_time`, `cron`, `every_minutes`, `intraday_windows`, `workday_only`, `run_once_per_day`, and `run_once_per_week`.
- Added safe Python executor mapping for `auction_ai_analysis` to `sync_tdx_intraday_detail` with `datasets=["auction"]`; left `application_analysis` and deprecated `daily_eod_incremental` unmapped because no corresponding Python data job exists in `cynexus-data`.
- Fixed manual trigger runtime by starting the Python trigger API and wiring Core to pass mapped job keys/default params/target-date aliases.
- Gateway manual trigger smoke passed for `test_scheduler_demo` with `status=success` and Python `run_id`.
- Gateway manual dry-run smoke passed for `auction_ai_analysis` with `status=success` and Python `run_id`.
- `mvn -pl cynexus-core/cynexus-core-starter -am package -DskipTests` succeeded; existing duplicate-Lombok and MapStruct warnings remain unrelated.

## Phase 27 - Scheduler Chain Progress And Run History Data

Status: complete

- Inspect the old Python project scheduler/chain implementation for progress events, step history, and result summaries.
- Repair Java/Core + `cynexus-data` trigger integration so manual/auto runs expose meaningful success/failure, step progress, and output metrics instead of only a bare run record.
- Treat `market_sentiment_chain_refresh` as a chain job, not a single opaque Python task.
- Preserve UI compatibility where practical so existing scheduler history/progress panels can render useful data without a large redesign.
- Rebuild/restart affected local services and smoke-check trigger, progress, and history endpoints.

### Phase 27 Verification Results

- Added a `cynexus-data` Python job for `market_sentiment_chain_refresh` that runs the 14-step old Python MSI chain and publishes `lastStep`, `lastCompletedSteps`, `lastStepResults`, `lastMessage`, and row counters.
- Fixed the Core mapping so `market_sentiment_chain_refresh` triggers the chain job instead of the old single `rebuild_msi`/MSI-only task.
- Changed Core manual trigger handling for the chain to return `processing` immediately, keep a processing history row, run Python in the background, and then update status/history with success/failure, run id, rows, completed steps, and message details.
- Added narrow scheduler status update SQL paths so progress/result updates can safely write JSONB `extra` without generic entity update JSONB type failures.
- Restarted local `cynexus-data` on `127.0.0.1:18083` and local Core on `127.0.0.1:18080`.
- Health checks passed: Core actuator `UP`, `cynexus-data` `/internal/health` ok, and Gateway `/api/core/scheduler/data-jobs/health` ok.
- Gateway dry-run trigger for `market_sentiment_chain_refresh` returned `processing`, then completed with status `success`, 14 completed steps, and `live.lastMessage=completed 14 MSI steps`.
- Latest Gateway history for `market_sentiment_chain_refresh` now shows a meaningful message plus `target_count=14` and `succeeded=14`.
- Java compile/package passed for Core: `mvn -pl cynexus-core/cynexus-core-starter -am compile -DskipTests` and `mvn -pl cynexus-core/cynexus-core-starter -am package -DskipTests`.
- Python compile passed for the changed `cynexus-data` scheduler/job files.

## Phase 28 - Scheduler Progress JSON Fields

Status: complete

- Add an explicit current-progress JSONB field to `cynexus_appl_core.scheduler_job_statuses` instead of relying on the generic `extra` field.
- Add an explicit per-run result JSONB field to `cynexus_appl_core.scheduler_job_run_history` instead of relying only on textual `remark`.
- Keep `extra` and existing `config.lastStep/live.lastMessage` response fields as compatibility fallback during UI migration.
- Update Java PO/BO/DTO/Mapper/XML/AppService paths so scheduler status writes populate `progress`, and run history writes populate `result_summary`.
- Update Scheduler frontend types and page logic so it reads `job.progress` first and falls back to old `config.lastStep/lastCompletedSteps/lastStepResults`.

### Phase 28 Verification Results

- Applied `deploy/local/postgres/migrations/20260629_add_scheduler_progress_fields.sql` to local PostgreSQL; verified `scheduler_job_statuses.progress` and `scheduler_job_run_history.result_summary` are `jsonb`.
- Core compile passed: `mvn -pl cynexus-core/cynexus-core-starter -am compile -DskipTests`.
- Core package passed: `mvn -pl cynexus-core/cynexus-core-starter -am package -DskipTests`.
- Restarted local Core jar on `127.0.0.1:18080`; did not start Cynexus apps through Docker.
- Gateway trigger smoke for `test_scheduler_demo` succeeded and returned Python run id `job_run_439841f43fe34425aca5f5ade276c40b`.
- Gateway jobs response for `test_scheduler_demo` includes `progress.status=success`, `progress.percent=100`, `progress.metrics.rowsWritten=0`, and `progress.payload.lastDataJobKey=trigger_probe`.
- Gateway history response for `test_scheduler_demo` includes `result_summary.status=success`, rows metrics, and the Python run id payload.
- Frontend `npx tsc -b --pretty false` was attempted; it still fails on pre-existing unrelated TypeScript errors in `calendar.tsx`, global `src/lib/api.ts`, `poc/sentiment-overlay`, self-selected, stock-chart, and stock-overview market-pulse. No scheduler-specific error remains.

## Phase 29 - TDX Hsjday Var-Root Alignment

Status: complete

- Review the old TDX hsjday download design document at `F:\dev-repo\mp4-to-word-new\design\backend\tdx-hsjday-download-pipeline.md`.
- Keep the old pipeline shape but map its `reference` paths into `E:\project-repo\cynexus\cynexus-data\var`.
- Align the Python hsjday downloader defaults:
  - downloaded zip: `cynexus-data\var\stock\download\{date}\hsjday.zip`
  - temporary extract: `cynexus-data\var\stock\download\{date}\hsjday_extract_tmp`
  - dated verified target: `cynexus-data\var\tdx\day\hsjday-{date}`
  - live target: `cynexus-data\var\tdx\day\hsjday`
- Preserve existing-data freshness checks and keep recent zip files by default with old zip cleanup.
- Move the already downloaded local hsjday data from `var\tdx\hsjday` to `var\tdx\day\hsjday`.
- Make TDX local day discovery skip non-stock `.day` files such as `sz200b07.day`.

### Phase 29 Verification Results

- `python -m compileall -q src` succeeded.
- `PYTHONPATH=src pytest -q tests/unit/test_sources/test_tdx_hsjday.py tests/unit/test_sources/test_tdx_local_day.py tests/unit/test_pipelines/test_tdx_hsjday_pipeline.py tests/unit/test_pipelines/test_kline_sync_daily.py` passed: 10 tests.
- Restarted local `cynexus-data` on `127.0.0.1:18083`; health returned ok.
- Direct trigger API dry-run for `sync_tdx_hsjday` returns the new var paths.
- Direct trigger API dry-run for `market_sentiment_chain_refresh` completed all 14 steps successfully and reports `hsjday_root=E:\project-repo\cynexus\cynexus-data\var\tdx\day\hsjday`.

## Phase 30 - MSI Historical Percentile Repair

Status: complete

- Restore old-project historical percentile semantics for migrated MSI components.
- Make MA count write percentile scores for price strength and breadth while retaining raw fields.
- Make limit emotion write percentile-based sub-scores and percentile-ranked composite score.
- Make final MSI component assembly compute percentile scores from raw component rows for MA/profit-effect style raw factors.
- Make `market_sentiment_chain_refresh` force the final MSI rebuild and require all 9 factors by default.
- Make Java Market profit-effect read/display the percentile score from final MSI while keeping the factor-table score as raw value.
- Add targeted unit tests and verification.

### Phase 30 Verification Results

- `PYTHONPATH=src pytest -q tests/unit/test_domain/test_msi.py tests/unit/test_pipelines/test_msi_pipeline.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py` passed: 15 tests.
- `python -m compileall -q src` passed under `cynexus-data`.
- `mvn -pl cynexus-market/cynexus-market-starter -am compile -DskipTests` succeeded; existing duplicate-Lombok and MapStruct warnings remain unrelated.
