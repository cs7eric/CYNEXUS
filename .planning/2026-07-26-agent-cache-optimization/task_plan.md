# Agent Cache Optimization

## Goal

Increase Agent prompt-cache hit coverage by keeping the earliest request prefix stable while preserving dynamic runtime guidance and tool safety.

## Phases

- [completed] Inspect cache request construction and identify unstable prefix content.
- [completed] Refactor Agent request messages/context to isolate dynamic state after the stable prefix.
- [completed] Add focused regression tests for stable prefix behavior and cache metadata.
- [completed] Synchronize versions, run verification, and deploy the release image.
- [completed] Diagnose why cache reads remain fixed near 6.7K across Agent turns.
- [completed] Confirm the root cause from request construction, persisted usage, and runtime logs.
- [completed] Report the concrete optimization path without changing behavior.
- [completed] Design provider-neutral conversation cache routing, state chaining, and per-turn telemetry contracts.
- [completed] Implement domain contracts and provider adapter behavior with safe fallbacks.
- [completed] Add persistence/migration and Chinese knowledge-base documentation.
- [completed] Add focused tests and synchronize Maven/UI versions.
- [completed] Build, apply migrations, deploy Release images, and verify runtime behavior.
- [completed] Implement Responses WebSocket v2 transport with HTTP fallback.
- [completed] Add protocol, tool-call, reconnect, and adapter regression tests.
- [completed] Synchronize versions, rebuild Release images, and verify runtime behavior.

## Constraints

- Preserve provider-specific cache strategies and current tool behavior.
- Keep DB-managed prompt text as the source of truth with code fallback.
- Do not modify unrelated user changes.
- Do not leave planning scratch files in the repository root.

## Errors Encountered

- OpenAI developer-doc MCP tools were unavailable; following the docs skill fallback path.
- A combined `rg` regex for final line references was malformed; retry with fixed-string patterns.
- A PowerShell-quoted version regex was malformed; use fixed-string/XML reads for version verification.
- A large run-loop patch missed the exact budget-message text; split the semantic edit into smaller verified patches.
- The first focused Maven command was parsed incorrectly by PowerShell because comma-separated `-Dtest` and `-Dsurefire` values were unquoted; quoting both properties resolved it.
- The new workflow test exposed a null `freshness` value reaching `Stream.findFirst`; filter null freshness values before resolving the optional result.
- The persistence contract check referenced pre-hardening foreign-key names; align it with the owner-qualified constraint names created by the canonical migration.
- Live Release verification showed binding cache/state params were persisted but not merged into the runtime model extra; merge only validated protocol params through `AgentReasoningPolicy`.
- The xhhai HTTP Responses endpoint rejected `previous_response_id` with HTTP 400 because that gateway only supports it on Responses WebSocket v2; the stateless full-history fallback worked as designed.
