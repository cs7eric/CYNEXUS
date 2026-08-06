# Agent Prompt Cache Rules

- Keep the DB-managed Agent system prompt stable. Runtime date, budget, tool count, and force-final-answer values are later runtime-context messages, not part of the stable prefix.
- Place runtime context after existing conversation history so completed Agent turns remain reusable as the next request's prefix. Send runtime context as a trailing user message because provider-side system/instruction normalization breaks cache-prefix growth for dynamic values.
- Replay native reasoning, function calls, Tool outputs, and runtime context only inside the active Run. Across Runs, include only visible user/assistant messages from `SUCCEEDED` or `NEEDS_CLARIFICATION` Runs; exclude failed/cancelled Run tails.
- Generate `PromptCacheContext` per model turn using the exact Tool set sent on that turn. A no-Tool final-answer turn must not reuse a Tool-enabled cache key.
- Sort Tool definitions deterministically before serialization and version the cache key whenever the prefix layout or Tool serialization contract changes.
- A `prompt_cache_key` is a routing hint only; verify real cache hits from provider usage fields.
