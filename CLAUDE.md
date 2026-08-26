# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Primary guidance: treat AGENTS.md as CLAUDE.md

**Always use root `AGENTS.md` as the project operating manual.** It is the canonical agent guide for this repository. When instructions conflict, prefer `AGENTS.md` (and the closest nested area `AGENTS.md`) over generic defaults.

Before editing an area, also read the nearest nested guide when present:

- `cynexus-ui/AGENTS.md`
- `cynexus-data/AGENTS.md`
- `cynexus-ai/AGENTS.md`
- `deploy/AGENTS.md`

Do not restate or fork those files here. Follow them, then load the task-routed docs they point to under `docs/agent/` and `.agents/skills/`.

## What this monorepo is

Cynexus is a personal intelligent workspace platform:

- Java 21 multi-module services under `cynexus-*` (Maven reactor in root `pom.xml`)
- Python job executor / market-data under `cynexus-data`
- React 19 + TypeScript + Vite 8 + Tailwind CSS v4 UI under `cynexus-ui`
- Docker Release runtime and shared infra under `deploy`

The tree may contain unrelated user changes. Never discard or rewrite changes you did not make for the current task.

## Hard runtime invariants (from AGENTS.md)

- Start code inspection with CodeGraph (CLI if MCP tools unavailable).
- Deprecated Dev application must not be started. Do not start local Java, Python, or source-tree backend runtimes for day-to-day work.
- Frontend-only development is allowed: only `cynexus-ui` Vite on port `5174` (HMR), proxying the existing Release backend.
- Only Release backend/application services may be online, plus the allowed Vite server. Shared PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor stay running.
- UI code changes default to the `develop` (Vite/HMR) workflow. Switch to Release UI deployment only when the user explicitly asks.
- Functional changes keep Maven and UI versions synchronized as `x.y.z-SNAPSHOT`.
- Backend / data / migration / shared-runtime changes that can affect Release must be deployed before handoff; compile/tests alone do not update a running Release stack.
- Do not create temporary files, logs, dumps, jars, secrets, or one-off SQL in the repository root.

## Architecture (big picture)

### Java (DDD modules)

Typical layering per service module:

- `api` — request/response DTOs and Feign contracts
- `application` — orchestration, transaction boundary, DTO mapping
- `domain` — entities, BO/VO, policies, domain services, repositories, gateway ports
- `infra` — persistence, external clients, port implementations
- `starter` — assembly and boot wiring

Business rules belong in the domain layer, not application services or infrastructure.

Top-level modules include `cynexus-common`, `cynexus-core`, `cynexus-ai`, `cynexus-market`, `cynexus-knowledgebase`, `cynexus-gateway`, `cynexus-wechat`.

### Python (`cynexus-data`)

Job Executor pipeline:

```text
job -> runtime/job_runner -> pipeline -> source/domain/storage -> connection/hook
```

Sources do not write DBs; domain does not hit external systems; jobs do not own scheduling state. Details: `cynexus-data/AGENTS.md`.

### UI (`cynexus-ui`)

- Pages: `src/views/<feature>/` with private `components/` and `lib/`
- Shared UI: `src/components/ui`, product pieces under `src/components/app` or `src/components/cynexus`
- Shell: `src/layout/workspace-shell.tsx`
- API access: `src/services` (not duplicated in views)
- Route, sidebar entry, and breadcrumb label must stay in sync
- Visual/implementation refs: `cynexus-ui/DESIGN.md`, `FRONTEND_UI_IMPLEMENTATION_GUIDE.md`, `PRODUCT.md`

### Deploy

- Shared infra + Postgres migrations: `deploy/local`
- Versioned Docker Release entrypoints: `deploy/release`
- Migrations: `deploy/local/postgres/migrations/YYYYMMDD_*.sql`
- Version contract: root `pom.xml` module versions and `cynexus-ui/package.json` share `x.y.z-SNAPSHOT`; Release images use `x.y.z-YYMMDD-RELEASE`

## Task routing

Load guidance for the area involved (from root `AGENTS.md`):

| Work | Read first |
| --- | --- |
| Java backend | `docs/agent/backend-ddd.md` + closest module `AGENTS.md` |
| Code search / impact | `docs/agent/codegraph.md` |
| AI capability / prompt / routing / cache | `cynexus-ai/AGENTS.md`, `docs/agent/ai-capability.md` |
| Python data | `cynexus-data/AGENTS.md` |
| React / layout / a11y | `cynexus-ui/AGENTS.md` (+ DESIGN / FRONTEND guides for visual or implementation work) |
| DB / migration | `deploy/AGENTS.md`, `docs/agent/repository-and-database.md` |
| Redis / cache | `docs/agent/cache.md` |
| Release / images / versions | `deploy/AGENTS.md`, `docs/agent/release.md` |
| Agent tools exposure | `.agents/skills/cynexus-agent-tools/SKILL.md` |

Repeatable workflows live under `.agents/skills/` (`cynexus-frontend`, `cynexus-backend`, `cynexus-data`, `cynexus-ai`, `cynexus-release`, `cynexus-version-sync`, `cynexus-agent-tools`). Files under `docs/` and `design/` are references, not auto-loaded just because they exist.

## Common commands

### UI (`cynexus-ui`)

```powershell
cd cynexus-ui
pnpm install
pnpm dev          # Vite on 5174; proxy to Release backend
pnpm build        # tsc -b && vite build
pnpm lint         # eslint .
pnpm preview
```

### Java (repo root)

```powershell
mvn -DskipTests compile
# Prefer the narrowest useful module/reactor command for the change
# Run focused tests for changed behavior (module-specific surefire as applicable)
```

### Python (`cynexus-data`)

```powershell
cd cynexus-data
# Prefer area guidance for env setup; then:
pytest path/to/test_file.py
pytest path/to/test_file.py::test_name
# Dry-run jobs where applicable (see cynexus-data/AGENTS.md and skills)
```

### Release / deploy

```powershell
# Start/rebuild Release application stack (versioned images)
.\deploy\release\start-release.ps1 -Build -Version <version>
.\deploy\release\stop-release.ps1
```

Backend, data, migration, or shared-runtime impact: rebuild/verify affected Release services via `start-release.ps1 -Build -Version <version>`; full stack when the impact boundary is uncertain. UI-only: verify `5174` develop server unless the user explicitly requests Release UI.

### Versions

After functional changes, keep Maven parents/projects and `cynexus-ui/package.json` on the same `x.y.z-SNAPSHOT`. Use the version-sync skill/workflow when bumping.

## Required verification checklist

1. CodeGraph on affected symbols/callers before editing shared code.
2. Focused tests for changed behavior.
3. Java: `mvn -DskipTests compile` or narrower Maven command.
4. UI: relevant `pnpm` checks from `cynexus-ui`.
5. Python: relevant `pytest` (and dry-run jobs when applicable).
6. Maven and UI versions identical after functional changes.
7. Deploy Release for backend/data/migration/shared-runtime impact; UI-only stays on develop unless user asks for Release.
8. `git status`; remove unintended scratch output before finishing.

## Communication

Keep updates concise and task-focused. Report findings and verification results clearly. Do not claim a check passed when it was not run.
