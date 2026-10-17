export type FlowStep = {
  name: string
  location: string
  input: string
  responsibility: string
  output: string
  owner: "Cynexus" | "Framework" | "Data" | "Compatibility"
}

export const controlPlane: FlowStep[] = [
  {
    name: "Capability route resolution",
    location: "cynexus-ai-domain/.../applicationanalysis/impl/ApplicationAnalysisRoutingDomainServiceImpl.java",
    input: "capability + tenant/principal runtime context",
    responsibility: "Reads enabled ai_usage_bindings, produces ordered provider/model candidates, and preserves fallback order.",
    output: "AiModelBO candidate list",
    owner: "Cynexus",
  },
  {
    name: "Prompt runtime resolution",
    location: "cynexus-ai-infra/.../ai/prompt/AiPromptRuntimeResolverImpl.java",
    input: "binding.promptId + prompt key + declared variables",
    responsibility: "Prefers the binding prompt, resolves the active ai_prompt version, validates variables, renders the prompt, and records Java fallback when DB content is unavailable.",
    output: "AiPromptRuntime: key, version, rendered prompt, fallback marker",
    owner: "Cynexus",
  },
  {
    name: "Execution context",
    location: "cynexus-ai-domain/.../execution/AiExecutionContext.java",
    input: "request identity + route + prompt runtime",
    responsibility: "Carries executionNo, capability, provider/model, prompt metadata, tenant, principal, requestId and traceId without moving policy into adapters.",
    output: "correlated runtime context",
    owner: "Cynexus",
  },
]

export const normalRuntime: FlowStep[] = [
  {
    name: "Feature entry",
    location: "cynexus-ai-application/.../controller or feature application service",
    input: "validated REST DTO, scenario command, or transcript",
    responsibility: "Maps transport data into a feature command. It does not choose a model or parse provider JSON.",
    output: "feature Domain Service call",
    owner: "Cynexus",
  },
  {
    name: "Feature Domain Service",
    location: "cynexus-ai-domain/.../mediatranscription/impl/MediaTranscriptionDomainServiceImpl.java and feature domain services",
    input: "business command + Prompt Runtime + AiModelBO",
    responsibility: "Applies input bounds and feature semantics. For example, transcript size, allowed category, title length, image candidate and trading rules.",
    output: "feature completion request",
    owner: "Cynexus",
  },
  {
    name: "Completion adapter",
    location: "cynexus-ai-application/.../*CompletionAdapter.java -> AgentExecutor.java",
    input: "final rendered prompt, model candidate and execution context",
    responsibility: "Bridges a feature port to the selected execution path while preserving retry, fallback and execution identity.",
    output: "completion request",
    owner: "Cynexus",
  },
  {
    name: "Typed AI service",
    location: "cynexus-ai-infra/.../ai/langchain4j/LangChain4jCompletionAdapter.java + LangChain4jServiceFactory.java",
    input: "text / JSON contract / image input",
    responsibility: "Uses LangChain4j only as a typed service interface and transport mapping. It cannot access repositories, pick a provider, or write business records.",
    output: "typed model response",
    owner: "Framework",
  },
  {
    name: "Dynamic model execution",
    location: "cynexus-ai-infra/.../ai/spring/SpringAiModelFactory.java + SpringAiModelExecutor.java",
    input: "AiModelBO, normalized provider options and message payload",
    responsibility: "Builds or reuses an OpenAI-compatible Spring AI ChatModel using base URL, secret, model code, timeout, JSON mode, reasoning and vision options.",
    output: "provider response + token usage",
    owner: "Framework",
  },
  {
    name: "Compatibility fallback",
    location: "cynexus-ai-infra/.../ai/gateway/SpringAiCompletionGateway.java -> AiCompletionGatewayImpl.java -> OpenAiCompatibleChatAdapter.java",
    input: "eligible runtime failure + next candidate",
    responsibility: "Keeps the old HTTP adapter as an explicit rollback path. Agent streaming still uses this adapter directly.",
    output: "fallback completion or classified failure",
    owner: "Compatibility",
  },
]

export const mediaAndRagFlow: FlowStep[] = [
  {
    name: "Upload and transcript lifecycle",
    location: "cynexus-core/.../media + transcription workflow",
    input: "MP4/audio upload",
    responsibility: "Persists the transcript and transitions the media task to ai_enriching after transcription completes.",
    output: "stored transcript + media task state",
    owner: "Data",
  },
  {
    name: "Single enrichment call",
    location: "cynexus-ai-domain/.../mediatranscription/impl/MediaTranscriptionDomainServiceImpl.java",
    input: "bounded transcript",
    responsibility: "Calls media_transcription_enrich exactly once. The JSON contract covers polishedText, summary, shortSummary, keyPoints, topics, methodology, actionItems, risks, title, categories and tags.",
    output: "validated MediaTranscriptionEnrichmentBO",
    owner: "Cynexus",
  },
  {
    name: "Structured output policy",
    location: "cynexus-ai-domain/.../structured/AiStructuredOutputParser.java",
    input: "raw model response",
    responsibility: "Extracts JSON from fences/text, checks schema and enum values, limits fields/arrays, permits one bounded repair, then exposes raw failure detail for logs.",
    output: "structured output or business failure",
    owner: "Cynexus",
  },
  {
    name: "Knowledge document write",
    location: "cynexus-core/.../KnowledgeBaseDocumentWriterImpl.java",
    input: "validated enrichment fields",
    responsibility: "Creates the knowledge document in its lifecycle boundary. A write failure is stored as knowledgeError and must not erase the successful AI result/history.",
    output: "knowledge document or knowledgeError",
    owner: "Cynexus",
  },
  {
    name: "Published document event",
    location: "cynexus-knowledgebase/.../infra/messaging/KnowledgeDocumentIndexEventPublisherImpl.java",
    input: "knowledge document lifecycle change",
    responsibility: "Publishes the indexing event only after the document is eligible for the knowledge lifecycle.",
    output: "RabbitMQ index event",
    owner: "Data",
  },
  {
    name: "Embedding and vector storage",
    location: "cynexus-ai-infra/.../ai/rag/KnowledgeIndexRabbitListener.java -> KnowledgeIndexingService.java",
    input: "index event + published document",
    responsibility: "Loads document content, chunks it, invokes DashScope text-embedding-v4, and writes vectors plus tenant/owner/status metadata through Spring AI PGVector.",
    output: "vector_store rows",
    owner: "Framework",
  },
  {
    name: "Permission-filtered RAG answer",
    location: "cynexus-ai-infra/.../ai/rag/SpringAiKnowledgeRagAdapter.java + cynexus-ai-domain/.../rag/KnowledgeRagAnswerDomainService.java",
    input: "question + tenant/principal/scope",
    responsibility: "Filters vector retrieval before generation, injects permitted source material into the answer Prompt, and returns citations without modifying source documents.",
    output: "RAG answer + citations",
    owner: "Cynexus",
  },
]

