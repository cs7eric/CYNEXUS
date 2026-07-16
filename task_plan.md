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

## Phase 52 - Market Information Decision Dashboard

Status: complete

- Reframe the Market Information page around the seven user-approved layers: market pulse, key catalysts/risks, impact map, company events, upcoming triggers, and raw feed.
- Keep the first pass UI-only where existing API fields are sufficient; do not add unapproved data sources or new concepts.
- Preserve the existing event Sheet, industry filtering, pagination, and raw 7x24 access.
- Verify scoped frontend lint/build and keep Dev running on the documented ports.

### Phase 52 Verification Results

- Added an impact map, risk/watchlist, company-event lane, and an explicit upcoming-trigger POC using only existing Market Information fields.
- Renamed the primary feed section to “What matters today” while preserving pagination, impact filters, event Sheet navigation, and raw 7x24 expansion.
- Scoped Market Information ESLint and direct Vite production build passed.
- Full Maven package passed at `1.0.39-SNAPSHOT`; Maven and UI versions remain aligned.
- Upcoming triggers intentionally remains an empty state until a verified calendar source is approved; no new provider or table was added.

## Phase 53 - Information Dashboard V2 Proposal

Status: complete

- Replace the current vertically stacked report layout with a three-layer Information workspace: Market Pulse, decision workspace, and full information feed.
- Keep the first layer compact and decision-oriented: market stance, market status, key risk, event count, source freshness, and a small market breadth signal.
- Use a two-column middle workspace: left-side What matters today event stream; right-side Upcoming Triggers, Impact Map, and Risk/Watchlist.
- Replace impact-count bars with event-to-industry relationships: event title, affected industry, direction, impact strength, and related stocks when deterministic data exists.
- Keep the bottom table as the complete feed with filters, pagination, AI status, source, and event Sheet; keep Raw 7x24 as a secondary view rather than a competing primary section.
- Introduce task-oriented filters inspired by the reviewed competitors: Focus, Macro/Policy, Company, Industry, Calendar, and 7x24.
- Do not add new data providers in this phase. Upcoming Triggers remains data-source gated; any calendar source requires a separate POC and approval before implementation.
- After approval, implement the UI first, then evaluate whether a dedicated trigger API/table is needed.

### Phase 53 Verification Results

- Rebuilt the page as a compact Market Pulse, a two-column decision workspace, and a full information feed.
- Added task-oriented views for Focus, All, Macro / Policy, Company, Industry, Calendar / Events, and 7x24.
- Replaced the impact count bars with event-to-industry rows showing direction and relationship counts.
- Kept Upcoming Triggers data-source gated with a compact empty state; no provider or table was added.
- Scoped Information ESLint and direct Vite build passed. Full Maven package passed at `1.0.42-SNAPSHOT`.
- Dev was restarted from the versioned `1.0.42-SNAPSHOT` artifacts and all application health checks are up.

## Phase 54 - Information Event Relationship Chain

Status: complete

- Make the decision workspace visibly render the complete relationship chain: Event → Industry → Direction → Impact strength → Related stocks.
- Use existing deterministic impact mappings, feed issuer values, and structured brief `affectedCompanies` values; do not invent stock relationships.
- Keep missing relationships explicit as `Unmapped industry`, `Unknown direction`, or an omitted related-stock segment.
- Add the same related-stock context to the compact Impact Map where deterministic constituent data exists.
- Extend the existing Market API impact mapping with up to five latest THS constituents per mapped industry; no new provider or table was added.
- Scoped Information ESLint and direct Vite build passed. Full Maven package passed at `1.0.45-SNAPSHOT`; Dev restarted from the new artifacts.

### Phase 54 Errors

- Maven repackage initially hit a locked Market starter JAR because Dev was still running; stopped Dev and reran the Market reactor successfully.
- Vite logged transient proxy connection errors while the restarted Gateway was still booting; subsequent UI, Gateway, Core, AI, and Market health checks returned `200`.

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

## Phase 31 - QFQ Reconciliation Runtime Optimization

Status: complete

- Diagnose why `market_sentiment_chain_refresh` step 3 is much slower than before.
- Keep the pipeline layering intact: chain defaults in `jobs`, kline logic in `pipelines`, ClickHouse write behavior in `storage/clickhouse`.
- Make the chain's QFQ reconciliation step run only `qfq`, not both `qfq` and `hfq`.
- Reduce ClickHouse mutation pressure by supporting date-scope replacement for adjusted daily writes.
- Add/repair targeted tests for adjusted daily batching and progress.
- Rebuild/restart the data job executor and verify the running job uses the optimized path.

## Phase 31 Verification Results

- `PYTHONPATH=src python -m pytest tests/unit/test_pipelines/test_kline_sync_adjusted_daily.py tests/unit/test_pipelines/test_kline_sync_daily.py tests/unit/test_pipelines/test_fund_flow_pipeline.py -q` passed: 19 tests.
- Interrupted old slow run `job_run_0e05248405a24059996b71d6a2dcf9e6`, marked it failed with an explicit deployment-interruption message, and cleared its lock.
- Rebuilt/recreated `cynexus-data-job-executor`; `/internal/health` returns ok.
- Verified the container-loaded source has `replace_scope`, `flush_rows`, deferred `pending_checkpoints`, and the chain `qfq`/date-scope defaults.
- Verified `cynexus_appl_core.task_job_lock` is empty after restart.
- Verified ClickHouse has no active `daily_qfq` / `daily_hfq` mutations after stopping the old run.

## Phase 32 - THS Industry Constituents Job Fallback

Status: complete

- Restore the old-system behavior where THS industry constituents sync can discover industry codes itself instead of requiring Core to pass 90 codes.
- Prefer explicit `industry_codes` / `codes`, then fall back to latest stored THS constituent snapshots or historical industry fund-flow name/code mappings.
- Keep the fallback inside `cynexus-data` storage/pipeline layers, not in Core scheduler mappings.
- Enrich THS industry fund-flow rows with stable historical `sector_name -> sector_code` mappings when the live source omits codes.
- Add targeted tests and, if needed, repair existing rows with empty latest `sector_code`.

## Phase 32 Verification Results

- `sync_ths_industry_constituents` now resolves codes from latest `ths_industry_constituent_snapshot` first, then `sector_fund_flow_daily_snapshots` historical mappings.
- `sync_sector_flow` now enriches missing THS industry `sector_code` values from stored `sector_name -> sector_code` mappings before writing snapshots.
- Existing local missing `sector_code` rows for 2026-06-29, 2026-06-30, and 2026-07-01 were repaired: 270 rows updated.
- Local latest industry fund-flow rows now have 90 distinct sector codes and 0 missing codes.
- Container-loaded repository fallback check returns `constituent_codes=90` and `fund_flow_name_code=90`.
- `PYTHONPATH=src python -m pytest tests/unit/test_pipelines/test_ths_industry_constituents_pipeline.py tests/unit/test_pipelines/test_fund_flow_pipeline.py tests/unit/test_domain/test_fund_flow.py -q` passed: 15 tests.
- `python -m compileall -q src` passed.
- Rebuilt/recreated `cynexus-data-job-executor`; `/internal/health` returns ok.

