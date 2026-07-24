DO NOT send optional commentary!!!

## Codex Operating Rules

- Read this file before making project changes.
- After completing a functional change, increment Maven and UI versions together and keep them fully synchronized. See **Version Sync Rules** below. Development versions stay `x.y.z-SNAPSHOT`; the matching Release image/version is `x.y.z-YYMMDD-RELEASE` (for example `1.0.1-260712-RELEASE`).
- Release runs only from versioned Docker images and the built UI image. Never use local Java processes, `cmd.exe`, Vite, or the source tree as the Release runtime.
- Development uses the `dev` Spring profile and separate application ports.
- PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor may be shared, but Dev and Release application processes and ports remain separate.
- Only one application environment may be online at a time. Starting Dev must stop the Release application stack first; starting Release must stop Dev application processes first. Never stop the shared infrastructure containers during this switch.
- Dev entrypoint: `deploy/dev/start-dev.ps1` and `deploy/dev/stop-dev.ps1`.
- Release entrypoint: `deploy/release/start-release.ps1 -Version <version>` and `deploy/release/stop-release.ps1`.
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
- **Release images** still use `x.y.z-YYMMDD-RELEASE` derived from the synchronized base version.

## Repository Layout & File Placement

- **Do not dump temporary files in the repo root.** No ad-hoc scratch scripts, logs, jars, decompile dirs, replay dumps, provider keys, or one-off SQL in the project root.
- **Canonical folders (put new files here):**
  - cynexus-*/ : Java modules only (api / application / domain / infra / starter / common)
  - cynexus-ui/ : frontend only (src/views, src/components, src/services, etc.)
  - deploy/ : runtime and deploy scripts
    - deploy/local/postgres/migrations/ : SQL migrations and prompt seeds
    - deploy/dev/ and deploy/release/ : start/stop and release scripts
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

## DDD Layering Discipline

- `application` is an orchestration boundary only: it accepts requests, invokes domain use cases, coordinates transactions/cross-aggregate calls, and converts DTOs.
- Business rules must live under the module's `domain` package, including validation, normalization, scoring, routing, parsing, scheduling policy, chunking, and aggregate state transitions.
- Do not put domain algorithms or business decisions in `application/controller` services. Move them into domain entities, value objects, domain services, or domain-level policies.
- Domain code must not depend on API DTOs, MVC, persistence PO classes, MyBatis, Feign, Spring AI, or infrastructure clients. Define ports in domain and implement them in `infra`.
- `infra` owns persistence mappings, external clients, and repository implementations. Controllers must not depend directly on `infra`; the starter assembles application, domain, and infrastructure modules.
- For existing modules, refactor in this order when touching shared business logic: `knowledgebase`, `market`, `core`, `ai`.
- `domain/service` must not become a catch-all for unrelated business logic. Organize domain policies, services, implementations, and ports under feature folders such as `domain/applicationanalysis`, `domain/marketinformation`, `domain/scheduler`, `domain/stock`, or `domain/port`.

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
