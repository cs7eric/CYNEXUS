# Cynexus Setup Progress

## 2026-07-15 - Market Information decision dashboard

- User approved a seven-layer information architecture for the Market Information page.
- Existing brief/feed data is sufficient for the first UI pass; new providers or extra concepts are intentionally deferred.
- Added the seven-layer decision dashboard UI: deterministic industry impact map, AI risk/watchlist lane, company events from existing feed rows, and an explicit upcoming-trigger calendar POC.
- Renamed the primary table section to “What matters today” and preserved event Sheet, pagination, industry/impact filters, and raw 7x24 access.
- Scoped Market Information ESLint, direct Vite build, and full Maven package passed at aligned version `1.0.39-SNAPSHOT`.
- Reviewed 财联社、东方财富、同花顺、Koyfin、TradingView，以及 Wind/Bloomberg 的公开产品结构，形成 Information Dashboard V2 三层布局提案；本轮只记录方案，未修改页面实现。
- Implemented the approved Information Dashboard V2: compact Market Pulse, two-column decision workspace, event-to-industry impact rows, compact risks/triggers, task-oriented feed views, and a bottom full information feed.
- Kept Upcoming Triggers source-gated and did not add unapproved providers or tables.
- Added the explicit relationship chain to priority events: Event → Industry → Direction → Impact strength → Related stocks, using only existing deterministic mappings, issuer fields, and structured AI company fields.
- Extended the existing Market API mapping to attach up to five latest THS constituent stocks per mapped industry, while leaving unmapped relationships explicit.
- Added related-stock context to the compact Impact Map and verified live API rows now carry related stocks for mapped industries.
- Scoped Information ESLint, direct Vite build, and full Maven package passed at aligned version `1.0.45-SNAPSHOT`; Dev restarted successfully.
- Scoped Information ESLint, direct Vite build, and full Maven package passed; versions are aligned at `1.0.42-SNAPSHOT` and Dev was restarted from the new artifacts.

## 2026-06-26

- Read `guide/TASKS.md`.
- Tried the exact `guide/PROJECT_BRIEF .md` path; it does not exist.
- Listed `guide` and found `guide/PROJECT_BRIEF.md`.
- Read `guide/PROJECT_BRIEF.md`.
- Read `guide/history.md` and extracted the Nacos/OpenFeign microservice requirements.
- Inspected current root `pom.xml`; it is a minimal Maven file with the wrong group/version for this project.
- Created planning files for the initial setup task.
- User clarified Maven `groupId` should keep `cn.cccs7`.
- User clarified Java package root should be `cn.cccs7.cynexus`, not `com.cynexus`.
- Verified Java 21 and Maven 3.8.8 are installed.
- Created Maven directory skeleton for common, AI, and market modules.
- Added root, common, AI, and market POM files.
- Added starter classes, market API DTOs, Feign client, mock market controller, AI market probe controller, and application configs.
- Added `docker-compose.core.yml` with PostgreSQL, Redis, and Nacos only.
- Ran first Maven package attempt; failed because `org.springframework.ai:spring-ai-tools` is not managed by the Spring AI 2.0.0 BOM.
- Removed the unused `spring-ai-tools` dependency from `cynexus-ai-infra`.
- Ran `mvn clean package -DskipTests` again; build succeeded across all 23 Maven reactor projects.
- Checked root directories and confirmed only `cynexus-common`, `cynexus-ai`, and `cynexus-market` were created for first-phase services.
- User pointed out the first pass was too empty versus `history.md`; reopened the history requirements.
- Added common core/web/db/cloud baseline classes: error/result/page/trace/json/single-user/db id/web trace/cloud properties.
- Added AI DTOs, domain model concepts, application services, model adapter registry, dynamic client factory, tool registry, secret resolver, trace/token recorders, and AI controllers.
- Added market feature DTOs, Feign endpoints, domain objects, application services, and split controllers for overview, sectors, and stock features.
- Added PostgreSQL SQL schema samples and Nacos config samples under `infrastructure/`.
- Re-ran `mvn clean package -DskipTests`; build succeeded after the fuller first-phase implementation.
- Counted Java/YAML/SQL content files under common/AI/market/infrastructure: 86.
- User flagged that the implementation still ignored folder layering and pointed to `guide/folder-convention.md`.
- Read `guide/folder-convention.md`.
- Moved common classes into convention subpackages.
- Moved AI application services into `application.service` and AI infra adapter/factory/registry into `infra.ai.*`.
- Moved Market application services into `application.service`.
- Moved Docker/Nacos/SQL files to `deploy/docker`, `deploy/nacos`, and `sql`.
- Re-ran `mvn clean package -DskipTests`; build succeeded after package/folder refactor.
- Removed empty legacy directories left by the move.
- User flagged that the implementation still did not follow interface/impl, ORM, and object layering concepts; read `guide/layer-and-object-convention.md`.
- Reworked common result/exception/enum/db base classes to match the layer/object convention.
- Split AI domain services into interfaces and `service/impl` implementations, registered domain service beans from application config without adding Spring dependency to domain.
- Moved AI domain records into `domain/entity/*BO` and `domain/valueobject`.
- Added AI ORM basic layer: `AiModelConfigPO`, mapper, basic service/interface/impl, XML mapper, converter, and infra repository implementation.
- Updated AI application routing to use `AiModelRepository` and injected domain service interfaces.
- Updated AI controllers to return `Result<Response>` and added response records for model/scenario/prompt lists.
- Updated Market sector/stock application flow to use BOs plus assemblers and API response records.
- Updated Market Feign and AI market tool calls to use response contracts instead of DTO contracts.
- Ran `mvn clean package -DskipTests`; build succeeded across all 23 Maven reactor projects.
- Ran layer audit searches for domain/common-core/controller DTO leaks; no remaining violations found in the checked patterns.
- Added MapStruct integration with a common `BaseMapper<S, T>` defining only `toTarget` and `toSource`.
- Converted AI/Market PO converters and response assemblers to MapStruct mapper interfaces where the mapping is single-object.
- Kept list mapping out of `BaseMapper`; list responses use stream mapping in the caller/assembler wrapper.
- Re-ran `mvn clean package -DskipTests`; build succeeded after MapStruct integration.
- Updated API output convention to use DTOs instead of separate Response records.
- Added list conversion methods to common `BaseMapper`.
- Deleted duplicate `api/response` records and removed source references to `api.response`.
- Updated controllers and Feign contracts to return `Result<DTO>` or `Result<List<DTO>>`.
- Updated execution flow to build an `AiExecutionBO` and map it to `AiExecuteDTO`.
- Re-ran `mvn clean package -DskipTests`; build succeeded after DTO convention cleanup.
- Updated local Nacos registration config for AI and Market starters.
- Changed default local Nacos namespace from `cynexus-dev` to public namespace by using an empty `NACOS_NAMESPACE` default.
- Explicitly enabled Nacos discovery registration for `cynexus-ai` and `cynexus-market`.
- Updated `deploy/local/docker-compose.yml` to expose Nacos 2.x gRPC server port `9849` in addition to `8848` and `9848`.
- Re-ran `mvn clean package -DskipTests`; build succeeded after Nacos registration config changes.
- Updated AI and Market datasource defaults to connect to the existing local Docker PostgreSQL at `127.0.0.1:25432`.
- Default datasource user is now `postgres`, default password is `C020611.`, and default database is `postgres`.
- Updated `deploy/nacos/cynexus-common.yml` datasource defaults to the same local PostgreSQL connection.
- Verified TCP connectivity to `127.0.0.1:25432`.
- Re-ran `mvn clean package -DskipTests`; build succeeded after PostgreSQL config changes.
- Updated AI and Market Redis defaults to match the local Docker Redis service at `127.0.0.1:6379`.
- Default Redis password is now `C020611.` in both starter `application.yml` files and shared Nacos config.
- Updated `deploy/nacos/cynexus-common.yml` with shared Redis host, port, password, and timeout settings.
- Verified TCP connectivity to `127.0.0.1:6379`.
- Re-ran `mvn clean package -DskipTests`; build succeeded after Redis config changes.
- Inspected the manually started Postgres container `62852359c804` and found the real data volume is anonymous Docker volume `f9b03225196ab4753a432dc502f3f74454ef4134a0973af24671c8038b5d9397`.
- Verified Postgres readiness with `pg_isready`; it is accepting connections.
- Listed non-template databases; only `postgres` exists.
- Created a logical backup at `deploy/local/postgres-backups/postgres-dumpall-20260626-144554.sql`.
- Added the existing Postgres 18 volume to `deploy/local/docker-compose.yml` as an external compose volume.
- Added local Redis data and Postgres backup directories to `.gitignore`.
- Validated `deploy/local/docker-compose.yml` with `docker compose -f deploy/local/docker-compose.yml config`.
- Added the `postgres` profile to the compose Postgres service so default compose startup does not collide with the still-running original `postgres` container on port `25432`.
- Validated the full Postgres profile with `docker compose -f deploy/local/docker-compose.yml --profile postgres config`.
- Confirmed original container `62852359c804` / `postgres` is still running on `127.0.0.1:25432`; no compose-managed `cynexus-postgres` container has been created yet.
- Changed local compose Postgres from the original anonymous external volume to `./postgres/data:/var/lib/postgresql`.
- Changed project-owned local Postgres host port to `127.0.0.1:25433` so it can run at the same time as the original `postgres` container on `25432`.
- Updated AI starter, Market starter, and shared Nacos datasource defaults from port `25432` to `25433`.
- Added `deploy/local/postgres/data/` to `.gitignore`.
- Validated local compose config after switching Postgres to a bind mount.
- Started `cynexus-postgres` from `deploy/local/docker-compose.yml`; it is exposed on `127.0.0.1:25433`.
- Confirmed the bind mount source is `E:\project-repo\cynexus\deploy\local\postgres\data` and it contains Postgres 18 data under `18/docker`.
- Imported old container data with `pg_dump` from `62852359c804` into `cynexus-postgres`.
- Verified old and new `postgres` databases both have 29 user base tables and 27068 total rows.
- Verified TCP connectivity to `127.0.0.1:25433`.
- Verified both containers are running: original `postgres` on `25432`, project `cynexus-postgres` on `25433`.
- Re-ran `mvn clean package -DskipTests`; build succeeded after switching default datasource port to `25433`.
- User requested the project initial database be `cynexus` instead of `postgres`.
- Confirmed project Postgres currently has only non-template database `postgres`.
- Changed local compose `POSTGRES_DB` to `cynexus`.
- Changed AI starter, Market starter, and shared Nacos datasource defaults from database `postgres` to `cynexus`.
- Created database `cynexus` inside `cynexus-postgres`.
- Copied the previously imported project data from database `postgres` to database `cynexus` using `pg_dump | psql`.
- Verified `postgres` and `cynexus` inside `cynexus-postgres` both have 29 user base tables and 27068 total rows.
- Verified compose config now uses `POSTGRES_DB: cynexus`.
- Re-ran `mvn clean package -DskipTests`; build succeeded after switching default database to `cynexus`.
- User requested adding application services to local Docker Compose so AI and Market do not need to be run manually.
- Found AI and Market starter jars were thin jars, so added Spring Boot `repackage` executions to both starter POMs.
- Fixed datasource `url` indentation in AI and Market `application.yml` before runtime containerization.
- Added `.dockerignore` to keep Git metadata, Maven targets, and local Docker data directories out of Docker build context.
- Added `deploy/local/Dockerfile.app` as a generic Java 21 Spring Boot application image build.
- Added `market` and `ai` services to `deploy/local/docker-compose.yml` with container DNS env vars for Nacos, PostgreSQL, and Redis.
- Ran `mvn clean package -DskipTests`; build succeeded and Spring Boot repackage produced executable starter jars.
- Validated `deploy/local/docker-compose.yml` with `docker compose -f deploy/local/docker-compose.yml config`.
- First compose application startup built both images but failed to bind `127.0.0.1:18082` because IntelliJ-launched Java processes were still listening on `18081` and `18082`.
- Confirmed the port owners were local Java processes running `CynexusAiApplication` and `CynexusMarketApplication`, then stopped them so compose could own those ports.
- Started `market` and `ai` with Docker Compose. Both containers became healthy.
- Verified host health endpoints: `http://127.0.0.1:18081/actuator/health` and `http://127.0.0.1:18082/actuator/health` both returned `status=UP`.
- Verified compose now shows `cynexus-ai`, `cynexus-market`, `cynexus-nacos`, `cynexus-postgres`, and `cynexus-redis` running together.
- Updated `deploy/local/Dockerfile.app` to expose the actual application ports `18081` and `18082`.
- Rebuilt/recreated `market` and `ai` with Docker Compose after the Dockerfile update.
- Re-verified both containers are healthy and both actuator health endpoints return `status=UP`.
- User requested service-separated PostgreSQL schemas and BIGINT distributed-ID primary keys instead of UUID defaults.
- Added Phase 13 to `task_plan.md`.
- Confirmed only Postgres, Redis, and Nacos are currently running; AI and Market containers are stopped before migration.
- Inspected the authoritative `cynexus` database: 21 service-owned business tables are in schema `app`, with UUID primary keys and related UUID reference columns.
- Decided to leave `public` JobRunr and old public self-selected tables unchanged for this migration.
- Created a fresh backup before migration at `deploy/local/postgres-backups/cynexus-before-schema-bigint-20260626-153602.sql`.
- Added `deploy/local/postgres/migrations/20260626_split_service_schemas_bigint_ids.sql`.
- Executed the migration successfully: business tables moved from `app` into `cynexus_appl_ai` and `cynexus_appl_market`.
- Verified all 21 service-owned `id` columns are `bigint`, `NOT NULL`, primary-key-backed, and have no database default.
- Verified no UUID columns remain in the two service schemas.
- Updated AI and Market datasource URLs to use PostgreSQL `currentSchema`.
- Added per-service `POSTGRES_SCHEMA` values to local compose.
- Moved datasource settings out of shared Nacos common config and into service-specific Nacos config samples.
- `docker compose -f deploy/local/docker-compose.yml config` succeeded with per-service `POSTGRES_SCHEMA`.
- `mvn clean package -DskipTests` succeeded after config and migration-script changes.
- First `docker compose up -d --build market ai` exceeded the command timeout; the partially started containers exited with code 137 from the timeout interruption.
- Restarted with `docker compose -f deploy/local/docker-compose.yml up -d market ai`; both services recreated and started normally.
- Verified `cynexus-ai`, `cynexus-market`, `cynexus-postgres`, `cynexus-redis`, and `cynexus-nacos` are all running; AI and Market are healthy.
- Verified `http://127.0.0.1:18081/actuator/health` and `http://127.0.0.1:18082/actuator/health` both return `UP`.
- Marked Phase 13 complete.
- Found a post-migration startup issue: JobRunr SQL storage auto-configuration followed the service datasource `currentSchema` and looked for `jobrunr_migrations` outside `public`.
- Added `spring.autoconfigure.exclude` entries for JobRunr auto-configuration in AI and Market starter configs because JobRunr server/dashboard/scheduler are disabled for now.
- Rebuilt AI and Market images and restarted compose services; both are now healthy again.
- User requested a new `cynexus-core` service matching AI/Market structure and moving scheduler tables to `cynexus_appl_core`.
- Added Phase 14 to `task_plan.md`.
- Read `guide/folder-convention.md` and `guide/layer-and-object-convention.md` before creating Core files.
- Added `cynexus-core` to the root Maven modules.
- Created Core api/common/domain/infra/application-controller/starter modules and a minimal `/api/core/info` endpoint.
- Added `deploy/nacos/cynexus-core.yml`.
- Added Core service to `deploy/local/docker-compose.yml` on `127.0.0.1:18080`.
- Updated compose dependencies so AI and Market wait for Core health.
- Created a fresh database backup at `deploy/local/postgres-backups/cynexus-before-core-scheduler-schema-20260626-162425.sql`.
- Added and executed `deploy/local/postgres/migrations/20260626_move_scheduler_tables_to_core_schema.sql`.
- Moved all five scheduler tables from `cynexus_appl_market` to `cynexus_appl_core`.
- Ran `mvn clean package -DskipTests`; build succeeded across 31 reactor projects.
- Rebuilt and started Core, Market, and AI with Docker Compose.
- Verified Core, AI, and Market health endpoints all return `UP`.
- Verified `http://127.0.0.1:18080/api/core/info` returns the Core service info payload.
- Continued the table-layer cleanup after the user asked to keep going.
- Fixed AI application record-style accessor leftovers after DTO/BO/request classes were converted to Lombok classes.
- Added AI application DTO converters, `AiConfigurationCrudAppService`, and `AiConfigurationCrudController` for providers, usage bindings, analysis configs, analysis targets, daily snapshots, current results, and history results.
- Added Market application DTO converters, `MarketDataCrudAppService`, and `MarketDataCrudController` for all nine tables remaining in `cynexus_appl_market`.
- Reworked `MarketOverviewBasicServiceImpl` from mock data to real MyBatis-Plus mapper-backed CRUD and added the missing `MarketOverviewMapper.xml`.
- Added `id` and audit fields to Market overview DTO/BO and mapped those fields through PO converters.
- Replaced remaining Market assembler record-style DTO calls with Lombok getter calls.
- Added `createdAt`, `updatedAt`, and `deletedAt` to Core scheduler BO/DTO types to align with BasePO-backed tables and remove MapStruct unmapped-field warnings.
- Audited converters for the required `INSTANCE = Mappers.getMapper(...)`; no missing converter instances remained.
- Audited PO-to-XML mapper coverage; every checked PO has a matching mapper XML.
- Ran `mvn compile -DskipTests`; build succeeded across 31 reactor projects.
- Ran final `mvn clean package -DskipTests`; build succeeded across 31 reactor projects.
- User asked to make Docker reruns always pick up the latest code.
- Added `deploy/local/up-latest.ps1` and `deploy/local/up-latest.cmd` as the supported local rerun entrypoint.
- Changed `deploy/local/Dockerfile.app` to copy already-packaged starter jars instead of compiling inside each Docker build.
- Updated `.dockerignore` so only the three starter jars are included from `target` directories.
- Updated `deploy/local/README.md` with the new rerun command.
- First run of `up-latest.ps1` rebuilt images but exposed a Core runtime bean issue: `SchedulerRepositoryImpl` implemented a duplicate infra repository interface instead of the domain repository interface.
- Deleted the duplicate Core infra `SchedulerRepository` interface and changed `SchedulerRepositoryImpl` to implement `cn.cccs7.cynexus.core.domain.repository.SchedulerRepository`.
- Re-ran `up-latest.ps1`; Maven package succeeded, Docker images rebuilt, and Core/Market/AI containers were recreated.
- Verified `http://127.0.0.1:18080/actuator/health`, `http://127.0.0.1:18081/actuator/health`, and `http://127.0.0.1:18082/actuator/health` all return `UP`.
- User requested controllers stop calling domain services directly.
- Added `CoreInfoAppService` and changed `CoreInfoController` to depend on the app service instead of `CoreServiceInfoDomainService`.
- Added `SchedulerAppService` and changed `SchedulerController` to depend on the app service instead of `SchedulerDomainService`.
- Audited AI/Market/Core controllers for direct `DomainService` references; none remain.
- Ran `mvn compile -DskipTests`; build succeeded across 31 reactor projects.
- Ran `deploy/local/up-latest.ps1`; Maven package succeeded, Docker images rebuilt, and Core/Market/AI containers were recreated with the new app-service layer.
- Verified Core, AI, and Market health endpoints all return `UP` after the container rebuild.
- User rejected an nginx-style local proxy and requested a maintainable microservice Gateway.
- Added `cynexus-gateway` and `cynexus-gateway-starter` to the Maven reactor.
- Added `CynexusGatewayApplication` and Spring Cloud Gateway WebFlux route config for Core, AI, and Market through Nacos service names.
- Added `deploy/nacos/cynexus-gateway.yml` as the service-specific config sample.
- Added Gateway to `deploy/local/docker-compose.yml` on `127.0.0.1:18000`.
- Updated `.dockerignore`, `Dockerfile.app`, `deploy/local/up-latest.ps1`, and `deploy/local/README.md` so Gateway participates in the latest-code Docker workflow.
- Ran `mvn clean package -DskipTests`; build succeeded across 33 reactor projects.
- Ran `docker compose -f deploy/local/docker-compose.yml config`; compose config succeeded.
- Ran `deploy/local/up-latest.ps1`; Core, Market, AI, and Gateway were rebuilt and recreated.
- Verified Gateway health and successful single-port forwarding to Core and AI.
- Found `GET /api/market/data/overview-snapshots` reached Market through Gateway but failed because `MarketOverviewPO` still used old columns not present in the real database table.
- Aligned `MarketOverviewPO`, `MarketOverviewBO`, `MarketOverviewDTO`, converters, assembler, and trade-date lookup with the real `cynexus_appl_market.market_overview_snapshots` columns.
- Ran `mvn compile -DskipTests`; build succeeded after the Market overview model fix.
- Ran `deploy/local/up-latest.ps1 -Service market,gateway`; Maven package succeeded and Docker services were recreated.
- Verified through `http://127.0.0.1:18000`: Gateway health, Core info, AI provider list, and Market overview list all return success.
- User asked to inspect the old mixed Python/React/model project before migrating it into the new `cynexus-collector`.
- Read the planning skill instructions and reloaded `task_plan.md`, `findings.md`, and `progress.md`.
- Confirmed current repo has an empty `cynexus-collector` directory.
- Confirmed old project `F:\dev-repo\mp4-to-word-new` Git status is clean.
- Inspected old Flask entrypoints, app factory, bootstrap, database config, settings, requirements, frontend package metadata, scheduler config names, API route surfaces, and large data/model files.
- Intentionally did not read the old `.env` file.
- Added Phase 17 for collector migration inventory and safe bridge planning.
- Added `docs/collector-migration-inventory.md` with service boundaries, startup risks, large data mount rules, and proposed collector contract.
- User requested DuckDB migration in phases, starting with a Docker ClickHouse service.
- Verified Docker can resolve `clickhouse/clickhouse-server:25.8`.
- Added `clickhouse` to `deploy/local/docker-compose.yml` with persisted data/log bind mounts, local HTTP/native ports, database/user `cynexus`, and a `clickhouse-client` healthcheck.
- Added ClickHouse local data/log directories to `.gitignore` and `.dockerignore`.
- Documented local ClickHouse startup and connection details in `deploy/local/README.md`.
- Added Phase 18 for the DuckDB-to-ClickHouse migration foundation.
- Validated `deploy/local/docker-compose.yml` with `docker compose config`.
- Started only the `clickhouse` service from local compose.
- Confirmed `cynexus-clickhouse` became healthy.
- Verified ClickHouse query access with `clickhouse-client`; version is `25.8.24.21`.
- Verified the initialized databases include `cynexus`.
- Marked Phase 18 complete.
- User provided PostgreSQL table name mapping for selected DuckDB tables and requested target tables under `cynexus_appl_market`.
- Inspected current `cynexus_appl_market` table conventions, constraints, and indexes.
- Added `deploy/local/postgres/migrations/20260626_create_market_duckdb_target_tables.sql`.
- Created a local PostgreSQL backup at `deploy/local/postgres-backups/cynexus-before-duckdb-target-tables-20260626-213927.sql` before applying DDL.
- First DDL execution failed on `current_date` in `mkt_index_return_daily`; PostgreSQL treated it as keyword syntax. The transaction rolled back.
- Renamed the target columns to `current_close` and `current_trade_date`.
- Re-ran the DDL successfully and created 15 empty target tables in `cynexus_appl_market`.
- Verified all 15 target `id` columns are `bigint NOT NULL` with no default.
- Verified all requested natural-key unique indexes exist.
- Verified all 15 target tables currently have 0 rows.
- Re-ran the migration script successfully to confirm idempotency.
- User requested remaining DuckDB tables not migrated to PostgreSQL be moved to ClickHouse, naming `daily_raw`, `daily_qfq`, `adj_factors`, and `index_daily_raw`.
- Compared the full DuckDB table list against the PostgreSQL mapping and found additional ClickHouse candidates: `daily_hfq`, `ingest_state`, `intraday_bars`, `quotes`, and `schema_meta`.
- Verified ClickHouse initially had no tables after the recent named-volume recreation.
- Tested DuckDB Parquet export to ClickHouse with a 10-row sample.
- First sample insert failed on Windows bind-mounted ClickHouse data due to MergeTree atomic rename permission errors.
- Changed ClickHouse storage in `deploy/local/docker-compose.yml` from bind mounts to Docker named volumes `clickhouse-data` and `clickhouse-logs`.
- Recreated `cynexus-clickhouse`; it became healthy and the Parquet smoke test succeeded.
- Added ClickHouse DDL file `deploy/local/clickhouse/migrations/20260626_create_duckdb_market_tables.sql`.
- Added migration script `deploy/local/clickhouse/scripts/migrate_duckdb_to_clickhouse.py`.
- Applied ClickHouse DDL and created 9 target tables.
- Migrated small/empty tables first: `adj_factors`, `index_daily_raw`, `ingest_state`, `intraday_bars`, `quotes`, and `schema_meta`.
- First full `daily_raw` insert failed because monthly partitioning touched too many partitions in one insert block.
- Changed daily/detail table partitioning from monthly to yearly using `toYear(trade_date)`.
- Recreated ClickHouse target tables and reloaded the small/empty tables.
- Migrated and reconciled `daily_raw` with 28,516,739 rows.
- Migrated and reconciled `daily_qfq` with 28,482,626 rows.
- Migrated and reconciled `daily_hfq` with 28,482,612 rows.
- Final reconciliation confirmed all 9 DuckDB-to-ClickHouse target row counts match their DuckDB source counts.

