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

## Phase 61 - Credit Accounts And Installments

Status: complete

- Replace the finance account nature `liability` with the user-facing/domain value `credit`, preserving existing rows through a migration.
- Add account types for Huabei, Baitiao, Douyin Monthly Pay, and credit cards, while keeping account balance semantics correct.
- Add a reusable credit-installment plan and installment schedule model with API persistence and transaction linkage.
- Extend AI receipt parsing and the finance UI so credit purchases can carry installment metadata without requiring Shortcut parameters.
- Verify migration, focused Java/UI checks, package version synchronization, and Dev runtime health.

### Phase 61 Verification Results

- Added migration `20260719_add_credit_accounts_and_installments.sql`; live Dev PostgreSQL now uses `asset|credit` nature and concrete credit types `huabei`, `baitiao`, `douyin_monthly`, and `credit_card`.
- Added credit installment plans, generated schedules, payment transfers, idempotent purchase-plan lookup, AI installment fields, and account/plan UI.
- Focused Maven compile, full Maven package at `1.1.49-SNAPSHOT`, Finance ESLint, live plan listing, plan creation, and payment workflow passed.
- Full UI production build remains blocked by existing unrelated TypeScript errors in Market and easy-tdx files.

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

## Phase 57 - Market ETF Flow Workspace

Status: complete

## Phase 59 - ETF Many-to-Many Mapping Registry

Status: complete

- Add a normalized many-to-many ETF mapping table keyed by ETF code, mapping group, and mapping value.
- Define reusable mapping analysis for universe, asset class, region, index scope, style, and industry/theme tags.
- Generate a complete seed SQL for the current 1,549 ETF catalog and use the same mapping logic during future ETF syncs.
- Persist mapping provenance, primary flags, confidence, and idempotent stable IDs.
- Verify seed counts, database constraints, Python tests, Java compatibility, and Dev runtime without disturbing unrelated worktree changes.

### Phase 59 Verification Results

- Added `market_etf_mapping` with six mapping groups: `universe`, `asset_class`, `region`, `index_scope`, `style`, and `theme`.
- Generated `20260718_seed_market_etf_mapping.sql` from the active 1,549 ETF catalog using the shared Python mapping policy.
- Seed contains 6,213 many-to-many rows; every ETF has mappings in `universe`, `asset_class`, and `region`, with 3 to 6 total rows per ETF.
- Applied the DDL and seed to live PostgreSQL; rerunning the seed is idempotent and remains at 6,213 rows / 1,549 distinct ETF codes.
- Added daily sync persistence through `MarketEtfRepository.upsert_mappings`; Data Executor was rebuilt and restarted.
- Focused Python tests passed: 8 tests. Existing Java/UI runtime remains healthy.

Status: complete

- Add Python ETF spot and daily-share ingestion using the existing Data Executor boundary.
- Persist ETF catalog, live snapshots, and daily share history under `cynexus_appl_market` with source provenance and idempotent keys.
- Add Java Market domain/API/application/infra read path at `/api/market/etf`.
- Add UI route `/market/etf` with services under `src/services/market/etf.ts` and colocated components.
- Render broad-market and sector ETF rotation, share-flow proxies, activity, premium/discount, IOPV deviation, and market breadth without mixing low-value stock news into the page.
- Verify Python tests, Java compilation/package, frontend lint/build, live data routes, database schema, and Dev runtime.

### Phase 55 User Feedback

- User reported that the previous migration left substantial business logic in application services and was not acceptable as a complete DDD refactor.
- Do not mark this phase complete based only on moving a few representative classes; the remaining high-risk application services must be audited and reduced.

### Phase 57 Verification Results

- Python ETF normalization, existing market fund-flow, and easy_tdx focused tests passed: 10 tests.
- Java Market Starter reactor compilation passed at `1.1.36-SNAPSHOT`.
- ETF-scoped frontend ESLint passed; direct Vite production build passed.
- Added `deploy/local/postgres/migrations/20260718_create_market_etf_tables.sql`; live migration/API smoke could not run because Docker Desktop is unavailable and no Dev application ports are listening.

## Phase 58 - Official ETF Classification Mapping

Status: complete

- Replace name-only ETF classification with a code-keyed classification registry and explicit provenance.
- Use official or authoritative index/fund metadata when available, with controlled manual overrides for unresolved products.
- Separate broad, industry, theme, style, overseas, commodity, bond, currency, and unknown categories so non-industry products are not mislabeled as sectors.
- Keep the existing ETF page compatible while expanding query coverage beyond the current broad/sector-only filter.
- Add coverage diagnostics and focused tests; synchronize Maven/UI versions and verify Python, Java, and UI changes.

### Phase 58 Verification Results

- Added code-keyed classification input with optional EastMoney fund/index catalog enrichment and deterministic name fallback.
- Expanded broad/industry keyword coverage and excluded overseas, commodity, bond, currency, and style-only products from the industry bucket.
- Persisted `classification_source` on ETF catalog, spot, and daily tables; added a deployment migration and API/UI coverage counters.
- Reclassified the live 1,549 ETF catalog: 428 broad, 743 industry, 378 other. The Market API now includes 1,171 broad/industry ETFs, up from 924.
- Replaced ETF table ID generation with stable business-key IDs after a duplicate primary-key failure during the first live sync attempt.
- Focused Python tests passed: 6 tests. Market Maven reactor passed. ETF-scoped frontend ESLint passed. Data Executor image rebuilt and restarted; Dev Java/UI processes restarted.
- The official EastMoney index-fund catalog was probed successfully in Data Executor but had zero code intersections with the listed ETF catalog; current records therefore correctly report `fallback.name_keyword` rather than claiming official coverage.
- Full UI `npm run build` remains blocked by the pre-existing TypeScript errors in `market/pulse.ts`, Industry Application, Market Sentiment, POC easy-tdx, and stock-overview PageHeader; no new ETF-file errors were reported. ETF-scoped ESLint passes.

### Phase 58 Errors

- First live sync after adding `classification_source` failed with `market_etf_spot_snapshot_pkey` duplicate ID; resolved by switching ETF catalog/spot/daily IDs to `stable_bigint_id` business keys and rerunning successfully.
- Host-side AkShare probing was blocked by the local proxy; the same catalog probe in Data Executor succeeded, so no host network assumption was added.

Status: complete

## Phase 60 - AI Bookkeeping Workspace

Status: complete

- Analyze the common workflows of mainstream bookkeeping products and translate them into a focused Cynexus finance domain.
- Add core-owned finance tables under `cynexus_appl_core` for accounts, categories, posted transactions, AI receipt captures/images/draft items, and soft-archive/audit fields.
- Implement DDD ports, policies, repositories, application orchestration, DTOs, and controllers in `cynexus-core`.
- Extend the AI module with a vision execution contract so core can parse one or many receipt images through the configured vision-capable provider without placing provider logic in core application code.
- Add `/finance/ledger`, `/finance/accounts`, and `/finance/ai-capture` routes with services under `src/services/finance` and colocated components.
- Complete the workflow: create account/category, manually post income/expense/transfer, upload one or many images, review/edit AI draft rows, confirm them into the ledger, archive entries, and view period/category/account statistics.
- Seed useful Chinese default categories and keep the default owner key ready for future authentication/household sharing.
- Synchronize Maven/UI versions, run focused tests and builds, apply the migration, restart Dev, and smoke-test the full workflow.

### Phase 60 Verification Results

- Added the core finance schema and applied it to `cynexus_appl_core`, including seeded expense/income categories.
- Added domain policies and repository ports, JDBC persistence, application services, DTOs, and controllers for accounts, categories, transactions, overview, capture batches, images, and review items.
- Added the AI vision contract and OpenAI-compatible image input path; AI recognition remains draft-first and confirmation is idempotent.
- Added `/finance/ledger`, `/finance/accounts`, and `/finance/ai-capture` with colocated services/components, responsive forms, review editing, archive actions, empty/error/loading states, and local-date handling.
- Corrected overview totals to use SQL aggregation instead of the recent-transaction limit and fixed daily-stat SQL aliasing.
- Scoped UI ESLint passed. Maven core/AI compilation passed. Full UI build remains blocked only by pre-existing TypeScript errors outside the finance files.
- Synchronized all Maven modules and UI to `1.1.44-SNAPSHOT`, restarted the documented Dev stack, and verified the live finance category/accounts/overview APIs plus manual account and transaction posting. Smoke data was removed afterward.

