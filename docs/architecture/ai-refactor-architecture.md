# Cynexus AI Architecture and Refactoring Baseline

> This document is the implementation contract for the AI refactor. Before changing an AI capability, verify that the change preserves the ownership, identifiers, security boundaries, and verification gates below.

## 1. Goals and boundaries

The target combines Cynexus governance with Spring AI, LangChain4j, and AgentScope. The frameworks must not own the same concern or the same Agent Run state.

Goals:

- Keep Cynexus responsible for business meaning, routing policy, permissions, persistence, and audit.
- Use Spring AI for model, vision, embedding, document-reader, vector-store, and RAG primitives.
- Use LangChain4j for typed Java AI Services and reusable Tool/Memory composition where it reduces code.
- Use AgentScope only for isolated complex multi-agent orchestration behind a Cynexus port.
- Preserve public APIs, DB Prompt routing, Provider fallback, telemetry, and Agent write confirmation.
- Migrate capability by capability with rollbackable adapters and equivalent tests.

Non-goals:

- Do not rewrite Domain Services around framework annotations.
- Do not let frameworks read Cynexus repositories or write business tables directly.
- Do not let a framework replace `ai_usage_bindings` as the route source.
- Do not run two Agent state machines for one Run.
- Do not remove the custom adapter before production evidence exists.

## 2. Current implementation

### 2.1 Modules

```text
cynexus-ai-api       request/response DTOs and Feign contracts
cynexus-ai-domain    capabilities, policies, BO/VO, repositories, ports
cynexus-ai-application controllers, orchestration, DTO mapping
cynexus-ai-infra     persistence, HTTP adapters, Langfuse and Spring wiring
cynexus-core         media lifecycle and knowledge-base write orchestration
cynexus-knowledgebase knowledge document API and persistence
cynexus-ui           capability metadata and configuration/user flows
```

### 2.2 Active model call

```text
Controller -> Application Service -> Domain Service
  -> Completion Port -> AiCompletionGatewayImpl
  -> DynamicChatClientFactory -> AiModelAdapterRegistry
  -> OpenAiCompatibleChatAdapter -> Java HttpClient
  -> Chat Completions / Responses API
```

`OpenAiCompatibleChatAdapter` currently owns endpoint selection, secret resolution, timeout, JSON mode, Responses API, image payloads, Agent HTTP/SSE/WebSocket, retry, response extraction, and token extraction. Spring AI and LangChain4j dependencies are declared, but production code does not currently use `ChatClient`, `ChatModel`, LangChain4j `AiServices`, or AgentScope.

### 2.3 Configuration model

| Object | Owner and meaning |
|---|---|
| `ai_providers` | Provider code/type, base URL, secret reference, default model, enabled state, model pricing/config |
| `ai_prompt` | Prompt key/version/status, system prompt, user template, variables, output schema, tags and remarks |
| `ai_usage_bindings` | Capability to Provider/model/Prompt binding, enabled state and runtime parameters |
| `ai_execution_log` | Attempt, capability, Prompt/Provider/model, status, token counts, latency, bounded previews and error |

Stable identity:

```text
backend capability
  = ai_usage_bindings.capability
  = ai_prompt.prompt_key (normally)
  = execution_log.capability
  = UI AI_CAPABILITIES.code / prompt_key
```

Legacy aliases must be explicitly documented. Persisted capability names cannot be casually renamed.

### 2.4 Route resolution

`ApplicationAnalysisRoutingDomainServiceImpl` currently resolves:

```text
enabled binding
  -> enabled bound Provider
  -> preferred model
  -> binding.model_override
  -> params.fallback_chain / fallback_models
  -> Provider default model
  -> optional fallback Provider codes/models
  -> AiModelBO list
```

Provider `extra` and binding `params` are merged into the routed model. They carry `wire_api`, response format, reasoning effort, timeout, Responses state, streaming, and cache options. This policy remains Cynexus-owned.

### 2.5 Prompt resolution

Intended order:

```text
binding.prompt_id -> active ai_prompt
  -> active prompt by prompt_key and optional prompt_version
  -> behavior-preserving Domain fallback
```

The implementation is currently duplicated across Domain Services, with inconsistent rendering and structured parsing. This is the first refactoring target.

### 2.6 Agent runtime

```text
AutonomousAgentController -> Agent Application Service
  -> AgentRunDomainService / AgentTurnDomainService
  -> system prompt + runtime context
  -> AgentToolRegistry -> model turn -> tool result -> next turn
  -> Run / Step / Message persistence
```

The native runtime owns identity/tenant boundaries, quotas, cancellation, retry, stale-run recovery, SSE, Prompt Cache, history replay, tool schemas, and write-command preview/confirmation/audit. These are governance capabilities and must remain outside a generic framework.

