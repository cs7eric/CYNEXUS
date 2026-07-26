DO NOT send optional commentary!!!

## Codex Operating Rules

- Read this file before making project changes.
- After completing a functional change, increment Maven and UI versions together and keep them fully synchronized. See **Version Sync Rules** below. Source and Maven versions stay `x.y.z-SNAPSHOT`; the matching Release image/version is `x.y.z-YYMMDD-RELEASE` (for example `1.0.1-260712-RELEASE`).
- The Dev application environment is deprecated and must not be started. Do not use `deploy/dev/start-dev.ps1`, local Java processes, `cmd.exe`, Vite, or the source tree as an application runtime.
- Release runs only from versioned Docker images and the built UI image.
- PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor are shared infrastructure and must remain running while the Release application stack is switched or restarted.
- Only the Release application stack may be online. Use the Release entrypoint for every application restart; never start a parallel Dev or local application process.
- Release entrypoint: `deploy/release/start-release.ps1 -Version <version>` and `deploy/release/stop-release.ps1`.
- After completing a change, rebuild/recreate and verify the Release stack through `deploy/release/start-release.ps1 -Version <version>`; do not rely only on compilation results or frontend hot reload.
- Every new AI capability must be registered in the AI Provider page's Capability routing list, with its runtime entry, prompt key, group, and description kept synchronized with the backend capability code before the feature is considered complete. Prompt text itself must also be seeded into `ai_prompt` and bound via `ai_usage_bindings.prompt_id` - see **AI Prompt & Capability Management Guide** below.

## Version Sync Rules

- **One version everywhere.** Root `pom.xml`, every module `pom.xml` parent `<version>`, and `cynexus-ui/package.json` must use the exact same `x.y.z-SNAPSHOT`.
- **Never partial bump.** Do not update only root/`package.json` and leave module parents on an older version. That breaks Maven parent resolution and causes mass compile failures.
- **What to update on every functional version bump:**
  1. Root `pom.xml` → project `<version>`
  2. All module `pom.xml` files → parent `<version>` (currently every `cynexus-*` module)
  3. `cynexus-ui/package.json` → `"version"`
- **Sync command example (PowerShell):**
  ```powershell
  $ver = "x.y.z-SNAPSHOT"
  # set root + all module parent versions, then set UI package.json version to $ver
  Get-ChildItem -Recurse -Filter pom.xml |
    Where-Object { $_.FullName -notmatch "\\target\\|\\node_modules\\" } |
    ForEach-Object {
      (Get-Content $_.FullName -Raw) -replace "<version>1\.1\.\d+-SNAPSHOT</version>", "<version>$ver</version>" |
        Set-Content $_.FullName -NoNewline
    }
  ```
  Prefer a careful replace limited to Cynexus parent/project versions; do not rewrite third-party dependency versions.
- **Verify before finishing:**
  - all Cynexus SNAPSHOT versions in `pom.xml` are identical
  - UI `package.json` version matches
  - `mvn -DskipTests compile` succeeds
- **Release images** use `x.y.z-YYMMDD-RELEASE` derived from the synchronized base version. Release verification must use the versioned Docker stack, not local Java or Vite processes.

## Repository Layout & File Placement

- **Do not dump temporary files in the repo root.** No ad-hoc scratch scripts, logs, jars, decompile dirs, replay dumps, provider keys, or one-off SQL in the project root.
- **Canonical folders (put new files here):**
  - cynexus-*/ : Java modules only (api / application / domain / infra / starter / common)
  - cynexus-ui/ : frontend only (src/views, src/components, src/services, etc.)
  - deploy/ : runtime and deploy scripts
    - deploy/local/postgres/migrations/ : SQL migrations and prompt seeds
  - deploy/release/ : Release start/stop and image scripts
  - deploy/dev/ : deprecated legacy scripts; do not use for application startup
  - docs/ : durable design notes, release notes, architecture docs
  - guide/ : project guides and onboarding docs
  - design/ : UI/UX or product design assets
  - sql/ : long-lived schema references (not one-off repair scripts)
  - db/ : DB contracts and checks that are part of the product
  - ci/ : CI helpers
  - artifacts/ : intentional checked-in screenshots or release evidence only
  - .planning/ : short-lived planning notes for ongoing work (not root task_plan.md)
  - .planning/scratch/ : session-only temp files; delete before finishing
  - var/ : tiny local runtime samples only (for example media fixtures). Never generated Java/Python dumps.
