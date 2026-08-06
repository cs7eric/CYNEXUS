---
name: cynexus-agent-tools
description: Use when adding, changing, reviewing, or registering a Cynexus autonomous Agent tool or capability that exposes project data reads, queries, calculations, domain operations, or confirmed writes; also use when adding a foundational API/data operation that should be callable by the Agent. Covers AgentToolHandler metadata and schemas, Spring/runtime registration, governance persistence, tenant and identity boundaries, bounded outputs, citations/freshness, tests, and the separate preview-confirm-audit workflow required for writes.
---

# Cynexus Agent Tool Development

Use this skill whenever a new project read/query method should become available
to the autonomous Agent, or when adding a user-confirmed write action. An Agent
tool is an intentional, bounded domain capability. It is not a generic wrapper
around a controller, repository, mapper, SQL statement, or CRUD service.

## Read First

Read:

1. `AGENTS.md`, `cynexus-ai/AGENTS.md`, and `docs/agent/backend-ddd.md`
2. `.agents/skills/cynexus-ai/SKILL.md` when the tool itself invokes a model or
   adds/changes an AI prompt/provider/binding
3. `docs/agent/agent-prompt-cache.md` when changing tool definition ordering,
   serialization, availability, or the Agent request prefix

Start with CodeGraph. Inspect the owning business method and its impact before
creating the adapter. Then inspect the closest implementation:

- Finance reads: `FinanceOverviewAgentToolHandler` and
  `FinanceTransactionsAgentToolHandler`
- Knowledge reads: `KnowledgeSearchAgentToolHandler` and
  `KnowledgeDocumentAgentToolHandler`
- Market API wrappers: `AbstractMarketAgentToolHandler` and its subclasses
- Market research Feign wrappers: `AbstractMarketResearchAgentToolHandler`
- Direct ClickHouse read with strict SQL/security rules:
  `ClickHouseDailyBarsAgentToolHandler`
- AI-backed tool: `FuturesPositionAiAgentToolHandler`
- Definitions and governance: `AgentToolHandler`, `AgentToolRegistry`,
  `AgentCapabilityDescriptorPolicy`, and `AgentCapabilityRepositoryImpl`
- Confirmed write: `AgentWriteDomainService`, `AgentWritePolicy`, and
  `KnowledgeDocumentWriteExecutor`

## Decide Whether a Tool Is Needed

When adding or changing a foundational business read/query operation, evaluate
Agent exposure as part of the feature. Add an Agent tool when the operation:

- answers a user-recognizable question or supplies reusable evidence;
- has a stable domain/API contract;
- can enforce owner/tenant scope and authorization;
- can be bounded by dates, identifiers, filters, limits, or pagination;
- returns a compact structured result with source and date/freshness metadata;
- is safe for model-directed invocation.

Do not expose:

- raw repositories, MyBatis mappers, arbitrary SQL, file paths, URLs, HTTP
  methods, or generic CRUD endpoints;
- administrative, secret-bearing, cross-tenant, destructive, bulk-export, or
  unbounded search operations;
- an operation whose authorization or domain validation only exists in the UI;
- duplicate tools that differ only in naming while returning the same data.

Prefer one task-level tool over many storage-level tools. For example, expose a
bounded finance overview or published-document search, not separate tools for
every table and mapper method.

## Choose Read or Write Path

Classify the operation before implementation.

### Autonomous read path

Use `AgentToolHandler` and `AgentToolRegistry` only when:

```text
readOnly = true
requiresConfirmation = false
```

The registry intentionally refuses every non-read-only or confirmation-required
handler. These tools are included in the model's tool list and may execute
inside the Agent loop without another user action.

### Confirmed write path

Never register a write as an autonomous `AgentToolHandler`. Use:

```text
preview
  -> persist exact arguments + digest + owner/tenant/principal
  -> issue short-lived confirmation token
  -> user reviews exact payload
  -> confirm or reject
  -> atomically bind token to the pending command
  -> AgentWriteExecutorPort executes the allow-listed action
  -> persist result/error and audit every transition
```

The current supported write is `knowledge.document.create`, and it only creates
a DRAFT. Adding another write requires extending the write policy and executor
dispatch explicitly. Do not replace the tool-code allow-list with reflection,
class names from the database, arbitrary URLs, generic service dispatch, or SQL.

The current write panel is user-initiated and is not injected into the model's
autonomous tool list. Do not describe a confirmed write as model-callable until
the Agent loop has a first-class proposal/confirmation state.

## Implement a Read Tool

### 1. Reuse the owning contract

Use an existing domain service, domain gateway, or read-only Feign API. Add a
purpose-built API DTO/contract in the owning module if no safe contract exists.

- The Agent adapter normally belongs under
  `cynexus-ai-infra/.../infra/agent/<bounded-context>/`.
- Cross-module calls use API/Feign contracts, not another module's repository,
  mapper, PO, controller, or AppService internals.
- A same-module AI/domain computation may be called through its domain service,
  as with `FuturesPositionAiAgentToolHandler`.
