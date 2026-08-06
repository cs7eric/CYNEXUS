---
name: cynexus-backend
description: Use when modifying Java APIs, application services, domain policies, repositories, infrastructure, controllers, or starter wiring in Cynexus.
---

# Cynexus Backend Workflow

1. Read the closest module `AGENTS.md` and `docs/agent/backend-ddd.md`.
2. Start with CodeGraph status/search and run impact before changing shared symbols.
3. Trace Controller -> AppService -> Domain Service/Policy -> Port -> Infra.
4. Keep Application Service orchestration and DTO mapping only.
5. Keep business rules, normalization, routing, parsing, scoring, and lifecycle transitions in domain.
6. Check dependency direction and focused domain tests.
7. When adding or changing a foundational user-facing data read/query/write API, load `$cynexus-agent-tools`; add a bounded read-only Agent tool or the confirmed-write path, or document why Agent exposure is unsafe/unsuitable.
8. Run affected tests and `mvn -DskipTests compile` as appropriate.
9. Synchronize Maven and UI versions after functional changes.