## Phase 33 - F10 Query Chain Migration

Status: complete

- Follow `docs/migration-guides/python-to-java-api-migration-guide.md`.
- Migrate the stock-detail-dialog F10 query chain from legacy Python page calls to the new layered path:
  - `cynexus-ui` -> `cynexus-market` -> `cynexus-data`
- Keep frontend Java client under `cynexus-ui/src/services/market/f10.ts`; do not leave business API usage in `src/services/legacy/f10.ts`.
- Add internal `cynexus-data` F10 query endpoints under the existing FastAPI internal app instead of routing UI traffic to Python directly.
- Keep `cynexus-data` focused on source-facing/raw structured payloads; keep UI normalization in `cynexus-market`.
- Cover the F10 scopes currently used by `stock-detail-dialog`:
  - valuation
  - finance-report
  - business-composition
  - profit-forecast
  - announcements
  - news
  - roadshows
  - company-news
- Keep Java layering explicit:
  - Controller -> AppService -> Domain Repository -> Infra HTTP client -> `cynexus-data`
- Verify with targeted Java compile, Python compile, and frontend type/build checks after migration.

## Phase 33 Verification Results

- Added `cynexus-data` internal F10 query endpoints under the existing FastAPI app:
  - `/internal/f10/valuation`
  - `/internal/f10/finance-report`
  - `/internal/f10/business-composition`
  - `/internal/f10/profit-forecast`
  - `/internal/f10/announcements`
  - `/internal/f10/news`
  - `/internal/f10/roadshows`
  - `/internal/f10/company-news`
- Added the `cynexus-market` stock-chart F10 query chain:
  - controller endpoints under `/api/market/stock-chart/f10/*`
  - app-service mapping
  - domain repository methods
  - infra HTTP client `HttpStockChartF10DataClient`
- Added frontend market service `cynexus-ui/src/services/market/f10.ts` and switched `StockDetailDialog` plus compatibility barrel exports away from `src/services/legacy/f10.ts`.
- Scope search found no remaining `@/services/legacy/f10` imports under `cynexus-ui/src`.
- Verification succeeded:
  - `python -m compileall -q src` under `cynexus-data`
  - `mvn -pl cynexus-market/cynexus-market-starter -am compile -DskipTests`
  - `npx tsc -b --pretty false` under `cynexus-ui`
  - `npm run build` under `cynexus-ui`
- Runtime note: this pass did not restart local `cynexus-data` / `cynexus-market` processes, so live HTTP smoke of the new endpoints was not executed in this turn.

## Phase 34 - A Stock Data Source Integration

Status: complete

- Integrate missing `a-stock-data.md` capabilities into `cynexus-data` without breaking the existing source -> domain/pipeline -> storage/runtime layering.
- Keep provider HTTP/TCP quirks in `sources/*`; keep live connectivity validation in scripts/docs; do not mix persistence, API routes, and provider parsing in one file.
- Add a reusable EastMoney rate-limited HTTP helper before adding EastMoney report/datacenter/news/hot/limit/fund-flow clients.
- Add provider-scoped source modules for Baidu, iwencai, CNInfo, THS extras, Sina options/financials, and mootdx quote/detail helpers.
- Add internal query routes only as a thin service-to-service surface over source modules; persistence pipelines remain a later targeted step unless an existing table already owns the data.
- Record implementation plan, process log, and result/test matrix under `cynexus-data/docs/a_stock_data_integration/`.
- Run unit/import checks plus live smoke checks where public endpoints are reachable; record failures with exact reasons.

## Phase 34 Verification Results

- Added provider-scoped source modules for EastMoney, Baidu, iwencai, CNInfo, THS, Sina, and mootdx quote/detail helpers.
- Added internal `cynexus-data` routes under `/internal/a-stock/*` without putting provider parsing in route code.
- Added live smoke script and documentation under `cynexus-data/docs/a_stock_data_integration/`.
- `python -m compileall -q src scripts` passed under `cynexus-data`.
- `PYTHONPATH=src python -m pytest -q tests/unit/test_sources/test_a_stock_data_sources.py` passed: 5 tests.
- FastAPI app import/route registration check found 37 `/internal/a-stock/*` paths.
- Live smoke report `cynexus-data/var/reports/a-stock-data-smoke/20260706-233539` recorded 25 ok, 5 failed, 1 skipped.
- Failures were recorded as upstream/proxy connection resets for selected EastMoney push2/emappdata endpoints; iwencai was skipped because `IWENCAI_API_KEY` is not configured.

## Phase 35 - A Stock Data Persistence And Knowledge Base Docs

Status: complete

- Add PostgreSQL and ClickHouse table contracts for the Phase 34 A-stock source/query capabilities.
- Keep storage in `storage/*`, source fetchers in `sources/*`, pure row shaping in `domain/*`, orchestration in `pipelines/*`, and job registration in `jobs/*`.
- Persist compact UI/query datasets to PostgreSQL and high-frequency/minute/detail datasets to ClickHouse.
- Reuse existing tables where the project already owns the matching contract, especially existing stock basic, margin, market limit, sector fund-flow, and stock fund-flow tables.
- Add a data-job entry that can sync one dataset or a small list of datasets with explicit anti-scraping sleeps and batch limits.
- Document scheduler interval guidance for EastMoney, THS, CNInfo, Sina, iwencai, and mootdx endpoints.
- Land detailed table and capability documentation into the knowledge-base documentation area for later feature work.
- Verify with Python compile, targeted unit tests, DDL parse checks, route/job registry checks, and local DB DDL application where available.

## Phase 35 Verification Results

- Added PostgreSQL DDL `cynexus-data/sql/postgres/20260707_create_a_stock_data_tables.sql`.
- Added ClickHouse DDL `cynexus-data/sql/clickhouse/20260707_create_a_stock_data_tables.sql`.
- Added persistence normalization, PG/CH repositories, `sync_a_stock_dataset` pipeline/job, registry entry, `jobs.yaml` entry, and console script.
- Added knowledge-base Markdown docs under `docs/knowledge-base/market-data/`.
- Published the same knowledge content into local `cynexus_appl_knowledge_base.kb_document`:
  - `332571765114957824` / `A Stock 数据落库表设计`
  - `332571765354033152` / `Cynexus Data A 股能力全量地图`