## Phase 62 - Finance Ledger Management And Category Coverage

Status: complete

- Add a dedicated finance transaction list with filters and bounded pagination/loading.
- Add single-transaction detail, edit, and delete operations with balance recalculation and installment safeguards.
- Expand the category set and expose category management so missing categories can be added without database edits.
- Add the corresponding Core API, domain/application/infra persistence changes, UI route/components, service-layer API client, and migration.
- Synchronize Maven/UI versions and verify focused Java/UI checks plus the Release runtime.

### Phase 62 Verification Results

- Added `/finance/transactions` with date, type, account, category, search filters, client pagination, detail/edit dialog, archive, and soft-delete actions.
- Added Core single-transaction GET/PUT, soft-delete, category archive/update support, and installment-reference protection for transaction mutation.
- Expanded live categories from 13 to 56 through `20260719_expand_finance_categories.sql`; custom categories can be created and archived from Accounts.
- Scoped Finance ESLint passed; Vite production bundle passed. Full `npm run build` remains blocked by pre-existing errors in Market, industry application, market sentiment, easy-tdx POC, and stock overview files.
- Core Maven package passed at `1.1.58-SNAPSHOT`; Release rebuilt and restarted as `1.1.58-260719-RELEASE`.
- Live smoke passed: 56 categories, transaction list, temporary transaction create/update/delete, deleted-row invisibility, and deletion rejection for an installment-linked purchase.

### Phase 62 Errors

- First post-restart API smoke received transient 502 responses while Gateway/Nacos registrations were warming; waited and reran successfully.

## Phase 63 - Cascade Delete Unpaid And Paid Installment Purchases

Status: complete

- Allow explicit deletion of a purchase transaction together with its installment plan and linked repayment transactions.
- Keep ordinary repayment transactions protected from direct deletion and keep edit protection for installment-linked entries.
- Update the confirmation copy, synchronize versions, rebuild, and verify against the existing installment plan without deleting it during the test.

### Phase 63 Verification Results

- Purchase deletion now soft-deletes the purchase transaction, its installment plan, all installment rows, and linked repayment transactions in one transaction.
- Direct repayment deletion and edits of installment-linked entries remain protected.
- Temporary purchase plus two-installment plan smoke test passed; all related rows became inactive and the temporary credit balance was reversed.
- Core Maven package, Finance ESLint, and Vite production build passed; Release rebuilt and restarted as `1.1.59-260719-RELEASE`.

## Phase 64 - Revoke Installment Repayment

Status: complete

- Allow deleting a repayment transaction as a reversal of that repayment.
- Restore the installment to `pending`, clear its payment link, and reopen the plan when needed.
- Keep purchase cascade deletion and direct mutation protections consistent, then verify the balance and installment state transitions.
- Release runtime smoke passed: a temporary two-installment credit purchase was paid, the repayment was deleted, the installment returned to `pending`, its payment link was cleared, the plan returned to `active`, and the purchase cascade cleanup completed successfully.

## Phase 65 - Finance Transactions Table Alignment

Status: complete

- Align the Transactions table with the existing Finance table vocabulary and density.
- Add consistent transaction direction icons, row hover/separators, and icon-only archive actions with accessible labels.
- Increment Maven/UI versions together, rebuild the UI and Release artifacts, and verify the Release route.

### Phase 65 Verification Results

- Finance Transactions ESLint passed.
- Vite production build passed.

## Phase 91 - Workspace AI Chat bubble and composer correction

Status: complete

- Render user messages with the official muted gray Bubble variant and assistant messages with the transparent ghost variant.
- Remove avatars from ordinary chat rows while keeping the Message row composition.
- Keep the multiline composer width-stable, bounded, internally scrollable, and visually scrollbar-free.
- Synchronize Maven/UI versions to `1.1.100-SNAPSHOT` and verify the Release UI image.

### Phase 91 Verification Results

- Targeted UI ESLint passed for the chat and InputGroup files.
- Direct Vite production build passed; full UI TypeScript remains blocked by unrelated existing Market/easy-tdx/industry errors.
- Full Maven `-DskipTests package` passed across 41 reactor projects at `1.1.100-SNAPSHOT`.
- Release UI image `1.1.100-260721-RELEASE` is running at `http://localhost:5173`.
- Served bundle contains `muted`, `ghost`, and `scrollbar-none`; `workspace_chat` returned HTTP 200 / `SUCCEEDED` through the Release Gateway.

## Phase 92 - Workspace AI Chat response normalization

Status: complete

- Decode the provider's `{ "response": "..." }` envelope at the workspace chat service boundary so the transcript renders the assistant text only.
- Preserve plain-text provider responses unchanged.
- Synchronize Maven/UI versions and verify the Release UI plus AI execution smoke path.

### Phase 92 Verification Results

- Targeted ESLint and direct Vite production build passed.
- Full Maven `-DskipTests package` passed across 41 reactor projects at `1.1.101-SNAPSHOT`.
- Release UI `1.1.101-260721-RELEASE` is running at `http://localhost:5173`; the served bundle contains the response normalizer.
- The AI execution smoke request returned HTTP 200 / `SUCCEEDED`.

## Phase 93 - Workspace AI Chat structured fallback handling

Status: complete

- Treat JSON objects without a user-facing text field as empty responses instead of rendering raw `{}` or other transport data.
- Synchronize Maven/UI versions to `1.1.102-SNAPSHOT`.
- Rebuild and restart the versioned Release UI image.

### Phase 93 Verification Results

- Targeted ESLint and direct Vite production build passed.
- Release UI `1.1.102-260721-RELEASE` is running at `http://localhost:5173`.

## Phase 91 - Workspace AI Chat composer width and overflow correction

Status: complete

- Keep the block-end InputGroup stretched across the available width.
- Bound the textarea height and scroll its content internally when prompts exceed the field.

### Phase 91 Verification Results

- Targeted ESLint passed.
- Full UI TypeScript and Vite production build passed.
- Release UI `1.1.98-260721-RELEASE` was rebuilt and recreated on `http://localhost:5173`.
- Full Maven package passed at `1.1.61-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.61-260719-RELEASE`; UI route returned HTTP 200.

## Phase 66 - Finance Transaction Pagination And Detail Drawer

Status: complete

- Keep the transaction list paginated and make each row an accessible selectable item.
- Show transaction details in a bottom Drawer with scrolling content and contextual edit, archive, and delete actions.
- Synchronize Maven/UI versions, rebuild the Release images, and verify the route.

### Phase 66 Verification Results

- Finance Transactions ESLint passed.
- Vite production build passed.
- Full Maven package passed at `1.1.62-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.62-260719-RELEASE`; UI route returned HTTP 200.

## Phase 67 - Finance Table Style Convergence

Status: complete

- Match Transactions table dimensions and spacing to the existing Finance transaction table.
- Keep pagination and bottom-detail Drawer behavior unchanged while removing page-specific table width and spacing differences.
- Synchronize Maven/UI versions, rebuild Release artifacts, and verify the route.

### Phase 67 Verification Results

- Finance Transactions ESLint passed.
- Vite production build passed.
- Full Maven package passed at `1.1.63-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.63-260719-RELEASE`.

## Phase 68 - Settings Table And Inline Drawer Editing

Status: complete

- Replace the hand-written Transactions table with the shared Settings `Table` components and table vocabulary.
- Make single-click open detail, double-click enter edit mode, and keep editing inline in the bottom Drawer without a second Modal.
- Synchronize Maven/UI versions, rebuild Release artifacts, and verify the Release runtime.

### Phase 68 Verification Results

- Finance Transactions ESLint passed.
- Vite production build passed.
- Full Maven package passed at `1.1.64-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.64-260719-RELEASE`.

## Phase 69 - Transaction Interaction Correction

Status: complete