## 3. Capability families and contracts

Current UI metadata describes Media (`media_transcription_enrich`, legacy polish/summary/metadata/QA), Finance vision, Portfolio screenshot parsing/review, Workspace/Agent, Stock/Market analysis, Market Information, and Knowledge Base generation. Every capability needs an explicit input boundary, output contract, fallback policy, and persistence owner.

All structured capabilities must define:

- stable capability and Prompt key;
- Prompt version and DB output schema;
- bounded input size and allowed variables;
- JSON extraction, field bounds, enum filtering and normalization;
- malformed/empty output behavior and bounded repair;
- Provider/model fallback and execution logging.

## 4. MP4 vertical slice

### 4.1 Automatic process

```text
upload MP4 -> Python transcription -> transcript persistence
  -> non-empty validation -> phase ai_enriching
  -> MediaTranscriptionAiClient.enrich
  -> structured MediaTranscriptionAiResultBO
  -> normalize and validate
  -> history (transcript/polished/summary/metadata)
  -> phase knowledge_base -> create knowledge draft
  -> store knowledgeDocumentId or bounded knowledgeError
  -> completed process
```

`media_transcription_enrich` should be one model request returning:

```json
{"polishedText":"","summary":"","shortSummary":"","keyPoints":[],"topics":[],"methodology":[],"actionItems":[],"risks":[],"title":"","categories":[],"tags":[]}
```

Categories are derived from content, such as review, trading technique, investment method, market analysis, industry/company research, macro policy, study note, or other. AI proposes fields; Core/Knowledge Base owns the draft transaction.

### 4.2 Compatibility APIs

```text
POST /api/ai/media-transcription/enrich
POST /api/ai/media-transcription/polish
POST /api/ai/media-transcription/summary
POST /api/ai/media-transcription/metadata
POST /api/ai/media-transcription/ask
```

`/enrich` is the automatic pipeline. The other endpoints remain explicit reprocessing/compatibility paths and must not become a second automatic pipeline.

## 5. Target architecture

```text
UI / REST / Feign
       |
Cynexus Application + Domain
 capability / Prompt / route / policy / identity / tenant
 validation / persistence / audit / confirmation / quota
       |                         |
 ordinary capability            Agent
       |                         |
LangChain4j AI Service   Native Cynexus Agent OR AgentScope
       |                         |
       +----------+--------------+
                  |
          Spring AI Runtime
       Chat / Vision / JSON / RAG
                  |
          routed Provider models
```

### 5.1 Cynexus Domain layer

Add framework-neutral ports/value objects:

```java
AiPromptRuntimeResolver
AiModelExecutor
AiStructuredOutputParser
AgentOrchestratorPort
```

Domain code must not import Spring AI, LangChain4j, AgentScope, MVC, Feign, MyBatis, or provider SDKs.

### 5.2 Spring AI layer

Infrastructure provides a model registry/executor keyed by routed `AiModelBO` and supports text/vision, structured output options, provider `extra`, timeout/retry, usage metadata, and trace correlation. Spring AI is an execution primitive, not the capability router or business persistence layer.

### 5.3 LangChain4j layer

Use typed services such as:

```java
interface MediaTranscriptAnalyzer {
    MediaTranscriptResult analyze(MediaTranscriptInput input);
}
```

LangChain4j may provide AI Service, Tool, Memory and POJO mapping. It receives a Cynexus-selected model/executor and cannot load repositories or commit writes.

### 5.4 AgentScope layer

Add infrastructure/application implementation:

```java
interface AgentOrchestratorPort {
    AgentRunResult execute(AgentRunCommand command);
}
```

Implement `NativeCynexusAgentOrchestrator` and `AgentScopeOrchestrator`. Simple runs stay native. AgentScope is only for explicitly classified multi-agent tasks, calls a bounded Cynexus Tool Gateway, and cannot access databases or bypass confirmation/audit.

## 6. Responsibility matrix

| Concern | Cynexus | Spring AI | LangChain4j | AgentScope |
|---|---|---|---|---|
| Capability identity | owner | none | none | none |
| Provider/model route | DB policy owner | execute selected model | use selected model | use selected model |
| Prompt version/fallback | owner | message rendering | service template | system prompt/context |
| HTTP/provider protocol | policy/options | client implementation | delegates | delegates |
| Structured output | business validation | conversion primitive | typed wrapper | result wrapper |
| RAG/embedding | permissions/metadata | reader, embedding, vector store | retrieval composition | access via tools |
| Tool safety | allow-list/schema/identity | transport primitive | wrapper | orchestration only |
| Agent Run state | owner | none | none | orchestration events |
| Write confirmation/audit | owner | none | none | must call boundary |
| Token/cost/logging | owner | usage metadata | listener metadata | orchestration metadata |

