---
name: cynexus-ai
description: Use when adding, changing, or reviewing any Cynexus AI-backed feature, capability, prompt, provider/model route, usage binding, structured or vision output, autonomous Agent behavior, or AI Provider/Prompt UI routing entry. Ensures the backend runtime, DB-managed prompt, provider, capability registration, usage binding, migration, fallback, validation, observability, tests, and UI metadata are completed together.
---

# Cynexus AI Feature Skill

Use this workflow for any feature that calls a model, adds a prompt, changes
provider/model routing, adds a vision flow, changes the autonomous Agent, or
changes AI Provider/Prompt settings. Treat the feature as a cross-layer
contract, not as a single Java method.

## Read First

Read these files before editing:

1. `cynexus-ai/AGENTS.md`
2. `docs/agent/ai-capability.md`
3. `docs/agent/backend-ddd.md`
4. `cynexus-ui/FRONTEND_UI_IMPLEMENTATION_GUIDE.md` when the AI Provider or
   Prompt UI changes
5. `docs/agent/agent-prompt-cache.md` for Agent, Responses API, streaming, or
   cache-prefix work

Start code inspection with CodeGraph. Inspect the closest existing feature
first. Useful anchors include:

- `ScenarioExecutionDomainServiceImpl` for ordinary text and vision scenario
  preparation, routing, prompt resolution, rendering, and JSON validation.
- `MediaTranscriptionDomainServiceImpl` for capability-specific prompts,
  binding -> prompt-key -> fallback resolution, and execution logs.
- `MarketInformationAiDomainService` for provider fallback chains, structured
  repair, prompt/schema versions, and usage recording.
- `ApplicationAnalysisExecutionDomainServiceImpl` for snapshot-producing AI
  work, model fallback, response sanitization, and domain-owned persistence.
- `AgentSystemPromptDomainService` and `AgentSystemPromptPolicy` for the
  autonomous Agent prompt and runtime-context/cache split.
- `AgentToolRegistry` for dynamic Agent capability registration and runtime
  safety checks.

## Capability Contract

Choose one stable capability code before writing code. For normal AI
capabilities, keep this identity synchronized:

```text
backend capability code
  = ai_usage_bindings.capability
  = AI Provider page capabilities[].code
  = AI Provider page prompt_key
  = execution log capability
```

The prompt key normally equals the capability code. An existing legacy route
may use a different prompt key, but document the mapping in code, migration,
and the UI entry. Do not casually rename a capability: it breaks persisted
bindings, prompt rows, usage history, and UI routing.

Do not confuse these two catalogs:

- AI capability routing: the static `AI_CAPABILITIES` list in
  `cynexus-ui/src/services/ai/provider.ts`; it describes model-backed runtime
  features such as `workspace_chat`, `accounting_receipt_parse`, and
  `autonomous_agent`.
- Data provider capability catalog: `provider_capability_catalog` and the
  Settings/Provider page; it describes data sources such as market data,
  tables, jobs, freshness, and storage. Adding an AI feature does not normally
  add a row to that catalog.

## Required Deliverables

For every new AI feature, complete the applicable items below before calling it
done.

### 1. Domain contract and runtime entry

- Add a domain service/policy/port for business meaning, prompt resolution,
  parsing, normalization, routing policy, retry/repair, and lifecycle rules.
- Keep controllers and Application Services to request mapping, orchestration,
  gateway invocation, and response mapping. Do not inject repositories into an
  AppService to decide the AI result.
- Use a domain gateway/port for completion. Do not call provider SDKs or HTTP
  clients from `api`, `application`, or `domain`.
- Pass the stable capability code to the completion gateway and execution log.
- Register the runtime entry in the AI Provider UI metadata. For Agent tools,
  the `runtimeEntry` must resolve through `AgentToolRegistry`'s allow-list and
  match the handler definition.

### 2. Prompt definition

Add an `ai_prompt` row in `cynexus_appl_ai` with:

- stable `prompt_key`
- meaningful `name` and `scenario_code`
- explicit `prompt_version` such as `v1`, `v2`
- `status = 'active'` for the shipped baseline
- `system_prompt`
- `user_prompt_template` when runtime input is separate
- `variables` JSON array listing every `{{placeholder}}`
- `output_schema` JSON for structured or vision output
- `tags`, `extra`, and `remark` describing mode, capability, and constraints