- Local PostgreSQL DDL applied successfully after backup `deploy/local/postgres-backups/cynexus-before-a-stock-persistence-20260707-011519.sql`.
- PostgreSQL verification: 19 `a_stock_%` tables exist and `sec_stock_basic` has the EastMoney extension fields.
- Local ClickHouse DDL applied successfully after fixing a nullable sorting-key issue.
- ClickHouse verification: 6 new A-stock tables exist.
- Live write smoke passed:
  - `cninfo_announcements` wrote 1 row to PostgreSQL `a_stock_announcement`.
  - `tdx_order_book` wrote 1 row to ClickHouse `ch_ods_stock_order_book_snapshot`.
- Job registry check found `sync_a_stock_dataset` registered.
- CLI dry-run payload parsing check succeeded.
- `python -m compileall -q src scripts` passed under `cynexus-data`.
- `PYTHONPATH=src python -m pytest -q tests/unit/test_sources/test_a_stock_data_sources.py tests/unit/test_pipelines/test_a_stock_persistence_pipeline.py` passed: 9 tests.

## Phase 36 - Settings Data Source Health Check

Status: complete

- Build a settings data-source health page from all current `cynexus-data` data sources.
- Add Python internal health checks for storage, filesystem, HTTP, Python-package, API-key, and TCP providers.
- Expose the Python health payload through Core using the existing Java -> Python internal client.
- Persist health check runs and per-source items in `cynexus_appl_core` with project table conventions.
- Keep Java layering explicit: API DTO -> controller/app service -> domain repository/client -> infra repository/client/basic mapper.
- Keep frontend API clients under `src/services`, not global `src/lib/api.ts`.
- Upgrade the existing Settings/Data Source UI with health summary, provider table, manual full check, and keep freshness as a separate view.
- Verify Python compile/import, Java compile, frontend type/build where feasible, local DDL, and live Core/Gateway smoke.

### Phase 36 Verification Results

- Added `cynexus-data` health checks for 24 source/runtime/storage targets, including PostgreSQL, ClickHouse, DuckDB, TDX local files, pytdx/eltdx/mootdx packages, EastMoney Push2/Push2His/Datacenter/Report/Search/HotRank, Tencent, Sina, THS data/basic, exchange margin, Baidu, CNInfo, iwencai, AkShare, and optional mootdx TCP.
- Added `cynexus-data` internal route `GET /internal/data-sources/health`.
- Added Core layered API `GET /api/core/data-source-health/latest`, `GET /snapshot`, and `POST /checks/run`, with persisted run/item rows in `cynexus_appl_core`.
- Added PostgreSQL migration `deploy/local/postgres/migrations/20260707_create_data_source_health_tables.sql`.
- Added Settings -> Data Source UI health view plus frontend service under `cynexus-ui/src/services/core/data-source-health.ts`; freshness remains as a separate tab.
- Local PostgreSQL DDL was applied after backup `deploy/local/postgres-backups/cynexus-before-data-source-health-20260707-164010.sql`; current local counts are 5 health runs and 64 item rows.
- Rebuilt/recreated `cynexus-data-job-executor`; direct Python health returned total=24, healthy=18, degraded=1, failed=1, skipped=4 in the latest smoke.
- Gateway/Core health smoke passed: `GET /api/core/data-source-health/latest` returned the persisted latest run `ds_health_a0528a52212845cb941455b14a5c3539` with total=24.
- Verification passed: `python -m compileall -q src scripts`, targeted Python pytest for health/A-stock tests (12 passed), and `mvn -pl cynexus-core/cynexus-core-starter -am package -DskipTests`.
- Frontend `npx tsc -b --pretty false` was attempted; it still fails on pre-existing unrelated TypeScript errors in market pulse, industry application chart, market sentiment sparkline, and stock-overview market-pulse. No new health-page TypeScript errors appeared in the reported diagnostics.

## Phase 37 - Application Analysis Stock Fund Flow

Status: complete

- Make the Market stock fund-flow endpoint read EastMoney `a_stock_fund_flow_daily` history for main, super-large, large, medium, and small net flow.
- Merge Tencent `stock_fund_flow_snapshot` quote/ratio fields when a same-stock snapshot exists; keep TDX daily bars only as a final quote fallback.
- Extend the typed Java BO/DTO and React service contracts without introducing map-based shortcuts.
- Update the Fund Flow date list to render the five EastMoney net-flow fields and clearly distinguish missing data from zero flow.
- Add Core scheduler dataset mapping/defaults so the EastMoney 120-day stock flow job can run for configured stock codes.
- Backfill the currently configured/selected stock universe where feasible, then restart and smoke-test the full UI -> Market -> PostgreSQL chain.

### Phase 37 Verification Results

- Market `/stock-chart/fund-flow` now merges EastMoney `a_stock_fund_flow_daily` order-size history with Tencent daily snapshot ratios and TDX quote fallback fields.
- Added typed main/super-large/large/medium/small net-flow fields through BO, DTO, app service, frontend service, and date-list UI.
- Added enabled `stock_fund_flow_daily` at 15:20 on trading days for self-selected stocks, backed by `sync_stock_fund_flow`.
- Tencent parsing now uses the quote payload's actual trade date, preventing weekend runs from storing the prior trading day's quote under a weekend date.
- Backfilled six active self-selected stocks for actual trade date `2026-07-10`; `600415` now returns active net amount, active buy/sell ratios, turnover, big-order ratios, amount, price, and change.
- Restarted Data Executor, Market, and Core; direct Market and Vite-proxy HTTP smoke checks passed.
- Targeted Python tests passed: 10. Market/Core Maven compile passed. Targeted frontend ESLint passed.
- Full frontend TypeScript build remains blocked by pre-existing errors in market pulse, industry chart, sparkline, and PageHeader files; no Fund Flow diagnostics were reported.

### Phase 37 Errors Encountered

| Error | Resolution |
| --- | --- |
| EastMoney Push2His closes connections for all six self-selected codes in the current network | Kept EastMoney historical table support and added Tencent daily snapshots as the reliable current-data path; the UI renders each available data scope without fake zeroes. |
| Manual weekend snapshot initially used the task date | Parsed Tencent field 30 quote time, removed the invalid `2026-07-11` batch, and re-collected it under actual trade date `2026-07-10`. |
| Market Spring Boot repackage could not rename the running jar | Stopped the old Market process, packaged successfully, and restarted it on port 18082. |

## Phase 38 - EastMoney Fund Flow Resilient Collection

Status: complete

- Reuse one rate-limited EastMoney HTTP session across stock requests so anti-crawl throttling applies globally within a job.
- Add realistic request headers, request jitter, bounded retries, and exponential cooldown for connection resets and throttling responses.
- Use `push2his` for full history and `push2delay` only as a one-day incremental fallback.
- Preserve the actual source (`eastmoney.push2his` or `eastmoney.push2delay`) in PostgreSQL.
- Validate requested history coverage so a one-row delayed response cannot be reported as a successful 120-day backfill.
- Add tests, rebuild Data Executor, run a self-selected live sync, and verify persisted source/date/row counts.

