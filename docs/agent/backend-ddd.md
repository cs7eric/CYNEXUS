# Java DDD Layering Rules

These rules are mandatory for Java service changes.

## Responsibilities

| Layer | Owns | Must not own |
| --- | --- | --- |
| `api` | Request/Response DTO, Feign contracts | Business rules, repositories |
| `application` | Orchestration, transaction boundary, DTO <-> domain mapping | Algorithms, repository access, prompt rendering, BO assembly |
| `domain` | Entities/BO/VO, policies, domain services, ports/gateways | API DTO, Spring MVC, MyBatis PO/mapper, Feign clients |
| `infra` | Port implementations, persistence, external clients | Business decisions that belong in domain |
| `starter` | Assembly/boot wiring | Business logic |

## Canonical Flow

```text
Controller / Request DTO
  -> AppService (orchestration only)
    -> DomainService / DomainPolicy (business meaning)
      -> Domain BO / ValueObject
    <- DomainService returns BO/VO
  -> AppService maps BO/VO to Response DTO
<- Controller returns DTO
```

Application Service may see BO only as the return value of a domain call for DTO mapping. It must not construct business BO graphs, keep long-lived BO state, or mutate BO fields with business meaning. Prefer feature-specific domain services over god AppServices.

## Forbidden in Application

- Repository or port access to decide business outcomes.
- Hand-built BO workflows that fill or update business fields as business steps.
- Prompt template loading, binding selection, or `{{var}}` rendering.
- Market/finance scoring, ranking, scheduling policy, parsing, normalization, or tool-budget decisions.
- Long run-loop rules such as answer normalization, clarification parsing, cost estimation, history collapse, and redaction policy.
- New public APIs returning BO instead of DTO. Existing thin CRUD pass-through BO APIs are transitional and must not be copied.
- Imports from `infra`, MyBatis, Feign clients, or Spring AI adapters.

## Required in Domain

- Repository and gateway ports.
- Prompt resolution and fallback.
- Validation, normalization, scoring, routing, and parsing.
- Aggregate state transitions and run lifecycle rules.
- Pure policies under `domain/policy/**` and use-case services under `domain/service/**` or feature folders.

Domain must not depend on `api`, `application`, `infra`, MVC, MyBatis, Feign, or Spring AI. It may depend on domain and module common code only. Infrastructure implements domain ports.

## BO Clarification

BO is a domain object, not an application DTO. Domain returns BO/VO to Application Service, which maps it to an API response DTO. Application may build a simple BO shell from request fields only as input to a domain command; it must not decide business meaning while assembling it.

## Completion Checklist

- [ ] Business rules live in domain policy/service.
- [ ] AppService only orchestrates and maps DTOs.
- [ ] No new repository dependency exists in AppService.
- [ ] No new BO-returning public API was added.
- [ ] Prompt work follows `ai-capability.md`.
- [ ] Focused domain tests cover changed behavior.
- [ ] Maven and UI versions are synchronized after a functional change.

## Existing Guidance

Good domain ownership includes `AgentSystemPromptDomainService`, `AgentSystemPromptPolicy`, `ScenarioExecutionDomainService.prepare*/finalize*`, `PromptRenderPolicy`, `AgentFinalAnswerPolicy`, and `AgentHistoryCollapsePolicy`.

Good thin AppServices include `AgentFeedbackAppService`, `MediaTranscriptionAiAppService`, `ModelRouteAppService`, and `ScenarioExecutionAppService` after the prepare/finalize split. Do not copy the historical god AppServices, especially the still-being-thinned autonomous-agent and scheduler paths.

When refactoring shared logic, prioritize: AI agent/scenario/prompt paths; core scheduler/media/finance capture; market query aggregation; knowledgebase.

`domain/service` is not a catch-all package. Group domain code under a feature folder such as `domain/applicationanalysis`, `domain/marketinformation`, `domain/scheduler`, `domain/stock`, or `domain/port`; keep `service` and `impl` subfolders inside that feature only when the feature has service contracts and implementations.
