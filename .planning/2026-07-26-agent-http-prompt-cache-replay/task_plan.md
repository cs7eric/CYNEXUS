# Agent HTTP Prompt Cache Replay

## Goal

Implement an OpenAI-guide-aligned, stateless Responses conversation replay path that improves prompt-cache growth when WebSocket transport and HTTP `previous_response_id` are unavailable.

## Phases

### Phase 1: Official guidance and current-flow audit
Status: complete

- Confirm exact-prefix, cache-key, retention, and stateless reasoning replay requirements.
- Trace request construction, response parsing, persistence, and cross-run history reconstruction.

### Phase 2: Domain and persistence design
Status: complete

- Define opaque provider-output ownership without leaking Jackson/provider parsing into domain services.
- Define migration and compatibility behavior for existing messages.
- Define shared, sharded stable-prefix cache routing keys.

### Phase 3: Implementation
Status: complete

- Capture complete Responses `output[]` for regular, SSE, and WebSocket responses.
- Persist output items and replay them losslessly before new input/tool outputs.
- Keep existing simplified message behavior as a backward-compatible fallback.
- Switch xhhai Agent routing to HTTP stateless replay and remove unsupported state chaining.

### Phase 4: Documentation and version sync
Status: complete

- Update the Chinese knowledge-base document and seed migration.
- Synchronize Maven module and UI versions.

### Phase 5: Verification and release
Status: complete

- Add focused domain/adapter/persistence tests.
- Run focused tests and full Maven compile.
- Apply migrations, build versioned Release images, restart Release, and inspect cache telemetry.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| xhhai HTTP rejected `previous_response_id` | Direct two-turn probe | Use official stateless full-output replay instead of HTTP state chaining. |
| Initial sensitive-data policy path was incorrect | File inspection | Located the existing policy under `domain/policy/sensitivedata`. |
| Combined Adapter patch missed the nested stream-usage declaration context | First patch attempt | Split the semantic changes into smaller verified patches. |
| New history-policy test called a non-existent `collapse` method | First focused test run | Corrected the test to use the existing `collapseForProvider` API. |
| Routing-key test asserted the unhashed internal routing material | Second focused test run | Assert the public hashed key prefix and bounded shard cardinality instead. |
| SSE replay test fixture contained unescaped nested `arguments` JSON | Third focused test run | Added the required Java text-block escape layer to keep the SSE event valid JSON. |
| PowerShell parsed the comma-separated Maven `-Dtest` value as syntax | Focused rerun command | Quoted the complete Maven property. |
| PowerShell treated unquoted `-Dsurefire.failIfNoSpecifiedTests=false` as a lifecycle fragment | Focused rerun command | Quoted the complete Maven property. |
| Release migration order restored xhhai stateful parameters after stateless migration | First 1.1.291 deployment verification | Moved stateless replay migration after the legacy response-state migration and added an ordering contract test. |
