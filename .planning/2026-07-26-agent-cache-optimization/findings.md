# Findings

- The supplied usage page shows 20 visible requests: 9 with cache reads and 11 without.
- The visible requests contain 284,577 uncached input tokens and 70,144 cache-read tokens, about 19.8% of combined input tokens.
- `prompt_cache_key` is sent by the generic strategy, but it partitions the provider cache; it cannot make different prompt prefixes equal.
- `AgentPromptCacheKeyPolicy` hashes provider, model, and canonical tool definitions. That key is stable until tool metadata changes.
- `AgentRunExecutionDomainService` renders the first system prompt message with current date, tool budget, used tool count, and force-final state on every turn.
- The dynamic system prompt appears before the conversation messages, so changing budget/tool state invalidates the provider's reusable prefix.
- Tool definitions are removed when the soft budget is exhausted, which also changes the request prefix and cache shape.

## Proposed Direction

Keep a stable DB-managed system prompt as the first message. Pass date, budget, used-tool count, and force-final state in a later runtime-context message. Keep tool definitions stable for normal turns and use an explicit later control message when no more tools are allowed.

## Fixed 6.7K Follow-up

- A recent four-turn run persisted 55,279 input tokens and 19,968 cached input tokens.
- `19,968 = 6,656 * 3`, so the first turn missed and every later turn reused exactly the same 6,656-token prefix.
- This indicates real provider-side caching, but no incremental reuse of the growing conversation/tool history.
- Current domain policy already computes both a conversation-specific key and a global stable-prefix key.
- The Agent Responses payload still defaults `store` to `false` because `disable_response_storage` defaults to `true`.
- The Agent request contract carries no `previous_response_id`, so stateful Responses chaining is not currently possible.
- The adapter serializes the full system prompt and accumulated message history into `input` on every turn, then appends runtime context.
- `OpenAiPromptCacheStrategy` and the generic fallback both send `stablePrefixKey`, not the available conversation-specific key.
- `xhhai` will normally fall through to the generic strategy unless its model/provider metadata is recognized by `XaiPromptCacheStrategy`.
- The run loop deliberately carries each prior runtime-context suffix into the next turn, so the full prior request input is designed to remain a byte-identical prefix.
- Therefore the fixed hit is not explained by message ordering alone; cache-key partitioning or upstream gateway semantics is now the primary suspect.
- Provider identity `xhhai` does not match the XAI/Grok strategy, so it uses the generic strategy: global `stablePrefixKey`, no conversation header.
- The official Codex manual exposes usage counters such as `cached_input_tokens`, but does not document Codex's private prompt-cache request construction; exact Codex internals cannot be claimed from the public manual.
- The deployed release services are healthy on version `1.1.286-260726-RELEASE`.
- Official OpenAI caching uses exact prompt-prefix matching; `prompt_cache_key` is combined with the initial-prefix hash for routing, not a substitute for prefix equality.
- Official guidance recommends roughly no more than 15 requests/minute across all prefixes for one cache key; higher or highly divergent traffic should be partitioned with stable keys.
- OpenAI documents that complete messages, assistant interactions, tools, and schemas are cacheable, so a correctly replayed growing transcript can produce growing cache reads.
- Stateful `previous_response_id` is not required for caching: `store=false` with manual replay is supported, but the complete prior Responses `output` array should be replayed, including reasoning items and assistant phase metadata.
- Current Cynexus replay keeps assistant content and function calls but does not preserve the complete Responses output array or encrypted reasoning items.
- `ai_agent_run` stores cache usage only as run-level aggregate counters; it cannot prove each turn's key, input hash, or cached count independently.
- Recent persisted data is not universally fixed at 6.7K: some one/two-turn runs in the same conversation show 11,776 cached tokens, and one later two-turn run totals 25,600 cached tokens.
- The latest four-turn run is specifically pathological: 19,968 aggregate cached tokens equals exactly three 6,656-token hits, with no apparent growth after the first miss.
- This makes an upstream-wide hard cap unlikely; behavior varies by run/conversation and points to cache routing/collision or request-shape continuity.
- The active `autonomous_agent` binding specifies only Responses API, prompt v3, and low reasoning; it does not enable response storage, retention, or a conversation-scoped cache policy.
- The Responses parser extracts text and function calls but discards response identity and non-visible output items, so the next request is a reconstructed transcript rather than an exact Responses continuation.
- Release logs do not emit per-turn cache key hash, input-prefix hash, input item count, or cached-token count. The current aggregate run row is insufficient to distinguish an upstream cache eviction from an integration mismatch with certainty.

## Root Cause Assessment

- High confidence: the fixed 6,656 tokens are the shared static system/tool prefix; the cache itself is working.
- High confidence: Cynexus does not currently use canonical stateful Responses continuation and does not replay the complete prior output.
- Medium-high confidence: sharing one `stablePrefixKey` across all conversations reduces long-tail reuse at the `xhhai` gateway; use a conversation-partitioned key for Agent turns.
- Medium confidence: `xhhai` may implement `prompt_cache_key` more rigidly than native OpenAI. A controlled A/B request trace is required to prove that provider-specific behavior.