- Keep business calculations and access policy in the owning domain. The
  handler adapts arguments/results and does not duplicate business rules.

### 2. Define a stable tool contract

Implement a Spring `@Component` that implements `AgentToolHandler`. Define one
immutable `AgentToolDefinition` with all applicable fields:

- `code`: stable lower-case dotted identifier such as `finance.transactions`
- `name`: short human-readable capability name
- `module`: bounded context such as `finance`, `market`, or `knowledge-base`
- `description`: explain what the tool returns and when it is appropriate
- `guidanceJson`: model-facing selection/interpretation guidance
- `inputSchemaJson`: strict JSON Schema
- `outputSchemaJson`: actual returned JSON contract
- `readOnly = true`
- `requiresConfirmation = false`
- `freshness`: data currency or snapshot semantics
- `source`: owning API/store name
- `sourceUrl`: app-relative route only, never an external/untrusted URL
- `version`: set or preserve a semantic version when the contract changes
- `tenantScope`: normally `TENANT`
- `availability`: normally `AVAILABLE`
- `priority`: deterministic tool ordering/selection priority
- `promptKey`: normally `autonomous_agent`
- `runtimeEntry`: omit to use the concrete handler class name unless an
  existing handler contract deliberately sets it
- `routeGroup`: `Agent / <bounded-context>` or an established group

The default `capabilityDescriptor()` maps the definition into governed runtime
metadata and uses `registrationMode = HANDLER`.

### 3. Write strict schemas

Put shared schemas in an owning `*AgentToolSchemas` class. Schemas are a runtime
security boundary, not documentation only.

Input schema requirements:

- root type is `object`;
- use `required` only for genuinely mandatory inputs;
- declare every allowed property and set `additionalProperties: false`;
- use exact types; do not accept numeric/date/boolean strings as substitutes;
- apply `minLength`, `maxLength`, `minimum`, `maximum`, `enum`, `maxItems`, and
  date/date-time formats;
- bound every result-size control;
- describe IDs, symbols, dates, units, defaults, and null semantics.

The registry currently enforces a maximum serialized argument size of 16,000
characters and schema depth of 8. Design well below those limits.

Output schema requirements:

- match the actual serialized DTO/output exactly;
- identify required fields and nullable values;
- include date, source, unit/currency, pagination/truncation, and status fields
  when applicable;
- avoid returning secrets, internal errors, raw provider responses, hidden
  reasoning, unnecessary PII, huge text bodies, or full unbounded histories.

If the schema changes incompatibly, bump the tool/capability version and add
contract tests. Tool definitions affect the Agent prompt-cache prefix, so keep
ordering and serialization deterministic.

### 4. Add model guidance

Add concise guidance to `AgentToolGuidanceCatalog` or the owning guidance class.
Use JSON that tells the model:

- when to use and when not to use the tool;
- required evidence and suitable follow-up tools;
- date/freshness/source interpretation;
- limits, units, unknown/null behavior, and common confusion points;
- whether the result is factual input rather than a trading/business decision.

Do not hide essential validation in prose. Anything safety-critical must also
be enforced by schema or code.

### 5. Execute safely

In `execute(call, context)`:

- accept only schema-declared arguments;
- apply the same bounds again at the owning API/domain boundary;
- use `AgentExecutionContext` for owner, tenant, conversation, run, workspace,
  locale, and trade date when relevant;
- never use caller-provided tenant/owner identifiers to override the trusted
  context;
- distinguish no data from an infrastructure failure;
- return `AgentToolResult.success` with valid JSON, a compact summary,
  `dataDate`, source, warnings, citations, and safe `sourceUrl` where available;
- return stable warning codes through `AgentToolResult.failure`, such as
  `ARGUMENT_REQUIRED`, `ARGUMENT_INVALID`, `ARGUMENT_FORMAT:<field>`, `NO_DATA`,
  or a domain-specific safe code;
- do not leak stack traces, credentials, SQL, internal hostnames, or raw
  exception details to the model;
- bound fallback-date searches, retries, rows, payload size, and text length.

Use direct database access only when an owning API/gateway cannot provide the
contract and the implementation has strict parameterization, table/column
allow-lists, read-only credentials, timeout/row limits, tenant filtering, and
security tests. `ClickHouseDailyBarsAgentToolHandler` is an exception pattern,
not the default.

### 6. Register through Spring and governance

`AgentToolRegistry` receives every Spring `AgentToolHandler` automatically.
Do not manually add the handler to a static Java list.

At startup, `AgentCapabilityRepositoryImpl.synchronize` persists the handler
descriptor in `cynexus_appl_ai.ai_agent_capability`, preserves governance
choices, retires older/missing handler versions as stale, and writes capability
audit records. A runtime capability is visible only when all conditions hold:

```text
handler exists in the runtime allow-list
AND is_enabled = true
AND availability = AVAILABLE
AND governance_availability = AVAILABLE
AND stale_at is null or in the future
AND runtimeEntry and input/output schemas match the handler
AND readOnly = true
AND requiresConfirmation = false
```