## 2026-06-28 Market Pulse Data Backfill

- User reported Market Pulse data mismatches after switching UI-serving APIs from Python to Java.
- Started Phase 21 to trace old Python data sources, import latest persisted data into PG, and enforce previous-trading-day display for non-trading days.
- Initial broad search found old Python references to `reference/market-overview`, `reference/market-pulse`, `reference/stock/cache/intraday`, THS fund-flow, heatmap, and limit-emotion routes/services; next step is precise route and file inspection.
- Imported local TDX index `.lc1` files for `sh000001`, `sz399001`, and `sz399006` into ClickHouse `ch_dwd_kline_1m`; inserted 48,240 rows total, 16,080 rows per file.
- Backfilled PostgreSQL Market Pulse tables for 2026-06-26 from local old reference files: limit snapshot/stocks, industry heatmap, concept heatmap, and 29 style sector rows.
- Added Java Market Pulse ClickHouse read path for index minute bars; first compile failed because market-infra lacked an explicit Jackson dependency, so `jackson-databind` was added to that module.
- `mvn clean package -DskipTests` succeeded through `deploy/local/up-latest.ps1`, but `-Service market,gateway` was parsed as one service name and Docker reported `no such service: market,gateway`; rerunning compose directly with separate `market gateway` service arguments.
- Fixed ClickHouse `max(trade_date)` alias collision in Java index minute lookup and corrected ClickHouse time formatting from `%M` to `%i` for minutes.
- Backfilled `sz399001` and `sz399006` rows into ClickHouse `index_daily_raw` from local TDX day files using `unit_scale=10`.
- Rebuilt local Docker services and verified Gateway Market Pulse endpoints: limit emotion returns 2026-06-26; style sectors count=29; industries heatmap count=90; concepts heatmap count=200; index-kline request for 2026-06-28 returns 2026-06-26 with 240 points for each of 000001/399001/399006.
- Phase 21 marked complete after final verification. Build commands succeeded: `mvn -pl cynexus-market/cynexus-market-starter -am package -DskipTests` and local Docker compose rebuild/recreate for Market/Gateway. Residual warnings were existing Maven duplicate-Lombok/model-mapping warnings, not blocking.

## 2026-06-28 Market Pulse Layering Repair

- User rejected the temporary Market Pulse implementation because SQL was embedded in Java, return contracts were raw `Map`, controller responses were not wrapped in `Result`, and frontend page API functions were placed in global `src/lib/api.ts`.
- Re-read `infra/cynexus-layering-and-dataflow.md` and confirmed target flow: Controller -> AppService -> Domain repository abstraction -> infra repository/basic service/mapper XML -> DB, with DTO/BO/PO object boundaries.
- Added Phase 22 to `task_plan.md` and recorded the violations in `findings.md`.
- User rejected the attempted `Result<JsonNode>` fix as still non-compliant. Re-read the current Market Pulse controller/app service and confirmed the remaining violation is the API/application boundary returning and accepting `JsonNode` instead of explicit DTO/Request types.
- Inspected the page-local frontend API contract under `F:\dev-repo\mp4-to-word-new\frontend\src\views\market\market-pulse\lib\api.ts`; Java can keep the existing response field shape through explicit DTO fields and `@JsonProperty` for legacy snake_case names.
- Added explicit Market Pulse response DTOs and a manual fund-flow save request under `cynexus-market-api`.
- Added application converters for Market Pulse overview, index returns, index kline points, style-sector items, and limit-emotion stocks.
- Replaced `MarketPulseQueryAppService` public contracts from `JsonNode` to concrete DTO/Request types. JSONB strings are now parsed only into typed DTOs inside the application layer.
- Replaced `MarketPulseQueryController` with concrete `Result<MarketPulse*DTO>` methods and removed `JsonNode` request/response usage.
- Deleted the temporary starter-level `MarketJacksonConfig`; the app service now owns a private JSONB parser for persisted payload strings.
- First Docker runtime verification failed because no global `ObjectMapper` bean existed after deleting the temporary config; changed the app service to use a private `new ObjectMapper().findAndRegisterModules()` instance and rebuilt.
- Fixed heatmap DTO aliases after verifying the persisted payload uses `changePercent` and `stockCount`; output now includes frontend-compatible `change_pct` and `count`.
- `mvn -pl cynexus-market/cynexus-market-starter -am package -DskipTests` succeeded after the DTO refactor. Remaining Maven warnings are pre-existing duplicate Lombok declarations in common-db/core-infra POMs.
- Rebuilt/recreated `cynexus-market`; compose also recreated `cynexus-core` as part of dependency/build handling. `cynexus-market` and `cynexus-gateway` are healthy.
- Gateway verification passed for Market Pulse overview, limit emotion, style sectors, heatmap industries/concepts, index returns, and index kline batch. Response payloads no longer contain Jackson node metadata fields.
- User requested not to run cynexus applications in Docker. Stopped Docker application containers (`cynexus-market`, `cynexus-gateway`, `cynexus-core`, `cynexus-ai`) and kept only infrastructure containers running.
- Restarted local Java `cynexus-market` and local Java `cynexus-gateway` from the packaged jars, with logs under `deploy/local/logs`.
- Fixed the UI crash `Cannot read properties of undefined (reading 'filter')` by extending `MarketPulseHeatmapDTO` to preserve `children`, `conceptTags`, `changePercent`, `stockCount`, and related `HeatmapSectorNode` fields, and by normalizing missing arrays to `List.of()` in `MarketPulseQueryAppService`.
- Verified through local Gateway that heatmap industries/concepts first items now return `children` as an array, `conceptTags` as an array, and both `changePercent` and `change_pct`.
- Fixed Market Pulse serving data drift after Java API migration:
  - `market-overview-akshare/history` now selects the latest N overview rows before returning them ascending, so 60d history reaches 2026-06-26 instead of stopping around 2026-05-27.
  - Java overview DTO assembly overlays rising/falling/flat/stock counts from ClickHouse `daily_qfq` breadth facts and limit-up/limit-down counts from `market_limit_daily_snapshots`.
  - Limit emotion JSON parsing now uses explicit DTOs plus `jackson-datatype-jsr310`; streak leaders, distribution, and broken-streak stocks are preserved from PG `extra`.
  - Verified local Market and Gateway endpoints: 2026-06-26 overview shows rising/falling/flat `746/4407/53`, limit counts `57/35`, and limit emotion returns 20 leaders, 5 distribution buckets, first leader `兴业科技` at 6 boards.
- Fixed expanded limit-streak tier rendering: Java now keeps `streak` on limit-up stocks and fills each `streak.distribution[].stocks` bucket from `limitUp.stocks`, because the persisted PG distribution only stores `height/count`. Verified local Market and Gateway distribution buckets: `6板=1`, `4板=1`, `3板=1`, `2板=3`, `1板=51`, all with matching stock lists.

## 2026-06-29 Industry Application API Migration

- Started Phase 23 for migrating Stock Overview -> Industry / Concept Application page APIs from old Python routes to Java/Gateway.
- Re-read `docs/migration-guides/python-to-java-api-migration-guide.md` and the existing planning files before implementation.
- Inspected `F:\dev-repo\mp4-to-word-new\frontend\src\views\industry-application\index.tsx` and confirmed it imports industry application helpers from global `src/lib/api.ts`.
- Inspected the global API segment and confirmed current Python endpoints are `/api/stock-chart/industry-application/targets`, `target-codes`, `kline`, `refresh`, `results/{targetId}`, and `overview`.
- Next step: inspect old Python `industry_application_service` / store details and Java Market layering to choose typed API/storage contracts.
- Added Java Industry Application application-layer DTO converter, AppService, and Controller under the existing Market application-controller module.
- Added frontend Java client at `F:\dev-repo\mp4-to-word-new\frontend\src\services\market\industry-application.ts` and changed the industry-application page to import Java API helpers from that service instead of global `src/lib/api.ts`.
- Fixed repository implementation details while wiring the app layer: avoided `List.getLast()` usage and replaced dynamic meta `Map` read/write with typed `IndustryApplicationBO.Result` JSON.
- First Java compile failed because `cynexus-market-infra` used `jackson-datatype-jsr310` for TDX day-bar JSON snapshots without declaring the module dependency; added the dependency to the infra POM.
- During endpoint smoke, `GET /api/market/pulse/heatmap?kind=styles` returned an empty payload because styles live in `mkt_style_sector_snapshot`, not `mkt_heatmap_snapshot`; added a Java adapter so the industry-application page can keep using one heatmap service for industry/concept/style tabs.
- Frontend heatmap service now applies the old `亿 -> 元` conversion only to industry/concept heatmaps; styles use sample counts and are left unscaled.
- Applied `deploy/local/postgres/migrations/20260629_create_industry_application_tables.sql` to local PostgreSQL `cynexus`; verified counts: config=1, target=12, result=0 before refresh.
- `mvn -pl cynexus-market/cynexus-market-starter -am compile -DskipTests` passed after adding the Jackson JSR310 dependency.
- `mvn -pl cynexus-market/cynexus-market-starter,cynexus-gateway/cynexus-gateway-starter -am package -DskipTests` initially failed because the running local Market jar locked the target jar; stopped local Market/Gateway jar processes, reran package successfully, and restarted local jars.
- Verified local Java services through `http://127.0.0.1:18082/actuator/health` and `http://127.0.0.1:18000/actuator/health`, both `UP`.
- Verified Gateway endpoints:
  - `/api/market/industry-application/targets` returns 12 configured targets.
  - `/api/market/industry-application/target-codes?kind=industry` returns 32 industry codes.
  - `/api/market/industry-application/kline?target_type=industry&symbol=sh880301&period=day&count=5` returns bars through `2026-06-26`.
  - `POST /api/market/industry-application/refresh` with `industry-sh880301` returns `ok=true`, `kline_count=240`.
  - `/api/market/industry-application/results/industry-sh880301` returns persisted result data.
  - PG result table for `industry-sh880301` has `jsonb_array_length(kline)=240`, `latestTime=2026-06-26 15:00:00+08:00`, `source=tdx.local_day`.
  - `/api/market/pulse/heatmap?kind=styles` now returns 29 style items from `mkt_style_sector_snapshot`.
- Ran frontend `node_modules/.bin/tsc.cmd -b`; it failed on existing unrelated TypeScript errors in shared UI/global API/other pages. Reported errors did not identify the new `src/services/market/industry-application.ts` file.

## 2026-06-29 Industry Application Drawer Data Repair

- User reported that clicking an item opened the right constituents drawer but showed no data.
- Verified Gateway `/api/market/industry-application/fund-flow?top=5` returned rows with `code: null`.
- Verified Gateway `/api/market/pulse/industries/881121/constituents` returned `count=0`, `rows=[]`.
- Verified PG before repair: `sector_fund_flow_daily_snapshots` had 1,080 rows and 0 non-empty `sector_code`; `ths_industry_constituent_snapshot` had 0 rows.
- Inspected old reference data under `F:\dev-repo\mp4-to-word-new\reference\ths-industry`; found 90 successful constituent files and 4,663 rows, fetched on `2026-06-26`.
- Imported the reference snapshots into PG with one capture batch and 4,663 constituent rows.
- Updated existing industry fund-flow snapshots from `industry_list.json` `nameToCode`; all 1,080 fund-flow rows now have non-empty `sector_code`.
- Added repeatable local migration script `deploy/local/postgres/scripts/import_ths_industry_constituents.py`.
- Changed `F:\dev-repo\mp4-to-word-new\frontend\src\views\industry-application\components\sector-heatmap.tsx` to use `@/services/market/pulse` instead of old global `@/lib/api` for constituent drill-down.
- Changed shared drawer title fallback so `industryName` remains visible when backend `name` equals the code.
- Cleaned unused imports/options in Industry Application heatmap and removed unused `Separator` import from the page.
- Verified PG after repair: 4,663 constituent rows, 90 industries, snapshot date `2026-06-26`; fund-flow missing `sector_code` count is 0.
- Verified Gateway after repair: fund-flow top10 carries THS codes; `/api/market/pulse/industries/881270/constituents` returns 62 rows.
- `python deploy/local/postgres/scripts/import_ths_industry_constituents.py --dry-run` succeeded.
- `python -m py_compile deploy/local/postgres/scripts/import_ths_industry_constituents.py` succeeded.
- `frontend tsc -b --pretty false` still fails, but remaining errors are outside Industry Application (`calendar.tsx`, global `src/lib/api.ts`, `poc/sentiment-overlay`, self-selected, stock-chart, stock-overview market-pulse).
- Added the drawer repair lessons to `docs/migration-guides/python-to-java-api-migration-guide.md` as reusable migration rules:
  - Do not stop at the main list API; inventory drawer/dialog/drill-down secondary entry points.
  - Verify backing PG/CH tables, row counts, date ranges, and join keys before declaring an endpoint complete.
  - Ensure list DTOs carry stable business keys needed by detail queries.
  - Search shared components for old Python/global API imports.
  - Turn manual data backfills into repeatable scripts with dry-run and verification output.

## 2026-06-29 Stock Overview Application Analysis API Migration