## Implementation Constraints

- OpenAI developer-doc MCP was installed in the prior turn but is not exposed until the desktop session restarts; use the already-fetched official prompt-caching and conversation-state guides as the implementation source.
- Responses continuation must remain provider-configurable and fall back to stateless full replay when an OpenAI-compatible gateway rejects stateful fields.
- OpenAI's official WebSocket Mode guide documents a persistent `wss://api.openai.com/v1/responses` connection using `response.create`; follow-up turns send `previous_response_id` and incremental input, and omit HTTP-only fields such as `stream`.
- The current adapter uses Java HTTP POST plus SSE parsing for Responses, so WebSocket support requires a transport branch rather than a payload-only switch.
- The current domain request already carries full transcript, continuation messages, tools, and provider response ID, which is sufficient for a provider adapter transport branch with stateless HTTP fallback.
- Public Codex documentation does not establish Codex's private transport implementation; the WebSocket protocol is nevertheless explicitly designed for agentic coding and tool-heavy workflows.
- Agent conversations already have a domain/PO/repository update path, so provider continuation state can be persisted on the conversation aggregate without leaking persistence into application services.
- The existing model response contract has only visible content/tool calls/usage/raw response; it needs explicit provider response identity and replay-item state to support canonical Responses continuation.
- Stateful continuation cannot simply add `previous_response_id` to the existing full-history payload: each follow-up must send only items created since the prior provider response, while retaining full history for fallback.
- The conversation aggregate already has JSON metadata, but dedicated provider-state fields are safer for atomic invalidation when provider/model changes and avoid mixing runtime protocol state with user metadata.
- Stateful fallback can remain inside the infra adapter: retain both full transcript and continuation delta in the domain request, retry statelessly when a gateway rejects stored-response fields, and return the new response ID when accepted.
- Existing `ai_agent_run_event` is the natural per-turn telemetry owner; adding a JSONB details field and a `CACHE_USAGE` event avoids a parallel analytics table while preserving run/turn ordering.
- The repository already contains a long Chinese Agent Prompt Cache knowledge-base seed/update migration. Extend that document to a new version and also add a durable Markdown source under `docs/`.
- The worktree has only the two existing untracked planning directories; tracked application files are clean at commit `64ffe381` before implementation.
- `AgentPromptCacheKeyPolicy.context` has one production call site and a focused test, so adding stable-prompt input and a routing key has a contained blast radius.

## Chosen Design

- Add a conversation-partitioned routing key while retaining the global stable-prefix hash for diagnostics.
- Include stable system-prompt content in the prefix signature so DB prompt edits invalidate stale provider state automatically.
- Persist the last provider response ID plus provider/model/state signature on the conversation aggregate.
- Carry both full transcript and continuation delta in the model request; Responses adapters use the delta only when state chaining is active and retain full replay for fallback.
- Persist one structured cache-usage event per model turn with hashed request/cache identifiers and token counters.
- Both synchronous and streaming Agent paths construct payloads before transport; stateful fallback must be implemented in both paths or centralized so UI streaming does not bypass it.
- Existing adapter tests already use a local `/v1/responses` server and can verify `store`, `previous_response_id`, delta-only input, response ID parsing, and stateless fallback without external API calls.
- Responses streaming exposes the final response object in `response.completed`; the parser currently reads usage there but can also capture its `id` without storing raw reasoning content.
- Streaming non-2xx failures currently leave through the runtime-exception path, so state fallback needs a typed provider HTTP failure check rather than relying only on transport fallback.

## Errors

- A read attempted a non-existent `AnthropicPromptCacheStrategy.java`; the registry confirms the active strategy set is XAI, OpenAI, generic.

## Release Verification

- Release `1.1.288-260726-RELEASE` runs from versioned Docker images and the AI health endpoint returns HTTP 200.
- The first live verification exposed that `AgentReasoningPolicy` only copied `wire_api` and reasoning from binding params, so response storage/state settings were silently absent at runtime.
- Runtime extra now copies only validated `wire_api`, `responses_state_mode`, `disable_response_storage`, and `prompt_cache_retention` values; arbitrary binding keys remain excluded.
- A new two-turn live run recorded 11,776 cached tokens out of 12,270/12,291 input tokens, approximately 95.8-95.9% cache coverage.
- The first turn recorded `responseStorageEnabled=true` and a Provider Response ID.
- The second turn attempted state continuation, but xhhai returned HTTP 400: `previous_response_id is only supported on Responses WebSocket v2`; Adapter telemetry recorded `STATELESS_FALLBACK` and the run still succeeded.
- The current HTTP integration therefore has high prefix-cache coverage and safe continuation fallback, but canonical state chaining on xhhai requires a separate Responses WebSocket v2 transport implementation.