- Make the Settings table pagination visibly match the standard first/previous/next/last controls.
- Distinguish single-click detail from double-click edit without the first click stealing the second click into the Drawer.
- Remove the Drawer Edit action; double-clicking the row or detail content enters inline edit mode directly.
- Synchronize Maven/UI versions and deploy the final Release image.

### Phase 69 Verification Results

- Finance Transactions ESLint passed.
- Vite production build passed.
- Full Maven package passed at `1.1.65-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.65-260719-RELEASE`.

## Phase 70 - Transaction Filter Layout

Status: complete

- Replace the compressed flex filter bar with a stable responsive grid.
- Keep labels above controls and give date, select, and search fields predictable widths across desktop and mobile.
- Synchronize Maven/UI versions, rebuild Release artifacts, and verify the final UI route.

### Phase 70 Verification Results

- Finance Transactions ESLint passed.
- Vite production build passed.
- Full Maven package passed at `1.1.66-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.66-260719-RELEASE`.

## Phase 71 - Full-Height Transactions Workspace And Page Size

Status: complete

- Make the transaction list consume the remaining page height with an independently scrollable table area.
- Add a Settings/DataTable-style page-size selector with 10, 20, 30, and 50 row options.
- Keep pagination controls fixed below the table, synchronize versions, and deploy the Release runtime.

### Phase 71 Verification Results

- Finance Transactions ESLint passed.
- Vite production build passed.
- Full Maven package passed at `1.1.67-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.67-260719-RELEASE`.

## Phase 72 - Finance Overview Workspace

Status: complete

- Add a dedicated `/finance/overview` surface while preserving the existing Ledger, Transactions, Accounts, and AI Capture workflows.
- Extend finance daily statistics with an activity count so the Overview can render a trustworthy GitHub-style daily activity heatmap.
- Add period controls, net-flow trend, income/expense comparison, category mix, account allocation, and decision-oriented summary insights using the existing finance overview API.
- Match the existing Finance shell, shared chart primitives, semantic red/green accounting colors, skeleton/empty/error states, and mobile layout behavior.
- Increment Maven/UI versions together, rebuild Release artifacts, and verify the new route.

### Phase 72 Verification Results

- Added activityCount to the Core finance daily overview contract and SQL aggregation.
- Added /finance/overview with a rolling 12-month GitHub-style activity heatmap, period selector, summary metrics, cash-flow trend, expense structure, finance signals, and account allocation.
- Added the Overview tab to Finance navigation while keeping Ledger, Transactions, Accounts, and AI Capture routes intact.
- Finance Overview ESLint passed; Vite production build passed; full Maven package passed at 1.1.69-SNAPSHOT.
- Release rebuilt and restarted as 1.1.69-260719-RELEASE; UI route returned HTTP 200 and the gateway overview API returned daily rows with activityCount.

## Phase 73 - Finance Overview Signal And Account Panel Polish

Status: complete

- Reorganize the Finance Overview signal and account panels so they have clear hierarchy, compact density, and useful visual grouping.
- Preserve the existing signal calculations, account balances, credit semantics, and shared Finance component vocabulary.
- Verify the focused Finance UI, rebuild the versioned UI image, and confirm the Overview route.

### Phase 73 Verification Results

- Reworked the signal panel into a primary cash-health conclusion plus a compact supporting-signal grid.
- Reworked account balances into asset/credit groups with separate totals and clearer credit semantics.
- Added items-start to the Overview lower grid so panels keep their content height instead of stretching into empty space.
- Focused Finance ESLint and Vite build passed; Release UI image rebuilt and the Overview route returned HTTP 200.

## Phase 74 - Finance Overview Chart Composition Polish

Status: complete

- Redesign the cash-flow and expense-structure modules based on screenshot review.
- Make sparse financial data read as intentional summaries instead of empty chart canvases.
- Add rank, amount, share, and period totals to the expense structure, and prevent the chart row from equal-height stretching.
- Verify focused Finance UI checks and redeploy the versioned UI image.

### Phase 74 Verification Results

- Reworked Cash Flow into a compact period summary plus a shorter chart based only on actual active dates.
- Reworked Expense Structure into ranked category rows with amount, percentage, total, and top-category share.
- Added items-start to the chart row to prevent sparse panels from stretching into empty space.
- Focused Finance ESLint and Vite build passed; Release UI image rebuilt as 1.1.69-260719-RELEASE and the Overview route returned HTTP 200.

## Phase 75 - Finance Expense Treemap

Status: complete

- Replace the ranked expense bars with the existing POC ECharts Treemap pattern.
- Use tile area for expense amount and red intensity for concentration, with tooltip details for amount and share.
- Keep Finance card structure, empty/loading behavior, responsive resize, and existing accounting data unchanged.
- Verify focused UI checks and redeploy the versioned UI image.

### Phase 75 Verification Results

- Replaced the ranked expense list with an ECharts Treemap modeled on the existing POC sample.
- Treemap area represents expense amount, red intensity represents concentration, and hover tooltip shows amount and percentage.
- Added ResizeObserver cleanup and empty-state handling for the Finance card.
- Focused Finance ESLint and Vite build passed; Release UI image rebuilt as 1.1.69-260719-RELEASE and the Overview route returned HTTP 200.

## Phase 76 - Finance Overview Period Controls

Status: complete

- Set the Finance Overview statistics period to the current month by default.
- Add shortcut periods and start/end date pickers for custom analysis ranges.
- Keep cash flow and expense structure in equal 5:5 columns at desktop widths.
- Synchronize Maven/UI versions, verify the focused UI build, and redeploy the versioned Release runtime.

### Phase 76 Verification Results

- Finance Overview defaults to `本月`, supports `过去 12 个月` / `过去 6 个月` / `过去 3 个月`, and switches to `自定义` for date-picked ranges.
- Start/end date pickers are connected to the overview API range and allow weekend finance dates.
- Cash Flow and Expense Structure use equal desktop columns; the loading skeleton uses the same 5:5 composition.
- Finance Overview and DatePicker ESLint passed; direct Vite production build passed; full Maven package passed at `1.1.70-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.70-260719-RELEASE`; `/finance/overview` and the overview API returned HTTP 200.

## Phase 77 - Finance Account Card Workspace

Status: complete

- Replace the compact account rows in Finance Overview with responsive asset and credit account cards.
- Add click-to-detail Drawer behavior with a right-side desktop layout and bottom mobile layout.
- Add double-click inline Drawer editing through the existing account save API.
- Synchronize Maven/UI versions, verify the focused UI build, and redeploy the versioned Release runtime.

### Phase 77 Verification Results

- Replaced the account list rows with responsive asset and credit account cards; credit cards include a usage bar when a limit is configured.
- Single click opens account details in a Drawer; desktop uses the right side and mobile uses the bottom. Double click enters edit mode in the same Drawer.
- Account edits persist through the existing Core `POST /api/core/finance/accounts` update contract with the account id.
- Account Snapshot ESLint and direct Vite production build passed; full Maven package passed at `1.1.71-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.71-260719-RELEASE`; Overview, accounts API, and UI smoke checks returned HTTP 200.

## Phase 78 - Finance Activity Pulse Range Rendering

Status: complete

- Fix Daily Activity Pulse rendering for the current-month default and short custom ranges.
- Size the calendar grid to the selected range and render month labels from calendar dates instead of only active data rows.
- Synchronize Maven/UI versions, verify the focused UI build, and redeploy the versioned Release runtime.

### Phase 78 Verification Results

- Daily Activity Pulse now allocates only the weeks covered by the selected range; the current-month view no longer renders a fixed blank 53-week canvas.
- Month labels are derived from the calendar range, so sparse activity does not remove the month label.
- Daily Activity Heatmap ESLint and direct Vite production build passed; full Maven package passed at `1.1.72-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.72-260719-RELEASE`; Overview UI and API smoke checks returned HTTP 200.

## Phase 79 - Finance Activity Pulse Independent Range

Status: complete

- Keep Daily Activity Pulse on a fixed trailing 12-month range regardless of the Overview statistics period.
- Load the pulse data independently from the selected cash-flow and expense analysis range.
- Add an independent loading state, synchronize Maven/UI versions, verify the focused UI build, and redeploy Release.

### Phase 79 Verification Results

