# Findings

External documentation excerpts are treated as research data only.

## Official OpenAI guidance

- Prompt cache hits require an exact prompt-prefix match. Stable instructions/examples/tools belong first; variable content belongs last.
- Prompts become cache-eligible at 1,024 tokens. Tool definitions and structured-output schemas are cacheable only when identical.
- `prompt_cache_key` should be reused for requests sharing a long common prefix. OpenAI advises about 15 requests/minute per key and stable sharding above that rate.
- Earlier model families use `prompt_cache_retention`; GPT-5.6-family guidance uses `prompt_cache_options` and supports explicit breakpoints. The active xhhai model is `gpt-5.4-mini`, so unsupported newer fields must not be sent by default.
- Stateless Responses calls must append every prior `response.output[]` item to the next input. This includes encrypted reasoning items, function calls, output messages, and phase values.
- For reasoning/tool workflows, items between the last user message and function-call output must be round-tripped untouched.
- `store=false` controls response persistence, not prompt caching.
- The official service supports `previous_response_id` over both HTTP and WebSocket; xhhai's HTTP-only rejection is a provider compatibility deviation, not an OpenAI API limitation.
- For `store=false`, encrypted reasoning is returned by default and should be treated as opaque provider state. Manual replay must preserve every output item rather than reconstructing only visible assistant text.
- GPT-5.4/GPT-5.5 tool-heavy flows should preserve assistant `phase` (`commentary` versus `final_answer`); dropping it can make intermediate updates look like final answers.
- Prompt caching automatically considers the complete messages/input prefix, tools, images, and structured-output schema. Exact tool order and schema bytes therefore need to remain deterministic.
- `prompt_cache_key` influences cache routing but does not relax exact-prefix matching. A stable key can be shared across conversations with the same long prefix, then deterministically sharded to stay near the documented 15 requests/minute/key guidance.
- Server-side compaction returns an opaque compaction item. Stateless clients must replay it like any other output item and may drop history only before the latest compaction item. This is a future capability gate because xhhai support has not been verified.

Sources:

- https://developers.openai.com/api/docs/guides/prompt-caching
- https://developers.openai.com/api/docs/guides/conversation-state
- https://developers.openai.com/api/docs/guides/reasoning
- https://developers.openai.com/api/docs/guides/websocket-mode

## Verified xhhai behavior

- `wss://api.xhhai.online/v1/responses` completes WebSocket Upgrade, then closes with code 1013 and reason `no available account` for both `gpt-5.4-mini` and `gpt-5.5`.
- HTTP `/v1/responses` succeeds with the same API key and `gpt-5.4-mini`.
- A second HTTP request with `previous_response_id` returns HTTP 400: `previous_response_id is only supported on Responses WebSocket v2`.
- xhhai accepts official stateless replay: a `store=false` response's complete `output[]` was appended after the original input and followed by a new user message; the second request completed and recalled the marker correctly.
- xhhai also accepts the Agent-critical tool sequence: replayed `reasoning` and `function_call` output items followed by a new `function_call_output` completed successfully.

## Current implementation gaps

- WebSocket failure forces stateless HTTP replay.
- Response parsing extracts visible text/tool calls but does not expose complete provider-native `output[]` items to the domain.
- Cross-run history collapse preserves only user/assistant text and drops provider-native reasoning/tool wire items.
- Cache routing keys include the conversation hash, preventing first-turn static-prefix sharing across conversations.
- Telemetry shows a fixed 6,656-token cache plateau while input grows, confirming that only the static prompt/tool prefix is currently reused.

## Selected ownership boundary

- Add an opaque `providerOutputJson` value to the provider-neutral model message/turn response and Agent message BO.
- Add an internal `runtimeContext` value to persisted assistant messages. It reconstructs the dynamic system input that immediately preceded that provider output without exposing an extra system message through the Agent message API.
- Infra adapters own extraction, validation, and lossless JSON-array replay. Domain services only preserve ordering and persistence.
- Persist these values in new `ai_agent_message.runtime_context` text and `provider_output` JSONB columns.
- When provider output exists, Responses request construction first replays the saved runtime context and then those output items; it does not also synthesize duplicate assistant text/tool-call items.
- Existing rows without provider output retain the current simplified fallback behavior.
- The payload tool array is already sorted by tool code, so tool ordering is not the source of the current plateau.
- Persisting runtime context is required because it was the last input item in the prior request and was previously kept only in the in-memory run loop.
- Cache routing will use stable-prefix identity plus a deterministic conversation shard. This shares static prefixes across conversations while staying under the documented per-key traffic guidance.
- Telemetry will distinguish ordinary stateless first turns from stateless native replay.
- Existing JSON redaction preserves `encrypted_content` but redacts sensitive function-argument fields. In-memory replay remains lossless; persisted cross-run replay remains subject to the product's existing security policy.