### Phase 38 Verification Results

- EastMoney requests now share one session with a 2-second minimum interval, 0.5-2 second jitter, realistic browser headers, retry-after handling, exponential retry backoff, and proxy-bypass retry after connection failures.
- Push2His failures open a 15-minute circuit breaker, preventing repeated history requests for every stock in the same run.
- `push2delay` is used only as `daily_fallback`; rows persist as `eastmoney.push2delay` and job metadata reports fallback/history-complete counts.
- Verified the delayed endpoint ignores `beg/end` and always returns only the latest trade date; it cannot backfill historical dates.
- Live self-selected run completed in 21 seconds: 6 codes, 6 delayed rows written, `daily_fallback_count=6`, `history_complete_count=0`.
- `600415` now serves EastMoney main net `4365.1509` wan and Tencent active net `4635.6894` wan for 2026-07-10 with explicit combined source.
- Targeted A-stock tests passed: 13. Combined fund-flow/A-stock tests passed earlier: 22. Python compile, Market Maven compile/package, Data Executor rebuild, Market restart, and HTTP smoke passed.

## Phase 39 - Automatic EastMoney Browser Session

Status: complete

- Add a Playwright persistent Chromium profile under `cynexus-data/var/eastmoney-browser-profile`.
- Reuse cached `qgqp_b_id` and `nid18` without launching a browser on normal jobs.
- On Push2His disconnect or incomplete history, open the persistent profile, visit EastMoney home at low frequency, wait for client scripts, extract only the two required cookies, and retry history once.
- If browser refresh or the retry fails, open the existing 15-minute circuit and use Push2Delay daily fallback.
- Install Chromium in the Data Executor image and persist the profile through the existing `/app/var` mount.
- Add deterministic unit tests plus a live one-code and six-code backfill verification.

### Phase 39 Verification Results

- Playwright now uses a persistent Chromium profile at `/app/var/eastmoney-browser-profile` and a two-cookie cache at `/app/var/eastmoney-browser-cookies.json`.
- Normal collection reuses cached `qgqp_b_id` and `nid18`; Chromium is launched only after Push2His disconnects or returns incomplete history.
- Refresh visits EastMoney home, waits five seconds, extracts only the two required cookies, retries Push2His once, then opens the 15-minute circuit and falls back to Push2Delay.
- A live cookie refresh produced both required cookies, and a no-manual-cookie `600415` history run persisted 120 Push2His rows.
- Historical backfill is intentionally paced at one missing-history stock per workday at 20:30; daily collection uses Push2Delay for all self-selected stocks.
- Debian Chromium is installed in the Data Executor image and configured through `EASTMONEY_CHROMIUM_EXECUTABLE=/usr/bin/chromium`.
- Recreated Data Executor from the verified local image; the host `cynexus-data/var` bind mount retained both browser profile and cookie cache.
- `.env`, the cookie cache, and browser profile are ignored by Git. The image contains empty cookie environment values.
- Targeted browser/source/persistence tests passed: 17. `git diff --check` passed.

## Phase 40 - Application Analysis Information Tab

Status: complete

- Audit the existing Python -> Market F10 information query chain and reuse its typed endpoints.
- Add a dedicated Market frontend service for information data; do not add page API calls to `lib/api.ts`.
- Add an Application Analysis information tab with a compact, date-oriented operational UI for announcements, news, research, and roadshows.
- Verify the Java endpoint contract, frontend lint, and the scoped Market build.

### Phase 40 Verification Results

- Reused the established F10 typed contract: Python internal F10 routes -> `HttpStockChartF10DataClient` -> Market stock-chart controller -> frontend Market service.
- Added the Information tab and dedicated `src/services/market/stock-information.ts`; no `lib/api.ts` usage or direct Python frontend request was added.
- Scoped frontend ESLint passed with only three existing unused-disable warnings in the page.
- Direct Vite-proxy smoke for symbol `600415` returned HTTP 200 from announcements, news, company-news, and roadshows endpoints.

## Phase 41 - Viewable Stock Information Feed

Status: complete

- Populate viewable information records for self-selected stocks through the existing Python A-stock dataset pipeline.
- Expose a typed Market information feed backed by PostgreSQL announcement/news/report/IRM records, with roadshows retained from the typed F10 route.
- Prefer canonical URLs and report-detail URLs; do not render title-only TDX records in the Application Analysis information feed.
- Move the UI to the typed feed endpoint and verify collection, Market compilation, and live endpoint results.

### Phase 41 Verification Results

- Added a typed Market endpoint at `GET /api/market/stock-information`, backed by persisted PostgreSQL announcements, news, research reports, and investor interactions; title-only TDX news/research records are not returned.
- Announcements use CNInfo PDFs, research reports use EastMoney report detail URLs, CNInfo IRM answers render inline, and roadshows retain their F10 URLs.
- Added the `sina_stock_news` fallback to the A-stock pipeline, persistence, watchlist warmup, and the Core daily self-selected dataset job. A live collection for `600415` wrote 40 viewable Sina news rows, 99 research reports, and 30 announcements.
- Verification passed: 17 focused Python tests, Core and Market Maven compilation/package, scoped frontend ESLint (only three pre-existing page warnings), Data Executor health, Vite-proxy endpoint smoke, and `git diff --check`.

## Phase 42 - Information Content And AI Analysis Design

Status: complete

- Inspect the existing persisted information schema, Python internal API conventions, Market-to-AI contracts, and AI persistence patterns.
- Produce an implementation-ready, layered plan for on-demand content retrieval, extraction, cache validation, AI analysis, and Application Analysis UI states.
- Define source safety boundaries, retention, idempotency keys, async execution behavior, and a phased rollout without introducing a direct UI-to-Python path.

### Phase 42 Design Results

- Source records remain in Market, extracted content is cached in Market, and AI analysis plus execution traces belong to AI. Market is the only UI-facing aggregation boundary.
- The proposed path is `UI -> Market -> Data` for approved-source content extraction and `UI -> Market -> AI` for a typed, persisted analysis request; neither browser nor model receives an arbitrary URL.
- Full documents are stored as extracted text with immutable content hashes and chunks. AI analyses are keyed by content hash, analysis schema version, prompt version, and model code so repeat reads are cache hits and new source revisions can be re-analysed.
- The implementation is phased: HTML and text-PDF fetch/display first, structured AI analysis next, then OCR/long-document map-reduce and background prefetch only after the explicit user action path is stable.

## Phase 43 - Information Content Reading And AI Analysis

Status: complete