- Started Phase 24 for migrating `/stock-overview/application-analysis` application-analysis APIs from old Python `/api/stock-chart/application-analysis/**` to Java AI/Gateway.
- Reviewed the migration guide and confirmed the page calls targets, result, trigger, scheduler, recent30, and manual run APIs; it also still calls broader stock-chart K-line/intraday/annotation/auction helpers that are outside this phase.
- Verified backing PG data exists in `cynexus_appl_ai`: targets=4, current=4, history=1128, daily=22, configs=1.
- Added Java AI page contract DTOs and requests under `cynexus-ai-api`: `ApplicationAnalysisPageDTO` and `ApplicationAnalysisPageRequest`.
- Added domain aggregate/repository abstraction: `ApplicationAnalysisPageBO` and `ApplicationAnalysisPageRepository`.
- Added infra mapper XML queries and BasicService methods for default config, active targets, current result by target key, history by target key, and recent30 daily snapshots.
- Added `ApplicationAnalysisPageRepositoryImpl` with target upsert/soft-delete behavior and stable `target_key` as the UI id.
- Added `ApplicationAnalysisPageAppService` and `ApplicationAnalysisPageController` under `/api/ai/application-analysis/**`.
- Kept public controller results as typed DTOs wrapped in `Result`; JSONB payload parsing stays internal to AppService and is emitted through explicit DTO fields, not `Result<JsonNode>`.
- Added Jackson dependencies to `cynexus-ai-application-controller` after the first compile exposed missing `ObjectMapper`/`JsonNode` classes.
- Added frontend Java API client at `F:\dev-repo\mp4-to-word-new\frontend\src\services\ai\application-analysis.ts`.
- Changed `views/application-analysis/index.tsx`, `components/ai-direction-card.tsx`, `components/target-card.tsx`, `components/chart-header.tsx`, and `components/intraday-analysis-dialog.tsx` to import application-analysis functions/types from `src/services/ai/application-analysis`.
- Java compile succeeded: `mvn -pl cynexus-ai/cynexus-ai-starter -am compile -DskipTests`.
- Java package succeeded: `mvn -pl cynexus-ai/cynexus-ai-starter -am package -DskipTests`.
- Started local `cynexus-ai` jar on `127.0.0.1:18081`; did not use Docker for Cynexus applications.
- Direct AI smoke passed:
  - targets: success=true, items=4, horizon.days=120.
  - result `stock-600021`: success=true, `analysis_result` present, history=20.
  - recent30 full `stock-000636`: success=true, ok=true, items=2, firstDate=2026-06-20, trend present.
- Gateway smoke passed:
  - targets: success=true, items=4.
  - scheduler: success=true, running=false, enabled=0, total=4, runs=4.
  - trigger/recent30 refresh: success=true with business `ok=false` and explicit Java executor-not-connected error.
  - manual run: `Result.fail` with `AI_APPLICATION_ANALYSIS_EXECUTOR_NOT_CONNECTED`.
- Frontend `tsc -b --pretty false` still fails on pre-existing unrelated files (`calendar.tsx`, global `src/lib/api.ts`, `poc/sentiment-overlay`, self-selected, stock-chart chart panel, old stock-overview market-pulse notification usage); no error was reported for the new AI service or application-analysis page changes.

## 2026-06-29 Application Analysis Stock Chart Runtime Repair

- User reported the application-analysis page still had many non-working functions after migrating the AI persistence APIs.
- Confirmed remaining page dependencies were the shared stock-chart runtime calls: K-line, intraday/timeshare, annotations, auction, technical indicator context, and fund-flow.
- Confirmed `ch_dwd_kline_1m` has large post-2010 1m data for stocks: `SH 600021` has 964,000 rows through `2026-06-26`.
- Added typed domain boundary `StockChartBO` and `StockChartQueryRepository`.
- Added infra `ClickHouseStockChartClient` and SQL templates under `cynexus-market-infra/src/main/resources/clickhouse/stock-chart`.
- Added `StockChartQueryRepositoryImpl` to centralize TDX daily bars, ClickHouse minute bars/timeshare, PG stock metadata, and PG/fallback fund-flow reads.
- Replaced the old stock-chart AppService direct infra access with a repository-backed AppService.
- Changed stock-chart intraday and auction API DTOs from empty/dynamic map contracts to explicit DTOs.
- Wired `/api/market/stock-chart/intraday` to ClickHouse and `/api/market/stock-chart/klines` minute periods to ClickHouse aggregation.
- Added fund-flow fallback from TDX daily bars because `cynexus_appl_market.stock_fund_flow_snapshot` is currently empty.
- Fixed `effective_adjust` / `requested_adjust` JSON aliases by adding getter-level `@JsonProperty`.
- Built and restarted local Market jar; Gateway remained local jar on `127.0.0.1:18000`.
- Verified Gateway intraday for `600021` on `2026-06-26`: `timeshare=241`, `1m=241`, `5m=50`, source `clickhouse.ch_dwd_kline_1m`.
- Verified Gateway daily K for `600021`: 520 bars, last date `2026-06-26`, close `15.04`.
- Verified Gateway fund-flow fallback for `600021`: `ok=true`, source `java-tdx-local-daily-fallback`, 30 history rows.
- Searched application-analysis scope and found no remaining old Python/global API references.
- Frontend `npx tsc -b --pretty false` still fails on unrelated pre-existing TypeScript errors; no application-analysis-specific error appeared in the reported output.
- Recorded residual data-contract gaps: server-side annotation persistence needs a proper PG table/repository, and auction snapshots need either a Java table-backed import or a collector/executor contract. Current implementation avoids old Python calls and avoids introducing Java file-backed stopgaps.

## 2026-06-29 Core Scheduler Runtime And Python Executor Bridge

- Started Phase 26 to make Scheduler UI manual triggers reach `cynexus-data`, add Core automatic scheduled triggering, and add safe scheduler-to-Python mappings.
- Started local `cynexus-data` FastAPI trigger API on `127.0.0.1:18083` with PostgreSQL/ClickHouse environment variables; `/internal/health` returned `ok=true`.
- Verified Gateway/Core data job health now succeeds and manual Scheduler trigger for `test_scheduler_demo` returns `status=success` with a Python `run_id`.
- Inspected `cynexus-data` job registry and confirmed `application_analysis`, `auction_ai_analysis`, and `daily_eod_incremental` are not registered Python jobs.
- Added a Java mapping for `auction_ai_analysis` to `sync_tdx_intraday_detail` with default params `datasets=["auction"]` and `create_table=true`.
- Left `application_analysis` and deprecated `daily_eod_incremental` unsupported for now because current migration docs place AI analysis out of `cynexus-data`, and the EOD monolith was intentionally split into independent jobs.
- Added `SchedulerRuntimeService` with a scheduled scan for enabled jobs/statuses and support for `run_time`, 5/6-field `cron`, `every_minutes`, and `intraday_windows`.
- Added `SchedulerAppService.triggerScheduledJob` so auto triggers reuse manual trigger execution while writing run history with `triggerType=auto`.
- First Core compile failed because `SchedulerRuntimeService` used `this::pickLatestStatus` for a static merge method; changed it to `SchedulerRuntimeService::pickLatestStatus`.
- Added a narrow scheduler-status update path for `is_running`, `scheduler_started_at`, and `stopped_at` so the runtime no longer uses generic JSONB entity updates that fail PostgreSQL `schedule` typing.
- Enabled Spring scheduling in the Core starter and rebuilt Core with `mvn -pl cynexus-core/cynexus-core-starter -am package -DskipTests`.
- Restarted local Core on `127.0.0.1:18080` and confirmed `/actuator/health` is `UP`.
- Confirmed Gateway data job health succeeds through Core and `cynexus-data`.
- Verified manual Scheduler trigger through Gateway for `test_scheduler_demo`: returned `status=success` and Python run id `job_run_9b115828c7494ee99abfeeace941a91a`.
- Verified the new auction mapping safely with Gateway dry-run trigger for `auction_ai_analysis`: returned `status=success` and Python run id `job_run_c0ecba934385421fa91ab93fc60281f9`.
- Checked Core logs after restart/tick; no new Scheduler runtime SQL or trigger errors were reported.

## 2026-06-29 Scheduler Chain Progress And History Data

- User reported Scheduler UI can trigger jobs but cannot tell whether runs succeeded, progress does not update, history only shows bare `run`, and `market_sentiment_chain_refresh` should be a chain.
- Compared the old Python scheduler implementation in `F:\dev-repo\mp4-to-word-new`; confirmed `market_sentiment_chain_refresh` runs a 14-step chain and writes `lastStep`, `lastCompletedSteps`, `lastStepResults`, `lastTargetsProcessed`, and `lastMessage` while keeping a `processing -> success/failed` history run.
- Confirmed the Java frontend already reads those progress fields from scheduler management jobs/history responses, so the repair belongs in Core/Data integration rather than a large UI redesign.
- Added `cynexus-data` chain job `market_sentiment_chain_refresh` with the old step order: TDX HSJ day download, initial backfill, QFQ reconciliation, limit emotion, risk appetite, MA count, volatility sentiment, style risk, profit effect, market overview daily, turnover activity, THS industry fund flow, sector breadth, and final MSI build.
- Added Python job-store progress persistence so chain steps can update run metadata while executing.
- Registered the new Python chain job in `cynexus-data` and added job config metadata.
- Changed the trigger API to persist job state into PostgreSQL-backed `task_job_run` instead of memory-only state for this local integration.
- Fixed Core job mapping so `market_sentiment_chain_refresh` maps to the new Python chain job instead of the old single MSI rebuild key.
- Added narrow Core status update methods for progress/result JSONB writes and run summary fields, avoiding generic MyBatis updates against JSONB columns.
- Changed Core chain manual trigger to create a processing history row, return `processing` immediately to the UI, run the Python chain asynchronously, and update the same history row plus scheduler status after completion.
- Enriched normal and chain scheduler result handling with Python `run_id`, data job key, rows read/written, pipeline, completed step count, last message, and per-step summaries.
- Updated scheduler history DTO assembly so remarks like `completed_steps=14` or `rows_written=N` populate visible `target_count` and `succeeded`.
- Increased the Core data-job executor timeout default to 600 seconds for long-running chain jobs.
- Restarted local `cynexus-data` on `127.0.0.1:18083` and local Core on `127.0.0.1:18080`; Gateway remained the local jar entry point.
- Verified Core actuator health is `UP`, `cynexus-data` `/internal/health` returns ok, and Gateway `/api/core/scheduler/data-jobs/health` returns ok.
- Verified Gateway dry-run trigger for `market_sentiment_chain_refresh`: initial response `status=processing`, final management status `success`, 14 completed steps, `lastTargetsProcessed=14`, and `live.lastMessage=completed 14 MSI steps`.
- Verified latest Gateway history now shows the chain run message with Python run id, rows, `completed_steps=14`, `target_count=14`, and `succeeded=14`.
- Java Core `compile` and `package` with `-DskipTests` succeeded; Python compile for changed `cynexus-data` scheduler/job files succeeded. Existing Maven duplicate-Lombok/MapStruct warnings remain unrelated.

## 2026-06-29 Scheduler Progress JSON Fields

- User clarified the desired model: add JSON fields for jobs to write progress-specific content, then render that content in UI.
- Decided not to put mutable progress on `scheduler_jobs`; kept static job definition separate from runtime state.
- Added migration `deploy/local/postgres/migrations/20260629_add_scheduler_progress_fields.sql`:
  - `cynexus_appl_core.scheduler_job_statuses.progress jsonb NOT NULL DEFAULT '{}'::jsonb`
  - `cynexus_appl_core.scheduler_job_run_history.result_summary jsonb NOT NULL DEFAULT '{}'::jsonb`
- Applied the migration to local PostgreSQL through `cynexus-postgres`; verified both columns are `jsonb`.
- Added `progress` / `resultSummary` through Java Core PO/BO/DTO layers and added `JsonbTypeHandler` to the JSONB PO fields.
- Extended narrow scheduler status update SQL so `updateRunResultDetailById` and `updateProgressById` write `progress = CAST(#{progress} AS jsonb)` alongside legacy `extra`.
- Changed `SchedulerAppService` to build a stable progress envelope and result summary envelope, while preserving existing `extra` compatibility fields.
- Changed Scheduler frontend types and page logic to prefer `job.progress.currentStep/completedSteps/stepResults` and fall back to legacy `job.config.lastStep/lastCompletedSteps/lastStepResults`.
- Rebuilt and restarted local Core jar.
- Verified Gateway `test_scheduler_demo` trigger:
  - jobs response includes `progress.status=success`, `progress.percent=100`, `progress.metrics.rowsRead=0`, `progress.metrics.rowsWritten=0`, and payload run id / data job key.
  - history response includes `result_summary.status=success` with rows metrics and payload run id / data job key.
- `mvn -pl cynexus-core/cynexus-core-starter -am compile -DskipTests` succeeded.
- `mvn -pl cynexus-core/cynexus-core-starter -am package -DskipTests` succeeded.
- Frontend `npx tsc -b --pretty false` still fails on existing unrelated TypeScript errors, but the scheduler-specific type error introduced during this change was fixed.

## 2026-06-29 TDX Hsjday Var-Root Alignment

- User pointed to `F:\dev-repo\mp4-to-word-new\design\backend\tdx-hsjday-download-pipeline.md` and clarified that the design should be followed, but the new project should store data under `E:\project-repo\cynexus\cynexus-data\var`, not old `reference` paths.
- Reviewed the old design and mapped paths to the new project:
  - `reference\stock\download\{date}\hsjday.zip` -> `cynexus-data\var\stock\download\{date}\hsjday.zip`
  - `reference\tdx\day\hsjday-{date}` -> `cynexus-data\var\tdx\day\hsjday-{date}`
  - `reference\tdx\day\hsjday` -> `cynexus-data\var\tdx\day\hsjday`
- Updated `sync_tdx_hsjday` to perform existing-data freshness checks, use the new var paths, extract to a temp directory, move to dated target, verify, then replace live target.
- Added hsjday source helpers for existing live-root freshness and old zip cleanup.
- Expanded verification samples across SH/SZ/BJ based on the design intent.
- Moved the already downloaded 12,246-file local hsjday directory from `cynexus-data\var\tdx\hsjday` to `cynexus-data\var\tdx\day\hsjday`.
- Fixed `discover_day_files` to skip non-stock TDX `.day` files such as `sz200b07.day`.
- Updated related unit tests for the expanded hsjday samples and non-stock day-file skip behavior.
- Verification:
  - `python -m compileall -q src` passed.
  - `PYTHONPATH=src pytest -q tests/unit/test_sources/test_tdx_hsjday.py tests/unit/test_sources/test_tdx_local_day.py tests/unit/test_pipelines/test_tdx_hsjday_pipeline.py tests/unit/test_pipelines/test_kline_sync_daily.py` passed: 10 tests.
  - Restarted local `cynexus-data` on `127.0.0.1:18083`; `/internal/health` returned ok.
  - Direct `sync_tdx_hsjday` dry-run shows zip/extract/live paths under `cynexus-data\var`.
  - Direct `market_sentiment_chain_refresh` dry-run completed all 14 steps and reports `hsjday_root=E:\project-repo\cynexus\cynexus-data\var\tdx\day\hsjday`.

## 2026-06-30 MSI Historical Percentile Repair

- User asked to fix the MSI calculation/storage/history percentile issues found in review.
- Changed `build_ma_count` so non-dry-run writes historical percentile scores for `priceStrengthScore` and `breadthScore`.
- Changed `build_limit_emotion` so non-overridden runs apply old-project history percentile semantics to up/down, inverse break-board, yesterday return, and composite score.
- Changed `MsiRepository.read_component_scores` so final MSI assembly recomputes percentile components from raw fields for MA count and profit effect, and keeps fallback percentile support for turnover/risk/style raw values.
- Changed `market_sentiment_chain_refresh` so the final `market_sentiment_index_refresh` step defaults to `force=true` and `require_full=true`.
- Changed Java `MarketSentimentQueryAppService` so profit-effect display/history prefer `msi_index_daily.profit_effect_score` while retaining the factor-table score as `rawValue`.
- Added targeted Python tests for MSI component percentile assembly, MA count percentile writes, limit emotion percentile writes, and chain final-step defaults.
- Verification:
  - `PYTHONPATH=src pytest -q tests/unit/test_domain/test_msi.py tests/unit/test_pipelines/test_msi_pipeline.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py` passed: 15 tests.
  - `python -m compileall -q src` passed.
  - `mvn -pl cynexus-market/cynexus-market-starter -am compile -DskipTests` succeeded with existing unrelated Maven/MapStruct warnings.

## 2026-06-30 MSI Rerun And Before/After Comparison

- User asked to rerun MSI and record before/after comparison.
- Exported full before snapshot to `cynexus-data/var/reports/msi-rerun-20260630-115929/msi_before.csv`.
- Ran `rebuild_msi` for `2018-04-02` through `2026-06-29` with `force=true` and `require_full=true`.
- The full-factor constraint rewrote 14 dates only because `msi_sector_breadth_daily` currently has 14 rows (`2026-06-09` through `2026-06-29`); older MSI rows were left unchanged instead of being overwritten with missing-factor neutral scores.
- Attempted recent factor-level rerun for `build_ma_count` and `build_limit_emotion`; direct ClickHouse limit-emotion OHLCV read failed with `ATTEMPT_TO_READ_AFTER_EOF`.
- Switched to a safer PostgreSQL raw-factor batch recalculation:
  - Updated `msi_ma_count_daily.price_strength_score` and `breadth_score` for 1997 rows from historical percentiles of `new_high_252d_pct` and `breadth_raw`.
  - Updated `msi_limit_emotion_daily` sub-scores/composite/level for 1597 rows from historical percentiles of existing raw factor columns.
- Re-ran final `rebuild_msi` with `force=true` and `require_full=true`; 14 full-factor dates were rewritten.
- Exported final after snapshot to `cynexus-data/var/reports/msi-rerun-20260630-115929/msi_after.csv`.
- Generated diff CSV and summary:
  - `cynexus-data/var/reports/msi-rerun-20260630-115929/msi_diff.csv`
  - `cynexus-data/var/reports/msi-rerun-20260630-115929/summary.md`
- Final comparison: 1997 before rows, 1997 after rows, 14 changed rows, composite delta min/max `-3.39 / +2.97`.
- Notable level changes:
  - `2026-06-18`: `45.80 -> 44.63`, `normal -> weak`.
  - `2026-06-26`: `28.63 -> 31.60`, `ice -> weak`.
- Verification after rerun:
  - `PYTHONPATH=src pytest -q tests/unit/test_domain/test_msi.py tests/unit/test_pipelines/test_msi_pipeline.py tests/unit/test_pipelines/test_msi_remaining_factor_pipeline.py` passed: 15 tests.

## 2026-07-02 QFQ Reconciliation Runtime Optimization

