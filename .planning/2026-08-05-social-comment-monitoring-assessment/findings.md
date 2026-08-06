# Findings: Social Comment Monitoring Feasibility Assessment

## Initial Requirements

- Platforms: Xiaohongshu and Douyin.
- Scale: no more than 50 monitored creators per platform.
- Discovery: automatically detect each creator's newly published posts every day.
- Collection: incrementally collect public comments without paid APIs or data providers.
- Operation: scheduled and automated.

## Workflow Constraints Loaded

- Repository work must begin with CodeGraph; CLI fallback is permitted when MCP is unavailable.
- Cynexus data work separates connections/configuration, hooks/clients, sources/adaptation, pipelines/orchestration, storage/writes, jobs, and JobRunner lifecycle.
- Jobs must remain dry-run capable and return JobResult.
- This assessment does not authorize implementation or Release deployment.
- CodeGraph index is healthy but has 11 added and 8 modified pending files; sync is required before architecture queries.
- CodeGraph synchronized successfully over 22 changed files before architecture queries.
- `cynexus-data` owns external collection, parsing, normalization, persistence, checkpoints through JobRunner, and its internal trigger API; Java owns scheduler configuration and user-facing APIs.
- Required execution chain is `jobs -> runtime/job_runner -> pipelines -> sources/domain/storage -> hooks -> connections`.
- Existing infrastructure guidance explicitly supports source-level rate/fallback configuration, reusable HTTP hooks, idempotency, retry, checkpoints, quality/freshness checks, metrics, alerts, and audit records.
- Platform credentials/session state belong in Connections; browser or HTTP client construction belongs in Hooks; platform page/API adaptation belongs in Sources.
- The durable architecture confirms jobs must stay thin and pipelines receive runtime context; the proposed collector should not embed scheduling, locking, or checkpoint ownership in platform-specific jobs.
- Java should own cron, enable/disable state, creator-monitor configuration APIs, permissions, and UI response assembly; Python should execute discovery/comment sync and persist results.
- Existing JobRunner semantics already cover trigger idempotency, locking, run records, errors, and checkpoint lifecycle, so the proposal should reuse them rather than introduce a second scheduler or state machine.
- CodeGraph confirms the implemented `JobRunner` is in `cynexus-data/src/cynexus_data/runtime/job_runner.py`, is used by the internal trigger API, and has dedicated unit tests.
- A Python `CheckpointStore` already exists, while Java also exposes task-checkpoint persistence; platform-specific cursor state should use the established runtime checkpoint abstraction and avoid creating a parallel ad hoc table unless the existing value shape proves insufficient during implementation.
- CodeGraph identifies a centralized Python `JOB_REGISTRY`, a reusable `_pipeline_job` adapter, and `PipelineContext.default(persist_job_state=...)`; the social collector should register thin jobs and use the same context lifecycle.
- Existing pipeline signatures consistently accept `(PipelineContext, JobRequest)` and return a result dictionary later mapped into JobResult metrics.
- `JOB_REGISTRY` marks all registered jobs as dry-run capable by default and exposes descriptions through the trigger API; proposed jobs should preserve this contract.
- `HookBundle` currently creates hook instances from the connection registry, while persistent runs use PostgreSQL-backed JobStore, CheckpointStore, and LockManager via `PipelineContext.default(persist_job_state=True)`.
- Adding Douyin/Xiaohongshu collection would require new hook accessors and connection types/configuration, but no new execution framework.
- The existing `BaseHttpHook` is a lightweight `requests.Session` factory used by provider hooks. Browser-backed platform access would need a separate managed-browser hook abstraction rather than overloading the HTTP hook.
- Platform hooks should return purpose-built clients (for profile discovery and comment traversal), matching the existing EastMoney-style hook-to-client pattern.
- The existing checkpoint value is an arbitrary JSON object, sufficient for creator discovery watermarks, per-post comment cursors, retry state, and rolling-window metadata without a new checkpoint schema.
- Representative pipelines short-circuit all external and database access during dry-run, expose planned scope, and return row counters; the proposed social jobs should do the same.
- Existing pipelines also support bounded paging controls and explicit per-page sleep, a useful precedent for platform-safe rate control.

## Agent Exposure Boundary

- Collection/discovery jobs are operational mutations of monitoring state and external access; they should not be registered as autonomous Agent tools.
- A later user-facing query such as “show new comments for creator X on date Y” is suitable for one bounded, tenant-scoped, read-only Agent tool backed by an owning Java API.
- The read tool must cap creators/date range/rows, return source platform and freshness timestamps, redact unnecessary PII, and never accept arbitrary URLs, SQL, repository access, or trigger-job commands.
- No AI model/provider/prompt is needed for the ingestion feature itself; sentiment/topic analysis would be a separate AI capability with its own routing and usage binding.

## Java And Persistence Boundary

