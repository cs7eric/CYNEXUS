# Cynexus AI Agent Long-Term Decisions

This document is the durable memory for the Cynexus Agent product. It records decisions that must survive later implementation sessions.

## Product Contract

Cynexus accepts a natural-language request, identifies the user's intent, selects registered capabilities, executes them in a safe order, interprets the returned data, and produces a useful answer with an executable plan and evidence. The model owns planning and sequencing. Cynexus owns authorization, validation, limits, persistence, cancellation, retries, provenance, and truthful failure handling.

The Agent must never claim that a data source was consulted unless a registered capability actually ran and returned evidence. No production success may come from a mock, synthetic response, hard-coded keyword router, placeholder result, or fabricated fallback.

## Architecture Rules

- The implementation lives in `cynexus-ai`.
- `domain` owns business rules, value objects, policies, aggregates, and ports. It must not depend on DTOs, MVC, persistence, MyBatis, Spring AI, LangChain4j, Feign, or infrastructure clients.
- `application` accepts requests, resolves authenticated identity, orchestrates use cases, coordinates transactions, and maps DTOs.
- `infra` implements persistence, provider adapters, Feign clients, capability handlers, migrations, and observability integrations.
- Java-to-service APIs belong under the relevant frontend `src/services/<business-area>` folder, never in `lib/api.ts`.
- AI capability routing must stay synchronized across the capability registry, runtime entry, prompt key, provider route, group, description, input/output schema, source, freshness, and availability.
- All Agent-owned persistence belongs in PostgreSQL schema `cynexus_appl_ai` and follows BIGINT IDs, owner/tenant scope, soft deletion/retention, JSONB bounds, timestamps, and indexed business identifiers.

## Runtime Safety Rules

- Read capabilities can be model-selected only when registered, enabled, available, non-stale, authorized, schema-valid, and within limits.
- Write capabilities are separate from reads and require a user-confirmed, expiring, owner/tenant/conversation/run/tool/digest-bound command.
- Hidden chain-of-thought is never persisted or shown. Persist only bounded public workflow steps, summaries, evidence, and redacted data.
- Cancellation is durable and must survive Redis lease expiry, process restart, disconnects, and late Provider completion.
- SSE reconnect replays persisted events by sequence and never re-runs the Provider or a tool.
- Missing, stale, conflicting, or unavailable data is explicit. The Agent must clarify or report no-data instead of guessing.
- Real-provider Release evidence uses `xtyoo / gpt-5.4-mini`; test providers may validate protocol behavior but cannot close runtime acceptance.

## Delivery Discipline

Each feature is complete only after implementation, focused tests, Release runtime evidence, anti-fabrication audit, synchronized Maven/UI version bump, and an individual commit. Update `task_plan.md`, `progress.md`, and `findings.md` after each feature. Keep unresolved evidence visible as `in_progress` or `pending`.

## Product Completion Areas

The first usable Agent loop is complete. Product completion still requires grounded citations, broader finance/knowledge/application-analysis capabilities, feedback and evaluation, usage/cost/quota routing, confirmed writes, background tasks, durable cancellation/resume/replay, governed dynamic discovery, enterprise security/observability, and browser-verified responsive UI workflows.