- Diagnosed the slow `qfq_reconciliation_refresh` step: the chain was defaulting adjusted daily sync to both `qfq` and `hfq`, producing roughly double the planned items, and adjusted writes were issuing per-code ClickHouse `ALTER TABLE ... DELETE` mutations.
- Added chain defaults for `qfq_reconciliation_refresh`: `adjust=qfq`, `flush_rows=100000`, `replace_scope=date`, and `workers=4`.
- Added `replace_scope` support to `ClickHouseAdjustedDailyKlineSink.replace_daily`, preserving old key-scope behavior by default while allowing date-scope replacement for one-day chain refreshes.
- Added batched adjusted-frame flushing in `sync_adjusted_daily` so the chain can collect fetched rows and write them to ClickHouse in one date-scope replacement instead of per-code replacements.
- Fixed a concurrency bug introduced during the optimization: the `workers > 1` branch now only builds the per-code error string inside the `except` block.
- Added a regression test for successful `workers > 1` adjusted-daily fetches.
- Changed batched adjusted-daily checkpointing so `resume=true` checkpoints are written only after the batch flush succeeds.
- Verification: `PYTHONPATH=src python -m pytest tests/unit/test_pipelines/test_kline_sync_adjusted_daily.py tests/unit/test_pipelines/test_kline_sync_daily.py tests/unit/test_pipelines/test_fund_flow_pipeline.py -q` passed: 19 tests.
- Triggered full `market_sentiment_chain_refresh` run `job_run_f9a1e8c6e1d946fdac44a8c4099f2b85` at 2026-07-02 00:20:08 +08:00 to measure runtime.
- The run verified the intended QFQ optimization shape: step 3 `qfq_reconciliation_refresh` planned `totalItems=5530` instead of the old `11060`, and fetched roughly `5400/5530` symbols by elapsed second 264.
- The run failed at elapsed 266s during adjusted daily date-scope ClickHouse delete because `daily_qfq` has no `source` column while the shared date delete helper added `source IN ('eltdx')` from the input frame.
- Fixed date-scope delete to apply the source filter only when the target ClickHouse table actually has a `source` column.
- Added ClickHouse writer tests covering date-scope delete with source on `daily_raw` and without source on `daily_qfq`.
- Verification after the source-filter fix: `PYTHONPATH=src python -m pytest tests/unit/test_storage/test_clickhouse_writers.py tests/unit/test_pipelines/test_kline_sync_adjusted_daily.py tests/unit/test_pipelines/test_kline_sync_daily.py tests/unit/test_pipelines/test_fund_flow_pipeline.py -q` passed: 27 tests.
- Rebuilt/recreated `cynexus-data-job-executor` after the source-filter fix and verified `/internal/health` returned ok.
- Triggered second full `market_sentiment_chain_refresh` run `job_run_fcfb6c2978fb479496de76b06c79fc7a` at 2026-07-02 00:27:11 +08:00.
- The second run completed successfully in 346 seconds (5m46s), with 14/14 steps complete, rows_read=43771, rows_written=27095.
- Step 3 `qfq_reconciliation_refresh` completed with rows_read=5185 and rows_written=5185; polling showed it started after about 201-212s elapsed and the chain had entered step 4 by 284s elapsed, so QFQ reconciliation took roughly 70-80s.
- ClickHouse mutation check after the successful run showed `daily_qfq` had 1 mutation and 0 active mutations for the run, confirming date-scope replacement avoided thousands of per-code mutations.
- Core scheduler history and Gateway history both report success and duration around 346 seconds, but two success history rows were written for the same Python run id; this duplicate history write is a separate Core-side cleanup item.

## 2026-07-02 THS Industry Constituents Job Fallback

- User confirmed that if live THS industry codes cannot be fetched, using previous stored code data is acceptable because the industry list is stable.
- Added Phase 32 to `task_plan.md`.
- Re-read the current `sync_ths_industry_constituents`, THS constituent repository, sector fund-flow pipeline, sector fund-flow repository, and related tests.
- Added `ThsIndustryConstituentRepository.list_latest_industry_codes()` to read distinct industry codes from the latest alive THS constituent snapshot.
- Added `SectorFundFlowRepository.latest_sector_code_by_name()` and `latest_sector_codes()` to read stable historical THS industry mappings from sector fund-flow snapshots.
- Changed `sync_ths_industry_constituents` so live mode prefers explicit params, then latest constituent snapshot codes, then historical fund-flow codes. It now returns `code_source` and planned count in the result.
- Changed sector fund-flow normalization to accept an injected `sector_code_by_name` mapping and fill missing industry codes by sector name while keeping domain logic DB-free.
- Changed `sync_sector_flow` to load the historical mapping from the repository and pass it into normalization for industry fund-flow writes.
- Added tests covering constituent-code fallback and fund-flow code enrichment.
- Verification: `PYTHONPATH=src python -m pytest tests/unit/test_pipelines/test_ths_industry_constituents_pipeline.py tests/unit/test_pipelines/test_fund_flow_pipeline.py tests/unit/test_domain/test_fund_flow.py -q` passed with 15 tests.
- Queried local PostgreSQL and confirmed latest industry fund-flow rows for 2026-06-29, 2026-06-30, and 2026-07-01 each had 90 missing codes before repair.
- Repaired existing local sector fund-flow rows from historical mappings; 270 rows updated.
- Verified recent industry fund-flow dates now have 0 missing codes, latest date has 90 distinct codes, and THS constituent snapshot fallback has 90 codes.
- Rebuilt/recreated `cynexus-data-job-executor`; `/internal/health` returned ok.
- Container-loaded code verification returned `{'constituent_codes': 90, 'fund_flow_name_code': 90}`.
- Verification: `python -m compileall -q src` passed under `cynexus-data`.

## 2026-07-02 F10 Query Chain Migration

- User clarified the desired runtime shape: `cynexus-ui` calls `cynexus-market`, then `cynexus-market` calls `cynexus-data`, and the result is normalized back up the chain.
- Re-read `task_plan.md`, `findings.md`, `progress.md`, and `docs/migration-guides/python-to-java-api-migration-guide.md` before implementation.
- Re-read `AGENTS.md`; it contains `DO NOT send optional commentary!!!`.
- Attempted to run the planning skill catch-up script using the `.claude` path from the skill docs; that failed because the file does not exist in this environment.
- Located the actual planning skill script path under `.agents`, confirming the earlier failure was environmental rather than repository-related.
- Inspected current frontend F10 consumer `cynexus-ui/src/components/stock-detail-dialog.tsx`; confirmed stock chart/meta/sectors already use Java services while F10 still imports from `@/services/legacy/f10`.
- Inspected legacy frontend adapter `cynexus-ui/src/services/legacy/f10.ts`; confirmed the page-facing normalized response shapes that need Java equivalents.
- Inspected old Python F10 route and service stack in `F:\dev-repo\mp4-to-word-new\backend\api\stock\f10.py` and `backend/services/stock/f10/*`; confirmed the exact legacy endpoints and adapter-backed data scopes.
- Inspected `cynexus-data` FastAPI internal app and confirmed there is an existing `/internal/*` service surface suitable for adding F10 query endpoints.
- Inspected `cynexus-core` `HttpDataJobExecutorClient`; it is the cleanest current pattern for Java -> Python internal HTTP calls in this repo.
- Inspected `cynexus-market` stock-chart controller/app-service/domain-repository/infra-repository chain; this is the right place to add typed F10 query contracts for the stock detail dialog.
- Added Phase 33 to `task_plan.md` for the F10 query-chain migration.
- Added `cynexus-data/src/cynexus_data/sources/tdx/f10.py` with scoped F10 source parsers for valuation, finance report, business composition, profit forecast, announcements, news, roadshows, and company news.
- Added `cynexus-data/src/cynexus_data/interfaces/trigger_api/f10_routes.py` and registered it in the existing FastAPI app so `market` has internal `/internal/f10/*` query endpoints.
- Extended `StockChartBO`, `StockChartDTO`, `StockChartQueryRepository`, `StockChartQueryRepositoryImpl`, `StockChartQueryAppService`, and `StockChartQueryController` with typed F10 query contracts under `/api/market/stock-chart/f10/*`.
- Added `cynexus-market/.../HttpStockChartF10DataClient.java` as the Java -> Python internal HTTP client for F10 queries.
- Added `cynexus-ui/src/services/market/f10.ts` and switched `src/components/stock-detail-dialog.tsx` plus `src/lib/api.ts` compatibility exports to the new market service.
- Updated `src/services/legacy/README.md` to remove F10 from the still-legacy page list.
- Verification:
  - `python -m compileall -q src` passed under `cynexus-data`.
  - `npx tsc -b --pretty false` passed under `cynexus-ui`.
  - `mvn -pl cynexus-market/cynexus-market-starter -am compile -DskipTests` passed.
  - `npm run build` passed under `cynexus-ui`.
  - `rg -n "@/services/legacy/f10|services/legacy/f10" cynexus-ui/src -S` returned no matches.
- Runtime validation note: the new endpoints were not smoke-tested over live HTTP in this turn because local `cynexus-data` / `cynexus-market` processes were not restarted here.

## 2026-07-06 A Stock Data Source Integration

- User requested all missing/partial `a-stock-data.md` capabilities be integrated into `cynexus-data` with plan/process/result records, live connectivity testing, and no layering shortcuts.
- Added Phase 34 to `task_plan.md`.
- Created `cynexus-data/docs/a_stock_data_integration/plan.md`, `process.md`, and `result.md`.
- Added EastMoney shared HTTP helper and source functions for reports, industry reports, stock info, concept blocks, minute/daily fund-flow, industry rank, dragon tiger, lockup, margin, block trades, holders, dividends, news, global news, limit pools/sentiment, hot rank, and hot concept.
- Added provider modules for Baidu K-line MA, iwencai search/query, CNInfo announcements/IRM, THS EPS/hot/northbound/limit/hot-list, Sina financial/options, and mootdx quote/order-book/transactions.
- Added thin FastAPI internal query routes under `/internal/a-stock/*` and registered them in the existing trigger API app.
- Added unit tests in `cynexus-data/tests/unit/test_sources/test_a_stock_data_sources.py`.
- Added live smoke script `cynexus-data/scripts/a_stock_data_live_smoke.py`.
- Verification:
  - `python -m compileall -q src scripts` passed under `cynexus-data`.
  - `PYTHONPATH=src python -m pytest -q tests/unit/test_sources/test_a_stock_data_sources.py` passed: 5 tests.
  - FastAPI app import/route check found 37 `/internal/a-stock/*` paths registered inside included routers.
  - Live smoke run `var/reports/a-stock-data-smoke/20260706-233403`: 26 ok, 4 failed, 1 skipped.
  - Fixed Baidu endpoint/headers after initial 403.
  - Live smoke run `var/reports/a-stock-data-smoke/20260706-233539`: 25 ok, 5 failed, 1 skipped.
- Latest live smoke failures are recorded in `cynexus-data/docs/a_stock_data_integration/result.md`: selected EastMoney `push2`/`push2his`/`emappdata` endpoints reset the connection through the current network/proxy; iwencai skipped because no `IWENCAI_API_KEY` is configured.
- Tooling notes:
  - A PowerShell heredoc-style `python - <<'PY'` check failed because PowerShell does not support that redirection syntax; reran with a PowerShell here-string piped to Python.
  - A first FastAPI route count looked at top-level route `.path` only; this FastAPI version keeps included routers as `_IncludedRouter`, so the check was corrected to inspect each `original_router.routes`.

## 2026-07-07 A Stock Data Persistence And Knowledge Base Docs

- User asked to connect all Phase 34 A-stock capabilities to persistence, design anti-scraping scheduler intervals, and land detailed table/capability documentation into the knowledge base.
- Re-read planning files and the planning skill instructions.
- Added Phase 35 to `task_plan.md`.
- Confirmed the persistence work must preserve the existing `cynexus-data` layering: provider source modules stay read-only, domain modules shape rows, pipelines orchestrate, storage modules write, and jobs register executor entrypoints.
- Added A-stock PostgreSQL DDL at `cynexus-data/sql/postgres/20260707_create_a_stock_data_tables.sql`.
- Added A-stock ClickHouse DDL at `cynexus-data/sql/clickhouse/20260707_create_a_stock_data_tables.sql`.
- Added pure A-stock persistence normalization in `cynexus-data/src/cynexus_data/domain/stock/a_stock_persistence.py`.
- Added A-stock storage repositories:
  - `cynexus-data/src/cynexus_data/storage/postgres/a_stock.py`
  - `cynexus-data/src/cynexus_data/storage/clickhouse/a_stock.py`
- Added `sync_a_stock_dataset` pipeline/job/registry/jobs.yaml/pyproject console wiring.
- Added unit tests in `cynexus-data/tests/unit/test_pipelines/test_a_stock_persistence_pipeline.py`.
- Created knowledge-base source Markdown docs:
  - `docs/knowledge-base/market-data/a-stock-data-persistence-tables.md`
  - `docs/knowledge-base/market-data/cynexus-data-a-stock-capability-map.md`
- Backed up local PostgreSQL before applying A-stock DDL: `deploy/local/postgres-backups/cynexus-before-a-stock-persistence-20260707-011519.sql`.
- Applied PostgreSQL DDL successfully; verified 19 `a_stock_%` tables and `sec_stock_basic` EastMoney extension fields.
- First ClickHouse DDL application failed because `ch_ods_stock_transaction` sorting key contained nullable columns; fixed the sorting key and re-applied successfully.
- Verified ClickHouse has the 6 new A-stock tables.
- Ran live persistence smoke:
  - `cninfo_announcements` for `000001` wrote 1 row to `a_stock_announcement`.
  - `tdx_order_book` for `600519` wrote 1 row to `ch_ods_stock_order_book_snapshot`.
- Published two KB documents to local PostgreSQL `kb_document`:
  - `332571765114957824`: `A Stock 数据落库表设计`, status `published`.
  - `332571765354033152`: `Cynexus Data A 股能力全量地图`, status `published`.
- Found CLI `--payload '[...]'` initially stayed as a string, so dry-run normalized 0 rows; added JSON parsing for string payload/payloads and a regression test.
- A Python ClickHouse repository create-table smoke initially failed because `CLICKHOUSE_PASSWORD` was absent from the shell environment; re-ran with `CLICKHOUSE_PASSWORD=C020611.` and it succeeded with 7 statements.
- Final verification:
  - `python -m compileall -q src scripts` passed.
  - `PYTHONPATH=src python -m pytest -q tests/unit/test_sources/test_a_stock_data_sources.py tests/unit/test_pipelines/test_a_stock_persistence_pipeline.py` passed: 9 tests.

## 2026-07-07 Settings Data Source Health Check

- User requested a settings health-check page for all `cynexus-data` data sources, flowing Python -> Java -> UI, with health metrics persisted in `cynexus_appl_core`.
- Added Phase 36 to `task_plan.md`.
- Inspected the existing Core Java -> Python data-job executor client and decided to extend it instead of creating an ad hoc HTTP client.
- Inspected existing UI `/settings/data-source`; it currently shows Market table freshness through `src/services/market/data-source.ts`.
- Started Python implementation with `quality/data_source_health.py` and `/internal/data-sources/health`, covering storage, TDX filesystem, EastMoney, Tencent, Sina, THS, exchange margin, Baidu, CNInfo, iwencai, AkShare, and mootdx checks.
- Extended the health source inventory to 24 checks by adding pytdx/eltdx/mootdx package probes plus EastMoney Datacenter/Report/Search/HotRank and THS basic forecast probes.
- Added Python unit tests for health summaries, missing iwencai API key behavior, and missing package import reporting.
- Added Core domain/application/infra/controller files for persisted health checks and Java -> Python bridging:
  - `DataJobExecutorClient.dataSourceHealth`
  - `DataSourceHealthAppService`
  - `DataSourceHealthController`
  - `DataSourceHealthRepository` and MyBatis basic services/mappers/XML
  - health run/item BO/PO/DTO/converters
- Added PostgreSQL migration `deploy/local/postgres/migrations/20260707_create_data_source_health_tables.sql` for `cynexus_appl_core.data_source_health_check_run` and `data_source_health_check_item`.
- Applied the local PostgreSQL migration after backup `deploy/local/postgres-backups/cynexus-before-data-source-health-20260707-164010.sql`.
- Fixed runtime insert failure where MyBatis-Plus mapped `successRate24h` to nonexistent column `success_rate24h`; added `@TableField("success_rate_24h")`.
- Added frontend Core service `cynexus-ui/src/services/core/data-source-health.ts`.
- Updated Settings -> Data Source page to support health/freshness tabs, manual quick/full checks, provider/type/status/search filters, summary cards, and a health table with connectivity/authenticity/latency/24h success rate.
- Added health UI components `health-summary.tsx`, `health-table.tsx`, and `health-status-badge.tsx`.
- Rebuilt/recreated the `cynexus-data-job-executor` container so the new Python route/source inventory is live.
- Direct `cynexus-data` smoke for `/internal/data-sources/health?include_slow=false` returned total=24, healthy=18, degraded=1, failed=1, skipped=4.
- Gateway/Core smoke for `POST /api/core/data-source-health/checks/run?includeSlow=false` succeeded after the MyBatis column fix; latest persisted Gateway response returns run `ds_health_a0528a52212845cb941455b14a5c3539` with total=24.
- Local PostgreSQL now contains 5 health runs and 64 health item rows.
- Verification:
  - `python -m compileall -q src scripts` passed under `cynexus-data`.
  - `PYTHONPATH=src python -m pytest -q tests/unit/test_quality/test_data_source_health.py tests/unit/test_sources/test_a_stock_data_sources.py tests/unit/test_pipelines/test_a_stock_persistence_pipeline.py` passed: 12 tests.
  - `mvn -pl cynexus-core/cynexus-core-starter -am package -DskipTests` passed.
  - `npx tsc -b --pretty false` was attempted under `cynexus-ui`; it still fails on unrelated existing errors in `src/services/market/pulse.ts`, `industry-tdx-fund-overlay-chart.tsx`, `sparkline.tsx`, and `PageHeader.tsx`.

## 2026-07-07 Settings Health Page 503 Follow-up

- User reported the page still showed 503 with no data.
- Rechecked Gateway and confirmed `GET /api/core/data-source-health/latest` was returning HTTP 503 because no live Core service was listening on port 18080; the older IntelliJ Java process remained alive but had released the port.
- Started the packaged Core jar on port 18080 with logs under `deploy/local/logs/cynexus-core-health.log`; Core registered to Nacos as `cynexus-core`.
- Verified direct Core health data endpoint returned HTTP 200.
- Verified Gateway health data endpoint returned HTTP 200.
- Verified Market freshness endpoint also returned HTTP 200.
- Patched the Settings Data Source page so health and freshness requests use `Promise.allSettled`; a failure in one tab no longer blanks the other tab.
- Re-ran `npx tsc -b --pretty false`; the same unrelated TypeScript errors remain, with no new diagnostics from the data-source health files.
## 2026-07-11 Application Analysis Stock Fund Flow

- Connected typed EastMoney order-size fund-flow history to the Market stock-chart fund-flow response and merged Tencent snapshot fields by trade date.
- Added Fund Flow UI rendering for main/super-large/large/medium/small net flow and an available-data fallback for Tencent active flow without empty metric grids.
- Added and applied `20260711_schedule_stock_fund_flow_daily.sql`; Core resolver maps it to `sync_stock_fund_flow` with self-selected codes.
- Added self-selected code resolution and source trade-date parsing to the Tencent stock-flow pipeline.
- Backfilled six self-selected symbols; `600415` now serves 2026-07-10 active net `46.356894` wan, active buy ratio `0.53335021`, and turnover `1.25` through both Market and Vite proxy.
- Verification passed: 10 targeted Python tests, Market/Core Maven compile, targeted frontend ESLint, Data/Market/Core restart, direct and proxy HTTP smoke.
- Full frontend TypeScript build still reports unrelated pre-existing errors outside Fund Flow.
- Follow-up: corrected Tencent active-net conversion from lots to shares; the original value was understated by 100x.

## 2026-07-11 EastMoney Fund Flow Resilient Collection

- Added a shared, paced EastMoney session with jitter, realistic headers, retry-after support, exponential retry backoff, connection cooldown, and proxy-bypass fallback.
- Added a 15-minute Push2His circuit breaker and a Push2Delay one-day fallback with explicit source/mode/history-complete metadata.
- Updated persistence to preserve `eastmoney.push2delay`; Market history reads prefer Push2His when both sources exist for the same date.
- Added source/fallback/history coverage counters to `sync_a_stock_dataset` results.
- Live run `job_run_fc79ce46ed9c4bc4aae75ffedbc0af29`: 6 selected codes, 6 rows written, all explicitly daily fallback, no false history completion.
- Rebuilt Data Executor and Market; Vite-proxy smoke for `600415` passed with EastMoney main net and Tencent active net as separate fields.
- Added browser-session support via ignored local `.env` variables `EASTMONEY_QGQP_B_ID` and `EASTMONEY_NID18`; no cookie values are committed.
- Added browser-equivalent mobile JSONP headers and JSONP response parsing. Targeted A-stock tests now pass: 15 source/persistence tests.
- The user cURL returned 120 rows with the two-cookie minimum, but subsequent repeated verification entered temporary upstream throttling; the running job correctly fell back to one day instead of retrying aggressively.

## 2026-07-11 Automatic EastMoney Browser Session

