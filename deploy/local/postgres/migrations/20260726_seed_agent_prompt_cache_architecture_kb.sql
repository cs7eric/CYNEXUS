DO $$
DECLARE
    v_document_id bigint := -202607260001;
    document_content text := $doc$# Agent Prompt Cache Enterprise Architecture

## 1. Scope

This document defines prompt caching for the autonomous Agent model turns. Prompt caching reduces provider input-token cost and latency when successive turns reuse a long stable prefix. It is not a response cache: every Agent turn still reaches the model because the user message, tool results, conversation state, and final answer are dynamic.

## 2. Design Decisions

- The Agent domain creates a provider-neutral `PromptCacheContext` once per run.
- The context contains a privacy-preserving conversation key, a stable tool/schema prefix key, and a cache-key version.
- Infrastructure owns provider wire details through `PromptCacheStrategy` implementations selected by `PromptCacheStrategyRegistry`.
- Unsupported providers use a no-op strategy and receive no provider-specific cache fields or headers.
- Cache usage is recorded as `cached_input_tokens` and `cache_write_tokens` on `ai_agent_run`.

## 3. Cache Keys

The conversation key is a SHA-256 digest of the owner key and conversation number. Raw tenant, user, or conversation identifiers are never sent to a provider.

The stable prefix key is a SHA-256 digest of the cache version, capability, provider identity, model code, and canonical tool definitions. Tool definitions are sorted by tool code and include description, structured guidance, input schema, and output schema. The key excludes current input, dates, tool results, timestamps, and turn counters.

This gives the following isolation and invalidation behavior:

| Change | Result |
| --- | --- |
| Different owner or conversation | Different conversation key |
| Different provider or model | Different stable prefix key |
| Tool description, guidance, or schema change | Different stable prefix key |
| Reordered tools | Same stable prefix key |
| New user input or tool result | Same prefix key, new request content |

## 4. Provider Adapter Layer

The common Agent adapter delegates cache behavior to a registry:

```text
AgentRunExecutionDomainService
  -> PromptCacheContext
  -> OpenAiCompatibleChatAdapter
       -> PromptCacheStrategyRegistry
            -> OpenAiPromptCacheStrategy
            -> XaiPromptCacheStrategy
            -> NoopPromptCacheStrategy
```

OpenAI uses `prompt_cache_key` and reads nested cached-token usage from `prompt_tokens_details` or `input_tokens_details`. xAI uses `x-grok-conv-id` for Chat Completions and `prompt_cache_key` for Responses. Usage extraction accepts the provider's cached-token field aliases. Other OpenAI-compatible endpoints are not assumed to implement these fields unless their model is explicitly identified as a supported provider.

This boundary keeps provider-specific protocol behavior out of the domain and prevents accidental cache fields from being sent to unsupported services.

## 5. Persistence and Observability

`ai_agent_run.cached_input_tokens` records cache-read tokens and `ai_agent_run.cache_write_tokens` records provider-reported cache-write or cache-creation tokens. Existing input, output, total token, cost, and quota accounting remain unchanged. Missing provider usage is treated as zero for cache metrics and does not make a run fail.

Operational dashboards should compare cache-read tokens with total input tokens by provider and model, and track cache-key version changes, provider cache hit rate, latency, and cache-related request failures. Cache metrics must not log raw prompts, tool results, owner identifiers, API keys, or cache keys that could reveal tenant relationships.

## 6. Cache Boundaries

- Prompt cache: provider-managed reuse of a stable request prefix.
- Redis/cache store: suitable for idempotency, rate-limit state, runtime leases, and short-lived tool-result caching where freshness and authorization are explicit.
- Response cache: only safe for deterministic, authorization-scoped, freshness-bounded read operations. It must not replace Agent conversation execution or cache mutations.
- Tool cache: key by tenant/owner, tool code, normalized arguments, and freshness policy. Never share user-scoped or sensitive tool results across owners.

## 7. Rollout and Tests

1. Apply the AI run-column migration and deploy the adapter changes.
2. Verify OpenAI and xAI requests in an HTTP contract test, including streaming and non-streaming calls.
3. Verify unsupported providers have no cache header or payload field.
4. Verify key stability for identical inputs, tool-order independence, and conversation isolation.
5. Compare provider usage reports with persisted cache-read and cache-write metrics before enabling cost dashboards.
6. Keep a feature-level kill switch available through provider/model configuration if a provider changes its cache contract.

## 8. References

- OpenAI Prompt Caching: https://developers.openai.com/api/docs/guides/prompt-caching
- xAI Prompt Caching: https://docs.x.ai/developers/advanced-api-usage/prompt-caching
- Anthropic Prompt Caching: https://docs.anthropic.com/en/docs/build-with-claude/prompt-caching
- Gemini Context Caching: https://ai.google.dev/gemini-api/docs/caching
- vLLM Automatic Prefix Caching: https://docs.vllm.ai/en/latest/features/automatic_prefix_caching/
- LiteLLM Prompt Caching: https://docs.litellm.ai/docs/completion/prompt_caching
$doc$;
BEGIN
    INSERT INTO cynexus_appl_knowledge_base.kb_document
        (id, title, description, content, content_format, status, extra)
    VALUES
        (v_document_id,
         'Agent Prompt Cache Enterprise Architecture',
         'Provider-aware prompt caching design for autonomous Agent model turns.',
         document_content,
         'markdown',
         'published',
         jsonb_build_object(
             'documentKey', 'agent_prompt_cache_enterprise_architecture',
             'sourcePath', 'docs/knowledge-base/agent-prompt-cache-enterprise-architecture.md',
             'documentVersion', 'v1',
             'tags', jsonb_build_array('ai-agent', 'prompt-cache', 'architecture')))
    ON CONFLICT (id) DO UPDATE SET
        title = EXCLUDED.title,
        description = EXCLUDED.description,
        content = EXCLUDED.content,
        content_format = EXCLUDED.content_format,
        status = EXCLUDED.status,
        extra = EXCLUDED.extra,
        updated_at = now(),
        deleted_at = NULL;

    DELETE FROM cynexus_appl_knowledge_base.kb_document_chunk
     WHERE kb_document_chunk.document_id = v_document_id;
    DELETE FROM cynexus_appl_knowledge_base.kb_document_index_task
     WHERE kb_document_index_task.document_id = v_document_id;

    INSERT INTO cynexus_appl_knowledge_base.kb_document_chunk
        (id, document_id, chunk_index, heading, content, content_hash, token_estimate, embedding_status, extra)
    VALUES
        (-202607260002, v_document_id, 0, 'Agent Prompt Cache Enterprise Architecture', document_content,
         md5(document_content), GREATEST(1, length(document_content) / 4), 'pending',
         jsonb_build_object('source', 'migration', 'documentVersion', 'v1'));

    INSERT INTO cynexus_appl_knowledge_base.kb_document_index_task
        (id, document_id, target, status, payload)
    VALUES
        (-202607260003, v_document_id, 'pgvector', 'pending', jsonb_build_object('reason', 'document_seed')),
        (-202607260004, v_document_id, 'elasticsearch', 'pending', jsonb_build_object('reason', 'document_seed'));
END $$;
