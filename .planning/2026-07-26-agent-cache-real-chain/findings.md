# Findings

## Starting evidence
- Release `1.1.293-260726-RELEASE` includes `include: [reasoning.encrypted_content]`.
- Real run `AGR-f5293c6e9896`, turn 2: input 13,315, cached 6,656, stateless replay, 8 input items.
- Subsequent run `AGR-237983dafa23` retried twice and failed with provider HTTP 502.
- Simplified provider demo reached about 10.5K cached tokens, but did not reproduce the real Agent payload.

## Runtime evidence
- The active stack is Release `1.1.293-260726-RELEASE`; the AI image has been running the latest build.
- Real Agent traffic is sent to `https://api.xhhai.online/v1/responses`, not Chat Completions.
- Repeated failures at 14:49-14:52 are upstream HTTP 502 responses whose body starts with a Cloudflare HTML error page. The adapter retried, but each retry received the same 502.
- A separate retry was caused by `EOFException: SSL peer shut down incorrectly`, so upstream transport instability is also present.
- `AGR-f5293c6e9896` persisted two assistant `provider_output` arrays. Both contain a reasoning item with summary, but neither contains `encrypted_content` despite the request-side `include`.
- `AGR-3779c4843e75` succeeded and reports 13,312 cached tokens across two model turns. This is exactly 2 x 6,656, proving run-level usage is cumulative and each request still only hits the static 6,656-token prefix.
- Therefore the fixed value is real provider usage, not a UI aggregation defect. Full stateless reasoning replay is not occurring on the deployed Agent stream.

## Code-path findings
- The deployed payload builder does send `include: [reasoning.encrypted_content]`, `store: false`, `stream: true`, deterministic tools, reasoning effort, and the Responses input array.
- The SSE parser persists `response.completed.response.output[]` without filtering fields. The missing encrypted state is therefore absent in xhhai's streamed completed response, not dropped by the current parser.
- Stateless replay inserts each historical assistant message's saved runtime context immediately before its native output, preserving the original request-then-output order. Tool outputs follow the native function call item.
- Because native reasoning replay lacks encrypted content, the provider can only reuse the request prefix up to the stable prompt/tool-schema region; this matches the observed per-call 6,656 plateau.

## Controlled transport A/B
- Using the same persisted xhhai credential, `gpt-5.4-mini`, `store=false`, low reasoning, and `include=[reasoning.encrypted_content]`:
  - Non-streaming `/v1/responses`: HTTP 200, output types `reasoning,message`, one encrypted field, encrypted length 996.
  - Streaming `/v1/responses`: HTTP 200, completed output type only `message`, zero encrypted fields.
- Both requests reported the same 3,840 cached input tokens, so the only material test variable was streaming.
- This proves the prior direct non-streaming demo was not equivalent to the UI Agent path. xhhai's HTTP SSE compatibility layer omits encrypted reasoning state from the completed response.
- OpenAI Docs MCP is configured but not exposed to this already-running desktop session; the Codex manual helper fallback received HTTP 403 through the configured proxy. Runtime A/B evidence is independent of that documentation lookup failure.

## Chosen fix
- Add a validated `responses_http_stream_mode` runtime parameter with `sse` and `buffered` values.
- In buffered mode, an Agent call initiated through the streaming gateway sends a non-streaming Responses request, preserving complete native output including encrypted reasoning.
- After completion, the adapter emits reasoning summary and final text through the existing UI delta consumers as buffered deltas, so no frontend protocol change is required.
- Configure the xhhai autonomous-agent binding to `buffered`; leave providers with correct SSE implementations on the existing `sse` default.
- This also avoids the xhhai SSE/Cloudflare path that produced repeated 502 and TLS EOF failures.

## Release verification results
- Release `1.1.294-260726-RELEASE` is healthy and the xhhai binding uses buffered HTTP.
- Real UI-equivalent run `AGR-583313c2cb37` succeeded with one Tool call and two model turns. Both assistant outputs contain non-empty encrypted reasoning.
- Its second model turn still reported 6,656/13,505 cached tokens. Encrypted replay alone does not remove the plateau.
- Cross-run continuation `AGR-990baada9aec` in the same conversation failed after three upstream HTTP 502 responses, reproducing the user's second-round failure even in buffered mode.
- Direct non-streaming A/B using the persisted native output proves:
  - exact final reasoning+message replay succeeds;
  - sanitized final replay succeeds;
  - visible-message-only replay succeeds;
  - full reasoning+function-call+tool-output+reasoning+message replay succeeds when request-level tools are omitted.
- The remaining differentiator is therefore the large request-level Tool definition block combined with the grown replay transcript, not encrypted replay validity by itself.

## Explicit cache-key A/B
- A six-request direct Responses run compared the same three-turn replay with and without `prompt_cache_key`.
- Explicit key results: turn 1 `0/8,227`, turn 2 `7,552/8,304`, turn 3 `8,064/8,393` cached/input tokens; all HTTP 200.
- Automatic results: all three turns reported zero cached tokens; all HTTP 200.
- Therefore xhhai currently requires the explicit routing key for this path. Removing it would regress caching and does not explain the real Agent's fixed `6,656` plateau.
- The next diagnostic must compare safe cumulative hashes at request-prefix boundaries (system, each input item, tools, request options) between real Agent turns; no raw prompt, tool result, or credential may be logged.