- Add first-party source extraction for CNInfo PDFs, Sina/EastMoney HTML, and inline IRM answers through the existing Data internal API boundary.
- Persist normalized extracted content and cached analysis state in Market-owned tables without storing source binaries or accepting browser-provided URLs.
- Add typed Market endpoints for content retrieval and analysis request/status; keep AI output and execution traces in the AI service.
- Add the Information-tab read/analyze interaction and focused tests, then rebuild/restart the affected services and smoke the complete path.

### Phase 43 Verification Results

- Added approved-source extraction in Data for inline IRM text, text PDFs, and HTML pages, with host allowlists, redirect validation, body/page limits, charset recovery, boilerplate trimming, and explicit `needs_ocr` state.
- Added Market-owned extracted-content and AI-analysis cache tables. Cache keys tie each source record to a content hash; analysis cache keys add schema and prompt versions.
- Added typed Market endpoints for read/fetch/analyse status. Market first resolves the persisted source record, then calls Data, and calls AI only after readable content has been cached.
- Added Information-tab Read and Analyze actions. The reader shows extracted source text; analysis renders summary, sentiment, materiality, and selected facts.
- Applied migration after backup `deploy/local/postgres-backups/cynexus-before-stock-information-content-20260712-132438.sql`.
- Live Vite-proxy validation for `600415` fetched Sina content in Chinese and returned a real DeepSeek `deepseek-v4-flash` structured analysis. Market, AI, and Data health endpoints are healthy.
- Verification passed: 21 focused Python tests, Market and AI Maven package, scoped frontend ESLint, live content/AI smoke, and `git diff --check`.

## Phase 44 - Evidence-Aware Information Analysis Prompt

Status: complete

- Upgrade the information-analysis schema and instructions with company relevance, time horizon, evidence quotations, confidence, uncertainty, and explicit non-advice constraints.
- Bump the analysis schema/prompt version so existing v1 cache rows are not reused.
- Render the new concise analysis fields in the Information reader and verify the refreshed AI response.

### Phase 44 Verification Results

- Prompt v2 now requires company relevance, impact horizon, confidence, verbatim evidence quotations, risk/uncertainty disclosure, and an explicit non-advice disclaimer. It treats source text as untrusted and rejects instructions embedded in the source.
- Bumped both analysis schema and prompt cache versions to `stock_information_v2`, so existing v1 output is not reused.
- The Information reader now renders relevance, horizon, sentiment, materiality, confidence, and evidence quotations alongside the summary.
- Direct and Vite-proxy smoke for `600415` returned a v2 DeepSeek analysis with source quotes. Market and AI packages plus scoped frontend ESLint passed.

## Phase 45 - Expand Market Information Sources

Status: complete

- Inventory all existing message-source clients and persistence contracts before adding new providers.
- Add stable official-source collectors for macro, policy, regulation, and exchange information.
- Add market-media and industry-source collectors with source-specific throttling, encoding validation, and failure isolation.
- Extend the Java aggregation API and UI feed to classify and display the expanded source set without reintroducing stock-only filtering.
- Add scheduler mappings, focused tests, version increments, and a versioned Release deployment.

### Phase 45 Progress

- Added a generic, encoding-safe official-source HTML collector for government, central-bank, statistics, finance, development/reform, regulator, exchange, Xinhua, and CCTV pages.
- Added the `official_market_information` data job and persisted 96 official headlines into `cynexus_appl_market.a_stock_news`.
- Expanded the market feed source set and changed Java news aggregation to round-robin per source so official rows are not hidden behind the higher-volume media feeds.
- Added CNInfo regulatory announcements for inquiry letters, regulatory measures, penalties, and disciplinary notices, including related stock codes and PDF links.
- Added automotive and nonferrous-industry association feeds and kept the existing leader-event aggregation over hot-rank/Dragon-Tiger stocks.
- Deployed `1.0.18-260713-RELEASE`; the live 60-item feed contains four rows from each of 15 source streams, with no replacement-character encoding failures.

### Phase 45 Errors

| Error | Attempt | Resolution |
| --- | --- | --- |
| Scope includes multiple providers with different access and licensing constraints | Initial "all sources" request | Split into source families and require stable/approved adapters; no arbitrary scraping or direct UI-to-source calls. |
| Python endpoint inventory probe had an unterminated URL string | First live probe | Correct the probe before interpreting source availability. |
| SSE returned 403 and BSE list URLs returned 404/anti-bot shells | Exchange-source probe | Keep SZSE and CNInfo regulation live; leave SSE/BSE out until a stable first-party endpoint is verified. |

## Phase 46 - Paginated Market Information History

Status: complete

- Replace the fixed-size Market News response with a server-side paginated contract.
- Preserve source-balanced ordering across pages so high-volume feeds do not hide official sources.
- Add compact pagination controls while keeping the secondary panels bounded.
- Expand EastMoney 7x24 history through its `sortEnd` cursor and raise official-source collection limits.
- Update scheduled collection defaults, backfill the expanded history, increment versions, verify, and deploy a versioned Release.

### Phase 46 Verification Results

- The Market API now returns a source-balanced page contract with 30 records per page and explicit total/page metadata.
- Market news and CNInfo market/regulatory announcements are unified into one information stream with category, kind, issuer, and contextual labels.
- The UI uses the project shadcn Table and Pagination components; page changes return the table to its visible top without hiding it behind the workspace header.
- EastMoney 7x24 uses `sortEnd` cursor pagination and the official-source collector retains up to 40 rows per source. The live backfill wrote 877 rows and expanded the unified feed to 1,157 records across 39 pages.
- Python tests, Maven package, scoped ESLint, Vite build, live API page checks, Playwright interaction checks, and Release deployment passed for `1.0.19-260713-RELEASE`.

## Phase 47 - Market Information AI Pipeline

Status: complete

- Add stable Market information identities, extracted-content cache, deterministic 48-hour event clustering, members, priority scoring, and pipeline state without copying raw facts.
- Add a `custom` Responses API provider, structured event/brief protocols, cross-provider fallback, AI-owned durable tasks/results/briefs, and sanitized execution records.
- Add typed Market-to-Data and Market-to-AI orchestration, asynchronous refresh/manual analysis APIs, table enrichment, event detail Sheet, and market brief UI.
- Add layered media/official schedules through Core, a 60-record evaluation dataset and offline/live evaluation harness, then bump versions, verify, and deploy Release.

### Phase 47 Guardrails

- The supplied bearer credential is local-only in ignored `.env`; it must not enter source, migrations, database rows, logs, screenshots, or final output.
- Custom `gpt-5.6-sol` is primary and unrestricted for development. DeepSeek is fallback only; live development/evaluation calls must not exceed 20.
- Preserve the unified paginated information table, raw source tables, source-level failure isolation, and one-online-environment deployment rule.

### Phase 47 Implementation Status