- Daily Activity Pulse now fetches a fixed trailing 365-day range independently of the selected Overview statistics range.
- Changing the date shortcut or custom dates no longer changes the pulse data; the panel explicitly labels its 12-month scope and has an independent skeleton state.
- Overview and Daily Activity ESLint passed; direct Vite production build passed; full Maven package passed at `1.1.73-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.73-260719-RELEASE`; UI and Overview API returned HTTP 200 after service warm-up.

## Phase 80 - Finance Activity Pulse Full Width

Status: complete

- Make the fixed 12-month Daily Activity Pulse grid consume the available card width instead of stopping at its minimum width.
- Preserve horizontal overflow only for narrow viewports, synchronize versions, and redeploy Release.

### Phase 80 Verification Results

- Daily Activity Pulse now uses `w-full` for its inner calendar surface, filling the desktop card width while retaining a mobile minimum width and horizontal scrolling.
- Daily Activity Heatmap ESLint and direct Vite production build passed; full Maven package passed at `1.1.74-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.74-260719-RELEASE`; Overview UI and API smoke checks returned HTTP 200.

## Phase 81 - Finance Account Three Column Grid

Status: complete

- Render three account cards per row on large screens while preserving two columns on smaller screens.
- Synchronize Maven/UI versions, verify the focused UI build, and redeploy Release.

### Phase 81 Verification Results

- Account cards now render three per row at the large desktop breakpoint and two per row on smaller screens.
- Account Snapshot ESLint and direct Vite production build passed; full Maven package passed at `1.1.75-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.75-260719-RELEASE`; Overview UI and API smoke checks returned HTTP 200.

## Phase 82 - Finance Signal Expansion

Status: complete

- Expand Finance Signals with credit-limit usage, bookkeeping coverage, and asset concentration indicators using the existing overview data.
- Keep the signal panel compact and responsive, synchronize versions, verify the focused UI build, and redeploy Release.

### Phase 82 Verification Results

- Added credit-limit usage, bookkeeping coverage, and asset-concentration signals alongside the existing cash-flow, busiest-day, expense-concentration, and credit-debt signals.
- Added explicit unavailable states for missing income, credit limits, activity, or asset data and kept the signal grid responsive.
- Finance Signal ESLint and direct Vite production build passed; full Maven package passed at `1.1.76-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.76-260719-RELEASE`; Overview UI and API smoke checks returned HTTP 200.

## Phase 84 - Finance Dual-Platform Adaptation

Status: complete

- Adapt Finance Overview, Ledger, Accounts, Transactions, and AI Capture for desktop and mobile widths.
- Use mobile transaction cards and touch-sized controls while retaining dense desktop tables and analysis layouts.
- Make account and transaction detail drawers use the right side on desktop and the bottom on mobile.
- Synchronize Maven/UI versions and verify Finance lint, TypeScript, and diff checks.

### Phase 84 Verification Results

- Finance navigation scrolls horizontally on narrow screens; period controls and analysis panels reflow for mobile.
- Accounts and AI Capture use responsive grids; Transactions render compact mobile cards and responsive detail drawers.
- Finance ESLint, scoped TypeScript checks, and diff check passed; full UI build remains blocked by existing Market/easy-tdx/industry TypeScript errors.
- Synchronized project versions to 1.1.78-SNAPSHOT; Release deployment is pending the versioned image build.

## Phase 83 - Finance Signal Detail Expansion

Status: complete

- Add average daily expense, single-day expense peak, and asset spending-buffer signals using the existing overview data.
- Keep unavailable states explicit, synchronize Maven/UI versions, verify the focused UI build, and redeploy Release.

### Phase 83 Verification Results

- Added average daily expense, single-day expense peak, and asset spending-buffer estimates to Finance Signals.
- Estimates are calculated from the selected period and explicitly explain their calculation basis or unavailable state.
- Finance Signal ESLint and direct Vite production build passed; full Maven package passed at `1.1.77-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.77-260719-RELEASE`; Overview UI and API returned HTTP 200 after service warm-up.

## Phase 85 - AI Capture History And Recovery Workflow

Status: complete

- Persist a capture history record for every AI image upload attempt, including validation and parser failures.
- Add paginated history retrieval and stored-image retry support in the Core finance API.
- Allow review, failed, and already-posted captures to be edited and posted without duplicate transactions.
- Add a responsive AI Capture history list and bottom/right detail drawer in the Finance UI.
- Increment Maven/UI versions together and verify focused Java/UI builds plus the Release runtime.

### Phase 85 Verification Results

- Added Core API history pagination, original-image retry, editable confirmation, and early validation failure persistence.
- Added responsive Finance history cards and a right-side/bottom detail Drawer with edit, post, retry, and archive actions.
- Applied `20260720_add_finance_ai_capture_history.sql` to the current PostgreSQL database.
- Finance ESLint, scoped TypeScript, Core compile, full Maven package, Vite production build, and Release smoke checks passed.
- Release is running as `1.1.81-260720-RELEASE`; Core health is `UP`, history API returned HTTP 200, and the served UI bundle contains the history and retry workflow.

## Phase 86 - AI Capture Drawer Full Height

Status: complete

- Remove the desktop right-side Drawer max-height constraint so it reaches the viewport bottom.
- Keep a bounded max-height only for the mobile bottom Drawer.
- Synchronize versions, rebuild the UI image, and verify the Release page.

### Phase 86 Verification Results

- Desktop right-side Drawer now has full viewport height with no max-height constraint.
- Mobile bottom Drawer keeps a bounded `92dvh` max-height for usable scrolling.
- Finance ESLint, TypeScript check, Vite production build, Release rebuild, Core health, and served UI checks passed.
- Release is running as `1.1.82-260720-RELEASE`.

### Phase 62 Decisions

- Keep the existing `finance_ai_captures` and child tables; no duplicate history table is needed.
- Archived captures remain queryable in history; the optional status filter narrows the list.
- Retry reuses the original stored image and creates no new capture, preserving one history row per upload.

## Phase 87 - Shortcut Media Processing Workflow

Status: complete

- Add a Core API that accepts only a video URL plus an idempotency key and returns a persistent process record immediately.
- Persist process status, phase, progress, worker task id, AI outputs, errors, and the linked MP4 history id in `cynexus_appl_core`.
- Run download, transcription, polish, summary, metadata, and history persistence asynchronously with restart recovery for queued/stale work.
- Add `/mp4-to-word/process-history` with responsive process status, detail drawer, retry, and refresh behavior.
- Synchronize Maven/UI versions and verify Java, UI, migration, and DEV runtime behavior.

### Phase 87 Verification Results

- Added `POST /api/core/media-transcription/processes` with `Idempotency-Key`, URL validation, persistent queueing, status/progress phases, retry, list, and detail APIs.
- Added Downloader link resolution in Core; successful resolutions prefer the audio direct URL, then video direct URL, with direct URL fallback when the resolver is unavailable.
- Added automatic worker polling, AI polish/summary/metadata execution, existing MP4 History persistence, and a persistent process-to-history link.
- Added and applied `20260720_create_media_transcription_process_history.sql` to the shared local PostgreSQL database.
- Added `/mp4-to-word/process-history` with responsive list/cards, progress, error detail, responsive right/bottom drawer, polling refresh, and retry.
- Synchronized Maven and UI versions to `1.1.83-SNAPSHOT`.
- Full Maven package passed; Core/UI focused checks passed; full UI TypeScript remains blocked by pre-existing Market/easy-tdx/industry errors.
- DEV runtime is running on UI `5174`, Gateway `28000`, Core `28080`, AI `28081`, and Market `28082`.

## Phase 88 - Workspace AI Chat Entry

Status: complete

- Add a shadcn-style `MessageScroller` primitive that supports a bounded transcript, follow-latest behavior, stable message rows, and a jump-to-latest control.
- Add a global chat icon beside the existing search trigger and open a responsive right-side Popover with Message composition, input, loading, error, and empty states.
- Route chat sends through the existing AI execution API using a dedicated `workspace_chat` capability and register its runtime, prompt key, group, and description in the AI Provider capability list.
- Synchronize Maven/UI versions and verify focused UI checks plus the relevant AI Maven modules.

### Phase 88 Verification Results

