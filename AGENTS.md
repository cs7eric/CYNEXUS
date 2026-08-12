# Cynexus Agent Guide

## Project

Cynexus is a personal intelligent workspace platform composed of:

- Java 21 multi-module services under `cynexus-*`
- A Python job executor and market-data project under `cynexus-data`
- A React 19, TypeScript, Vite 8, and Tailwind CSS v4 UI under `cynexus-ui`
- Docker-managed Release runtime and shared infrastructure under `deploy`

The repository is actively developed and may contain unrelated user changes.
Never discard or rewrite changes you did not make for the current task.

## Global Invariants

- Start code inspection with CodeGraph. Use the CLI when CodeGraph MCP tools are unavailable.
- Read the closest applicable `AGENTS.md` before editing files in that area.
- Use `apply_patch` for manual edits and keep changes scoped to the task.
- Do not create temporary files, logs, dumps, jars, secrets, or one-off SQL in the repository root.
- The deprecated Dev application must not be started. Do not start local Java, Python, or source-tree backend runtimes.
- Frontend-only development is allowed: start only `cynexus-ui`'s Vite server on port `5174` (HMR), proxying to the existing Release backend. Do not start the deprecated Dev application or any local backend runtime.
- UI code changes and UI deployments use the `develop` workflow by default. Before any UI deployment, inspect and record the current model/environment (`develop` or `release`). Only switch to the `release` workflow when the user explicitly asks to deploy Release; otherwise keep the UI on the `develop`/HMR workflow.
- The `develop` and `release` UI workflows share the same Release backend services; this rule applies only to the UI layer.
- Only the Release backend/application services may be online, plus the explicitly allowed frontend-only Vite server on `5174`. Shared PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor infrastructure must remain running.
- Functional changes require Maven and UI versions to remain synchronized as `x.y.z-SNAPSHOT`.
- Any backend, data, migration, or shared-runtime change that can affect Release behavior must be deployed before handoff. UI-only changes use the `develop` Vite workflow by default; rebuild/recreate Release UI only after the user explicitly requests a Release deployment. Compilation and tests alone do not update a running Release stack.
- New AI capabilities are incomplete until backend capability code, AI Provider routing, prompt seed, usage binding, and runtime resolution agree.
- Every foundational user-facing data read/query/write capability must evaluate autonomous Agent exposure. Implement a bounded read-only Agent tool when safe; route mutations through preview/explicit confirmation/audit. Document an explicit exclusion when the operation is unsafe or unsuitable for model invocation.
- Do not place business rules in application services or infrastructure when they belong in the domain layer.

## Repository Map

- `cynexus-common`: cross-service Java common modules
- `cynexus-core`: scheduler, media, and core platform modules
- `cynexus-ai`: AI APIs, domain policies, runtime integrations, and provider capabilities
- `cynexus-market`: market query and aggregation services
- `cynexus-knowledgebase`: knowledge-base services
- `cynexus-gateway`: gateway starter
- `cynexus-data`: Python sources, pipelines, jobs, storage, quality checks, and trigger API
- `cynexus-ui`: React application, shared primitives, views, and services
- `deploy/local`: shared infrastructure and PostgreSQL migrations
- `deploy/release`: versioned Docker Release entrypoints and migration application
- `docs`: durable architecture and operational documentation
- `design`: page-level product and visual design material
- `.planning`: task planning; use `.planning/scratch` only for session scratch
- `ci`: deterministic repository checks

## Architecture Summary

Java modules follow DDD layering:

- `api`: request/response DTOs and Feign contracts
- `application`: orchestration, transaction boundary, and DTO mapping
- `domain`: entities, BO/VO, policies, domain services, repositories, and gateway ports
- `infra`: persistence, external clients, and port implementations
- `starter`: assembly and boot wiring

The Python project is a Job Executor:

```text
job -> runtime/job_runner -> pipeline -> source/domain/storage -> connection/hook
```

Sources do not write databases, domain code does not access external systems, and jobs do not own scheduling state. See `cynexus-data/AGENTS.md`.

## Task Routing

Before editing, load the guidance for the area involved:

- Java backend: `docs/agent/backend-ddd.md`, plus the closest module `AGENTS.md` when present
- Code search and impact analysis: `docs/agent/codegraph.md`
- AI capability, prompt, routing, or cache work: `cynexus-ai/AGENTS.md` and `docs/agent/ai-capability.md`
- Python data work: `cynexus-data/AGENTS.md`
- React, layout, visual, or accessibility work: `cynexus-ui/AGENTS.md`; then read `cynexus-ui/DESIGN.md` for visual changes and `cynexus-ui/FRONTEND_UI_IMPLEMENTATION_GUIDE.md` for implementation changes
- Database or migration work: `deploy/AGENTS.md` and `docs/agent/repository-and-database.md`
- Shared Redis/cache work: `docs/agent/cache.md`
- Release, image, runtime, or version work: `deploy/AGENTS.md` and `docs/agent/release.md`
- Agent prompt-cache work: `docs/agent/agent-prompt-cache.md`
- Agent tools or exposing project data operations to the autonomous Agent: `.agents/skills/cynexus-agent-tools/SKILL.md`

Use the matching repository skill under `.agents/skills/` for repeatable workflows. Detailed files under `docs/` and `design/` are references, not automatically loaded just because they exist.

## Required Verification

1. Inspect the affected symbols and callers with CodeGraph before editing shared code.
2. Run focused tests for changed behavior.
3. For Java changes, run `mvn -DskipTests compile` or the narrowest useful Maven command.
4. For UI changes, run the relevant `pnpm` checks from `cynexus-ui`.
5. For Python changes, run the relevant `pytest` checks and dry-run jobs where applicable.
6. After functional changes, verify Maven and UI versions are identical.
7. For backend, data, migration, or shared-runtime changes, rebuild and verify every potentially affected Release service through `deploy/release/start-release.ps1 -Build -Version <version>`; use a full-stack deployment when the impact boundary is uncertain. For UI-only changes, verify the `5174` develop server unless the user explicitly requests Release.
8. Check `git status` and remove unintended scratch output before finishing.

## Communication

Do not send optional commentary. Keep necessary updates concise and task-focused. Report findings and verification results clearly. Do not claim a check passed when it was not run.