- [x] Persist extracted full text with summary-only fallback and SSRF/redirect/size controls.
- [x] Incrementally upsert stable 48-hour clusters and queue at most 10 high-value events per refresh.
- [x] Add Custom Responses routing, persisted task recovery/retry, structured analysis validation, fallback, and daily brief storage.
- [x] Add Market APIs, batch polling, brief/table/Sheet UI, and typed Core collection/pipeline schedules.
- [x] Add 60-case offline evaluation assets and guarded live evaluation runner.
- [x] Align Maven/UI versions to `1.0.20-SNAPSHOT` and pass focused Python/Maven/ESLint/migration verification.
- [ ] Populate the ignored Custom API key, run the 5-case live smoke/full evaluation, build versioned Release images, deploy `1.0.20-260714-RELEASE`, and run API/Playwright smoke.
## Market Information

- [x] Inventory existing cynexus-data information sources, storage conventions, and market UI patterns.
- [x] Design and implement Python market-information ingestion/API and market persistence.
- [x] Implement Java market application API/service/repository layers.
- [x] Implement UI route, services, components, filtering, and styling.
- [x] Increment versions and verify the versioned Release deployment.
## Market Information delivery

- [x] Reuse existing cynexus-data market-level datasets and confirm no duplicate fact table.
- [x] Add Java domain/API/application/infra layers for the aggregate feed.
- [x] Add market UI service, route, sidebar entry, and decomposed page components.
- [x] Build Maven and UI, update design documentation and versions.
## Market Information source expansion

- [x] Add Cailian Press telegraph source with anti-bot warm-up/cache-busting/retry handling.
- [x] Add market-level CNInfo policy announcement dataset and persistence path.
- [x] Add Java DTO/repository aggregation for policy announcements and leader events.
- [x] Add UI panels for Cailian wire, CNInfo policy, and leader-linked events.
- [x] Rebuild data executor and deploy `1.0.12-260713-RELEASE`.

## Phase 52 - Langfuse AI observability integration (2026-07-15)

Status: cancelled by user

- Add a Langfuse HTTP ingestion client in `cynexus-ai` without coupling domain/application code to a vendor SDK.
- Report every centralized `AgentExecutor` model completion with provider, model, status, latency, token usage, and optional redacted content.
- Keep Langfuse disabled by default and make all credentials/environment settings external; failures must never fail an AI request.
- Add a self-hosted Langfuse deployment definition and documented bootstrap/configuration using a dedicated observability stack.
- Add focused unit tests for payload construction, authentication, disabled mode, and failure isolation.
- Advance Maven and UI development versions together after the functional change; do not deploy Release until the user explicitly requests it.

Cancellation: the user chose a lighter native AI usage/cost view instead of running Langfuse as a separate observability stack. Langfuse code, configuration, local credentials, containers, volumes, images, and network were removed.

## Phase 53 - Shared-resource Langfuse API usage view (2026-07-15)

Status: complete

- Reintroduce only Langfuse Web/Worker and MinIO; use the existing Cynexus PostgreSQL, ClickHouse, and Redis containers with isolated schema/database/Redis namespace.
- Restore provider-neutral generation ingestion from the centralized AI completion boundary.
- Add a typed AI backend proxy for Langfuse observations and summary aggregation; the browser must not hold Langfuse credentials.
- Add a lightweight native AI Usage page with KPI cards, breakdowns, and recent calls.
- Keep Langfuse optional and fail-open; existing PostgreSQL execution logs remain the durable fallback.
- Advance Maven/UI versions together and verify shared infrastructure remains healthy.

### Phase 53 Verification Results

- Added shared-resource Compose with only Langfuse Web, Langfuse Worker, and MinIO; PostgreSQL schema, ClickHouse database, and Redis DB 4 are isolated from Cynexus application data.
- Added local start/stop scripts and documented the shared-resource bootstrap; no Langfuse credentials are committed or exposed to the browser.
- Added `/api/ai/usage` backend aggregation and a native Settings → AI Usage page with date range, KPI cards, provider/model/capability breakdown, recent calls, status, token, cost, and latency.
- Maven AI reactor compile passed under `1.0.48-SNAPSHOT`; scoped ESLint for the new usage service/page passed; Compose config validation passed.
- Full UI type-check remains blocked by the existing unrelated Market Pulse, Industry Application, Market Sentiment, and stock-overview errors. New Information dashboard type errors were corrected.
- Docker daemon was unavailable during this turn, so the optional Langfuse containers were not started or smoke-tested.

### Phase 53 Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| Full UI build failed in unrelated existing modules | `npm run build` | Scoped lint passed; recorded the pre-existing type errors and did not modify unrelated modules. |
| Docker daemon pipe unavailable | Compose/runtime smoke | Compose syntax still validates; startup is documented for the next Docker-enabled run. |

### Phase 52 Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| PowerShell split Maven `-Dcheckstyle.skip=true` into an invalid lifecycle phase | First focused compile | Quote Maven `-D` arguments in the retry. |
| `cynexus-ai-infra` had no JUnit 5 test dependency | First reporter test compile | Add the existing managed `junit-jupiter` test dependency to the infra module. |
| Full UI ESLint reports 98 existing errors across unrelated components | Version-sync verification | No Langfuse UI code was added; retain the repository's existing frontend lint baseline. |
| First PowerShell script parser check used an unescaped `$file:` interpolation | Diagnostic command only | Reran the parser check with `${file}`; both scripts parse successfully. |
| Langfuse Web resolved `postgres`/other service names to Cynexus shared-network containers | First runtime start | Renamed internal services to unique `langfuse-*` hostnames and recreated only Langfuse containers while preserving volumes. |
| Old Langfuse containers kept MinIO port mappings after service rename | Compose recreation | Removed only Compose orphan containers with `--remove-orphans`; Cynexus shared infrastructure remained running. |

### Phase 52 Verification Results

- `LangfuseGenerationReporter` reports success and failure generations asynchronously through `/api/public/ingestion`.
- `AgentExecutor` is instrumented as the shared completion boundary for current AI features.
- Provider/model identity, status, latency, and input/output/total usage are sent; Prompt/Response content is disabled by default.
- Capability names are propagated into generation names and metadata for business-level filtering in Langfuse.
- Langfuse failures are fail-open and cannot change the AI completion outcome.
- Self-hosted Langfuse v3 Compose and `.env.example` are under `deploy/observability/langfuse`; its databases and object storage are isolated from Cynexus application stores.
- Start/stop PowerShell entrypoints are provided; Compose config validates with the example environment file.
- Focused Maven test passed: 2 reporter tests under `1.0.46-SNAPSHOT`.
- AI infra/application compile passed under `1.0.46-SNAPSHOT`; Compose config validation passed.
- Complete `cynexus-ai-starter` reactor package passed under `1.0.46-SNAPSHOT`.
- Maven and UI development versions are now synchronized at `1.0.46-SNAPSHOT`.
- Release was intentionally not rebuilt or deployed; Langfuse credentials and model pricing require operator configuration first.
- Generated local ignored Langfuse credentials, started the stack, and verified Web/Worker/PostgreSQL/ClickHouse/Redis/MinIO are running; project and admin rows were initialized and HTTP health returned 200.
- Root `.env` now contains the Langfuse client settings. Existing Dev Java processes were not restarted in this turn, so AI reporting becomes active after the next Dev/Release application restart.

