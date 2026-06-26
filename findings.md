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