- **Temporary work location:** use OS temp (TEMP on Windows, /tmp on Linux) or an ignored local path outside git tracking. If a script must live briefly in-repo during a session, put it under .planning/scratch/ and delete it before finishing; never leave underscore-prefixed scripts at root.
- **SQL rules:**
  - schema/data migrations -> deploy/local/postgres/migrations/YYYYMMDD_*.sql
  - durable schema docs -> sql/<domain>/
  - never leave restore_*.sql or _tmp.sql at root
- **Logs/build outputs:** go to ignored locations only (logs/, target/, deploy local logs). Do not commit log files, hs_err_pid dumps, packaged jars, or decompiled sources.
- **Secrets:** never write keys to the repo (provider-key.txt, raw API keys, .env values). Use env vars / secret files already ignored by .gitignore.
- **Before finishing a task:** delete session scratch files; ensure git status does not show root junk (underscore scripts, jars, logs, replay dumps).

## DDD Layering Discipline (Mandatory)

These rules are **mandatory** for every change. Violations must be fixed before the change is considered complete.

### Layer responsibilities

| Layer | Owns | Must not own |
| --- | --- | --- |
| `api` | Request/Response DTO, Feign contracts | Business rules, repositories |
| `application` | Orchestration, transaction boundary, DTO <-> domain mapping | Algorithms, repository access, prompt rendering, BO assembly |
| `domain` | Entities/BO/VO, policies, domain services, ports/gateways | API DTO, Spring MVC, MyBatis PO/mapper, Feign clients |
| `infra` | Port implementations, persistence, external clients | Business decisions that belong in domain |
| `starter` | Assembly/boot wiring | Business logic |

### Canonical data flow

```text
Controller / Request DTO
  -> AppService (orchestration only)
    -> DomainService / DomainPolicy (business meaning)
      -> Domain BO / ValueObject
    <- DomainService returns BO/VO
  -> AppService maps BO/VO to Response DTO
<- Controller returns DTO
```

- AppService may **see** BO only as the return value of a domain call for DTO mapping.
- AppService must not construct business BO graphs, keep long-lived BO state, or mutate BO fields with business meaning.
- Prefer feature-specific domain services over "god" AppServices.

### Forbidden in `application` / AppService

- `import ...repository...` or direct repository/port usage for business resolution
- Hand-built BO workflows (create/fill/update many BO fields as business steps)
- Prompt template loading, binding selection, or `{{var}}` rendering
- Market/finance scoring, ranking, scheduling policy, parsing, normalization, tool-budget decisions
- Long run-loop business rules (answer normalization, clarification parsing, cost estimation, history collapse, redaction policy)
- Returning BO from public AppService APIs for **new** features (use DTO). Existing thin CRUD pass-through BO APIs are transitional debt and must not be copied.

### Required in `domain`

- Repository ports and gateway ports
- Prompt resolution (`ai_prompt` + bindings + fallback)
- Validation / normalization / scoring / routing / parsing
- Aggregate state transitions and run lifecycle rules
- Pure policies under `domain/policy/**` and use-case services under `domain/service/**` or feature folders

### Allowed AppService patterns (good)

- Thin delegate: map request -> call one domain service -> map response DTO  
  Examples: `AgentFeedbackAppService`, `MediaTranscriptionAiAppService`, `ModelRouteAppService`, `ScenarioExecutionAppService` (after prepare/finalize domain split)
- DTO mapping from domain BO returned by domain query services  
  Examples: market query AppServices that only map BO -> page DTO

### Forbidden AppService patterns (bad)

- AppService injects `AiPromptRepository` / any `*Repository` to decide business outcome
- AppService contains `normalizeFinalAnswer`, `render(template,...)`, MSI/score math, cron policy, clarification JSON parsing
- AppService builds `AiExecutionBO` / `AgentRunBO` business state without domain service ownership
- Application package imports infra mapper/PO/Feign implementation types