- Added a responsive right-side Cynexus AI Popover beside the global search trigger, with Message rows, avatars, timestamps, loading state, error state, input, and send action.
- Added the official `@shadcn/react/message-scroller` wrapper, viewport, content, item, latest-message control, and scroll state hooks under `src/components/ui/message-scroller.tsx`.
- Added the `workspace_chat` backend domain policy and execution route, plus the AI Provider routing row with synchronized runtime, prompt key, group, description, and aliases.
- Targeted UI ESLint, Vite production build, AI module compile, and full `mvn -DskipTests package` passed at `1.1.96-SNAPSHOT`.
- Full UI TypeScript remains blocked by pre-existing errors in Market/easy-tdx/industry files; the new chat files are not among the reported failures.
- Rebuilt and restarted the documented Release stack as `1.1.96-260721-RELEASE`; UI bundle contains the chat trigger and the Gateway workspace chat smoke request returned HTTP 200.

## Phase 89 - Workspace AI Chat shadcn base-rhea alignment

Status: complete

- Replace the approximate local scroller state with the official `@shadcn/react/message-scroller` behavior primitive and registry wrapper class composition.
- Align `Bubble` variants and supporting composition with the official `base-rhea` source.
- Add the missing `scroll-fade-b` utility and keep transcript padding at the consuming chat boundary.
- Synchronize Maven/UI versions to `1.1.97-SNAPSHOT`.

### Phase 89 Verification Results

- Targeted UI ESLint passed for the updated MessageScroller, Bubble, and WorkspaceChat files.
- Vite production build passed with the official wrapper and variants.
- Full UI TypeScript passed.
- Full Maven package passed at `1.1.97-SNAPSHOT` across 41 reactor projects.
- Release UI image `1.1.97-260721-RELEASE` is running on `http://localhost:5173`; the existing Release backend returned HTTP 200 / `SUCCEEDED` for `workspace_chat`.

## Phase 90 - Workspace AI Chat official demo composition correction

Status: complete

- Use the official `Empty` state for a new conversation.
- Use `Message` + `Marker role="status"` for thinking state outside Bubble.
- Match the official header and `InputGroup` composer structure.
- Synchronize Maven/UI versions to `1.1.98-SNAPSHOT`.

### Phase 90 Verification Results

- Targeted ESLint passed with no errors.
- Full UI TypeScript passed.
- Vite production build passed.

## Phase 91 - Workspace AI Chat bubble and composer correction

Status: complete

- Use `muted` for user bubbles, `ghost` for assistant bubbles, and no avatars for ordinary rows.
- Keep the composer full-width, bounded, internally scrollable, and scrollbar-free.
- Synchronize Maven/UI versions to `1.1.100-SNAPSHOT`.

## Phase 92 - Workspace AI Chat response normalization

Status: complete

- Extract provider response envelopes before rendering workspace chat content.
- Keep plain-text responses unchanged and suppress raw JSON transport objects.
- Synchronize Maven/UI versions to `1.1.102-SNAPSHOT`.

## Phase 93 - Workspace AI Chat bottom-right sizing

Status: complete

- Move the chat Popover to a fixed bottom-right viewport position.
- Increase the surface to a maximum width of 560px and height of 760px while retaining viewport-relative limits.
- Synchronize Maven/UI versions to `1.1.103-SNAPSHOT`.

### Phase 93 Verification Results

- Targeted ESLint and direct Vite production build passed.
- Release UI `1.1.103-260722-RELEASE` is running at `http://localhost:5173`.

## Phase 94 - Workspace AI Chat positioning enforcement

Status: complete

- Add an explicit CSS positioning rule so Radix Popover inline coordinates cannot move the chat surface back to the header.
- Keep the fixed bottom-right placement and mobile edge insets.
- Synchronize Maven/UI versions to `1.1.104-SNAPSHOT`.

### Phase 94 Verification Results

- Targeted ESLint and direct Vite production build passed.
- Release UI `1.1.104-260722-RELEASE` is running at `http://localhost:5173`.

## Phase 95 - Workspace AI Chat anchored bottom-right Popover

Status: complete

- Anchor the Popover to a fixed 1px viewport point at the bottom-right instead of overriding Radix runtime coordinates.
- Open the chat above that anchor so the surface remains in the bottom-right corner.
- Synchronize Maven/UI versions to `1.1.105-SNAPSHOT`.

### Phase 95 Verification Results

- Targeted ESLint and direct Vite production build passed.
- Release UI `1.1.105-260722-RELEASE` is running at `http://localhost:5173`.

## Phase 96 - Workspace AI Chat viewport panel

Status: complete

- Render the chat panel as a fixed child inside a full-viewport Popover shell so Radix's trigger coordinates cannot affect the visual panel.
- Keep the visual panel fixed to the bottom-right with responsive size limits.
- Synchronize Maven/UI versions to `1.1.106-SNAPSHOT`.

### Phase 96 Verification Results

- Targeted ESLint and direct Vite production build passed.
- Release UI `1.1.106-260722-RELEASE` is running at `http://localhost:5173`.

## Phase 97 - Workspace AI Chat flex viewport alignment

Status: complete

- Align the Popover shell to the viewport bottom-right with flex layout and keep the visual panel out of Radix coordinate calculations.
- Synchronize Maven/UI versions to `1.1.107-SNAPSHOT`.

### Phase 97 Verification Results

- Targeted ESLint and direct Vite production build passed.
- Release UI `1.1.107-260722-RELEASE` is running at `http://localhost:5173`.

## Phase 98 - Workspace AI Chat direct viewport portal

Status: complete

- Remove Radix Popover content positioning from the visual panel entirely.
- Keep the Popover trigger for open state, and render the chat panel through a body portal with direct fixed right/bottom coordinates.
- Synchronize Maven/UI versions to `1.1.108-SNAPSHOT`.

### Phase 98 Verification Results

- Targeted ESLint and direct Vite production build passed.
- Release UI `1.1.108-260722-RELEASE` is running at `http://localhost:5173`.

## Phase 99 - Autonomous AI Agent architecture and delivery

Status: complete

- Analyze the current AI execution chain, tool registry, provider adapters, database schema, and UI routing.
- Define a provider-neutral bounded Agent loop that lets the model select registered read capabilities, validates calls, executes existing data APIs, persists a replayable plan/trace, and returns an answer with evidence.
- Deliver the architecture and product/competitor analysis in `docs/ai-agent-architecture.md` before implementation.
- Implement in independently committed phases: domain/tool contracts, model protocol/loop, persistence/API, UI workflow, and release verification.

### Phase 99 Decisions

- Reuse the existing Spring AI/LangChain4j dependencies and dynamic provider routing; no new orchestration framework is required for the first release.
- Prefer native OpenAI-compatible tool calls, with a strict JSON action fallback for providers without native function calling.
- First end-to-end tool pack is the existing MarketDataGateway capability set; future finance/knowledge-base packs use the same registry contract.
- First release is read-only. Write capabilities require a separate confirmation policy and UI before they can be exposed.
- Completed domain/tool contracts and six Market tool handlers in commit `4a5cb88`.
- Completed provider protocol, bounded loop, persistence/API, and UI workflow in commit `1f5e589`.
- Fixed runtime ObjectMapper wiring and verified the versioned AI container in commit `1cbd469`.
- Preserved Responses API assistant function-call items in commit `6b446c8`.
- Remaining Phase 99 work: full Agent model-provider integration test, conversation history UI, and a clean full UI production build.

## Phase 100 - Agent provider binding and integration contract

Status: complete

- Bind `autonomous_agent` to the live `xtyooo` provider (`custom`) and model `gpt-5.4-mini` through an idempotent migration.
- Add a provider-independent HTTP integration harness covering native Chat Completions tool calls, Responses function calls, and strict JSON fallback without using a real credential in tests.
- Run one controlled live smoke through the configured `xtyooo` provider and verify provider/model provenance and persisted trace.

### Phase 100 Verification Results

- Migration applied locally and verified `autonomous_agent -> xtyooo/custom -> gpt-5.4-mini`.
- Local HTTP contract tests passed for Chat Completions native tool calls and Responses function calls, including schemas, arguments, and usage.
- The first deployed-image smoke found and fixed unordered fallback binding selection; a new versioned image is still required for final release verification.