- Added a Playwright persistent browser session that visits EastMoney home, waits five seconds, and caches only `qgqp_b_id` and `nid18` under `cynexus-data/var`.
- Wired Push2His failure handling to refresh cookies once, retry history once, then use the existing 15-minute circuit breaker and Push2Delay fallback.
- Added Debian Chromium to `Dockerfile.data` and configured the Data Executor to use `/usr/bin/chromium` with the existing `/app/var` persistent mount.
- Added a daily 20:30 history-backfill job that selects one self-selected stock with fewer than 100 Push2His rows; changed the normal A-stock daily job to incremental fund-flow mode.
- Live verification refreshed both cookies and completed a 120-row Push2His backfill for `600415` without manually configured cookie values.
- Recreated Data Executor from the verified local image and confirmed Chromium, cookie cache, and persistent browser profile remain available after recreation.
- Verified scheduler state: `a_stock_code_daily` is enabled in `daily` mode and `a_stock_fund_flow_history_backfill` is enabled at 20:30 in `history` mode.
- Verification passed: 17 targeted Python tests and `git diff --check`; `.env` and all browser state under `cynexus-data/var` remain ignored by Git.
## 2026-07-11 - Application Analysis Information Tab

- Started the information-tab implementation.
- Confirmed the existing typed `cynexus-data -> cynexus-market` F10 chain is the correct source; no parallel direct frontend-to-Python path is needed.
- Added the `Information` tab, page-level information feed component, and dedicated `src/services/market/stock-information.ts` aggregation layer.
- The feed loads announcements, news, research, and roadshows concurrently through the typed Market F10 endpoints and supports per-kind filtering, chronological grouping, partial-source failures, refresh, and original links.
- Live Vite-proxy smoke for `600415` returned HTTP 200 from all four Market endpoints.

## 2026-07-12 - Viewable Stock Information Feed

- Verified that TDX F10 details cannot supply reliable news/research content or URLs; implementation is moving the Information tab to the project-owned persisted information datasets.
- Added `sina_stock_news` to the pipeline, PostgreSQL normalization, watchlist warmup, and the daily Core scheduler. Focused Python source/persistence tests passed: 17 tests. A live Sina smoke for `600415` returned 40 dated rows with article URLs.
- Market packaging initially failed only at Spring Boot repackage because the running Market process locked the starter JAR; all Market modules had compiled successfully. The process will be restarted before rebuilding the executable JAR.
- The first Data Executor image build exceeded the two-minute command limit without reporting a Docker failure. Retry with a longer timeout to allow the Chromium/Python image layers to finish instead of altering the image or bypassing the runtime rebuild.
- The later Docker build did complete and produced the current image; Data Executor was recreated and `/internal/health` returned `ok`. Market was rebuilt/restarted and its health endpoint returned `UP`.
- Manual `sync_a_stock_dataset` backfill for `600415` wrote 169 rows: 40 Sina news items with article URLs, 99 EastMoney reports, and 30 CNInfo announcements. Vite-proxy smoke for `/api/market/stock-information?symbol=600415&limit=20` returned 20 viewable announcements, news, and research records plus seven roadshows.
- Final verification: 17 focused Python tests passed; Core compile and Market package passed; scoped frontend ESLint has only three existing unused-disable warnings; `git diff --check` passed.

## 2026-07-12 - Information Content And AI Analysis Design

- Designed the layered on-demand extraction and AI-analysis path. It keeps source extraction in Data, source/content aggregation in Market, and model execution/traces in AI.
- Confirmed a prior file read used a nonexistent `AiExecutionAppService` path; the actual reusable service is `ScenarioExecutionAppService`, with `MediaTranscriptionAiAppService` providing the stronger prompt/version/fallback/logging pattern.
- A source-capability probe initially ran Market file reads from the `cynexus-data` working directory and failed due to relative paths. Re-ran from the workspace root; this was a tooling-path error, not a code issue.
- Implemented the Data extraction endpoint, Market content/analysis cache schema and typed endpoints, Market-to-AI invocation, and the Information-tab read/analyze controls. Focused Python tests now pass: 20 tests.
- Data Executor image rebuild exceeded the ten-minute command cap without producing a new image. The Dockerfile and requirements are updated; use a controlled in-container dependency/source refresh plus image commit for this local runtime, rather than repeatedly retrying the same blocked build.
- The first runtime refresh copied source under `/app/src`, but the entry point imports the installed package in site-packages, so the new route returned 404. Refresh the installed package from `/app` before restarting; do not treat source-copy alone as a live deployment.
- First Market content smoke reached the new Data endpoint but failed at PostgreSQL insert because the content upsert listed one extra JDBC placeholder. The binding count was corrected before retrying.
- The corrected content smoke successfully persisted a Sina article, but its HTTP response declared ISO-8859-1 while containing GB18030 text, producing garbled Chinese. The extractor now treats that legacy declaration as GB18030 and preserves paragraph boundaries before the final retry.
- A second live fetch established the Sina body was valid UTF-8 despite its ISO-8859-1 header. The fallback now tries strict UTF-8 first and uses GB18030 only when UTF-8 decoding fails.
- AI analysis initially exposed an existing generic AI routing defect: `ScenarioExecutionAppService` reads a nonexistent `ai_model_config_p_o` table. Stock-information execution now resolves an enabled provider from the existing usage bindings, falling back to the configured `text_summary` capability until a dedicated binding is added.
- The first post-routing analysis still returned the generic executor's synthetic JSON. `AgentExecutor` now delegates no-tool scenarios to the configured provider, while the existing tool-enabled placeholder behavior remains unchanged for unrelated scenarios.
- Live AI verification found the `text_summary` binding points at a Minimax provider without an API key, while the existing `application_analysis` DeepSeek binding has one. Stock-information analysis now prefers a dedicated binding, then `application_analysis`, then `text_summary`, preserving explicit model overrides.
- Completed the source reading and AI analysis feature. Applied the Market migration after backup; the live `600415` Sina article fetch returned cleaned Chinese text and DeepSeek `deepseek-v4-flash` produced a structured analysis through the Vite-proxy Market endpoint.
- Final verification: 21 focused Python tests passed; Market and AI package builds passed; scoped Information-tab ESLint passed; Data/Market/AI health are `ok`/`UP`; `git diff --check` passed.

## 2026-07-12 - Evidence-Aware Information Prompt

- Upgraded stock information analysis to v2: strict source-only, no investment-advice generation, company relevance, impact horizon, confidence, risks, uncertainties, and verbatim source evidence. Existing v1 cache rows are bypassed.
- Docker Desktop stopped during the service restart, taking Nacos and the Vite process down. Restarted Docker Desktop, then restarted Market, AI, Gateway, and Vite. Vite-proxy requests now work again.
- The restored Redis container cannot bind host port `6379` because Windows currently reserves it, so Market/AI actuator health remains `DOWN` on Redis while their application endpoints and the Information content/analysis flow remain operational.
- Verification: Market/AI package builds and scoped frontend ESLint passed. Vite-proxy returned the refreshed DeepSeek v2 analysis for `600415`, including relevance, horizon, confidence, and verbatim evidence.
## 2026-07-13

- Started market information implementation; planning files initialized.
## Market Information (2026-07-13)

- Implemented market information domain, repository, PostgreSQL client, application service, DTO, and controller.
- Added the `/market/information` route and market sidebar entry with split news/signals/hot-rank/Dragon-Tiger components.
- Maven package and Vite production build pass; versions advanced together to `1.0.4-SNAPSHOT`.
## Market Information source expansion (2026-07-13)

- Python data executor image rebuilt and market source jobs triggered successfully for Cailian and CNInfo policy data.
- Java/UI release advanced to `1.0.12-260713-RELEASE`; `/api/market/information` returns macro news, policy announcements, leaders, and leader events.
- User approved integrating the full proposed source set: official macro/policy sources, exchanges, CNInfo regulatory data, media feeds, industry sources, and leader-company events.
- Phase 45 started with an inventory-first boundary: source adapters remain in `cynexus-data`, Java remains the aggregation boundary, and the UI will not call providers directly.
- Added the encoding-safe official-source collector and `official_market_information` dataset. Live run wrote 96 headlines from eight official source families; source failures remain isolated.
- Expanded the Java feed to include official and EastMoney global news, with round-robin source quotas so lower-volume official sources remain visible.
- Added and live-tested CNInfo regulatory announcements; 94 rows were persisted with issuer codes and canonical PDF links.
- Added China Automobile Industry Association and China Nonferrous Metals Industry Association news, policy, and statistics feeds. The refreshed official-source job wrote 156 rows.
- Released `1.0.18-260713-RELEASE`. Live API verification returns 60 news rows evenly interleaved across 15 sources, 60 CNInfo policy/regulatory rows, and zero malformed `�` characters.

## Market Information pagination (2026-07-13)

- Started Phase 46 to replace the fixed 60-row feed with source-balanced server pagination and collect deeper source history.
- The database already has more than 400 eligible market-news rows, but the previous contract exposes only the first 60 with no navigation.
- Implemented server-side source-balanced pagination and unified market news plus CNInfo market/regulatory announcements into one typed information page.
- Added category, kind, issuer, and contextual labels to the Market DTO and rendered the result through the project shadcn Table and Pagination components.
- Expanded EastMoney 7x24 collection to 300 cursor-paged rows per scheduled run and official sources to 40 rows per source. Live backfill wrote 877 rows; the Release API now reports 1,157 records over 39 pages.
- Deployed `1.0.19-260713-RELEASE`. Page 1 and page 2 each return 30 distinct records, all five categories are present, announcement rows are included, and Playwright found no browser console errors.

## Market Information AI pipeline (2026-07-14)

- Started Phase 47 from the approved implementation plan.
- Confirmed the existing OpenAI-compatible adapter already supports Responses API payloads, `reasoning.effort`, `store=false`, JSON output, token usage, and output-text extraction.
- Confirmed the remaining provider gap is a registered `custom` adapter and a dedicated cross-provider route; existing usage-binding fallback lists only switch model names within one provider.
- Confirmed existing stock-information extraction covers only a small host allowlist, so market-wide extraction must expand first-party hosts while retaining redirect/size/SSRF boundaries.
- Implemented the first pipeline pass: Market/AI migrations, Custom Responses adapter, persisted AI tasks/results/briefs, deterministic clusterer, enriched Market APIs, Market Brief, event Sheet, and extraction SSRF hardening.
- Partial verification passed: Market/AI Maven package, scoped Market Information ESLint, and 8 focused Python source/extraction tests.
- Full UI build remains blocked by pre-existing TypeScript errors in Market Pulse, Industry Application, Market Sentiment, and stock-overview modules; Market Information scoped lint has no errors.
- Remaining Phase 47 work: wire extraction persistence, make cluster refresh incrementally stable, add task recovery and batch status, add typed Core automation schedules, create 60-case evaluation assets/runner, then version/deploy/verify.
- The first recursive Maven version update only changed direct root children; a second reactor command failed because nested parent versions still referenced `1.0.19-SNAPSHOT`. Resolved with a mechanical all-POM replacement to `1.0.20-SNAPSHOT`, then continued verification.
- Completed the implementation pass: post-write Python content extraction/cache, incremental cluster upsert and member reconciliation, stale-task recovery, batch status API/polling, typed Core Market automation, three scheduler entries, 60-case evaluation fixture, guarded live-eval runner, and `1.0.20-SNAPSHOT` version alignment.
- Migration applied successfully twice to the local PostgreSQL instance, confirming idempotency. Provider rows contain only the environment variable name; no secret is persisted.
- Verification now passes: 9 focused Python tests, 3 deterministic cluster tests, 1 evaluation dataset test, Market Information scoped ESLint, Maven reactor test/package compile, and offline live-eval guard. Full UI build is still blocked only by the previously recorded unrelated TypeScript failures.
- The first Release start attempt correctly stopped Dev but failed before Docker mutation because the `1.0.20-SNAPSHOT` gateway artifact had not been built by the scoped reactor package. Resolution: run the full Maven package, then invoke the same versioned Release entrypoint.
- Release images were built and recreated as `1.0.20-260714-RELEASE`. The first combined health-smoke PowerShell command had an unquoted `&` in the query string and failed at parse time; no service state was changed by that failed diagnostic command.
- First container startup exposed a runtime-only DI mismatch: Spring Boot 4 did not provide the legacy Jackson `com.fasterxml.jackson.databind.ObjectMapper` bean expected by the new Market and AI constructors. Updated both components to own their existing Jackson mapper directly, matching other adapters in this repository, then rebuilt the affected images.
- Reconfigured the Custom provider to `https://am.xtyooo.top`, Responses API, `gpt-5.5`, high reasoning, and upstream storage disabled. A live event analysis and daily brief both succeeded through Custom without fallback.
- Migrated the Custom secret from the ignored `.env` entry into the existing AI Provider persistence contract (`ai_providers.api_key`), cleared `api_key_env`, recreated AI with a blank Custom environment variable, and verified the request still reached the provider.
- The follow-up smoke was rejected by the upstream provider with `429 DAILY_LIMIT_EXCEEDED`; Provider/Worker/database secret resolution is working, but further live evaluation must wait for quota reset or increased allowance.

## 2026-07-14 - My Codex provider addition

- Started Phase 48.
- Re-read `AGENTS.md` and restored the existing file-based plan context.
- Recorded the multi-provider adapter-dispatch risk and the secret-handling boundary before code changes.
- Logged and recovered from one planning-file append context mismatch without changing project code.
- Inspected the existing worker, model object, adapters, persistence cache keys, migration seed, and Provider masking path.
- Selected a non-breaking design: adapter code and provider-row identity will be carried separately.
- Added `AiModelBO.providerIdentity` and updated Market Information to dispatch through `provider_type` while persisting the unique provider row code.
- Added a focused multi-Custom-provider unit test.
- Added the secret-free, idempotent `20260714_add_my_codex_provider.sql` migration and corrected the old Custom provider remark.
- Focused AI reactor tests passed: 4 tests, including the new adapter/identity test.
- A diagnostic read referenced the old nonexistent `AiModelPO.java` filename; the actual converter uses `AiModelConfigPO`. No code change depended on the failed path.
- Mapped legacy model configuration `providerCode` into the new identity field to avoid a new MapStruct warning.
- Advanced all Maven modules and the UI together from `1.0.20-SNAPSHOT` to `1.0.21-SNAPSHOT`.
- Applied the new provider migration to PostgreSQL; `custom-yancc` now exists with `gpt-5.4`, low reasoning, `store=false`, and no environment-key reference.
- Stored the credential through the existing AI Provider CRUD API and verified the API never returns the raw key.
- Ran a live provider-only smoke successfully; the response confirmed model `gpt-5.4`, structured JSON output, and upstream storage disabled.
- Reapplied the provider migration and confirmed it preserves the stored credential, proving idempotency.
- Full Maven package attempt reached Core compilation but the local Corretto 21 JVM crashed with `EXCEPTION_ACCESS_VIOLATION`; this was a JVM process failure, not a Java compilation diagnostic. A safer compiler-mode retry is required.
- Retried with `-XX:TieredStopAtLevel=1`; the complete 41-module Maven package passed.
- Final runtime smoke passed: Gateway and AI are `UP`, the new Provider is visible and enabled, its key is masked, and no raw credential is returned.
- Completed Phase 48 without changing the existing Market Information usage bindings or redeploying Release.

## 2026-07-14 - Market Information DeepSeek switch

- Started Phase 49 and restored the file-based plan context.
- Confirmed the current binding/provider/runtime-limit state.
- Live provider smoke succeeded for `deepseek-v4-flash` with structured JSON output.
- Added an idempotent binding migration that makes DeepSeek V4 Flash primary for both event analysis and daily briefs, with no self-fallback.
- Kept Dev capped at 20 calls and changed only Release configuration to unlimited mode (`-1`).
- Advanced Maven and UI development versions together to `1.0.22-SNAPSHOT`.
- Applied the binding migration twice successfully and recreated the Release AI container with limit `-1`.
- The first queued event remained pending with `DeepSeek live-call development budget exhausted`; diagnosed an unreachable unlimited branch caused by constructor clamping.
- Fixed the clamp and added a regression test for unlimited Release requests.
- Deployed `1.0.22-260714-RELEASE`; all four Java services became healthy and the AI container received limit `-1`.
- The recovered task reached DeepSeek but failed strict evidence validation because a title-only source prompted inferred policy details.
- Upgraded the event prompt to v2 with explicit `summary_only` and exact-contiguous-quote rules; strict evidence validation remains unchanged.
- Advanced Maven and UI to `1.0.23-SNAPSHOT`, reran the binding migration with prompt version v2, passed focused tests and the full Maven package.
- Deployed immutable Release `1.0.23-260714-RELEASE`.
- Forced event task 55 succeeded on `deepseek-v4-flash` with no fallback; the latest daily brief is also successful on DeepSeek v2.
- Final runtime verification passed: Core, AI, Market, and Gateway are `UP`, and all Release images use `1.0.23-260714-RELEASE`.

## 2026-07-14 - Market Brief structure and freshness

- Started Phase 50 and restored the file-based plan context.
- Audited the live brief, worker prompt/validation, repository input query, API transport, and UI rendering.
- Confirmed the mismatch is caused by both a loose protocol and stale/multi-version analysis selection, not merely visual presentation.
- Inspected the complete AI repository contract/implementation, Market BO/DTO mapping, latest-brief query, frontend service types, and current brief component.
- Chose a backward-compatible boundary: keep `contentJson` storage/API while making its internal protocol strictly typed and versioned.
- Refactored the repository contract so brief input is selected by current event schema/prompt versions.
- Replaced the stale 36-hour row scan with one latest successful analysis per cluster, joined to the cluster's current content hash, plus deterministic source coverage and analysis window metadata.
- Added the v2 brief schema, v3 prompt, nested validation, reference validation, exact source/window checks, and two focused protocol tests.
- Rebuilt the Market Brief UI to display headline, executive summary, tone/confidence, structured events, separate policy/industry/company signals, risks, uncertainties, analysis window, and source coverage.
- Focused AI tests passed: 8 tests total.
- The first scoped ESLint invocation used the repository root and failed before linting; it must be rerun from the UI package directory.
- Scoped frontend ESLint passed. The subsequent full build failed only in the previously known unrelated Market Pulse, Industry Application, Market Sentiment, and PageHeader files.
- Executed the new brief-input SQL directly against live PostgreSQL; it returned 30 unique current events with deterministic coverage/window metadata.
- Added deterministic enrichment before validation so the model selects and summarizes events while trusted event-analysis metadata remains authoritative.
- Advanced Maven and UI versions together to `1.0.24-SNAPSHOT`.
- Focused AI tests passed with 8 tests, the full 41-module Maven package passed, and scoped frontend ESLint passed.
- Full `tsc -b` remains blocked by previously recorded unrelated files; direct Vite production build succeeded and refreshed the Release UI assets.
- Deployed `1.0.24-260714-RELEASE`; live DeepSeek generation produced the new structured brief successfully and the page rendered the complete structure without browser-visible layout failure.
- Found old pending daily-brief hashes in the live queue and added stale-task suppression to avoid repeated AI calls and duplicate briefs.
- Advanced Maven/UI to `1.0.25-SNAPSHOT`, passed 9 focused tests and the full Maven package, then deployed `1.0.25-260714-RELEASE`.
- The first stale-task runtime smoke inserted its row but failed to parse the combined psql output; follow-up verification will identify it by the unique smoke hash.
- Verified the stale task completed with zero provider executions after its creation; older execution logs reused the same numeric task ID and were excluded by timestamp.
- A Playwright CLI desktop screenshot confirmed the complete brief renders correctly. An additional inline console audit was skipped because Playwright is CLI-available but not locally importable as a Node package.
- Final Release/API verification passed: brief row `v2|v3|deepseek|deepseek-v4-flash|SUCCEEDED`, all services `UP`, and all images use `1.0.25-260714-RELEASE`.
- Removed the stale-task smoke row after confirming zero provider calls.
- Completed Phase 50.

