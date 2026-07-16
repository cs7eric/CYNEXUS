# DDD Discipline

- Application services only orchestrate use cases, transactions, cross-aggregate calls, and DTO conversion.
- Business rules must live in `domain`: entities, value objects, domain services, and domain policies.
- Do not place validation, normalization, scoring, routing, parsing, scheduling policy, chunking, or aggregate state transitions in `application/controller` services.
- Domain code must not depend on API DTOs, MVC, persistence PO classes, MyBatis, Feign, Spring AI, or infrastructure clients. Define domain ports and implement them in `infra`.
- Infrastructure owns persistence mappings, external clients, and repository implementations. Starters assemble application, domain, and infrastructure modules.
- Refactor order for shared business logic: `knowledgebase`, `market`, `core`, `ai`.
- `domain/service` is not a catch-all package. Group domain code under a feature folder such as `domain/applicationanalysis`, `domain/marketinformation`, `domain/scheduler`, `domain/stock`, or `domain/port`; keep `service` and `impl` subfolders inside that feature only when the feature has service contracts and implementations.