## Real-chain prefix telemetry and confirmed root cause
- Release `1.1.295-260727-RELEASE` recorded only SHA-256 hashes, byte lengths, and item kinds for real Agent request boundaries.
- Real run `AGR-d2627dcf79ab` completed six model turns. Every turn after the first remained fixed at `6,656` cached tokens even as input grew from `12,294` to `19,556` tokens.
- Across turns, Tool Schema hash and request-options hash were identical. Turn 2's first three input item hashes and every cumulative prefix hash exactly matched all three items from turn 1: stable system, user message, and the original runtime context.
- Therefore neither transcript replay, encrypted reasoning, Tool ordering, runtime context, reasoning effort, nor wire options break the prefix.
- `AgentPromptCacheKeyPolicy` currently creates only 16 routing keys per stable prefix. Different conversations sharing a shard diverge at the first dynamic input item. xhhai effectively pins/partitions cache extension by the explicit key, so a shared shard can reuse only the static system/Tool prefix and cannot grow a per-conversation transcript cache.
- The direct A/B used a dedicated explicit key and grew from `7,552` to `8,064`, which matches this diagnosis.
- The fix is a versioned conversation-specific routing key that still includes the stable-prefix identity and a non-sensitive conversation digest. This trades cross-conversation static-prefix reuse for full within-conversation cache growth on xhhai.

## Runtime-context A/B and remaining cross-run defect
- A strict A/B with the real 22 Tool definitions and full guidance showed that a dynamic runtime-context item using role `system` capped the second turn at `6,656/11,237`, while the same trailing item using role `user` reached `10,752/11,203`.
- Release `1.1.297-260727-RELEASE` now sends current and historical runtime contexts as `user`, uses cache-key contract `v7`, and is healthy on the versioned Docker Release stack.
- Real run `AGR-c8da91dc6de8` grew cached input across six model turns: `0`, `12,288`, `12,800`, `12,800`, `13,824`, `14,336`. This resolves the fixed `6,656` intra-run plateau.
- Two new Runs in the same conversation (`AGR-5b9e006fb4ba`, `AGR-be2fda0f7a5b`) failed before cache usage after all provider retries with HTTP 502.
- Failed Runs left trailing unmatched user messages in conversation history. Existing history collapse also preserves native provider output, runtime context, and Tool output from completed prior Runs even though the original response linkage is unavailable in a new Run.
- The remaining likely protocol defect is cross-Run orphaned/native Tool replay. Prior completed Runs should become provider-safe visible user/assistant history; native reasoning, function calls, Tool outputs, and runtime context should remain replayable only inside the active Run.
- `AgentRunExecutionDomainService` loads all persisted conversation messages before saving the new Run's user message, then calls `AgentHistoryCollapsePolicy` after converting them to `AgentModelMessage`. That conversion discards `runNo`, so the policy cannot enforce Run boundaries.
- The current policy preserves every user message, native assistant `provider_output`, linked Tool output, and assistant runtime context. This directly explains both the failed-Run orphan user tail and the unsafe native cross-Run replay.
- A provider-safe completed prior Run can be recognized structurally as a Run group containing a final assistant message with nonblank visible content and no Tool calls. Its replay projection should contain user messages plus that final visible assistant content only, with runtime context, native provider output, Tool calls, and Tool results removed.
- The adapter prepends each message's persisted runtime context and prefers native `provider_output` over the visible message. Therefore clearing only Tool calls is insufficient: the cross-Run projection must construct fresh `AgentModelMessage` values that carry only role/content.
- Persisted messages do retain `runNo`; the loss occurs only when `AgentRunDomainService.toModelMessage` is called before collapse. The safer API boundary is to collapse `AgentMessageBO` groups first, then return provider-neutral visible `AgentModelMessage` values.
- Message rows are ordered by `sequence_no`, and every saved execution message carries its Run number. `AgentRunRepository.findRun` can validate terminal status without introducing an Application-layer repository dependency.
- Replaying only Runs whose status is `SUCCEEDED` or `NEEDS_CLARIFICATION` is stronger than structural matching alone: it excludes failed/cancelled tails even if a partial assistant message exists. The policy should additionally require a nonblank final visible assistant before emitting any messages for that Run.
- Provider response IDs are reused only when provider, model, and cache routing signature match. The current xhhai path has no usable stored response linkage, so the new cross-Run replay must be valid as a standalone request.
- The durable Chinese cache document currently ends at v8 and still describes native replay as if it applies across Runs. The next revision must distinguish active-Run native replay from completed cross-Run visible-history projection and document the cache/safety tradeoff.
- Release `1.1.298-260727-RELEASE` is healthy and v9 is persisted. The UI uses `/api/ai/agent/runs/stream` without explicit identity headers, so verification should go through the Release UI proxy to match the browser path and avoid handling trusted identity secrets.
- Real cross-Run `AGR-bbd08e913118` reused conversation `AGC-75aef70a83d5`, which already contained one successful Tool-heavy Run and two failed orphan-user Runs. It succeeded with three Tool calls. Turn 1 input kinds were exactly `system,user,assistant,user,user`, proving old native Tool/reasoning/runtime items and failed tails were excluded; turn 2 retained current-Run native items and hit `12,288/13,717` (89.6%).
- A second new Run in the same conversation, `AGR-24aa9b2f6675`, succeeded on its first model call with visible input kinds `system,user,assistant,user,assistant,user,user` and cached `11,776/13,306` (88.5%). This verifies warm cross-Run cache reuse remains well above the former `6,656` plateau.

## Errors
- Codex manual helper failed once with HTTP 403 on the official manual HEAD request; no retry of the same method is planned.
- A planning-file patch failed because the expected line was shorter than the actual combined sentence; re-read the file and applied an exact-context patch.
- The first focused Maven command failed in PowerShell before Maven started because its comma-separated `-Dtest` argument was unquoted; rerun with that argument quoted.