## 2026-07-14 - Market Event value and analysis v3

- Started Phase 51 and restored planning/design context.
- Audited live scoring, current event analyses, full-text coverage, event Sheet rendering, and project design-system constraints.
- Confirmed the low-value output is a combined scoring, content, protocol, and presentation problem.
- Verified a current CNInfo regulatory PDF extracts successfully in Data Executor with 20,894 characters; the missing event content is a backfill/retry gap.
- Measured current event-member content coverage by source and confirmed CNInfo regulation is the largest recoverable full-text gap, while EastMoney 7x24 mostly has no separate article content.
- Logged two harmless inspection-command errors: one referenced the nonexistent `information-table.tsx` instead of `market-information-table.tsx`; another passed an invalid PowerShell/ripgrep glob order. Both were corrected in the next inspection and changed no files.
- Replaced source/category-only scoring with evidence quality, market breadth, actionable policy/company facts, cross-source confirmation, recency, and explicit routine-announcement penalties.
- Added regression cases proving CNInfo abnormal-volatility notices remain low value while a full-text material order can become high value.
- Added bounded full-text retries with a six-hour cooldown, existing-full-text reuse, prioritized recent backfill, and source-job-specific limits.
- Upgraded event analysis to schema v2 / prompt v3 with fact/delta/impact/transmission/expectation/scenario/watch fields while preserving exact evidence validation.
- Rebuilt the event Sheet around the actual impact-analysis flow and added safe polling while an event task is pending or running.
- Made Market reads and automatic queueing current-protocol aware so old v1 analysis rows no longer masquerade as current v2 results.
- Focused test setup hit three command-only issues without code impact: PowerShell required quoting Maven `-D` arguments containing commas/dots, local Python required `PYTHONPATH=src`, and one diagnostic read used the Data workdir for an AI path. The corrected Python run passed 6 tests.
- The first AI test run exposed a brittle prompt assertion split by text-block wrapping; changed it to assert the stable phrase rather than whitespace layout.
- The first scoring test showed that a single-source material order stopped at 4.9 despite full text and quantified significance. Added a separate evidence-backed materiality signal bonus (`重大`, revenue share, amount, first/record language) rather than weakening the high-value threshold.
- Focused verification now passes: 6 Python extraction/pipeline tests, 9 AI worker tests, 7 cluster scoring tests, scoped Market Information ESLint, and Market infrastructure/application compilation.
- Applied the v2/v3 protocol and bounded-backfill migration twice successfully; both AI bindings and both scheduler job defaults now expose the intended metadata.
- Advanced all Maven modules and UI together to `1.0.26-SNAPSHOT`.
- Full 41-module Maven package passed. Full UI type-check remains blocked by the already recorded unrelated Market Pulse, Industry Application, Market Sentiment, and stock-overview errors; scoped ESLint and direct Vite production build pass.
- The normal Data Executor Dockerfile rebuild stalled in its large dependency layer while the old container stayed healthy. Rebuilt an equivalent source-overlay image from the existing dependency-complete image, then recreated only Data Executor and verified health.
- The first forced full-text backfill selected 30 CNInfo/SZSE documents and exposed a real PDF edge case: selectable text may contain NUL bytes rejected by PostgreSQL. Added extraction-level NUL sanitization and a regression test; previously persisted rows remain valid because each document writes in its own transaction.
- Retried the backfill after sanitization: 30/30 rows persisted, 27 as full text and 3 as explicit summary-only fallback.
- First live scoring smoke on `1.0.26-260714-RELEASE` showed two inquiry-reply title variants still passed because the penalty matched only the contiguous phrase `问询函回复`; generalized it to any inquiry title containing a reply or verification opinion.
- Added named-document clustering so different headlines referencing the same `《政策/规划》` merge into one event instead of occupying several Key Event slots.
- Added two regression tests; all 9 cluster tests pass. Advanced Maven/UI together to `1.0.27-SNAPSHOT` for the corrected immutable Release.
- Live v2/v3 DeepSeek analyses now contain information delta, transmission paths, scenarios, watch items, and full/mixed content quality; they are no longer summary-only outputs.
- A second live clustering smoke found official policy pages and media wires for the same named document stayed separate solely because their raw categories differed. Allowed exact named-document matches to bridge `Market Wire` into the specific official category and promote the final cluster category.
- Added the cross-category named-policy regression; all 10 cluster tests pass. Advanced Maven/UI together to `1.0.28-SNAPSHOT` for the final corrected Release.
- Built and deployed immutable `1.0.28-260714-RELEASE`; all application health endpoints and UI responded successfully.
- Live final verification: active named-policy cluster is `Policy` with three sources/37 members and priority 9.5; routine inquiry/abnormal-volatility rows max at 0.9 and are not high value; current v2/v3 DeepSeek event analysis has full-text quality, three transmission paths, four watch items, and three exact evidence items.
- Refreshed the runtime Market Brief after the final pipeline pass. Its key events now prioritize national policies, regulatory notices with material consequences, and material company earnings rather than routine filings.
- Captured final desktop screenshot at `artifacts/market-information-v4.png`; the Market Brief and unified feed render without visible layout failure.

## 2026-07-15 - Langfuse observability

- Started Phase 52 after confirming `ai_execution_log` has per-call token fields but no external usage/cost dashboard.
- Confirmed `AgentExecutor.completeDetailed` is the shared completion boundary for current AI features.
- Selected the Langfuse public ingestion API with a fail-open Java HTTP client and content capture disabled by default.
- Added `LangfuseGenerationReporter` and centralized success/failure instrumentation in `AgentExecutor`.
- Added self-hosted Langfuse v3 Compose, isolated persistent services, `.env.example`, and setup documentation under `deploy/observability/langfuse`.
- Added `CYNEXUS_AI_LANGFUSE_*` configuration to AI and Release Compose; content capture remains disabled by default.
- Added two focused reporter tests covering authenticated ingestion payloads, usage fields, disabled mode, and fail-open behavior.
- Focused Maven test passed and the Langfuse Compose configuration validated successfully.
- Synchronized Maven/UI development versions to `1.0.46-SNAPSHOT`; Release was not deployed because Langfuse keys/pricing still require configuration.
- Full UI ESLint was attempted under `1.0.46-SNAPSHOT` and remains blocked by the repository's existing unrelated 98-error baseline; no Langfuse UI files are involved.
- Propagated AI capability names through `AgentExecutor` so Langfuse groups calls by business operation instead of a generic completion name.
- Re-ran the focused reporter test and AI application compile under `1.0.46-SNAPSHOT`; both passed.
- Built the complete `cynexus-ai-starter` reactor package under `1.0.46-SNAPSHOT` successfully.
- Added start/stop PowerShell entrypoints for the isolated Langfuse stack and revalidated its Compose configuration after credential placeholder corrections.
- Generated ignored local Langfuse credentials, started the six-container stack, fixed shared-network hostname collisions, and verified `http://localhost:33000` plus Langfuse health return HTTP 200.
- Langfuse initialization created one Cynexus AI project and one admin user. Root `.env` now has the client public/secret keys; existing Java Dev processes were intentionally left untouched and need a normal environment restart to load them.
- User chose a lighter native AI usage/cost view. Stopped and removed the Langfuse containers, network, volumes, deployment files, credentials, reporter code, runtime configuration, and capability instrumentation; Cynexus shared infrastructure stayed healthy.
- Removed the Langfuse images and synchronized Maven/UI versions to `1.0.47-SNAPSHOT`; the AI Starter package passed after removal.

## 2026-07-15 - Shared-resource Langfuse API usage view

- Added optional shared-resource Langfuse Web/Worker + MinIO Compose, isolated PostgreSQL schema, ClickHouse database, Redis DB 4, and local start/stop scripts.
- Added backend Langfuse observation proxy/aggregation at `/api/ai/usage`; credentials remain server-side and ingestion is fail-open.
- Added native Settings → AI Usage UI with date range, KPI cards, provider/model/capability breakdown and recent calls.
- Advanced Maven/UI versions together to `1.0.48-SNAPSHOT`.
- Maven AI compile, scoped frontend ESLint, and Compose config validation passed.
- AI application focused tests passed: 10 tests.
- Full UI type-check still reports only existing unrelated module errors; Docker daemon was unavailable, so runtime startup was not executed.
- PowerShell parser audit initially hit the usual `$file:` interpolation issue in the diagnostic command; rerun with `${file}` passed for both scripts.
- Initial Langfuse query used a limit above the v3 public API maximum of 100 and Java HTTP negotiation intermittently failed against the local Next.js endpoint; capped the request at 100 and forced HTTP/1.1.
- Application Analysis refresh failures were traced to Dev Nacos-disabled Feign discovery; pinned the AI Market Feign client to `CYNEXUS_AI_MARKET_BASE_URL` and configured Dev to use `http://127.0.0.1:28082`.
- AI Provider capability bindings now persist automatically after a provider/model selection change (debounced) and after model text input loses focus; the manual binding Save button was removed.

## 2026-07-15 - DDD business logic relocation

- Read the repository operating rules and restored the existing file-based planning context.
- Confirmed the worktree contains extensive pre-existing changes and preserved them.
- Started Phase 54 in the requested order: Knowledge Base, Market, Core, AI.
- Moved Knowledge Base indexing chunk construction, hashing, token estimation, and task creation into `knowledgebase-domain`.
- Removed the deleted application index service and moved its runtime dependency to the starter module.
- Moved Market stock-information content/AI analysis workflow behind Domain Ports and `StockInformationContentDomainService`; Infra HTTP clients now implement those ports.
- Moved Core scheduler timing decisions into `SchedulerTimingDomainService`.
- Moved AI application-analysis model routing into `ApplicationAnalysisRoutingDomainService` and AI completion/observability behind `AiCompletionGateway`.
- Attempted a version bump with an unquoted Maven property, which temporarily wrote version `1`; reran the command with quoted properties and restored all Maven modules to `1.0.78-SNAPSHOT`.
- Added the DDD discipline to both `AGENTS.md` and the requested root `AGENT.md`.
- Moved AI trace/tool dependencies behind Domain Ports and moved Core/AI runtime implementations to starter/Infra assembly.
- Focused tests passed and the non-clean full Maven package passed all 41 projects at `1.0.78-SNAPSHOT`; clean packaging remains blocked only by the running old Core Starter JAR lock.
- Extracted AI cross-service market access into the `MarketDataGateway` API port and removed all four application-controller modules' direct Infra dependencies.
- Re-ran AI tests: 10 passed. Re-ran the full non-clean Maven package after the final port change: all 41 projects passed.
- Final `git diff --check` is clean; version and dependency audits are aligned with the DDD rules.

## 2026-07-15 - DDD follow-up audit

- User reported that the first DDD migration was incomplete because application services still contain substantial business logic.
- Started Phase 55 and inventoried remaining application services in the requested module order.
- Confirmed the largest remaining violations are in Market query aggregation, Core scheduler/media workflows, and AI information-analysis/document-generation/recent30/K-line workflows.
## 2026-07-16 Domain Package Audit

- Reorganized domain code by bounded feature instead of keeping unrelated policies and ports in `domain/service`.
- Verified package/path consistency, focused tests, and the full Maven reactor.
## 2026-07-18 - Fund-flow 120 bootstrap option

- Confirmed active-stock 120-day fund-flow coverage is incomplete: 1048/5530 active stocks have at least 120 rows; 4482 still need bootstrap.
- Added `missing-120` stock scope to the history chain so a bootstrap run targets only active stocks with fewer than 120 distinct fund-flow history dates.
- Added scheduler job `a_stock_fund_flow_history_bootstrap_120` as a manual/no-schedule 120-day bootstrap option. It targets `stock_scope=missing-120`, `fund_flow_count=120`, `minimum_history_rows=120`, `shard_size=300`, workers=6, easy_tdx primary.
- Added Core mapping for the bootstrap scheduler job to the existing `a_stock_fund_flow_history_backfill` Python job and included it in the background polling path so scheduler progress can update while it runs.
- Applied migration `20260718_add_stock_fund_flow_120_bootstrap_job.sql`; scheduler API shows the job disabled with `{}` schedule and default params present.
- Synced data executor files and restarted `cynexus-data-job-executor`; Dev restarted via documented scripts.
- Verification passed: Python fund-flow job tests 8 passed; Maven package passed; Dev Core health UP; Data Executor healthy.

## 2026-07-18 - Market ETF Flow Workspace

- Started the ETF market-flow integration requested by the user.
- Read project layering rules, product/design register, existing Market Pulse/Sentiment routes, Python job registry/trigger API, PostgreSQL table conventions, and current TDX adapters.
- Confirmed free live source coverage through AkShare/EastMoney, SSE/SZSE share data, eltdx ETF quotes, and easy_tdx ETF/security APIs.
- Implementation is in progress; verification results will be appended after the Python, Java, schema, and UI layers are complete.
- Added the Python ETF catalog/spot/daily-share pipeline, AkShare share-source adapters, PostgreSQL tables, and `sync_market_etf` scheduler registration.
- Added the Market DDD read path and `GET /api/market/etf`, including direction, breadth, rotation, premium/IOPV, activity, and share-flow proxy fields.
- Added `/market/etf` with colocated service/components, loading/error states, trend chart, breadth panel, rotation table, broad ETF table, and sector ETF table.
- Synchronized Maven/UI development versions to `1.1.36-SNAPSHOT`; added the deployable ETF schema migration and corrected strict sector filtering/date-window SQL.
- Verification: 10 focused Python tests passed, ETF frontend ESLint passed, Market Starter compilation passed, and direct Vite production build passed. Full UI type-check remains blocked by the existing unrelated errors recorded above. Docker/runtime smoke is pending because Docker Desktop is currently unavailable.
- Runtime follow-up: started Docker shared infrastructure and Dev, applied ETF schema/scheduler migrations, rebuilt Data Executor, fixed non-finite AkShare JSON and source trade-date inference, and completed a live ETF sync with 1549 rows.
- Final runtime verification: Core, Market, Gateway, UI, and Data Executor are reachable; `GET /api/market/etf` returns `ok=true`, trade date `2026-07-17`, 924 full-scope ETFs, 24 rotations, and bounded broad/sector detail lists. Scheduler job `market_etf_daily` is registered for 17:35 on trading days.

## 2026-07-18 - ETF classification correction

- Audited the existing name-keyword mapping and live PostgreSQL catalog.
- Confirmed 625 of 1,549 active ETFs are `other` and excluded from the page; the current sector bucket also contains non-sector asset classes.
- Began Phase 58 to add code-keyed, source-aware ETF classification and coverage diagnostics.
- Added optional source-aware classification enrichment, expanded fallback rules, live reclassification, stable ETF IDs, and coverage counters. Python tests, Java package, scoped ESLint, live API, Data Executor rebuild, and Dev restart completed successfully.
- Full UI build was rechecked at `1.1.41-SNAPSHOT`; it remains blocked only by the existing unrelated TypeScript errors outside the ETF page/service.
- Follow-up audit found 378 `other` rows; 136 are intentional non-target products, while 242 need a second mapping pass, including several broad-index and domestic theme families.

## 2026-07-18 - ETF mapping registry

- Created and applied the many-to-many mapping DDL and 6,213-row seed for all 1,549 ETF codes.
- Added runtime mapping persistence to the ETF sync pipeline and rebuilt the Data Executor.
- Seed replay is idempotent; mapping coverage and group counts were verified in PostgreSQL.

## 2026-07-19 - AI bookkeeping workspace

- Started Phase 60 after reviewing the repository DDD rules, core schema/runtime, UI routing/service conventions, existing upload patterns, and current AI provider limitations.
- Mainstream product research and the review-first receipt workflow are recorded in `findings.md`; implementation is now organized around core-owned ledger aggregates and a multimodal AI port.

## 2026-07-19 - AI bookkeeping implementation complete

- Applied the finance ledger migration to the live Dev PostgreSQL instance and seeded 13 default categories.
- Implemented the core DDD/application/infra workflow for accounts, categories, income/expense/transfer entries, overview statistics, archiving, and AI capture review/confirmation.
- Added single-image upload UI and vision execution plumbing with automatic posting for complete items plus an explicit review exception flow.
- Added Finance navigation and routes: `/finance/ledger`, `/finance/accounts`, `/finance/ai-capture`.
- Scoped finance UI ESLint passed; Maven core/AI compilation passed; the full UI type-check still reports only existing unrelated errors.
- Restarted Dev through `deploy/dev/start-dev.ps1`; live Gateway/Core/AI/Market/UI ports are listening. Manual API smoke test confirmed account balance and period/category totals.

## 2026-07-19 - Finance overview redirected to consumption analysis

- Removed the manual quick-entry form from the Finance overview workflow.
- Added daily consumption analysis with average spend, peak spend day, income-day count, and daily expense bars.
- Promoted AI image ingestion as the primary action and relabeled the main route as 消费分析.
- Kept the existing multipart image API, draft review, confirmation, account, category, and archive capabilities unchanged.
- Changed `POST /api/core/finance/ai-captures` default mode to `auto`; complete parsed items are posted immediately and returned with `transactionId`/`confirmed` status.
- Added a separate `/review` endpoint for the web exception flow; the Shortcut endpoint accepts only one `image` file and an optional `Idempotency-Key` header.
- Added idempotency persistence under `cynexus_appl_core.finance_ai_captures` and a migration for existing databases.
- Automatic parsing falls back to current time for an absent receipt date and the seeded other-income/other-expense category; items still missing an amount or asset account remain visible as review items.
- AI parsing now receives the current account/category candidate lists and returns candidate IDs plus hints; Core validates and maps those IDs before automatic posting.

## 2026-07-19 - Credit accounts and installments

- Started Phase 61 to replace the liability vocabulary with credit, add Huabei/Baitiao/Douyin Monthly Pay/credit-card account types, and model installment schedules separately from consumption transactions.
- Added live schema migration, domain schedule policy, plan/payment API, credit account UI, AI installment extraction, automatic plan creation after a credit purchase, and repayment UI.
- Updated all Maven/UI versions to `1.1.49-SNAPSHOT`; full Maven package and Finance ESLint passed. Live API plan creation and first-installment payment passed with temporary test records fully removed.
- Full UI production build remains blocked by existing unrelated TypeScript errors in Market and easy-tdx files.

## 2026-07-19 - Finance ledger management

- Started Phase 62 after the user requested a dedicated ledger list, single-entry edit/delete, and more complete categories.
- Initial inspection found backend bounded listing/manual save capabilities, but no dedicated transaction-management route or category-management controls in the UI.

## 2026-07-19 - Finance ledger management complete

- Added `GET /api/core/finance/transactions/{id}`, `PUT /api/core/finance/transactions/{id}`, and `POST /api/core/finance/transactions/{id}/delete`; existing DELETE archive behavior remains available.
- Added custom category update/archive support and a 43-row category expansion migration; live active category coverage is now 56 rows (44 expense, 12 income).
- Added `/finance/transactions`, Finance navigation, service methods, filters, pagination, edit dialog, archive/delete actions, and category archive controls.
- Core Maven package passed at `1.1.58-SNAPSHOT`; scoped Finance ESLint and `npx vite build` passed. Full UI `npm run build` still reports only existing unrelated TypeScript errors.
- Applied the category migration, rebuilt/restarted Release `1.1.58-260719-RELEASE`, and verified live transaction CRUD plus installment deletion protection. Temporary smoke transaction was soft-deleted and no longer appears in queries.

## 2026-07-19 - Installment deletion follow-up

- User reported that installment purchases cannot be deleted; live inspection confirmed the existing plan contains three installments, including one paid row.
- Started Phase 63 to support explicit cascade deletion while preserving accounting consistency.

