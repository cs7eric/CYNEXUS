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