- Creator-monitor policy, active/inactive state, platform validation, ownership, and scheduling semantics belong in Java domain services/policies, not AppService or Python.
- Java API contracts should expose creator configuration, post/comment queries, run health, and re-login-required status through bounded DTOs.
- Product schema changes belong in versioned PostgreSQL migrations under `deploy/local/postgres/migrations`; durable data-specific schemas may additionally be documented under `cynexus-data/sql`.
- CodeGraph confirms Cynexus already has `SchedulerDomainService`, management/execution/trigger domain services, and a `DataJobExecutorClient` boundary from Java to the Python executor.
- Therefore daily social jobs should be scheduled as normal Java scheduler definitions invoking registered Python job keys; no Playwright-specific scheduler or Python cron should be introduced.
- Exact Java contracts already support job lookup, triggering, run retrieval, scheduler enablement, progress, target counts, duration, error messages, and run history; social jobs can report creator/post/comment counters through existing status fields.
- Scheduler configuration should use two independent job codes per platform or two cross-platform stages, so profile discovery failures do not suppress comment catch-up and retries can be targeted.
- Existing scheduler migrations store `data_job_key`, schedule JSON, default parameters, timezone, tick frequency, enablement, and run-once-per-day behavior; social jobs fit the established migration pattern.
- Unlike market jobs, social discovery should use calendar days, not workday/trading-day restrictions, and should include an overlap window so posts around midnight or delayed page rendering are not missed.

## Runtime Boundary

- The only supported runtime is the versioned Docker Release stack; the browser collector must run inside the Data Executor image or a deliberately added Release-side worker image, never as a source-tree Playwright process.
- PostgreSQL/Redis/ClickHouse/Data Executor are shared infrastructure; implementation would require a rebuilt Release image and service recreation, not merely Python tests.
- A browser-enabled image materially increases image size and runtime attack surface, so the preferred design is one managed browser worker with strict concurrency rather than one browser per creator.
- Initial filename-only discovery suggested no browser stack, but exact dependency/container inspection corrected that conclusion: Playwright is already an optional `live` dependency, Chromium is installed in the Data Executor image, and `CYNEXUS_PLAYWRIGHT_EXECUTABLE_PATH=/usr/bin/chromium` is already configured.
- The Data Executor image is defined under `deploy/local/Dockerfile.data` and participates in shared infrastructure rather than the Release application compose file.
- Browser runtime readiness materially improves feasibility: the proposal can extend the existing image and dependency set instead of introducing a new service solely for Playwright.
- CodeGraph found no indexed `sync_playwright` or executable-path references, so the existing browser capability may be provisioned for a narrow source implemented through a different import pattern; exact text search is required.
- Exact text search found mature Playwright use in EastMoney browser sessions and TDX fallback downloads, with unit tests, persistent Chromium profiles, system-browser resolution, headless mode, and container-safe flags.
- The EastMoney implementation serializes access with a process lock and persists browser state under `var`; the social design can reuse this operational pattern but must support one isolated profile per platform account and protect session material as credentials.
- Existing browser code opens/closes Chromium per request. For up to 100 creators, the optimized design should batch a platform run inside one persistent context and bounded page pool to reduce launch overhead while keeping concurrency low.
- The Data Executor bind-mounts `cynexus-data/var` into `/app/var`, so persistent login profiles survive container recreation; production design must ensure this directory is ignored, access-controlled, backed up selectively, and never exposed through APIs/logs.
- Existing browser-session unit tests mock Playwright at the source boundary; new platform adapters can be tested without live network access using the same pattern.

## Early Assessment

- The requested scale is computationally manageable on self-hosted infrastructure.
- The main feasibility risk is platform access stability and coverage, not CPU, storage, or creator count.
- Daily new-post discovery and comment incrementality must use separate checkpoints because comments continue to arrive after publication.
- Final verdict is conditional go: architecture/runtime feasibility is high, while data completeness and long-term platform stability are medium risk.
- The optimized MVP uses PostgreSQL only, two independent jobs, one isolated persistent profile per platform account, concurrency 1 by default, a 48-hour discovery overlap, and a 30-day rolling comment window.
- A 14-day POC is the release gate; each platform can pass or fail independently.

## POC Implementation Context

- CodeGraph is up to date before implementation inspection.
- The current `JobCallable` contract is `(PipelineContext, JobRequest, run_id) -> JobResult`; new POC jobs must follow this exact signature, not the simplified pipeline signature in the earlier assessment.
- The centralized `JOB_REGISTRY` and trigger API are the correct registration path for POC jobs.
- `run_pipeline_job` centralizes payload-to-JobResult mapping; POC jobs should expose thin `run(context, request, run_id)` wrappers around pipelines.
- `ConnectionRegistry` loads connection declarations from the existing resolver; social browser configuration should be added there instead of reading environment variables directly in sources.
- `Connection` already supports arbitrary `extra` fields, so platform/profile paths and selector settings can be declared without changing the connection model.
- `jobs.yaml` is a catalog of descriptions/parameters rather than scheduler state; POC job entries should document bounded parameters there while actual scheduling remains out of Python.
- Postgres repositories use SQLAlchemy sessions, schema-qualified SQL, SQL-file DDL, and stable IDs; the social POC can follow the same repository/storage pattern.
- Runtime checkpoints already persist through the configured Postgres client, so creator/post state does not need to be duplicated in the social tables for the first POC.