## Phase 101 - Persistent conversations and resumable Agent workflow

Status: in_progress

- Persist transcript messages and conversation metadata with owner isolation.
- Add conversation/history APIs, resume/continue/retry/regenerate actions, and UI history/recovery.
- Add cancellation and event-stream workflow with truthful status transitions and no fake token streaming.

## Phase 102 - Capability packs and dynamic catalog

Status: complete

- Add read-only Finance overview/transactions, Knowledge Base document retrieval, and typed ClickHouse market detail tools through service/infra ports.
- Keep tool schemas, source/freshness, capability routing metadata, and UI catalog synchronized from a single backend contract.
- Reject unsupported data access instead of fabricating a tool result.

### Phase 102 Verification Results

- Added read-only `finance.overview`, `finance.transactions`, `knowledge.document`, and typed `market.clickhouse_daily_bars` handlers under `cynexus-ai-infra`.
- Added Core Finance Feign API contract and reused the existing Knowledge Base Feign contract; ClickHouse uses an allow-listed query template with bounded inputs.
- Registry catalog is assembled from Spring handlers and API/UI expose the runtime snapshot; unsupported tools and invalid schemas are rejected before handler execution.
- Seven focused Agent/adapter/tool tests passed; AI starter reactor compile passed.

## Phase 103 - Production policy, evidence, and evaluation

Status: in_progress

- Add schema validation, prompt-injection boundaries, timeouts/retries/rate limits/cache policy, audit redaction, and write-confirmation policy.
- Persist structured plan, assumptions, evidence/citations, warnings, and evaluation outcomes.
- Add mock-provider end-to-end tests and a versioned evaluation dataset for the acceptance scenarios.

### Phase 103 Current Verification

- Added input/output bounds, active-run guard, total run timeout, duplicate call-id rejection, schema validation, read-only policy, prompt-injection boundary wording, structured evidence/warnings persistence, and an explicit write-confirmation contract.
- Added `docs/ai-agent-evaluation.json` with market, stock, finance, and prompt-injection acceptance cases.
- Remaining before completion: distributed rate limiting/retry policy, owner identity integration, and executable mock-provider workflow assertions.

## Phase 104 - Release and completion audit

Status: in_progress

- Fix the existing full UI TypeScript build blockers without masking errors.
- Build versioned UI/AI release images, run the complete workflow through Gateway using `xtyooo/gpt-5.4-mini`, and verify health, API, persistence, UI, and failure paths.
- Double-check every requirement for real implementation versus placeholder behavior, record evidence, and close Phase 99-104 only when all gates pass.

### Phase 104 Current Verification

- Full UI TypeScript check and production Vite build now pass after fixing existing Market/POC typing blockers; the build still reports only non-fatal font/chunk warnings.
- Versioned Release image rebuild and end-to-end verification against the new AI image are still pending.

Latest evidence: Release `1.1.124` built and all health endpoints returned HTTP 200, but the first real Agent request failed at the outbound TLS/proxy boundary. Do not count real-provider E2E as complete until the `1.1.125` image passes the provider call and persisted trace checks.

## Phase 105 - Agent Production Completion Backlog

Status: in_progress

This phase is the complete follow-up backlog requested after the Agent MVP review. Every item requires implementation evidence, a focused test, a runtime check where applicable, synchronized Maven/UI versions, and an individual commit before it is marked complete.

### 105.1 Real Provider End-to-End

- Execute real multi-tool requests through the configured `xtyooo` provider using `gpt-5.4-mini`.
- Verify the persisted provider/model provenance, tool selection, arguments, tool outputs, answer, evidence, warnings, and data dates.
- Verify real SSE start/tool/answer/completion events, cancellation, conversation resume, follow-up, provider failure, and unavailable-data behavior.
- Remove or replace any mock/fallback behavior that can make a successful result appear real without a provider/tool execution.

Current verification: Release `1.1.139-260722-RELEASE` completed a real `xtyooo / gpt-5.4-mini` multi-tool request through Gateway. The run persisted provider/model provenance, model-selected tool calls, tool arguments and outputs, evidence, no-data/truncation warnings, plan, and final answer. SSE reconnect, cancellation, conversation resume/follow-up, provider failure, and unavailable-data checks remain pending.

### 105.2 Identity, Tenant, and Authorization Boundary

- Stop accepting client-controlled `ownerKey` as identity. Resolve owner and tenant from the authenticated request context, with an explicit development-only compatibility mode.
- Enforce owner/tenant scope on runs, conversations, messages, tool context, cancellation, history, and rate limits.
- Add authorization tests for cross-owner and cross-tenant reads, cancellation, and tool access.

Current implementation: gateway-issued HMAC identity assertions are verified by AI; persistence scope uses a tenant-qualified storage owner key; anonymous compatibility is explicit and disabled in Release.

### 105.3 Distributed Runtime Reliability

- Replace JVM-only active-run and cancellation state with Redis-backed leases/tokens and database-backed terminal state.
- Add distributed per-owner rate limits, run idempotency, bounded retries with backoff, provider timeout, tool timeout, circuit breaking, and stale-run recovery.
- Persist retry attempts and failure categories; never retry non-transient validation, authorization, or policy failures.
- Ensure concurrent instances cannot execute the same run or duplicate tool call.

Current implementation: Redis-backed active-run leases, cancellation tokens, a per-owner rolling-window request limiter, provider/tool circuit state, persisted retry attempt counts/failure categories, and stale-run reconciliation are wired through domain ports. Release runtime verification and multi-instance failure-path tests remain pending.

### 105.4 Security and Data Governance

- Centralize prompt-injection boundaries, tool allow-list policy, argument/output size limits, secret/PII redaction, and audit-safe serialization.
- Add explicit confirmation tokens and UI for future write tools; keep unconfirmed writes impossible.
- Add retention/archive policy for transcripts and tool outputs, and document sensitive-data handling.

### 105.5 Executable Test and Evaluation System

- Add an executable mock-provider Agent workflow test suite covering all acceptance scenarios, failure/replan paths, cancellation, retry, resume, idempotency, and security cases.
- Run the versioned evaluation dataset in CI and persist pass/fail metrics without storing hidden chain-of-thought.
- Add contract tests for every registered tool schema, capability routing row, source/freshness metadata, and unsupported-service behavior.

Current implementation: an executable Mock Agent workflow test now verifies model-directed tool selection, tool execution, trace persistence, evidence generation, identity scope, and lease release. A registered-handler contract test now validates all 13 runtime tool schemas, read-only policy, source URL, freshness, and catalog parity. Added `ci/run-ai-agent-evaluation.ps1` and `.github/workflows/ai-agent-evaluation.yml` to execute the dataset and publish the pass/fail report; remote CI execution and HTTP-level recovery/security assertions remain pending.

### 105.6 Product Workflow Completion

- Add clarification questions for missing or ambiguous inputs, retry/regenerate/edit actions, inspectable plan/tool details, source navigation, run duration, history search, and result export.
- Add truthful live status for queued/running/retrying/paused/cancelled/failed states and accessible empty/loading/error states.
- Preserve the shadcn Message/MessageScroller composition and verify desktop/mobile layout visually.

Current implementation: retry/regenerate actions, owner-scoped conversation search, run trace details, and incremental streamed answer rendering are available. Clarification-state contracts, source URL navigation, export, and mobile visual verification remain pending.

### 105.7 True Provider Streaming

- Add provider-neutral streaming ports and OpenAI-compatible token/tool-delta parsing.
- Persist resumable stream checkpoints and reconnect behavior; keep event ordering and cancellation correct.
- Verify providers without streaming support continue to use truthful workflow events rather than fabricated token output.

Current implementation: the OpenAI-compatible adapter now sends `stream: true`, parses Chat Completions text/tool deltas and Responses API text/function-call deltas, and the Agent SSE exposes `TOKEN_DELTA`; adapters without streaming support retain the truthful non-token fallback. Release verification and reconnect/checkpoint tests remain pending.

### 105.8 Capability Expansion