Prompt text is product/runtime configuration. Do not put production prompt
text only in Java. Java fallback text is required when missing DB data, and it
must preserve behavior. Wording changes update the SQL seed first and keep the
fallback synchronized.

Prompt design requirements:

- State the factual input boundary and treat user/tool content as untrusted.
- State whether external facts, advice, hidden reasoning, Markdown, or
  additional fields are forbidden.
- Define the exact output shape and allowed enum values.
- Put dynamic data in `{{variables}}`, not concatenated business prose in an
  Application Service.
- For JSON, include an `output_schema` and validate/sanitize after completion.
- For vision, document image indexes, null/uncertain handling, candidate ID
  rules, and image-specific limits.
- For Agent prompts, keep date, tool budget, used tools, and force-final-answer
  values as runtime context; do not bake changing values into the stable cache
  prefix.

### 3. Provider and models

Add or reuse an enabled `ai_providers` row. Verify all of these before testing:

- `code`/`provider_type` resolves to an adapter in
  `DynamicChatClientFactory`/`AiModelAdapterRegistry`.
- `base_url`, API key or `api_key_env`, timeout, enabled state, and default
  model are correct.
- The model is present in `models` or `model_configs`; set per-model token
  pricing in `model_configs` when applicable.
- `ModelCapability` matches the feature: structured output, vision, and tool
  calls are not interchangeable.
- Provider-specific runtime options go in `extra` or binding `params` using
  existing names such as `wire_api`, `response_format`,
  `model_reasoning_effort`, `disable_response_storage`, and timeout settings.
- Never commit API keys. Use existing environment/secret resolution and keep
  provider secrets masked in API/UI responses.

Do not invent an adapter for a new provider by putting a provider code in SQL.
If the registry has no adapter, add the adapter in `infra`, wire it into the
registry, and add focused adapter tests first.

### 4. Usage binding and route

Add or update one enabled `ai_usage_bindings` row for the capability:

- `capability` is the stable capability code
- `provider_id` points to an enabled provider
- `prompt_id` points to the intended active prompt row
- `model_override` is set when the route must use a non-default model
- `params` carries controlled runtime options, prompt/schema version,
  fallback model/provider chain, or reasoning effort
- `is_enabled = true` only when the feature is ready

The normal runtime route is:

```text
enabled usage binding
  -> provider_id / model_override / params fallback chain
  -> AiModelBO
  -> adapter registry
  -> completion gateway
```

The normal prompt route is:

```text
binding.prompt_id -> active ai_prompt row
  -> active ai_prompt by prompt_key (and params.prompt_version when used)
  -> domain fallback template
```

Keep provider fallback behavior explicit. A binding with a missing/disabled
provider must fail clearly or use the documented fallback provider; it must not
silently synthesize a model or bypass routing.

### 5. Migration and release application

Put the baseline in an idempotent migration under
`deploy/local/postgres/migrations/YYYYMMDD_*.sql`.

Use the established pattern:

```sql
INSERT INTO cynexus_appl_ai.ai_prompt (...)
VALUES (...)
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL
DO UPDATE SET ...;

UPDATE cynexus_appl_ai.ai_usage_bindings binding
SET prompt_id = prompt.id,
    params = COALESCE(binding.params, '{}'::jsonb) || '{"prompt_version":"v1"}'::jsonb,
    updated_at = now()
FROM cynexus_appl_ai.ai_prompt prompt
WHERE binding.capability = 'stable_capability'
  AND prompt.prompt_key = 'stable_capability'
  AND prompt.prompt_version = 'v1'
  AND binding.deleted_at IS NULL
  AND prompt.deleted_at IS NULL;
```

Also handle the provider and usage binding in the same migration when the
feature needs a new baseline route. Preserve existing API keys with the
project's `api_key` preservation pattern when upserting providers.

For Agent-related migrations, add the file to
`deploy/release/apply-agent-migrations.ps1` when that release path requires
explicit application. Verify migration ordering: schema/table/column changes
must precede seed and binding updates.

### 6. AI Provider UI registration