## 2026-07-19 - Installment deletion follow-up complete

- Changed purchase deletion to cascade soft-delete the purchase, installment plan, installment rows, and linked repayment transactions; direct repayment deletion remains blocked.
- Verified with a temporary credit purchase and two-installment plan; delete succeeded and no active installment rows remained.
- Synchronized Maven/UI to `1.1.59-SNAPSHOT`, rebuilt the UI/Java artifacts, and restarted Release `1.1.59-260719-RELEASE`.

## 2026-07-19 - Repayment reversal

- User clarified that deleting a repayment should revoke the payment and return the installment to its previous pending state.
- Implemented repayment-specific soft deletion: the payment transaction is removed from active ledger views, the linked installment returns to `pending` with its payment link cleared, and the installment plan reopens as `active`.
- Full Maven package passed at `1.1.60-SNAPSHOT`; Release rebuilt and restarted as `1.1.60-260719-RELEASE`.
- Live Release smoke passed with temporary data: repayment deletion returned `true`, restored installment 1 to `pending`, cleared `paymentTransactionId`, reopened the plan, and purchase cascade cleanup returned `true`.

## 2026-07-19 - Finance Transactions table alignment

- Matched the dedicated Transactions table to the existing Finance table style: consistent container/header/row spacing, separators, hover state, direction icons, and icon-only archive action.
- Kept edit and destructive delete actions available with accessible labels and native tooltips.
- Synchronized Maven/UI to `1.1.61-SNAPSHOT`; Finance ESLint, Vite build, full Maven package, Release rebuild, and UI route smoke passed.

## 2026-07-19 - Finance transaction pagination and detail drawer

- Kept Transactions on a bounded 25-row page view with visible page position and navigation controls.
- Made each transaction row selectable with mouse or keyboard and added a bottom Drawer for full transaction details.
- Moved edit, archive, and delete actions into the detail Drawer; deletion and archive close the detail state after success.
- Synchronized Maven/UI to `1.1.62-SNAPSHOT`; Finance ESLint, Vite build, full Maven package, Release rebuild, and UI route smoke passed.

## 2026-07-19 - Finance table style convergence

- Matched Transactions to the existing Finance transaction table dimensions and spacing: `min-w-[680px]`, fixed header rhythm, `px-4 py-3` rows, and the standard empty-state density.
- Removed page-specific width and spacing differences while preserving paginated rows, keyboard selection, and the bottom detail Drawer.
- Synchronized Maven/UI to `1.1.63-SNAPSHOT`; Finance ESLint, Vite build, full Maven package, and Release rebuild passed.

## 2026-07-19 - Settings table and inline Drawer editing

- Replaced the hand-written Transactions table with the shared Settings `Table`, `TableHeader`, `TableHead`, `TableBody`, `TableCell`, and `TableRow` components.
- Single-click continues to show detail; double-click now enters edit mode directly in the bottom Drawer.
- Removed the edit Modal; the Drawer now contains the edit form with inline save/cancel actions.
- Synchronized Maven/UI to `1.1.64-SNAPSHOT`; Finance ESLint, Vite build, full Maven package, and Release rebuild passed.

## 2026-07-19 - Transaction interaction correction

- Changed the table to the Settings/DataTable pagination pattern with first, previous, next, and last page controls, always visible with disabled states.
- Added delayed single-click handling so double-click reliably enters inline edit mode before the Drawer captures the second click.
- Removed the Drawer Edit button; double-clicking a row or the detail content now enters edit mode directly.
- Synchronized Maven/UI to `1.1.65-SNAPSHOT`; Finance ESLint, Vite build, full Maven package, and Release rebuild passed.

## 2026-07-19 - Transaction filter layout

- Replaced the compressed horizontal filter flexbox with a responsive six-column desktop grid and two-column mobile layout.
- Standardized label placement above controls and stabilized widths for dates, selects, and search.
- Synchronized Maven/UI to `1.1.66-SNAPSHOT`; Finance ESLint, Vite build, full Maven package, and Release rebuild passed.

## 2026-07-19 - Full-height Transactions workspace

- Switched Transactions to a non-scrolling full-height workspace so the list fills the remaining viewport after header and filters.
- Made the table area independently scrollable with a sticky header and fixed pagination footer.
- Added the shared Select-based page-size control for 10, 20, 30, and 50 rows per page.
- Synchronized Maven/UI to `1.1.67-SNAPSHOT`; Finance ESLint, Vite build, full Maven package, and Release rebuild passed.

## 2026-07-19 - Finance Overview workspace

- Started Phase 72 after the user requested a dedicated Finance Overview with a GitHub-style daily activity graph and analysis panels.
- Confirmed the existing overview API has the required totals and sparse daily amount rows, but not daily activity counts; backend/API extension is required for accurate heatmap intensity.

## 2026-07-19 - Finance Overview workspace complete

- Added activityCount to the Finance Overview daily response and built the new /finance/overview page.
- Added a rolling-period selector, GitHub-style daily activity heatmap, cash-flow trend, expense structure, financial signals, account allocation, and loading/error/empty states.
- Added Overview to Finance navigation and kept the existing Ledger page as the month-scoped consumption-analysis surface.
- Synchronized Maven/UI to 1.1.69-SNAPSHOT; Finance ESLint, Vite build, full Maven package, Release rebuild, UI HTTP 200, and API activityCount smoke checks passed.

## 2026-07-19 - Finance Overview panel polish

- Started Phase 73 after screenshot review identified weak hierarchy and excessive empty space in the Finance Overview signal and account panels.

## 2026-07-19 - Finance Overview panel polish complete

- Replaced the stretched four-row signal list with a cash-health lead block and compact supporting signals.
- Grouped account balances into asset and credit sections, added asset/credit totals, and rendered all active accounts with distinct semantic styling.
- Prevented the lower Overview grid from equal-height stretching.
- Focused Finance ESLint and Vite build passed; Release UI rebuilt and /finance/overview returned HTTP 200.

## 2026-07-19 - Finance chart composition polish

- Started Phase 74 after screenshot review found the cash-flow and expense-structure modules visually empty and stretched.

## 2026-07-19 - Finance chart composition polish complete

- Reworked cash-flow rendering around active dates, period totals, and a compact chart height.
- Added ranked expense rows with amount/share hierarchy and stopped the chart row from equal-height stretching.
- Focused Finance ESLint and Vite build passed; Release UI rebuilt and /finance/overview returned HTTP 200.

## 2026-07-19 - Finance expense treemap

- Started Phase 75 after the user requested that expense structure use the existing POC heatmap/Treemap pattern.

## 2026-07-19 - Finance expense treemap complete

- Replaced the expense structure list with a POC-style ECharts Treemap using amount area and concentration color.
- Added tooltip details, responsive resize handling, and no-data rendering.
- Focused Finance ESLint and Vite build passed; Release UI rebuilt and /finance/overview returned HTTP 200.

## 2026-07-19 - Finance Overview period controls

- Started Phase 76 after the user requested a 5:5 cash-flow/expense layout, current-month default statistics, and date-picker controls.
- Added current-month and custom-range handling, start/end date pickers, and kept finance calendars usable on weekends.

## 2026-07-19 - Finance Overview period controls complete

- Cash Flow and Expense Structure now use equal 5:5 desktop columns, including the loading state.
- Overview defaults to the current month, keeps rolling shortcuts, and provides connected start/end date pickers with a custom-range state.
- Finance Overview and DatePicker ESLint passed; direct Vite build and full Maven package passed at `1.1.70-SNAPSHOT`.
- Release rebuilt and restarted as `1.1.70-260719-RELEASE`; UI and overview API smoke checks returned HTTP 200.

## 2026-07-19 - Finance account card workspace

- Started Phase 77 after the user requested block-style asset accounts with click detail and double-click editing.
- Replaced the Overview account rows with responsive asset/credit cards, credit utilization bars, and a responsive detail/edit Drawer using the existing account save endpoint.

## 2026-07-19 - Finance account card workspace complete

- Account Snapshot now renders asset and credit accounts as responsive blocks with semantic balance styling and credit-limit utilization.
- Added single-click detail Drawer, desktop right/mobile bottom direction switching, and double-click inline editing without a second modal.
- Account Snapshot ESLint, direct Vite build, full Maven package, Release rebuild, and UI/accounts/overview API smoke checks passed at `1.1.71-260719-RELEASE`.

## 2026-07-19 - Finance activity pulse range rendering

- Started Phase 78 after the user reported that the Daily Activity Pulse display was incorrect.
- Found the heatmap still allocated 53 weeks after the Overview default changed to the current month, and month labels depended on active data rows, so sparse periods could show an oversized blank grid with no month label.
- Changed the heatmap to size its weeks from the selected date range and derive month labels from calendar dates.

## 2026-07-19 - Finance activity pulse range rendering complete

- Fixed the current-month Daily Activity Pulse layout: it now renders a compact range-sized calendar and retains month labels even when the range is sparse.
- Daily Activity Heatmap ESLint, direct Vite build, full Maven package, Release rebuild, and Overview UI/API smoke checks passed at `1.1.72-260719-RELEASE`.

## 2026-07-19 - Finance activity pulse independent range

- Started Phase 79 after the user clarified that Daily Activity Pulse must not follow the selected statistics period.
- Split the pulse request from the selected-period overview request and fixed it to a trailing 12-month range with its own loading state.

## 2026-07-19 - Finance activity pulse independent range complete

- Daily Activity Pulse is now independent from the selected statistics period and always shows the trailing 12 months.
- Added a separate pulse loading state and explicit 12-month scope label.
- Overview/Daily Activity ESLint, direct Vite build, full Maven package, Release rebuild, and UI/API smoke checks passed at `1.1.73-260719-RELEASE`.

## 2026-07-19 - Finance activity pulse full width

- Started Phase 80 after the user reported that the Daily Activity Pulse left a large blank area on the right.
- Changed the calendar surface from minimum-width sizing to `w-full`, so it fills the card while retaining the mobile minimum width.

## 2026-07-19 - Finance activity pulse full width complete

- Daily Activity Pulse now fills the available desktop card width instead of leaving a large right-side gap.
- Daily Activity ESLint, direct Vite build, full Maven package, Release rebuild, and UI/API smoke checks passed at `1.1.74-260719-RELEASE`.

## 2026-07-19 - Finance account three column grid

- Started Phase 81 after the user requested three account cards per row.
- Updated the account card grid to use three columns at large desktop widths and two columns at smaller widths.

## 2026-07-19 - Finance account three column grid complete

- Account cards now render three per row on large screens as requested.
- Account Snapshot ESLint, direct Vite build, full Maven package, Release rebuild, and Overview UI/API smoke checks passed at `1.1.75-260719-RELEASE`.

## 2026-07-19 - Finance signal expansion

- Started Phase 82 after the user asked whether Finance Signals could contain more useful content.
- Added credit-limit usage, bookkeeping coverage, and asset-concentration signals using the existing overview contract, with responsive compact signal rows.

## 2026-07-19 - Finance signal expansion complete

- Finance Signals now includes six compact secondary signals: busiest day, expense concentration, credit debt share, credit-limit usage, bookkeeping coverage, and asset concentration.
- Finance Signal ESLint, direct Vite build, full Maven package, Release rebuild, and Overview UI/API smoke checks passed at `1.1.76-260719-RELEASE`.

## 2026-07-19 - Finance signal detail expansion

- Started Phase 83 after the user approved adding more financial signal details.
- Added average daily expense, single-day expense peak, and asset spending-buffer estimates using the selected period's existing daily and account data.

## 2026-07-20 - Finance dual-platform adaptation

- Adapted Finance navigation, period controls, overview charts, ledger analysis, account management, AI capture, and transactions for narrow touch screens and desktop layouts.
- Added mobile transaction cards with preserved pagination and responsive account/transaction detail drawers.
- Finance ESLint and scoped TypeScript checks passed. Full UI build remains blocked by pre-existing Market/easy-tdx/industry errors.
- Synchronized Maven and UI versions to 1.1.78-SNAPSHOT.

## 2026-07-20 - Finance activity heatmap bug fix

- Restored the missing week index in Daily Activity Heatmap week-grid generation.
- Finance ESLint and Vite production build passed; Release rebuilt as 1.1.79-260720-RELEASE.
- Finance Overview and its API both returned HTTP 200 after restart.

## 2026-07-20 - Finance transaction edit correction

- Fixed transaction editing so changing income/expense direction recalculates the category options and clears incompatible category selections.
- Expanded the desktop transaction Drawer to an explicit right-side full-height panel while keeping the mobile bottom sheet.
- Finance ESLint, Vite build, Maven package, and Release smoke checks passed.
- Release rebuilt as 1.1.80-260720-RELEASE; Transactions page and Finance API returned HTTP 200.

## 2026-07-19 - Finance signal detail expansion complete

- Finance Signals now includes daily average expense, single-day expense peak, and asset spending buffer in addition to the previous six signals.
- Finance Signal ESLint, direct Vite build, full Maven package, Release rebuild, and Overview UI/API smoke checks passed at `1.1.77-260719-RELEASE`.

## 2026-07-20 - AI capture history workflow

- Inspected existing capture persistence, parser port, ledger update path, and Finance drawer conventions.
- Started the history list, retry, edit/post recovery, early-failure persistence, responsive UI, and version synchronization work.

## 2026-07-20 - AI capture history workflow complete

- Added paginated `GET /api/core/finance/ai-captures`, stored-image retry, and update-safe confirmation for review, failed, and confirmed captures.
- Added capture processing mode persistence and ensured invalid/empty uploads return a failed history record instead of disappearing.
- Added Finance AI Capture history cards and responsive detail Drawer with edit, post, retry, and archive actions.
- Applied the history migration to PostgreSQL and redeployed Release as `1.1.81-260720-RELEASE`.
- Core compile, full Maven package, Finance ESLint, scoped TypeScript, Vite build, and HTTP smoke checks passed. Full UI `pnpm build` remains blocked by unrelated existing Market/easy-tdx/industry TypeScript errors.

## 2026-07-20 - AI capture Drawer height correction

- Found the generic AI Capture `max-height` was also constraining the desktop right-side Drawer.
- Changed the constraint to mobile bottom direction only; desktop right direction now uses full height with no max-height.

## 2026-07-20 - AI capture Drawer height correction complete

- Rebuilt and redeployed Release as `1.1.82-260720-RELEASE`.
- Verified Core health `UP`, Finance AI Capture HTTP `200`, and the served UI bundle contains both the desktop full-height and mobile bounded-height rules.

## 2026-07-20 - Shortcut media process workflow

- Added persistent `media_transcription_process_history` storage, domain port/aggregate, Core process API, and scheduled recovery dispatcher.
- The process accepts one URL, resolves it through the existing Downloader API, prefers the resolved audio URL, polls data transcription, runs existing AI polish/summary/metadata capabilities, and writes the existing MP4 History record automatically.
- Added responsive `/mp4-to-word/process-history` UI with progress, phase, failure detail, polling, and retry.
- Applied the migration, synchronized versions to `1.1.83-SNAPSHOT`, rebuilt Core, and restarted DEV.
- Core Maven package and focused UI ESLint passed. Full UI TypeScript remains blocked by existing Market/easy-tdx/industry errors.
- Smoke-tested queue, idempotency, failure persistence, retry, Core/Gateway APIs, and DEV health endpoints.

## Errors encountered

- Maven version plugin was first invoked without quoting the PowerShell `-DnewVersion` argument and temporarily wrote version `1`; reran it with quoted arguments and verified every Maven/UI version is `1.1.83-SNAPSHOT`.
- Core package initially failed because DEV held the executable JAR; stopped DEV, rebuilt, and restarted through the documented DEV entrypoint.

## 2026-07-20 - Release-only runtime

- Stopped DEV and deployed the latest `1.1.83-SNAPSHOT` artifacts as Docker Release `1.1.83-260720-RELEASE`.
- Rebuilt the UI static assets with Vite, rebuilt the Release UI image, and recreated only the Release UI container so `process-history` is included.
- Verified only Release application containers are running: UI `5173`, Gateway `18000`, Core `18080`, AI `18081`, Market `18082`.
- Release health, process-history page, and media process API returned HTTP 200.

## 2026-07-21 - Workspace AI Chat Entry

- Inspected the existing WorkspaceShell, global command palette, shadcn UI primitives, AI execution contract, and AI Provider capability registry.
- Started Phase 88 to add a right-side chat entry, MessageScroller composition, and a dedicated workspace chat execution capability.

## 2026-07-21 - Workspace AI Chat Entry complete

- Added `src/components/workspace-chat.tsx` and wired a chat icon beside search in `WorkspaceShell`.
- Added `src/components/ui/message-scroller.tsx` with bounded viewport scrolling, follow-latest behavior, stable message IDs, and a latest-message control.
- Added `src/services/ai/chat.ts` and registered `workspace_chat` in the AI Provider capability routing list.
- Added the backend `WorkspaceChatDomainPolicy`, plain-text execution branch, and scenario listing entry.
- Synchronized all Maven parent versions and UI package version to `1.1.96-SNAPSHOT`.
- Targeted UI ESLint, Vite production build, AI compile, and full Maven package passed. Full UI TypeScript remains blocked by unrelated pre-existing errors in Market/easy-tdx/industry.

## 2026-07-21 - Workspace AI Chat Release verification

- The old Release UI on port `5173` initially did not contain the new trigger.
- Rebuilt and restarted Release using the documented entrypoint as `1.1.96-260721-RELEASE`.
- Verified the served UI bundle contains `Open Cynexus AI`, all Release containers are up, and `POST /api/ai/executions` with `workspace_chat` returned HTTP 200.

## 2026-07-21 - Workspace AI Chat popup correction

- Replaced the full-height Sheet/Drawer presentation with a compact right-aligned Popover anchored to the Chat icon, matching the Message + MessageScroller sample interaction.
- Rebuilt only the versioned Release UI image and recreated the UI container.
- Verified the served bundle contains the Popover chat surface and no SheetContent marker; UI remains available at `http://localhost:5173`.

## 2026-07-21 - Workspace AI Chat shadcn base-rhea alignment

- Replaced the hand-rolled transcript state with the official `@shadcn/react/message-scroller` behavior wrapper, preserving message IDs, turn anchors, auto-follow, prepend position, visibility hooks, and animated jump controls.
- Aligned `MessageScroller`, `MessageScrollerViewport`, `MessageScrollerContent`, `MessageScrollerItem`, and `MessageScrollerButton` class composition with the official `base-rhea` registry source.
- Expanded `Bubble` to the official `default`, `secondary`, `muted`, `tinted`, `outline`, `ghost`, and `destructive` variants and added `BubbleGroup`/`BubbleReactions`.
- Added the official bottom scroll fade utility and moved transcript padding to the chat composition, matching the registry component boundary.
- Synchronized Maven and UI versions to `1.1.97-SNAPSHOT` for this functional correction.
- Targeted UI ESLint, full UI TypeScript, and Vite production build passed.
- Built `cynexus-ui:1.1.97-260721-RELEASE` and recreated only the Release UI container with `--no-deps`; `http://localhost:5173` serves the corrected bundle while backend Release services remain on the existing runtime image.
- Verified `POST http://localhost:18000/api/ai/executions` with `scenarioCode=workspace_chat` returned HTTP 200 and `SUCCEEDED`.

## 2026-07-21 - Workspace AI Chat official demo composition correction

- Replaced the initial assistant welcome bubble with the official `Empty` composition and centered empty state.
- Replaced thinking `Bubble` with the official `Message` + `Marker` status composition, keeping the spinner and status text outside any bubble surface.
- Rebuilt the header as the official `New Chat` / `How can I help you today?` layout with a reset conversation action.
- Added the official `InputGroup`, `Textarea`, and bottom addon composition with Plus and ArrowUp actions; removed the custom disclaimer and ordinary input styling.
- Synchronized Maven and UI versions to `1.1.98-SNAPSHOT`.

