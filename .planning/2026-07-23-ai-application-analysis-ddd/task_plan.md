# AI Application Analysis DDD Refactor

## Goal

Refactor `ApplicationAnalysisPageAppService` and `MediaTranscriptionAiAppService` so the application layer only invokes domain services, while business orchestration and repository access remain behind domain ports/services. Preserve API behavior, add meaningful domain/application tests, do not change Maven/UI versions, and commit the focused change.

Expanded in the full DDD audit to also cover Usage, Model Route, Scenario, Market Information AI, Agent Write/Feedback/Run.

## Phases

- [completed] Inspect project rules, current services, domain ports/implementations, assembly, and tests.
- [completed] Move business orchestration and repository access into feature-specific domain services and correct assembly.
- [completed] Add focused domain/application regression tests without changing API behavior.
- [completed] Run focused Maven tests, review diff/version/UI constraints, and create one clear commit.
- [blocked] Release Docker verification for 1.1.206-260723-RELEASE (Docker Desktop API 500 / hang).

## Constraints

- Application may depend on DomainService contracts only for business work.
- Domain must not depend on DTO, MVC, persistence PO, MyBatis, Feign, Spring AI, or infra clients.
- Version policy for functional slices: sync Maven/UI (finalized at 1.1.206-SNAPSHOT).