### Phase 52 Removal Verification

- No runtime Langfuse references remain in `cynexus-ai`, Release Compose, or root `.env`.
- No Langfuse containers, observability volumes, or Langfuse images remain.
- Shared Cynexus PostgreSQL, Redis, ClickHouse, and Data Executor remained healthy.
- AI Starter reactor package passed after removal under `1.0.47-SNAPSHOT`; Maven and UI versions are synchronized.

## Phase 48 - Add My Codex Custom Provider (2026-07-14)

Status: complete

- Preserve the existing `xtyooo` Custom provider and add a second uniquely identified provider row for `My Codex`.
- Keep the supplied credential in `ai_providers.api_key`; never place it in source, migrations, logs, or `.env`.
- Fix Market Information adapter dispatch so multiple provider rows with `provider_type=custom` share the registered `custom` adapter while retaining provider-row provenance.
- Add an idempotent secret-free migration seed for the provider metadata.
- Verify the provider CRUD response masks the key and run a focused Responses API smoke without invoking DeepSeek fallback.
- Keep existing Market Information usage bindings unchanged unless explicitly requested.
- Increment Maven and UI development versions together after the functional change.

### Phase 48 Verification Results

- Provider `custom-yancc` is enabled in PostgreSQL with model `gpt-5.4`, Responses API, low reasoning, `store=false`, and a database-managed credential.
- Provider API returns a mask only and does not return the raw credential.
- Live Responses API smoke returned valid JSON using `gpt-5.4`; DeepSeek was not called.
- Migration rerun preserved the stored credential.
- Focused AI tests passed: 4 tests.
- Full 41-module Maven package passed under `1.0.21-SNAPSHOT` after limiting tiered compilation to avoid a local Corretto JIT crash.
- Maven and UI versions are aligned at `1.0.21-SNAPSHOT`.
- Existing Release `1.0.20-260714-RELEASE` remains online; Gateway and AI health are `UP`.

### Phase 48 Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| Planning append context did not match `findings.md` | First append patch | Read file tails and append against exact current EOF context. |
| Full Maven package crashed in Corretto 21 JVM with `EXCEPTION_ACCESS_VIOLATION` | First full package attempt | Retry with tiered compilation limited to level 1; focused AI tests had already passed. |

## Phase 49 - Switch Market Information AI To DeepSeek V4 Flash (2026-07-14)

Status: complete

- Verify the persisted DeepSeek provider can call `deepseek-v4-flash` successfully.
- Switch both Market Information usage bindings to DeepSeek as the primary provider with no self-fallback.
- Keep the 20-call limit only for Dev evaluation; configure Release runtime for unlimited production processing.
- Add an idempotent migration and preserve the existing database-managed DeepSeek credential.
- Advance Maven and UI versions together to `1.0.22-SNAPSHOT`.
- Recreate only the Release AI container, verify binding state, queue processing, and health without stopping shared infrastructure.

### Phase 49 Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| Release AI still rejected DeepSeek after receiving limit `-1` | First queued event smoke | Constructor clamped all negative values to zero, making its existing unlimited branch unreachable. Preserve `-1`, add a regression test, and deploy the corrected version. |
| First DeepSeek event exhausted retries on strict evidence validation | First post-deploy event smoke | The event had only a title (`summary_only`), but the model inferred full policy details and fabricated non-verbatim quotes. Upgrade the prompt to v2 with an explicit evidence boundary and title-only behavior. |

### Phase 49 Verification Results

- Both Market Information bindings use provider `deepseek` and model `deepseek-v4-flash` with prompt version `v2`.
- The migration passed twice and keeps the existing database-managed provider credential unchanged.
- Dev retains its 20-call cap; Release uses unlimited mode and the regression test confirms negative limits remain unlimited.
- Focused AI tests passed: 6 total tests across the worker and evaluation fixture.
- Full 41-module Maven package passed under `1.0.23-SNAPSHOT`.
- Event analysis completed with `provider_code=deepseek`, `model_code=deepseek-v4-flash`, `fallback_used=false`, and strict evidence validation enabled.
- Latest daily brief also reports DeepSeek V4 Flash, prompt version v2, and successful status.
- Release `1.0.23-260714-RELEASE` is deployed across UI, Core, Market, AI, and Gateway; all Java health endpoints are `UP`.

## Phase 50 - Strengthen Market Brief Structure And Freshness (2026-07-14)

Status: complete

- Replace the loose string-list brief with a typed market-level protocol containing headline, executive summary, structured tone, structured key events, policy/industry/company/risk signals, uncertainties, source coverage, analysis window, and event references.
- Select only the latest successful analysis for each cluster whose content hash is still current and whose prompt/schema versions match the brief pipeline.
- Validate nested brief fields, enums, confidence ranges, unique/current event references, and source coverage instead of checking top-level field presence only.
- Keep backward-compatible API persistence while updating the Market Brief UI to render the richer structure without hiding policy/industry/source information.
- Add focused repository/worker/frontend verification, advance Maven/UI versions together, and deploy a versioned Release after live DeepSeek generation succeeds.

### Phase 50 Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| Scoped ESLint returned `ERR_PNPM_RECURSIVE_EXEC_NO_PACKAGE` | First frontend verification | Command was run from the repository root; rerun from `cynexus-ui`. Maven tests had already passed. |
| Full UI build failed in unrelated Market Pulse/Industry/Sentiment files | First UI build | Scoped Market Information ESLint passed before the build step. Record the existing errors and continue with scoped verification plus Release image smoke. |
| Stale-task smoke could not parse `INSERT ... RETURNING` output as a scalar | First runtime stale-task check | PostgreSQL emitted both command status and ID. Locate the inserted row by its unique smoke hash, then verify status and provider-call count. |
| Inline Node console audit could not import Playwright | Browser-console verification | The Playwright CLI is available but not a local importable package. Retain the successful CLI screenshot/visual QA and API smoke instead of adding test-only dependencies. |

### Phase 50 Verification Results