## 2026-07-21 - Workspace AI Chat composer width and overflow correction

- Fixed the block-end `InputGroup` flex cross-axis so the textarea fills the composer width instead of collapsing to min-content width and rendering one character per line.
- Fixed the composer height with a bounded textarea and enabled internal vertical scrolling for longer prompts.
- Rebuilt the `1.1.98-260721-RELEASE` UI image and recreated only the Release UI container.

## 2026-07-21 - Workspace AI Chat bubble and composer correction

- Set user messages to the shadcn `muted` Bubble variant and assistant messages to the transparent `ghost` variant.
- Removed avatars from ordinary Message rows.
- Stabilized the block-end InputGroup textarea as a full-width fixed-height control with internal scrolling and hidden scrollbar rendering.
- Fixed the InputGroup addon focus handler typing issue.
- Synchronized Maven and UI versions to `1.1.100-SNAPSHOT`; Release rebuild and smoke verification remain in progress.
- Full Maven `-DskipTests package` passed across 41 reactor projects; Release UI `1.1.100-260721-RELEASE` is serving at `http://localhost:5173` and the `workspace_chat` smoke request returned HTTP 200 / `SUCCEEDED`.

## 2026-07-21 - Workspace AI Chat response normalization

- The provider returned chat content as a JSON envelope (`{"response":"..."}`), so the UI was displaying the transport wrapper.
- Added a workspace-chat-only response normalizer that extracts the `response` string and leaves ordinary plain-text responses unchanged.
- Synchronized Maven and UI versions to `1.1.101-SNAPSHOT`; build and Release verification remain in progress.
- Direct Vite build, full Maven package across 41 reactor projects, Release UI verification, and AI execution smoke check passed. Release UI is running as `1.1.101-260721-RELEASE`.

## 2026-07-22 - Workspace AI Chat structured fallback handling

- Extended workspace chat normalization to extract `response`, `content`, `message`, or `text` fields and prevent raw JSON objects such as `{}` from appearing in the transcript.
- Synchronized Maven and UI versions to `1.1.102-SNAPSHOT`; rebuilt Release UI as `1.1.102-260721-RELEASE`.

## 2026-07-22 - Workspace AI Chat bottom-right sizing

- Moved the chat Popover to a fixed bottom-right viewport position with 24px offsets.
- Increased the chat surface to `min(78vh, 760px)` by `min(calc(100vw - 2rem), 560px)` while keeping it responsive on narrow screens.
- Synchronized Maven and UI versions to `1.1.103-SNAPSHOT` and rebuilt Release UI as `1.1.103-260722-RELEASE`.

## 2026-07-22 - Workspace AI Chat positioning enforcement

- Added an explicit important CSS rule for the Popover surface so Radix inline coordinates cannot move it back under the header.
- Kept fixed bottom-right placement with a mobile full-width inset fallback.
- Synchronized Maven and UI versions to `1.1.104-SNAPSHOT` and rebuilt Release UI as `1.1.104-260722-RELEASE`.

## 2026-07-22 - Workspace AI Chat anchored bottom-right Popover

- Replaced the CSS coordinate override with a Radix `PopoverAnchor` fixed at the viewport bottom-right and changed the content to open above that anchor.
- Synchronized Maven and UI versions to `1.1.105-SNAPSHOT` and rebuilt Release UI as `1.1.105-260722-RELEASE`.

## 2026-07-22 - Workspace AI Chat viewport panel

- Replaced the unreliable Popover anchor positioning with a full-viewport Popover shell and a fixed bottom-right visual panel inside it.
- Synchronized Maven and UI versions to `1.1.106-SNAPSHOT` and rebuilt Release UI as `1.1.106-260722-RELEASE`.

## 2026-07-22 - Workspace AI Chat flex viewport alignment

- Changed the visual panel to align through the full-viewport Popover shell's flex-end alignment, removing its dependence on fixed-position descendants.
- Synchronized Maven and UI versions to `1.1.107-SNAPSHOT` and rebuilt Release UI as `1.1.107-260722-RELEASE`.

## 2026-07-22 - Workspace AI Chat direct viewport portal

- Removed Radix Popover content positioning from the visual panel; the trigger now controls Popover state while the chat panel renders via a body portal with direct fixed right/bottom coordinates.
- Synchronized Maven and UI versions to `1.1.108-SNAPSHOT` and rebuilt Release UI as `1.1.108-260722-RELEASE`.

## 2026-07-22 - Autonomous AI Agent architecture

- Started Phase 99 for a model-directed Agent workflow inside `cynexus-ai`.
- Confirmed the current gap: `AgentExecutor` returns mock JSON when tool codes are present; `AiToolCatalog` exposes only strings; `MarketDataTools` has API gateways but no callable tool schemas/handlers; the provider adapter has no tool-call protocol; and no Agent run/step persistence exists.
- Added `docs/ai-agent-architecture.md` with the product workflow, competitor-informed UX, tool/input/output contracts, safety limits, persistence schema, API surface, delivery phases, and acceptance scenarios.

## 2026-07-22 - Autonomous AI Agent market tool foundation

- Added provider-neutral Agent tool contracts under `cynexus-ai-domain` for execution context, tool definitions, calls, results, catalog, executor, and handler boundaries.
- Added Spring-managed `AgentToolRegistry` with duplicate detection, unknown-tool rejection, and read-only/confirmation enforcement.
- Registered six model-callable Market data tools: overview, overview history, sector ranking, stock daily feature, stock intraday feature, and stock K-lines.
- Added required-argument and numeric-range validation at the Market tool boundary.
- Synchronized Maven and UI versions to `1.1.110-SNAPSHOT`.
- Targeted AI compile and four Agent registry/Market handler tests passed.

## 2026-07-22 - Autonomous AI Agent runtime, persistence, and UI

- Added provider-neutral Agent model-turn contracts and native OpenAI-compatible `tool_calls` / Responses API parsing with strict JSON argument handling.
- Extended the AI completion port and gateway so Agent orchestration remains independent of provider-specific payloads.
- Added bounded autonomous execution in `AutonomousAgentAppService`: 8 turns, 12 tool calls, 4 calls per turn, read-only tool enforcement, failure capture, and concise trace records.
- Added `ai_agent_conversation`, `ai_agent_run`, and `ai_agent_run_step` tables under `cynexus_appl_ai`, plus PO/basic-service/repository layers.
- Added Agent API: `POST /api/ai/agent/runs`, `GET /api/ai/agent/runs/{runNo}`, and `GET /api/ai/agent/tools`.
- Added `/ai/agent` UI with prompt composer, empty state, answer surface, tool trace, source dates, and available module catalog; global workspace chat now delegates to the Agent runtime.
- Registered `autonomous_agent` in the AI Provider capability routing list.
- Applied the Agent tables to the local `cynexus` database.
- Synchronized Maven and UI versions to `1.1.111-SNAPSHOT`.
- Targeted AI compile and four Agent tests passed; changed UI files pass ESLint. The full UI build remains blocked by pre-existing TypeScript errors in unrelated Market/POC files.

## 2026-07-22 - Autonomous Agent runtime verification

- Release Docker smoke test initially exposed a missing Jackson `ObjectMapper` bean for Market Agent handlers.
- Added `AgentJacksonConfig` in `cynexus-ai-infra` and synchronized Maven/UI versions to `1.1.112-SNAPSHOT`.
- Rebuilt the versioned AI image `cynexus-ai:1.1.112-260722-RELEASE` and recreated the Release AI container.
- Verified AI startup, actuator health, Agent tool catalog response, and all three Agent persistence tables in `cynexus_appl_ai`.
- The full Release UI image was not rebuilt because the existing repository-wide TypeScript build still has unrelated Market/POC errors.

## 2026-07-22 - Responses API tool transcript compatibility

- Preserved assistant function-call items when building Responses API follow-up turns, so tool outputs can be submitted with their original call IDs.
- Synchronized Maven and UI versions to `1.1.113-SNAPSHOT`.
- Targeted AI compile passed.

## 2026-07-22 - Agent provider protocol integration tests

- Added a local HTTP contract test for the OpenAI-compatible adapter using no real credential or external network.
- Verified native Chat Completions tool-call extraction, JSON argument parsing, schema transmission, Responses API function-call extraction, and token usage propagation.
- The first Maven invocation failed because PowerShell treated an unquoted `-D` property as a lifecycle phase; reran with quoted Maven properties and passed 2 tests.
- Synchronized Maven/UI versions to `1.1.116-SNAPSHOT`.

## 2026-07-22 - Agent binding priority repair

- Live smoke showed the previous deployed resolver could select an unrelated fallback binding (`auto`) because it used unordered `findFirst()` across capabilities.
- Changed model resolution to prefer `autonomous_agent`, then explicitly fall back to `workspace_chat`, then `text_summary`.
- Synchronized Maven/UI versions to `1.1.117-SNAPSHOT`; targeted AI compile is required before commit.

## 2026-07-22 - Persistent Agent conversation history

- Added `ai_agent_message` with ordered transcript persistence for user, assistant tool-call, tool result, and final answer messages.
- Added domain repository ports and infra MyBatis/basic-service/converter implementations for conversation history and owner-scoped message reads.
- Agent runs now restore the saved transcript when a conversation number is supplied, append the new user turn, and persist every model/tool message for later continuation.
- Added `GET /api/ai/agent/conversations` and `GET /api/ai/agent/conversations/{conversationNo}/messages`.
- Added Agent UI history list, reopen action, and follow-up requests on the selected conversation; targeted ESLint passed.
- Applied the message migration locally. Maven AI application compile passed.
- Synchronized Maven/UI versions to `1.1.118-SNAPSHOT`.

## 2026-07-22 - Agent stream, stop, and live status events

- Added a real SSE workflow at `POST /api/ai/agent/runs/stream`; events are emitted at run start, tool completion, answer, and terminal status boundaries.
- Added cancellation tokens and `POST /api/ai/agent/runs/{runNo}/cancel`; cancellation persists `CANCELLED` instead of being mislabeled as a provider failure.
- The Agent UI now consumes the stream, shows a stop action, and reloads the persisted final run after stream completion.
- Targeted AI compile and targeted Agent UI ESLint passed.
- Full token-level provider streaming remains intentionally out of scope for the current Responses/Chat adapter; the API exposes truthful workflow events rather than pretending a completed response is token-streamed.
- Synchronized Maven/UI versions to `1.1.119-SNAPSHOT`.

## 2026-07-22 - Agent capability packs and argument policy

- Added read-only Finance overview/transactions tools through a new Core Finance Feign contract.
- Added read-only Knowledge Base document retrieval through the existing Knowledge Base Feign contract.
- Added a typed `market.clickhouse_daily_bars` tool using an allow-listed `daily_raw` query with market/symbol/date/limit bounds; arbitrary SQL is not exposed.
- Extended `AgentToolRegistry` with schema-driven required/type/range/enum validation before handler execution.
- Added focused tests for capability handlers and invalid arguments. Initial Java-time serialization test failed with a bare mapper, was corrected with `JavaTimeModule`, and the final focused suite passed 7 tests.
- Updated the architecture document and capability-pack plan evidence.
- Synchronized Maven/UI versions to `1.1.120-SNAPSHOT`.

## 2026-07-22 - Agent evidence, runtime policy, and clean UI build

- Added structured Agent run context, assumptions, evidence, and warnings fields with PostgreSQL migration and PO/BO/DTO mapping.
- Successful tool steps now persist source/date/summary evidence; failed steps remain warnings and are not presented as facts.
- Added input/output bounds, duplicate tool-call rejection, a 600-second run limit, and a two-active-run owner guard. Prompt text now explicitly treats user/tool data as untrusted and prohibits secret disclosure or unregistered actions.
- Added versioned evaluation cases and a security contract documenting the remaining Redis requirement for multi-instance rate limiting.
- Fixed the repository-wide UI TypeScript blockers in Market Pulse, Easy TDX, industry chart, sparkline, and market-pulse type contracts. `npx tsc -b --pretty false` and `npm run build` now pass; Vite reports only existing font/chunk warnings.
- Synchronized Maven/UI versions to `1.1.121-SNAPSHOT`.

## 2026-07-22 - Strict JSON Agent fallback

- Added a strict `{"type":"tool_calls","calls":[...]}` fallback for providers that return structured JSON instead of native function calls.
- Validated fallback shape, call count, and object arguments before execution; arbitrary JSON answers remain final answers.
- Synchronized Maven and UI versions to `1.1.114-SNAPSHOT`.
- Targeted AI compile passed.

## 2026-07-22 - Agent provider binding audit

- Audited live PostgreSQL routing: provider `custom` is the enabled `xtyooo` row at `https://am.xtyooo.top`, with default/model override `gpt-5.4-mini`; `workspace_chat` already used it, while `autonomous_agent` was missing a dedicated binding.
- Added `20260722_bind_autonomous_agent_to_xtyooo.sql`, preserving provider-managed credentials and registering the Agent runtime with bounded tool-call parameters.
- Applied the migration to local `cynexus`; verified `autonomous_agent -> provider 910001000000000005 -> gpt-5.4-mini`.
- Synchronized Maven/UI versions to `1.1.115-SNAPSHOT`.
- AI starter reactor compile passed.

## 2026-07-22 - Agent production completion audit

- Re-read `AGENTS.md`, the Agent architecture, current Agent application/controller/runtime code, Release container state, and Git history before making changes.
- Confirmed the current Release stack is `1.1.121-260722-RELEASE`, while the worktree has an unrelated/uncommitted `findings.md` change.
- Confirmed the Agent MVP is implemented, but real production completion is not yet proven: owner identity is client-controlled/anonymous-compatible, active-run and cancellation state are JVM-local, persistent retry/recovery is absent, executable mock workflow coverage is incomplete, and the deployed Release image still needs a new end-to-end verification against `xtyooo/gpt-5.4-mini`.
- Added Phase 105 to `task_plan.md` covering real-provider verification, identity/tenant isolation, distributed reliability, security/data governance, executable evaluation, product workflow completion, true streaming, capability expansion, and the final Release audit.
- Release start verification exposed an explicit-version parsing defect: the script looked for a date-suffixed Maven SNAPSHOT artifact. The fix strips `-YYMMDD-RELEASE` to derive the matching Maven version.

## 2026-07-22 - Agent trusted identity and distributed lease implementation

- Removed Agent controller/query/body ownership selection from the trusted API path; the controller now requires a gateway-signed principal and tenant assertion.
- Added HMAC identity signing in `cynexus-common-security`, gateway header injection that discards client-supplied identity headers, and explicit anonymous compatibility only for configured non-Release environments.
- Added tenant-qualified persistence scope (`tenant::principal`) and removed the UI owner query parameter.
- Replaced JVM-local active-run/cancellation maps with a Redis-backed bounded lease and cancellation token port; the domain remains independent of Redis.
- Added identity authorization tests for valid signatures, cross-principal/tenant tampering, and explicit anonymous mode.
- Synchronized Maven/UI versions to `1.1.123-SNAPSHOT`; targeted compilation is currently passing, with full runtime verification pending the Release rebuild.
- Release identity bootstrap initially failed because the installed PowerShell/.NET runtime lacks static `RandomNumberGenerator.Fill`; changed both Dev and Release scripts to the compatible instance API.
- The first Mock Agent workflow test compile exposed a missing Mockito test dependency in the application-controller module; added `spring-boot-starter-test` with test scope before rerunning.
- The first Mock Agent workflow assertion expected one step; the actual contract persists one tool step plus one answer step. Updated the test to assert both.

## 2026-07-22 - Agent idempotency, retry metadata, and distributed rate limiting

- Added optional owner-scoped Agent idempotency keys with a PostgreSQL partial unique index; repeated requests return the persisted run instead of executing the provider again.
- Added persisted tool `attempt_count` metadata and bounded retry classification for transient provider/tool failures; validation and policy failures are not retried.
- Added a Redis rolling-window per-owner request limiter through a domain port, alongside the existing active-run lease and cancellation token.
- Applied `20260722_add_ai_agent_idempotency_and_retry_fields.sql` locally.
- Synchronized Maven/UI versions to `1.1.124-SNAPSHOT`; focused Agent workflow and identity tests passed.

## 2026-07-22 - Agent circuit breaking, stale-run recovery, and failure taxonomy

- Added a Redis-backed provider/tool circuit breaker with closed, open, and single-probe half-open behavior through a domain port.
- Added persisted run/step failure categories so timeout, upstream, validation, authorization, policy, cancellation, and stale recovery are distinguishable.
- Added a scheduled stale-run reconciler that only fails runs older than the recovery window after their distributed lease has expired.
- Hardened lease release so an expired/already-released lease cannot decrement the active-run counter below its true value.
- Added migration `20260722_add_ai_agent_failure_categories.sql` and synchronized Maven/UI versions remain at `1.1.124-SNAPSHOT`.
- Focused Agent workflow test and full Maven package/UI build passed; live migration and Release verification remain pending.

## 2026-07-22 - Provider token streaming

- Added a provider-neutral streaming port with a truthful complete-response fallback for adapters without streaming support.
- Added OpenAI-compatible Chat Completions SSE parsing for text and tool-call deltas, plus Responses API text/function-call delta parsing.
- Added `TOKEN_DELTA` Agent SSE events and incremental UI answer rendering while the persisted run remains the source of truth after completion.
- Added adapter contract coverage for streamed text and tool deltas; 3 focused adapter tests passed.

## 2026-07-22 - Agent retry, regenerate, and history search

- Added owner-scoped `retry` and `regenerate` run actions that create a new persisted execution in the existing conversation without reusing the prior idempotency key.
- Added server-side conversation search across conversation number, title, and latest message, with a UI search control.
- Added Agent UI retry/regenerate controls and kept the execution trace/evidence visible after reruns.
- AI focused Maven tests and UI TypeScript check passed.

## 2026-07-22 - Agent application and market-information capability expansion

- Added `market.information_feed` through the existing Market Feign boundary with bounded page/filter arguments and persisted feed source metadata.
- Added `application_analysis.result` and `application_analysis.recent30` through a domain gateway port backed by the existing application-analysis application service.
- All three tools are read-only, schema-validated, and assembled dynamically by `AgentToolRegistry`; provider routing remains centralized on `autonomous_agent` so tool capabilities do not introduce fake independent model routes.
- AI starter reactor compile and 7 focused registry/adapter tests passed.

## 2026-07-22 - Release provider proxy and 1.1.126 alignment

- Release `1.1.124` health checks passed, but the first real Agent smoke reached `xtyooo/gpt-5.4-mini` and failed with an SSL handshake because the container did not receive the host proxy environment.
- Fixed Release proxy environment propagation and configured the OpenAI-compatible Java `HttpClient` to use `HTTPS_PROXY`/`HTTP_PROXY` when present.
- Synchronized Maven/UI versions to `1.1.126-SNAPSHOT`; a new versioned Release build and real-provider smoke are required before claiming the provider path complete.

## 2026-07-22 - Agent JSON persistence boundary correction

- Rebuilt Release as `1.1.126-260722-RELEASE` through `deploy/release/start-release.ps1`; Core, Market, AI, Gateway, and UI health endpoints returned `UP`/HTTP 200 and AI logged the configured proxy.
- Real `xtyooo/gpt-5.4-mini` smoke proved provider routing and model-directed multi-turn tool selection, but exposed invalid `jsonb` from raw character truncation of oversized tool output.
- Normalized every tool output to valid JSON before persistence/model follow-up and replaced truncation with a valid `{truncated, originalChars, preview}` envelope.
- Added a workflow regression assertion that oversized/invalid tool output still persists valid JSON; targeted Maven test passed.
