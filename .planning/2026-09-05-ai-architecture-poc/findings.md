# Findings

## Pending inspection

- Locate the routing service implementation; initial assumed package path was wrong.
- Verify exact fallback ownership from AgentExecutor and gateway wiring.
- Verify knowledge indexing implementation after listener delegation.
- Verify durable execution-log paths separately from Langfuse.

## Confirmed so far

- `AiPromptRuntimeResolverImpl` resolves binding Prompt first, then active prompt by key/version, and emits `fallback-service-default` when DB content is unavailable.
- `LangChain4jCompletionAdapter` dispatches capability codes to typed AI services, serializes their POJOs, records Langfuse and only persists an execution-log failure locally. Its typed services are created with `SpringBackedLangChainChatModel(models.resolve(model))`.
- `SpringAiModelExecutor` is the primary generic executor and can execute text or image media through a registry-created Spring AI `ChatModel`.
- Redis is specifically the Agent runtime lease, cancellation, rate-limit and circuit-breaker implementation (`RedisAgentRuntimeLease`); it is not the routing or durable state store.
- `AgentScopeOrchestrator` is disabled by configuration by default, restricted to `market_research*`, passes a bounded HTTP command/result only, and receives no repository/business-writer/native-run-state handle.
- `KnowledgeIndexRabbitListener` manually acknowledges indexed/deleted knowledge events, rejects malformed identity events, and lets downstream vector/provider failures retry through listener policy.
- `ApplicationAnalysisRoutingDomainServiceImpl` constructs ordered candidates from binding model override, same-provider fallback chain/models, provider default model, then configured fallback provider/model candidates. It merges provider `extra` with binding params into `AiModelBO` and marks structured/vision capability support.
- The ordinary application bridge is `AgentExecutor`: LangChain4j is tried first. On typed-service failure it calls the primary `SpringAiCompletionGateway`; that gateway calls `SpringAiModelExecutor` and only then falls back to `AiCompletionGatewayImpl` for eligible transport failures. The previous diagram incorrectly showed a direct LangChain4j-to-legacy jump.
- `SpringAiKnowledgeRagAdapter` uses Spring AI `VectorStore`, keeps stable chunk IDs in metadata, and applies tenant + owner/scope + published-status filtering before similarity search.
- `KnowledgeRagAnswerDomainService` performs authorized retrieval before Prompt rendering, routes `knowledge_base_rag_answer` through the normal DB model candidate list, then invokes the generic `AiModelExecutor` directly (not the LangChain4j typed facade).
- `MediaTranscriptionProcessDomainServiceImpl` persists transcript/history and calls the AI enrichment client once after `ai_enriching`; the following knowledge draft write is isolated in its own try/catch, so a knowledge failure results in a completed media process with `knowledgeIngestionStatus=FAILED` and preserved AI output.
- `MediaTranscriptionDomainServiceImpl` itself loops DB-routed model candidates, creates a context/log per candidate and performs a bounded structured-output repair using `media_transcription_enrich_repair` only after malformed enrichment output.
- Knowledge lifecycle publishes its document-change event after create/update. The index service deletes existing chunks, indexes only `published` documents, chunks with `1200/120`, and sends chunks to the Spring AI VectorStore adapter.
- Spring AI model factory validates outbound origin, resolves API key/env secret, caches clients by provider/base URL/model/secret fingerprint/extra/runtime options, and applies timeout, retries, max tokens, temperature, reasoning effort and JSON response format.
- `SpringAiCompletionGateway` only falls back to legacy transport for 429, 5xx, connection and timeout-style failures. Agent complete/stream calls are delegated straight to legacy transport even though the gateway is primary for ordinary completion.
- RAG infrastructure is configured in `SpringAiPgVectorConfiguration`: Spring AI `OpenAiEmbeddingModel` (currently configured as DashScope-compatible `text-embedding-v4`) backs `PgVectorStore`; `SpringAiKnowledgeEmbeddingAdapter` exposes embeddings and `SpringAiKnowledgeRagAdapter` exposes vector retrieval/indexing.