### Import rules (quick check)

- `application` may import: `api`, `domain` (service/policy/entity/valueobject/port), application assemblers
- `application` must not import: `infra.*`, MyBatis, Feign clients, Spring AI adapters
- `domain` may import: domain + module `common` only
- `domain` must not import: `api`, `application`, `infra`, MVC, MyBatis, Feign, Spring AI
- `infra` implements domain ports and may use MyBatis/Feign/HTTP clients

### Definition of done for any feature touching services

- [ ] Business rule lives in `domain` (policy or domain service), not AppService
- [ ] AppService is orchestration + DTO mapping only
- [ ] No new repository dependency on AppService
- [ ] No new BO-returning public API unless migrating old CRUD and documented as transitional
- [ ] Prompt text (if any) follows **AI Prompt & Capability Management Guide**
- [ ] Maven + UI versions bumped together after functional change
- [ ] Focused tests cover domain policy/service behavior

### Refactor priority when touching shared logic

1. `ai` agent/scenario/prompt paths  
2. `core` scheduler / media / finance capture  
3. `market` query aggregation  
4. `knowledgebase`

### Reference implementations

- Good domain ownership: `AgentSystemPromptDomainService`, `AgentSystemPromptPolicy`, `ScenarioExecutionDomainService.prepare*/finalize*`, `PromptRenderPolicy`, `AgentFinalAnswerPolicy`, `AgentHistoryCollapsePolicy`
- Good thin AppService: `AgentFeedbackAppService`, `MediaTranscriptionAiAppService`, `ScenarioExecutionAppService`
- Do not copy: historical god AppServices (`AutonomousAgentAppService` run loop still being thinned; `SchedulerAppService` orchestration still being moved into `domain/scheduler`)

### BO usage clarification

- **BO is a domain object**, not an application DTO.
- Domain **returns BO / domain value objects** to Application Service. Application Service maps BO -> Response DTO for controllers/API.
- Application Service may build a simple BO shell from Request DTO fields only as input to a domain command; it must not decide business meaning while assembling that BO.
- Application must not:
  - compute business results itself (score / route / normalize / parse / reconcile / budget / prompt render)
  - query databases or inject `*Repository` / mappers
  - keep long-lived BO graphs as working state outside a domain use case
- Controllers should prefer API DTOs. Historical CRUD that still exposes BO is transitional debt and must not be expanded.


## AI Prompt & Capability Management Guide

- **Never hardcode production prompt text only in Java.** All new/changed AI prompts must be managed in `cynexus_appl_ai.ai_prompt` and editable from the AI Prompt management UI. Hardcoded text is allowed only as a **runtime fallback** when the DB row is missing.
- **One capability ↔ one prompt key.** Use a stable `prompt_key` that matches the capability code whenever possible (for example `autonomous_agent`, `accounting_receipt_parse`, `mp4_text_polish`). Keep the same key in:
  - backend capability / scenario code
  - `ai_prompt.prompt_key`
  - `ai_usage_bindings.capability`
  - AI Provider page Capability routing (`promptKey` / runtime entry / group / description)
- **Required data objects for any new AI feature:**
  1. `ai_prompt` row: `prompt_key`, `prompt_version`, `status=active`, `system_prompt`, optional `user_prompt_template`, `variables`, `output_schema`, `tags`, `extra`, `remark`
  2. `ai_usage_bindings` row: `capability`, provider, optional `model_override`, `is_enabled=true`, **`prompt_id` pointing to the prompt row**, runtime params in `params` JSON
  3. Capability routing registration so the AI Provider page can manage model/provider routing
- **Seed via SQL migration**, do not rely on manual UI clicks for baseline content. Put seeds under `deploy/local/postgres/migrations/` using names like `YYYYMMDD_seed_<capability>_prompt.sql`. Follow existing patterns such as:
  - `20260719_seed_accounting_receipt_prompt.sql`
  - `20260724_seed_autonomous_agent_prompt.sql`