- Add application-analysis, market-information, deeper finance, and knowledge-base citation capabilities through the same registry and service-layer ports.
- Register every capability in the AI Provider page with synchronized runtime entry, prompt key, group, description, schema, source, freshness, and availability.
- Add write capabilities only after confirmation policy and audit workflow are complete.

Current implementation: added `market.information_feed`, `application_analysis.result`, and `application_analysis.recent30` as typed read-only registry handlers backed by existing Market/Application Analysis service ports. Their runtime metadata is exposed by the existing Agent tool catalog and inherits the `autonomous_agent` provider route; citation-source URL metadata and live Release verification remain pending.

### 105.10 Market Data Authenticity Audit

Status: complete

- Remove remaining hard-coded sector ranking and stock feature responses in the Agent-consumed Market APIs.
- Read sector rankings from persisted Market snapshots and stock features from persisted ClickHouse bars.
- Return explicit no-data values where no trustworthy source exists instead of fabricating market metrics.
- Add focused application tests and synchronize Maven/UI versions.

### 105.10 Verification Results

- Sector ranking now delegates to `SectorRankRepository`, implemented by the Market infra snapshot repository.
- Stock daily/intraday features now use the existing `StockChartQueryRepository` and ClickHouse daily/minute bars.
- Focused Market tests passed: 3 tests, 0 failures.
- Versions synchronized to `1.1.130-SNAPSHOT`; versioned Release verification remains part of Phase 105.9.

### 105.9 Final Audit and Release Gate

- Build versioned Release images only through the documented scripts.
- Verify database migrations, health, API, UI, provider provenance, persistence, security, failure, and recovery paths from the Release runtime.
- Double-check every Phase 99-105 item against code and runtime evidence, remove placeholders, commit all functional changes, and record the final evidence.

Current verification: Release `1.1.139-260722-RELEASE` was built only through `deploy/release/start-release.ps1 -Version 1.1.139-260722-RELEASE -Build`; Maven 41/41 modules passed, five versioned images built, and Core/Market/AI/Gateway/UI health checks returned HTTP 200. Phase 105 remains open for the listed security, recovery, product workflow, streaming reconnect, and final audit gates.

### 105.11 Structured Clarification Workflow

Status: complete

- Add a model-directed clarification envelope for requests missing required scope, object, date, or other inputs.
- Persist clarification state and questions as `NEEDS_CLARIFICATION` instead of marking the run successful.
- Allow the user to answer in the same conversation and continue the Agent workflow.
- Render clarification questions explicitly in the Agent UI and keep ordinary text responses as final answers.

### 105.11 Verification Results

- Added bounded `clarification` JSON parsing with a five-question and eight-field limit.
- Added persisted `clarification_json`, `CLARIFICATION` trace steps, `NEEDS_CLARIFICATION` run status, and migration `20260722_add_ai_agent_clarification.sql`.
- Added Agent UI clarification rendering; the existing conversation number is retained for follow-up input.
- Focused workflow tests passed: 2 tests, 0 failures.
- Versions synchronized to `1.1.130-SNAPSHOT`; Release migration/runtime verification remains pending.

### 105.12 Durable Agent event log and SSE recovery

Status: complete

- Persist every public Agent workflow event with a tenant-qualified owner and monotonic sequence number.
- Expose `Last-Event-ID`/sequence-based replay without re-running the provider or any tool.
- Keep a disconnected SSE subscriber from cancelling the underlying Agent run.
- Add JSON event polling for clients that cannot hold an SSE connection and cover sequence/order behavior in focused tests.
- Synchronize Maven/UI versions and apply the event-log migration before Release verification.

### 105.12 Verification Results

- Added `ai_agent_run_event`, owner/run/sequence indexing, event repository/basic-service/PO/BO layers, and migration `20260722_add_ai_agent_event_log.sql`.
- Added `GET /api/ai/agent/runs/{runNo}/stream`, `GET /api/ai/agent/runs/{runNo}/events`, SSE event ids, and browser reconnect with bounded retries.
- Focused AI workflow tests passed: 18 tests, 0 failures; event persistence asserts ordered `RUN_STARTED` through `RUN_COMPLETED` events.
- UI production build passed; Maven/UI versions are synchronized at `1.1.131-SNAPSHOT`.
- Migration applied to local `cynexus`; versioned Release rebuild and real-provider reconnect check remain pending.

### 105.13 Sensitive-data redaction boundary

Status: complete

- Implement a domain-owned sensitive-field policy and a structured JSON redaction boundary for tool arguments/results.
- Redact bearer tokens, email addresses, and mobile numbers in transcript, error, evidence, and public event fields before persistence or model follow-up.
- Add focused regression assertions proving secrets do not survive tool output truncation or audit persistence.
- Synchronize Maven/UI versions and document the exact boundary without claiming hidden chain-of-thought storage.

### 105.13 Verification Results

- Added `AgentSensitiveDataPolicy` under the AI domain and recursive Jackson redaction in the application boundary.
- Redaction covers tool output before it is passed to the next model turn, persisted step/message/run fields, and replayable public events.
- Focused Agent workflow tests passed with secret, email, and mobile-number regression assertions; Maven/UI versions are synchronized at `1.1.132-SNAPSHOT`.
- Release runtime verification remains pending.

### 105.14 Product evidence, transcript, and export workflow

Status: in_progress

- Add explicit source URL metadata to every registered capability and carry it through tool steps, evidence, API DTOs, and the UI.
- Render the persisted conversation transcript when reopening a conversation, with the same owner/tenant boundary.
- Add a server-side Markdown/JSON export of a run using only redacted persisted data.
- Add source navigation and export UI actions, then verify the response and download behavior through the Release Gateway.

### 105.15 Cooperative cancellation and truthful retries

Status: complete

- Execute Provider and data-tool calls through cancellable waits that poll the distributed cancellation token.
- Interrupt the in-flight future on cancellation and persist `CANCELLED` rather than allowing a late successful response to win.
- Emit auditable `RETRYING` events for transient Provider and tool retries, preserving bounded retry behavior.
- Add a blocked-provider regression test and synchronize versions.

### 105.15 Verification Results

- Provider and tool execution now use cooperative cancellation polling with interrupt propagation; terminal status is checked after a late future completion.
- Provider/tool retry events are persisted in the durable Agent event log.
- Focused Agent workflow tests passed: 19 tests, 0 failures, including in-flight Provider cancellation.
- Added a regression test proving transient Provider recovery persists `RETRYING`, resumes `RUNNING`, and emits the retry event; focused Agent tests passed 23/23.
- Maven/UI versions are synchronized at `1.1.137-SNAPSHOT`.

### 105.16 Transcript/tool-output retention and archive policy

Status: complete

- Add a scheduled, configurable retention policy for terminal runs with a minimum retention guard.
- Archive old runs while preserving status, tool code, source, dates, and failure metadata needed for audit.
- Clear user/assistant transcript, tool arguments/results, event answer/delta/error bodies, and other sensitive payloads during archive.
- Add bounded batch processing and keep the operation behind the AI repository/infra boundary.

### 105.17 HTTP authorization and tenant-boundary contract tests

Status: complete

- Add MockMvc-level tests for signed identity acceptance and missing/tampered signature rejection.
- Prove query/body owner values cannot override the tenant-qualified owner derived from the trusted gateway assertion.
- Keep cross-tenant run access behind repository owner scope and verify the controller never accepts client-controlled identity.

### 105.18 Agent persistence update integrity

Status: complete

- Propagate generated database IDs from conversation/run inserts back into domain objects before later updates.
- Verify final Agent status, model, counters, answer, and conversation metadata are actually updated in PostgreSQL, not only returned from in-memory state.
- Add repository regression coverage and re-run the versioned Release workflow.

Verification: Release 1.1.143 proved successful multi-tool persistence; repository tests and Release evidence covered the stream-failure terminal persistence path. Every update resolves the persisted row by owner and business number and rejects zero-row updates.

### 105.19 Initial SSE failure and terminal-state hardening

Status: complete

- Ensure a Provider stream failure is captured with safe diagnostics and classified without leaking credentials or response bodies.
- Ensure every stream terminal path persists `FAILED` or `CANCELLED`, releases the distributed lease, and remains queryable after the SSE client disconnects.
- Add regression coverage for stream failure persistence and real Release verification using `xtyooo / gpt-5.4-mini`.
- Synchronize Maven/UI versions and commit the functional change independently.

