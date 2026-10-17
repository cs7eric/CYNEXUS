# Progress

- 2026-09-05: Started a code-first redraw after the earlier high-level POC did not make the participating technologies and responsibility boundaries sufficiently clear.
- 2026-09-05: Read Prompt, LangChain4j, Spring AI, Redis, AgentScope and Rabbit listener implementations. Corrected the diagram premise: Redis protects the native Agent runtime; LangChain4j is a typed facade backed by Spring AI; AgentScope is an isolated optional HTTP adapter.
- 2026-09-05: Verified routing and fallback. The normal fallback is two-stage: LangChain4j typed call -> Spring AI generic call -> legacy adapter only for eligible Spring AI failures. RAG retrieval is filtered before the generic Spring AI answer call.
- 2026-09-05: Verified MP4/core/knowledge lifecycle and factory behavior. Diagram will separate the three execution shapes: typed capability chain, generic Spring AI chain (RAG), and native Agent legacy chain; it will also show the asynchronous knowledge branch separately.
- 2026-09-05: Rebuilt the overview as five verified call-path lanes. First build exposed two unused imports after removing the older control-plane panel; removed them before rerunning checks.
- 2026-09-05: `pnpm lint`, `pnpm build`, HTTP route check and diff whitespace check passed. The POC is served by Vite at `http://localhost:5175/poc/ai`.
- 2026-09-05: Replaced the overview with explicit architecture views: container/system topology, normal capability components plus two-level fallback, MP4/RAG sequence diagrams, and native Agent/AgentScope topology. Incorporated verified routing, transport and storage behavior.
- 2026-09-05: Final checks passed: lint, TypeScript/Vite build, HTTP 200 on the POC route and diff whitespace validation.
- 2026-09-05: Added Unified architecture as the default POC view. It combines the service container boundary with normal component execution, candidate/transport fallback, RAG projection/retrieval, Native Agent/Redis/tool path, Langfuse and AgentScope boundary.
- 2026-09-05: Replaced the linear unified view with a free-layout architecture canvas. It uses one Cynexus service boundary, external infrastructure around it, and connector arrows between actual runtime and data paths.
- 2026-09-05: A redundant plan-status update failed because the unified-canvas phase was already complete; no product file was affected.