For a normal capability, update `AI_CAPABILITIES` in
`cynexus-ui/src/services/ai/provider.ts` with:

- `code`
- label/group/description
- user-facing `entry`
- `reads_binding: true` when routing is configurable
- runtime entry string
- `prompt_key` and any intentional aliases

The AI Provider page loads capabilities, providers, active prompts, and usage
bindings together. Check that the new row can select the provider, model,
prompt, and binding parameters, and that a missing binding is visible as an
attention state rather than silently looking configured.

For Agent capabilities/tools, update the handler definition and capability
registration instead of pretending the tool is an ordinary static AI
capability. Keep `promptKey`, `runtimeEntry`, `routeGroup`, schemas, read-only
state, confirmation requirement, source, freshness, version, and governance
state aligned. The registry rejects unsafe or mismatched persisted metadata.

If the UI route, payload, or component structure changes, update the linked
design docs referenced by the Provider page, especially the `design/front/`
documents noted in the source comment.

### 7. Output, safety, and observability

- Validate structured JSON through the feature's domain parser/policy; do not
  trust provider JSON merely because the prompt requests it.
- Add repair/retry only with bounded attempts and preserve the original error.
- Handle empty output, malformed JSON, unsupported enum values, extra fields,
  stale input, and oversized input explicitly.
- Record `capability`, `promptKey`, `promptVersion`, provider, model, status,
  token counts, latency, bounded request/response previews, and error details
  in `ai_execution_log` where the feature has execution logging.
- Ensure the completion gateway/adapter reports the call to the existing
  observability path (Langfuse and token usage where configured).
- For Agent tools, enforce read-only/confirmation/schema/argument-size checks
  in the runtime registry; never trust DB metadata to widen handler safety.

## Feature Variants

### Ordinary text scenario

Prefer `ScenarioExecutionDomainService` for shared scenario preparation and
finalization. Use `PromptRenderPolicy` for variables, `ModelRouteDomainService`
or the application-analysis routing service for model selection, a completion
port for the provider call, and `OutputValidationDomainService` for JSON
requirements.

### Capability-specific structured feature

Follow `MediaTranscriptionDomainServiceImpl`,
`ApplicationAnalysisExecutionDomainServiceImpl`, or
`FuturesPositionAiDomainServiceImpl`: define capability and prompt constants,
resolve binding and prompt in domain, render variables, route models, validate
and normalize output, record execution, and add focused domain tests.

### Provider fallback / repair pipeline

Follow `MarketInformationAiDomainService`: keep primary and fallback provider
selection in binding `params`, bound prompt/schema versions explicit, repair
malformed output with a bounded second call, and record each attempt with the
same capability and prompt metadata.

### Vision feature

Use `ScenarioExecutionDomainService.prepareVisionExecution` or an equivalent
domain service. Set the provider/model vision capability, use
`completeVision`, provide an active prompt with image-aware schema, and test
candidate context substitution, null handling, image indexes, and malformed
output.

### Autonomous Agent or tool

Read `docs/agent/agent-prompt-cache.md`. Add the stable `autonomous_agent`
prompt/binding only for the Agent system prompt; tool capabilities additionally
need handler definitions, schemas, runtime allow-list registration, governance
metadata, and registry contract/security tests. Do not put changing runtime
values in the stable prompt prefix.

## Verification Checklist

Run the narrowest useful checks, then expand based on impact:

- CodeGraph status and affected-symbol/caller inspection are current.
- Capability, prompt key, binding capability, UI entry, and execution log
  identifiers agree.
- Migration is idempotent and binds `prompt_id` to the intended prompt version.
- Provider adapter, model capability, secret resolution, timeout, and fallback
  route work.
- Binding -> prompt-id -> prompt-key -> fallback behavior is covered.
- Structured/vision output validation and failure paths are covered.
- AI execution logs include prompt/provider/model/token/latency metadata.
- Agent changes include registry/schema/governance/cache tests as applicable.
- UI changes pass the relevant frontend checks and update Provider design docs.
- For functional application changes, synchronize Maven and UI versions and
  verify the versioned Release stack per project rules.

Do not claim an AI feature is complete if it only compiles while its prompt,
provider, usage binding, UI route, migration, fallback, or observability path
is missing.