- **Migration seed checklist:**
  - `INSERT ... ON CONFLICT (prompt_key, prompt_version) ... DO UPDATE` so re-apply is idempotent
  - set `status = 'active'`
  - declare template placeholders in `variables` JSON (for example `["current_date","tool_budget"]`)
  - `UPDATE ai_usage_bindings SET prompt_id = prompt.id ... WHERE capability = '...'`
  - if the feature is Agent-related and applied by release scripts, also list the file in `deploy/release/apply-agent-migrations.ps1`
- **Runtime resolve order (required):**
  1. `ai_usage_bindings.prompt_id` → `ai_prompt` active row
  2. else active `ai_prompt` by `prompt_key`
  3. else service/domain hardcoded fallback template
- **Template variables:** put dynamic values in `{{var}}` placeholders and render in domain code. Do not concatenate business prompt sentences only in application services. Prefer a domain policy/service (for example `AgentSystemPromptPolicy`, media transcription `resolvePrompt`).
- **Fallback rule:** fallback text must preserve current behavior, but the managed DB prompt is the source of truth after seed. When changing prompt wording, update the SQL seed / active prompt row first; keep fallback in sync when behavior changes.
- **Do not invent a second prompt table.** Continue using `cynexus_appl_ai.ai_prompt` only. Versioning uses `prompt_version` (`v1`, `v2`, ...); bind the intended version through `ai_usage_bindings.prompt_id` (and optional `params.prompt_version`).
- **Reference implementations:**
  - Agent system prompt: `AgentSystemPromptPolicy` + `prompt_key=autonomous_agent`
  - Media transcription: `MediaTranscriptionDomainServiceImpl.resolvePrompt`
  - Market information: `MarketInformationAiDomainService.resolvePrompt`
  - Receipt parse: `accounting_receipt_parse` seed + usage binding
- **Definition of done for a new AI capability:**
  - [ ] capability code registered and routed on AI Provider page
  - [ ] `ai_prompt` seed migration added and applied
  - [ ] `ai_usage_bindings` created/updated with provider + `prompt_id`
  - [ ] runtime loads prompt from repository (binding → key → fallback)
  - [ ] Prompt UI can edit/publish the prompt without code redeploy for wording-only changes
- [ ] Maven + UI versions bumped after functional change

## Constants Placement Rules

- Repeated literals with shared meaning must be defined once in a `Constants` class and referenced by callers; do not create several near-identical copies of the same protocol key, header, status, schema, limit, or timezone value.
- Put truly cross-module values in `cynexus-common` (normally `cynexus-common-core`); put values shared by one bounded service in that service's `cynexus-xxx-common` module.
- Put values used only by one layer in that layer's local common package, for example `cynexus-xxx-*/src/main/java/.../<layer>/common/constant`, instead of leaking infra or adapter details into domain/common modules.
- Keep a constant in its owning module even when another module consumes the API. Do not move business-specific values into global common merely to remove a duplicate field name.
- Local `private static final` values remain appropriate for one class's implementation detail, derived values, regular expressions, SQL fragments, and single-use business rules. A refactor must preserve DDD dependency direction and must not make domain depend on infra.

## Agent Prompt Cache Rules

- Keep the DB-managed Agent system prompt stable: runtime date, budget, tool-count, and force-final-answer values must be sent as a later runtime-context message, not rendered into the stable prefix.
- Place runtime context after the existing conversation history so each completed Agent turn remains reusable as the next request's prefix; send it as a trailing `user` message because provider-side system/instruction normalization breaks cache-prefix growth for dynamic values.
- Replay native reasoning, function calls, Tool outputs, and runtime context only inside the active Run. Across Runs, include only visible user/assistant messages from `SUCCEEDED` or `NEEDS_CLARIFICATION` Runs; exclude failed/cancelled Run tails.
- Generate `PromptCacheContext` per model turn using the exact Tool set sent on that turn. A no-Tool final-answer turn must not reuse the cache key for a Tool-enabled turn.
- Sort Tool definitions deterministically before serialization, and version the cache key whenever the prefix layout or Tool serialization contract changes. A `prompt_cache_key` is only a routing hint; verify real hits from provider usage fields.
