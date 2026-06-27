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