Admin hot-add is metadata/alias registration only. It must bind to an existing
allow-listed Spring handler with identical schemas and safety attributes. It
cannot load arbitrary classes, URLs, scripts, SQL, or remote tools.

Verify the capability appears in the AI Provider page's Agent tools routing
view with the correct group, runtime entry, source, freshness, availability,
read-only state, priority, and governance controls.

## Implement a Confirmed Write

Use the existing write lifecycle; do not add the write to the autonomous tool
catalog.

If the product requires the model to propose a write, first extend the Agent
execution contract with a non-executing write proposal path:

```text
model emits a typed write proposal
  -> server validates the allow-listed tool code and arguments
  -> server persists a preview bound to owner/tenant/run
  -> Run emits NEEDS_CONFIRMATION and stops tool execution
  -> UI displays the exact persisted payload
  -> user confirms or rejects through the write-command endpoint
  -> confirmed executor performs the operation once
```

This requires domain state, API/event DTOs, UI state, resume/finalization rules,
identity checks, audit, and replay/idempotency tests. Never make a mutating
handler appear autonomous by setting `readOnly = true`, and never let the model
submit replacement arguments during confirmation.

### 1. Extend domain policy explicitly

Update `AgentWritePolicy` with a named tool-code allow-list/dispatcher and
operation-specific validation. Enforce:

- exact allowed fields and payload size;
- owner/tenant/principal scope;
- safe target status and transition rules;
- no arbitrary IDs, paths, URLs, SQL, service names, or class names;
- idempotency or duplicate protection where the operation can be retried;
- a short confirmation lifetime.

Keep the argument digest tied to both tool code and canonical exact arguments.
The confirmation token must be random, stored only as a hash, single-use,
owner-bound, expiry-checked, and atomically consumed.

### 2. Add an executor adapter

Implement/extend `AgentWriteExecutorPort` in `infra`. Dispatch by an explicit
tool code and call a safe owning API/domain command. Return a bounded
`AgentWriteResult`; do not expose raw exceptions.

The current knowledge write forces `DRAFT` regardless of model/user payload.
Apply equivalent conservative state restrictions to every new write.

### 3. Preserve preview and audit

Preview must store the exact reviewed arguments, digest, identity,
conversation/run linkage, status, and expiry. Confirmation must execute the
persisted payload, never replacement arguments submitted at confirmation time.

Audit `PREVIEW`, `CONFIRM`, `REJECT`, and `EXECUTE` with status, tool code,
digest, bounded result/error, owner, and timestamps. The UI must show the exact
payload and require an explicit user action before confirmation.

## Required Tests

For every read tool, add focused tests for:

- successful owning API/domain call and JSON serialization;
- required arguments, types, formats, enum/bounds, unknown properties, and
  oversized input;
- bounded limits and date-range behavior;
- no-data and downstream failure behavior;
- owner/tenant scope and sensitive-data redaction when applicable;
- output source, `dataDate`, warnings/citations, and schema compatibility;
- read-only/no-confirmation metadata.

Update `AgentToolDefinitionContractTest` so the new handler participates in the
definition/schema contract suite if that test uses an explicit handler list.
Run `AgentToolRegistryTest` for registry behavior and add a handler-specific
test similar to `CapabilityAgentToolHandlerTest`. Add direct SQL security tests
for any database-backed exception.

For every write, test:

- unsupported tool codes and malformed/oversized arguments are rejected;
- unsafe state transitions are impossible;
- preview persists exact canonical arguments and issues an expiry/token;
- wrong owner, wrong token, expired token, replay, double confirm, and
  confirm-after-reject do not execute;
- confirmation executes only the persisted payload once;
- downstream failure is safely recorded;
- every lifecycle transition is audited;
- the UI cannot execute without explicit confirmation.

Also run Agent workflow/evaluation tests when adding a tool changes selection
behavior, expected tool sets, tool budget, or prompt-cache keys.

## Completion Checklist

- [ ] The owning business read/write API exists and preserves DDD boundaries.
- [ ] Agent exposure was deliberately accepted or explicitly documented as
      unsuitable.
- [ ] Tool code, description, guidance, schemas, source, freshness, route group,
      version, and priority are complete.
- [ ] The operation is bounded, tenant/owner-safe, and returns compact JSON.
- [ ] Read tools are autonomous read-only handlers; writes use preview/confirm.
- [ ] Spring discovery and governed runtime synchronization are verified.
- [ ] The AI Provider Agent tools view shows the capability and governance state.
- [ ] Handler, schema, registry, security, and workflow tests pass.
- [ ] Agent cache/version behavior was reviewed when definitions changed.
- [ ] Functional changes synchronize Maven/UI versions and pass the required
      versioned Release verification.

Do not call the task complete because a handler bean exists. The tool must be
model-visible, governed, schema-valid, safely executable, observable, tested,
and discoverable in the Provider page. A write must additionally be previewed,
explicitly confirmed, identity-bound, single-use, and audited.
