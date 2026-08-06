# Progress

## 2026-07-26

- Reviewed the attached usage data and current Agent cache implementation.
- Confirmed the primary cache-efficiency issue is dynamic content in the earliest system prompt prefix.
- Added a stable system prompt plus a separate runtime-context message to the Agent model turn contract and both Chat Completions/Responses adapters.
- Added a v3 DB-managed autonomous-agent prompt migration and release migration registration.
- Added policy and adapter regression tests for stable-prefix ordering.
- Synchronized Maven and UI versions to `1.1.279-SNAPSHOT`.
- Focused policy, adapter, prompt-domain, and workflow tests passed.
- Moved runtime context after conversation history so completed turns remain reusable as the next cache prefix.
- Recomputed cache keys per turn Tool set, sorted serialized Tool definitions, and bumped the cache-key version to `v2`.
- Updated the Chinese knowledge-base source/migration and added the cache stability rules to `AGENTS.md`.
- Bumped synchronized project versions to `1.1.280-SNAPSHOT`.
- Resumed diagnosis after observing a fixed 6,656-token cache read on every later turn.
- Verified the request loop preserves prior runtime suffixes, while generic/OpenAI cache strategies route by the global stable-prefix key.
- Loaded the current official Codex manual for comparison; OpenAI developer-doc MCP tools are not currently exposed in this session.
- Installed the official OpenAI developer-doc MCP for future sessions; this session still requires the official-domain fallback.
- Confirmed `xhhai` resolves to the generic cache strategy rather than XAI/Grok handling.
- Verified official OpenAI prefix-cache and Responses conversation-state semantics from the current developer documentation.
- Confirmed current release logs lack per-turn cache diagnostics and the Responses adapter reconstructs only visible messages/tool calls.
- Completed diagnosis without changing application behavior or deployment.

## 2026-07-26 Implementation

- Started implementation of conversation-partitioned cache routing, Responses continuation, per-turn telemetry, and knowledge-base documentation.
- Added the v4 conversation-partitioned routing context, provider-state fields, cache telemetry value object, and structured event details contract.
- Updated cache strategies to send the conversation-partitioned routing key.
- Added Responses state payload construction, delta-only continuation input, response ID capture, hashed telemetry, and full-transcript fallback in sync/stream adapters.
- Integrated provider response state into the Agent run loop and persisted it after each successful model turn.
- Added per-turn redacted `CACHE_USAGE` events and exposed structured event details through the stream DTO.
- Added idempotent response-state/cache-telemetry schema migration and enabled state mode, response storage, and 24-hour cache retention on `autonomous_agent`.
- Replaced the durable Chinese architecture guide and added the `v3-zh` knowledge-base seed/update migration.
- Added policy, strategy, adapter, workflow, and persistence-contract regression coverage.
- Synchronized every Maven module and the UI to `1.1.287-SNAPSHOT`.
- Focused tests passed: 37 tests across cache key policy, strategies, adapter, and Agent workflow.
- Full `mvn -DskipTests compile` passed for all 41 reactor modules.
- Built and deployed Release `1.1.287-260726-RELEASE`, then ran a two-turn live Agent verification.
- Found and fixed the missing binding-to-runtime merge for Responses state/cache parameters.
- Added controlled runtime-param validation and four `AgentReasoningPolicyTest` cases.
- Corrected the persistence contract check to use current owner-qualified foreign-key names; the full SQL contract now passes.
- Synchronized every Maven module and the UI to `1.1.288-SNAPSHOT`.
- Focused tests passed: 41 tests across reasoning/cache policies, cache strategies, Adapter, and Agent workflow.
- Full 41-module `mvn -DskipTests compile` passed at `1.1.288-SNAPSHOT`.
- Built and deployed versioned Release images `1.1.288-260726-RELEASE`; AI and gateway health endpoints returned HTTP 200.
- Verified the DB-managed knowledge-base document is published as `v3-zh` with 54 chunks and two index tasks.
- Live two-turn Agent verification succeeded with approximately 95.8% prompt-cache coverage and confirmed safe stateless fallback when xhhai rejected HTTP `previous_response_id` in favor of WebSocket v2.

## 2026-07-26 WebSocket v2

- User approved implementing Responses WebSocket v2 support.
- Official OpenAI WebSocket Mode documentation was fetched through the skill fallback path because Docs MCP tools are not exposed in this session.
- Added configurable `responses_transport=websocket` routing, a serialized conversation session, event accumulation for text/reasoning/tool calls/usage, and HTTP Responses fallback.
- Added an idempotent xhhai-only migration and release migration registration.
- Added binding-policy, event-accumulator, and HTTP fallback regression coverage; focused Maven tests pass (19 tests).
- Added a separate execution lock so a reused WebSocket conversation cannot interleave concurrent turns, while listener callbacks remain outside the state monitor.
- Synchronized all 41 Maven modules and the UI to `1.1.290-SNAPSHOT`.
- Applied the xhhai WebSocket routing migration, built Release `1.1.290-260726-RELEASE`, and restarted the Docker Release stack.
- Release AI health returned HTTP 200 after startup; the xhhai binding contains `responses_transport=websocket`.
