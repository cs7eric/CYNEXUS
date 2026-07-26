# Progress

## 2026-07-26

- Created isolated planning files under `.planning/` per repository placement rules.
- Read the OpenAI documentation skill and official Prompt Caching, Conversation State, Reasoning, and WebSocket guidance.
- Verified xhhai WebSocket and HTTP continuation behavior with isolated probes.
- Audited current cache telemetry and identified the fixed 6,656-token plateau.
- Selected an opaque JSON persistence boundary that keeps provider parsing in Infra and sequence ownership in Domain.
- Identified non-persisted runtime context as another exact-prefix gap and selected an internal assistant-message field for replay ordering.
- Verified a two-turn xhhai HTTP `store=false` conversation using complete `response.output[]` replay.
- Verified xhhai stateless replay across a reasoning/function-call/function-output sequence.
- Finalized persistence, compatibility, routing-key, and telemetry design.
- Confirmed compatibility with the existing Agent sensitive-data persistence policy.
- Phase 1 and Phase 2 are complete; Phase 3 is in progress.
- Re-read the current official Prompt Caching, Conversation State, Reasoning, Compaction, and WebSocket guides.
- Added focused tests for stable-prefix routing shards, native history replay, persistence redaction, and Responses output capture/replay.
- First focused test run stopped at test compilation because the new history test used the wrong existing method name; corrected to `collapseForProvider`.
- Second focused test run passed all Domain behavior except an assertion that expected unhashed routing material; retained the privacy hash, corrected the test, and centralized cache-key prefixes in `AiConstants`.
- Third focused run passed all 68 Domain tests and reached Infra; the new SSE fixture had malformed nested JSON, which was corrected before an Infra-only rerun.
- Focused Adapter/WebSocket/Schema rerun passed 26 tests.
- Replaced the outdated stateful v3 knowledge-base guide with the detailed Chinese v4 stateless replay guide.
- Added and registered the idempotent `v4-zh` knowledge-base seed migration.
- Synchronized Maven and UI versions to `1.1.291-SNAPSHOT`.
- Phase 3 and Phase 4 are complete; Phase 5 verification and Release deployment are in progress.
- First Release deployment exposed a migration-order regression: a legacy migration re-enabled `previous_response_id` and response storage after the new stateless migration.
- Reordered Release migrations so the provider-specific stateless configuration wins and added a regression assertion.
- Re-ran all 68 AI Domain tests and the 78-test AI Infra suite; 77 Infra tests passed and the environment-dependent Redis test was skipped.
- Passed the 7-test Release migration-order contract and the full 41-module Maven compile on `1.1.292-SNAPSHOT`.
- Applied the schema and `v4-zh` knowledge-base migrations; verified `runtime_context`, `provider_output`, 64 document chunks, and 2 index tasks in PostgreSQL.
- Built and deployed all six `1.1.292-260726-RELEASE` images. Verified the AI service completed Spring startup and registered in Nacos.
- Verified the active xhhai Agent binding uses Responses HTTP with `store=false`, `prompt_cache_retention=24h`, and no `responses_state_mode`.
- Phase 5 is complete.