## 7. Required changes

### P0: Internal contracts

1. Centralize binding -> Prompt -> fallback -> variable validation/rendering in `AiPromptRuntimeResolver`.
2. Centralize model execution behind `AiModelExecutor`.
3. Centralize JSON extraction, schema checks, enum filtering, bounds and bounded repair.
4. Define `AiExecutionContext` with capability, Prompt key/version, Provider/model, request, tenant/principal and trace identifiers.
5. Add contract tests for binding precedence, missing Prompt, disabled Provider, fallback, malformed JSON, and telemetry.

### P1: Spring AI pilot

Migrate `media_transcription_enrich` first while preserving request/response DTOs, category/title rules, knowledge draft behavior, fallback order and logs. Then migrate text and vision paths.

### P1/P2: LangChain4j services

Wrap MP4 enrichment, knowledge generation, market information, factor analysis and futures analysis as typed AI Services backed by the Spring AI executor. Keep business parsing and persistence in Domain.

### P2: Spring AI RAG

Use readers, chunking, embedding, PGVector/Redis and retrieval for knowledge search. Keep document tables, status, categories, tags, permissions, deduplication, versions and transactions in Cynexus.

### P2: AgentScope

Start with one bounded market-research workflow. Map AgentScope events into Cynexus Run/Step/Message records. Preserve cancellation, quota, authorization, and `preview -> confirmation -> execute -> audit`.

### P3: Cleanup

Only after equivalent tests and Release verification, remove duplicate HTTP/protocol, Prompt rendering and JSON repair code. Remove unused dependencies only when confirmed unused.

## 8. Configuration and migration rules

Every migrated capability requires an idempotent migration with active Prompt, explicit version, variables, output schema, tags/extra/remark, enabled binding, Provider ID, Prompt ID, model override when needed, and fallback/runtime params. Provider code/type must resolve to a registered runtime adapter/factory; a database row cannot invent a Provider.

The UI catalog must match capability code, Prompt key, runtime entry, group, description and `reads_binding`. Missing/disabled bindings must be visible as configuration attention.

## 9. Failure, security and observability

Treat transcript, user text, retrieved documents, tool output and model output as untrusted. Enforce tenant/principal scope before model and tool execution. Read tools are bounded; model-proposed mutations become `NEEDS_CONFIRMATION` previews.

Each attempt records capability, Prompt key/version, Provider/model, status, token counts, latency, bounded previews, error and trace/execution number. Fallback is explicit. Repair is bounded and retains the original error. Streaming and buffered Agent paths must be equally observable. Secrets and unbounded transcript content must never enter logs.

## 10. Verification gates

Per capability:

- identifiers and Prompt binding agree;
- migration is idempotent;
- Domain remains framework-neutral;
- primary and fallback routes work;
- Prompt variables/schema and malformed/empty/oversized responses are tested;
- execution logs/Langfuse contain model, token, latency and error data;
- API and persistence behavior remain compatible.

Project-wide:

- focused Domain/Application tests;
- affected Maven compile and UI checks;
- Maven/UI versions synchronized;
- affected Release services rebuilt and health-checked;
- rollback adapter retained until migration closes.

## 11. Migration phases and deliverables

1. **Internal contracts:** Resolver, Executor, Structured Output, Execution Context, Orchestrator ports and contract tests.
2. **Spring AI pilot:** Spring model registry/executor and MP4 enrichment migration with equivalent logs/fallbacks.
3. **LangChain4j services:** typed services for stable capabilities.
4. **Spring AI RAG:** permission-aware document/vector pipeline.
5. **AgentScope:** isolated multi-agent adapter and one measured use case.
6. **Cleanup/release:** remove duplication, clean dependencies, verify versions, deploy and document rollback.

## 12. Anti-drift checklist

Before and after each change:

- Cynexus still owns capability, route, Prompt, identity, validation, persistence and audit.
- Routing still starts from `ai_usage_bindings`.
- Prompt key/version/schema are DB-managed and observable.
- Domain has no framework/infrastructure imports.
- Frameworks cannot write business tables directly.
- AgentScope is isolated from the native Run state machine.
- Writes remain previewed, explicitly confirmed, executed and audited.
- Fallback, timeout, malformed, empty and oversized cases are explicit.
- Streaming and non-streaming telemetry are equivalent.
- Public APIs remain compatible or are intentionally versioned.
- Focused tests and applicable Release checks are complete.

Any “no” requires an exception recorded here before implementation continues.
