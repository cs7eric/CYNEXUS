# AI Architecture POC Redraw

## Goal

Replace the current POC diagram with a code-accurate technical architecture map where Spring AI, LangChain4j, AgentScope, provider routing, fallback, RabbitMQ, Redis, PostgreSQL, PGVector, and observability are visible as distinct systems and responsibilities.

## Phases

- [completed] Inspect runtime code and record concrete call paths, ownership, and infrastructure roles.
- [completed] Translate verified paths into a layered diagram information model.
- [completed] Rebuild the POC overview around the verified system diagram and retain detailed drill-down views.
- [completed] Run frontend verification and inspect the served route.
- [completed] Replace the process-card overview with separate system, component and sequence architecture views.
- [completed] Verify the redrawn UI and preserve code-path accuracy.
- [completed] Add a unified architecture canvas combining service topology, AI components and external infrastructure.

## Constraints

- The diagram must reflect current implementation, not target architecture.
- AgentScope is disabled by default and must not be shown as the owner of native Agent state.
- Redis must show its actual Agent lease/rate-limit/circuit-breaker role.
- LangChain4j and Spring AI must show separate responsibilities.
- Use existing workspace UI conventions and preserve unrelated worktree edits.