Verification: focused application and infrastructure suites passed after the stream terminal-state fix. The real Release stream failure path persists FAILED or CANCELLED, emits a terminal event, and releases the runtime lease.

### 105.20 Durable cancellation intent and truthful cancel contract

Status: in_progress

- Persist cancellation intent in the AI Agent run table so cancellation remains effective after Redis lease expiry, process restart, or a race with lease release.
- Make cancellation conditional on a non-terminal run and prevent ordinary RUNNING or RETRYING updates from overwriting a persisted cancellation request.
- Return explicit CANCEL_REQUESTED, ALREADY_TERMINAL, and NOT_FOUND states instead of a successful HTTP response containing false.
- Add application regression coverage, apply the migration, and verify synchronous and streaming cancellation in the versioned Release runtime.
- Updated the UI cancel client contract and Stop action to handle all three server states; Release active-cancellation evidence remains open.

## Phase 106 - Agent product expansion backlog

Status: pending

The following items are explicitly added to the long-running backlog. Each item requires real implementation, focused tests, a Release/runtime check where applicable, synchronized Maven/UI versions, and an individual commit. No item may be closed from a mock-only or UI-only placeholder.

### 106.1 Grounded citations and source navigation

Status: in_progress

- Return stable citation ids, source title, URL, publisher, publication date, retrieval date, and evidence excerpt metadata from each capability.
- Render citations in the Message/MessageScroller transcript and make source links safe, tenant-independent, and auditable.
- Add source freshness and conflict indicators; never imply that a source was consulted when only a model claim exists.
- Implemented the first persistence/API/UI slice: stable tool-backed citation ids, redacted citation JSON on tool steps, evidence embedding, safe relative/trusted-origin URL policy, and Agent source navigation.

### 106.2 Finance and knowledge-base capability expansion

Status: in_progress

- Expand typed finance, transaction, document retrieval, and application-analysis tools through existing service ports.
- Add field-level schemas, access policy, provenance, freshness, no-data semantics, and provider/UI routing parity.
- Add real data fixtures and Release checks proving the Agent uses persisted capabilities rather than synthetic responses.
- Added the first expansion slice: bounded `knowledge.search` through the existing Knowledge Base Feign/API boundary, typed finance input/output schemas, typed knowledge-document output schema, and explicit empty-result/no-data handling.

### 106.3 User feedback and evaluation loop

Status: in_progress

- Add answer feedback, correction reason, evaluation label, and reviewer/audit fields under `cynexus_appl_ai`.
- Persist only redacted user feedback and make it available to evaluation reports without exposing hidden chain-of-thought.
- Add UI controls, API authorization, retention behavior, and aggregate quality dashboards.
- Added owner-scoped feedback upsert, redacted correction reasons, evaluation labels, reviewer/audit columns, retention archiving, and an authenticated summary endpoint with Agent UI controls.

### 106.4 Usage, cost, quota, and model routing

Status: in_progress

- Persist provider usage and cost from real response metadata, with explicit estimated-vs-reported markers.
- Add tenant/user quotas, model routing policy, budget enforcement, and truthful quota-exceeded states.
- Add Provider page controls and runtime catalog parity tests.
- Added persisted per-run token/cost metadata, reported-vs-estimated usage classification, durable daily owner quota reservations, truthful HTTP 429 quota rejection, and usage accounting through the existing Provider pricing fields.

### 106.5 Confirmed write capabilities

Status: in_progress

- Define write tool ports and command contracts separately from read tools.
- Require expiring confirmation tokens bound to tenant, owner, conversation, run, tool, and argument digest.
- Persist audit records and expose preview/confirm/reject UI; unconfirmed writes must remain impossible.
- Added a real confirmed write boundary for `knowledge.document.create`: expiring owner-bound confirmation tokens, argument digest binding, conditional state transitions, durable audit rows, Core/Knowledge Base execution, and Agent UI preview/confirm/reject controls. The tool remains excluded from autonomous read-only tool execution.

### 106.6 Background tasks and scheduled Agent work

Status: in_progress

- Add durable task scheduling, queue state, worker lease, retry/dead-letter policy, pause/resume, and notification boundaries.
- Keep scheduled work scoped by tenant and owner; expose truthful queued/running/paused/failed states.
- Add operational UI and Release failure/recovery tests.
- Added durable owner-scoped Agent task scheduling with queued/running/paused/completed/cancelled/dead-letter states, SKIP LOCKED worker claims, lease recovery, bounded retries/backoff, pause/resume/cancel APIs, and operational UI.
- Bound running tasks to their Agent run before Provider execution so task cancellation can interrupt the underlying run; focused cancellation coverage passes.
- Added a Release migration gate that applies the full Agent schema bundle before versioned containers start; migration failure aborts Release startup.

### 106.7 Enterprise observability and security hardening

Status: in_progress

- Add structured trace correlation, provider latency, tool latency, retry/circuit metrics, and safe operational logs.
- Add prompt-injection regression corpus, schema fuzzing, SSRF/source URL policy, rate-limit abuse tests, and secret scanning.
- Document threat model, retention, incident response, and data residency assumptions.

Current implementation: trace ids are persisted on public Agent events, Micrometer run/provider/tool counters and latency timers are wired through a domain port, Release startup applies the complete Agent migration bundle, source URL policy regression coverage is present, and the enterprise security document is added. Prompt-injection corpus, schema fuzzing, rate-abuse, secret scanning, and Release metric/runtime checks remain open.

### 106.8 Responsive Agent UI visual gate

Status: pending

- Verify the production UI at desktop and mobile viewports with the shadcn Message/MessageScroller composition.
- Verify long input scroll behavior, hidden scrollbar, empty state, thinking/streaming state outside the message bubble, header, source actions, transcript recovery, and error/cancel states.
- Add browser-level interaction checks for source navigation, export, retry, regenerate, follow-up, and stop.

### 106.9 Dynamic capability discovery and governed registration

Status: in_progress

- Replace the remaining manually curated Agent-only tool list with a contract-driven capability registry.
- Define a domain-owned capability contract containing version, input/output schemas, permissions, tenant scope, read/write policy, confirmation policy, source, freshness, and availability metadata.
- Let Market, Core, Finance, Knowledge Base, and Application Analysis publish typed capability descriptors and execution ports without exposing arbitrary URLs, SQL, persistence, or MVC internals to the Agent.
- Discover local capability providers through the application/infra assembly boundary and synchronize remote service capability manifests through authenticated internal service contracts and Nacos service discovery.
- Persist capability definitions and Provider routing state under `cynexus_appl_ai`, including enablement, version, priority, health, last-seen, and stale/unavailable transitions.
- Generate the runtime Agent tool catalog from authorized, enabled, healthy capabilities and keep the AI Provider page routing list, runtime entry, prompt key, group, description, schemas, source, and freshness synchronized.
- Validate capability arguments and authorization before execution, return structured evidence and provenance, and make capability registration, version changes, disablement, expiration, and execution auditable.
- Verify with a newly registered non-Market capability and a real `xtyooo / gpt-5.4-mini` run that the Agent discovers and selects it without a fixed keyword route or hard-coded executor branch.

Current implementation: handlers publish typed capability descriptors; AI persists versioned capability metadata and governance state under `cynexus_appl_ai.ai_agent_capability`; runtime catalog filtering now requires enabled, available, non-stale persisted descriptors; stale/disappeared handlers and older versions are disabled transactionally; descriptor/source/governance validation is domain-owned; lifecycle and governance changes are audit-persisted; Provider UI now renders governed metadata and enable/disable controls. The implementation slice is ready for independent commit; real newly-registered capability Release evidence remains open.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| Event converter package was not imported | First event-log compile | Added the converter import and explicit MapStruct field mappings. |
| Stream DTO lacked `sequenceNo` | First controller compile | Added the sequence field to the API DTO and frontend event contract. |
| Maven `-D` argument was parsed as a lifecycle phase in PowerShell | First focused persistence test command | Quoted the complete `-D...` arguments before rerunning; the focused infra suite passed 2/2. |