export const agentFlow: FlowStep[] = [
  {
    name: "Agent API and run creation",
    location: "cynexus-ai-domain/.../service/agent/execution/AgentRunExecutionDomainService.java",
    input: "trusted identity, conversation and Agent request",
    responsibility: "Owns Run/Step/Message lifecycle, quotas, cancellation, context assembly, prompt cache and history.",
    output: "native Agent Run state",
    owner: "Cynexus",
  },
  {
    name: "Model and tool loop",
    location: "cynexus-ai-infra/.../ai/adapter/OpenAiCompatibleChatAdapter.java",
    input: "native Agent runtime messages and tool definitions",
    responsibility: "Provides the existing Responses API, SSE and tool-call transport. This is intentionally outside the normal typed capability chain.",
    output: "assistant delta or requested tool call",
    owner: "Compatibility",
  },
  {
    name: "Bounded tool gateway",
    location: "cynexus-ai-infra/.../agent/registry/AgentToolRegistry.java",
    input: "validated tool name + schema-conformant arguments",
    responsibility: "Only registered tools can execute. Models do not reach Repository, Mapper or business write APIs directly.",
    output: "bounded observation or write preview",
    owner: "Cynexus",
  },
  {
    name: "Confirmed writes",
    location: "cynexus-ai-domain/.../service/agent/write/AgentWriteDomainService.java",
    input: "write preview",
    responsibility: "Enforces preview -> PENDING_CONFIRMATION -> explicit confirm -> execute -> audit. One runtime alone owns the Agent Run state machine.",
    output: "audited mutation or cancellation",
    owner: "Cynexus",
  },
]

export const capabilityRows = [
  ["Media enrichment", "media_transcription_enrich", "MediaTranscriptionDomainServiceImpl", "MediaTranscriptionCompletionAdapter", "MediaTranscriptAnalyzer -> Spring AI", "AI history; knowledge document; index event"],
  ["Receipt recognition", "accounting_receipt_parse", "ScenarioExecutionDomainService", "ScenarioExecutionAppService -> AgentExecutor", "ReceiptVisionAnalyzer -> Spring AI", "Candidate result; finance workflow decides final write"],
  ["Trade screenshot", "portfolio_trade_screenshot_parse / portfolio_trade_screenshot_review", "ScenarioExecutionDomainService + portfolio policy", "Scenario completion path", "TradeScreenshotAnalyzer -> Spring AI", "Validation of image index, BUY/SELL, candidate ID; human review boundary"],
  ["Wechat image route", "wechat_image_route", "ScenarioExecutionDomainService", "Scenario completion path", "WechatImageRouteAnalyzer -> Spring AI", "UNKNOWN/not-target result stays valid and bounded"],
  ["Knowledge draft", "knowledge_base_document_generate", "KnowledgeBaseDocumentGenerationDomainService", "KnowledgeBaseGenerationCompletionAdapter", "KnowledgeDocumentGenerator -> Spring AI", "Returns a draft; knowledge owner persists it"],
  ["Knowledge answer", "knowledge_base_rag_answer", "KnowledgeRagAnswerDomainService", "AiModelExecutor", "Spring AI RAG adapter", "Answer and citations only; no source mutation"],
  ["Autonomous Agent", "autonomous_agent", "AgentRunExecutionDomainService", "AiCompletionGateway", "Legacy Responses/SSE transport", "Run state plus audited confirmed writes"],
] as const

export const stackRows = [
  ["Cynexus Domain", "Policy and orchestration", "Capability semantic, routing, prompt governance, identity, validation, persistence and audit", "Does not implement provider protocol"],
  ["PostgreSQL configuration", "Control plane", "ai_usage_bindings, ai_prompt, ai_provider, execution metadata", "Does not execute a model call"],
  ["LangChain4j", "Typed capability facade", "AiService interfaces and response transport POJOs for stable text/vision capabilities", "Does not select models or access repositories"],
  ["Spring AI", "Provider and RAG execution", "dynamic ChatModel, JSON/vision transport, embedding client, PGVector integration", "Does not decide business validity or document lifecycle"],
  ["Legacy OpenAI adapter", "Compatibility boundary", "rollback HTTP route plus native Agent Responses/SSE/tool transport", "Does not own prompt/routing policy"],
  ["RabbitMQ + PGVector", "Asynchronous knowledge projection", "published knowledge event, chunk/vector persistence and filtered retrieval", "Does not bypass authorization"],
] as const