- Brief input now contains one latest successful analysis per current cluster content hash, filtered to event schema v1/prompt v2, with deterministic analysis window and source coverage.
- Brief output uses schema v2/prompt v3 with structured tone, key events, policy/industry/company/risk signals, uncertainties, source coverage, analysis window, event references, and disclaimer.
- Nested enums, confidence, current cluster references, metadata equality, and required fields are validated; authoritative event metadata is reattached before validation.
- Obsolete queued brief hashes are skipped without provider calls; the runtime smoke completed with zero new AI executions and its test row was removed.
- Focused AI tests passed: 9 worker/evaluation tests. Full 41-module Maven package passed.
- Scoped frontend ESLint and direct Vite production build passed; full `tsc -b` remains blocked only by documented unrelated modules.
- Live DeepSeek V4 Flash generation succeeded with schema v2/prompt v3 and no fallback.
- Desktop Playwright screenshot confirmed the new Market Brief renders headline, summary, tone/confidence, structured events, separated signals, risks/uncertainties, and coverage.
- Release `1.0.25-260714-RELEASE` is deployed across UI, Core, Market, AI, and Gateway; all service health checks are `UP`.
- Maven and UI versions are aligned at `1.0.25-SNAPSHOT`.

## Phase 51 - Market Event Value And Analysis V3 (2026-07-14)

Status: complete

- Expand market-information full-text extraction for official pages, CNInfo PDFs, exchange disclosures, and supported media while retaining SSRF/redirect/size controls and summary-only fallback.
- Replace source/category-only high-value scoring with explicit market breadth, novelty, source confirmation, content quality, actionable fact, leader relevance, and routine-announcement penalties.
- Prevent template abnormal-volatility notices, generic inquiry replies, and title-only single-company disclosures from becoming Key Events without material facts.
- Upgrade event analysis to schema v2/prompt v3 with event facts, information delta, why it matters, transmission paths, expectation gap, affected industries/companies, catalysts, scenarios, watch items, risks, uncertainties, evidence, and content-quality assessment.
- Keep evidence verbatim and make analysis depth degrade explicitly when only summary/title content is available.
- Redesign the event Sheet into a dense professional analysis view using the existing product design system, without nested-card clutter.
- Add Python/Java/frontend tests, advance Maven/UI versions together, deploy a versioned Release, backfill representative content, and verify live DeepSeek analysis quality.

### Phase 51 Verification Results

- Python content/extraction tests pass: 7 focused tests. CNInfo/SZSE backfill persisted 30 documents in the live database, 27 as full text and 3 as explicit summary-only fallback; PostgreSQL-incompatible PDF NUL bytes are sanitized.
- Deterministic cluster tests pass: 10 tests covering 48-hour clustering, stable content, routine notices, material company facts, named-document merging, and cross-category policy/media merging.
- AI worker tests pass: 9 focused tests covering provider budget, prompt protocol, exact evidence, event schema v2, brief validation, and stale-input suppression.
- Market Information scoped ESLint passes. Full Maven package passes all 41 modules. Direct Vite production build passes; the full UI type-check remains blocked only by the existing unrelated Market Pulse, Industry Application, Market Sentiment, and stock-overview errors.
- Live Release `1.0.28-260714-RELEASE` is healthy on Core 18080, AI 18081, Market 18082, Gateway 18000, and UI 5173. Data Executor remains healthy on 18083.
- Live cluster smoke confirms named policy/media coverage is merged into one Policy cluster, routine abnormal-volatility/inquiry rows have maximum active priority `0.9` and are not high value, and the latest v2/v3 DeepSeek analysis includes information delta, why-it-matters, transmission paths, scenarios, watch items, and exact evidence.
- Latest Market Brief refreshed at runtime with policy, macro, regulation, and material company events after the current scoring pass; no longer dominated by routine inquiry/abnormal-volatility notices.

## Phase 54 - DDD business logic relocation (2026-07-15)

Status: complete

- Relocate Knowledge Base indexing rules from the application controller module into the domain service layer.
- Relocate high-value Market query/aggregation rules into domain services while keeping DTO conversion at the application boundary.
- Relocate Core scheduler rules and AI prompt/validation/routing rules into domain services.
- Record the layering rule in `AGENTS.md` and keep Maven/UI development versions aligned.
- Run focused module tests and the full Maven package after the migration.

### Phase 54 Verification Results

- Knowledge Base, Market, Core, and AI targeted reactors compile successfully.
- Focused tests passed: AI application 10 tests and Market Domain 10 tests.
- Full `mvn package -DskipTests` passed for all 41 Maven projects at `1.0.78-SNAPSHOT`.
- `mvn clean package -DskipTests` reached all code compilation but could not delete the locked old Core Starter JAR; the non-clean package produced all new versioned artifacts successfully.
- Maven and UI versions are aligned at `1.0.78-SNAPSHOT`.

### Phase 54 Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| Initial recursive Java inventory included generated `target` sources and produced truncated output | 1 | Excluded `target` and inspected application/domain packages by module |

## Phase 55 - Complete Application Business Logic Relocation (2026-07-15)

Status: complete

- Re-audit every Knowledge Base, Market, Core, and AI application service after the first DDD pass.
- Move remaining validation, normalization, scoring, routing, parsing, aggregation, fallback, scheduling, and aggregate-state rules into domain entities, value objects, policies, domain services, and ports.
- Keep application services limited to request orchestration, transaction boundaries, cross-aggregate coordination, and API DTO conversion.
- Process modules in the requested order: Knowledge Base, Market, Core, AI.
- Add focused tests for each extracted domain policy and update Maven/UI versions together after functional changes.
- Verify with focused module tests and the complete Maven package; document any environment-only verification blocker.

## Phase 56 - Domain Feature Package Audit (2026-07-16)

Status: complete

- Move newly added domain policies, feature services, and domain ports out of the flat `domain/service` package.
- Group them by bounded capability: application analysis, knowledge base, market information, media transcription, scheduler, routing, and ports.
- Update package declarations/imports and verify Spring wiring, focused tests, and the full reactor.
- Leave legacy CRUD-oriented domain service conventions unchanged unless the package move is required for the audit.

### Phase 56 Verification Results

- Feature packages now own the relevant domain code: `applicationanalysis`, `knowledgebase`, `marketinformation`, `mediatranscription`, `scheduler`, `routing`, `validation`, `port`, `pulse`, `sentiment`, `stock`, `sector`, `fundflow`, `information`, `calendar`, `feature`, `industry`, `margin`, `coreinfo`, and `task`.
- No Java source remains under the four module `domain/service` roots; no application import points to the moved feature classes through the old flat package.
- Market Domain tests: 10 passed. AI application tests: 10 passed. Full `mvn package -DskipTests`: 41 projects passed.

### Phase 55 User Feedback

- User reported that the previous migration left substantial business logic in application services and was not acceptable as a complete DDD refactor.
- Do not mark this phase complete based only on moving a few representative classes; the remaining high-risk application services must be audited and reduced.
