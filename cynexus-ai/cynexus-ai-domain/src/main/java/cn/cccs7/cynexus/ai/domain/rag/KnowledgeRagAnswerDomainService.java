package cn.cccs7.cynexus.ai.domain.rag;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import java.util.Map;
import java.util.List;
import cn.cccs7.cynexus.common.core.constant.TraceId;

/** Domain-owned RAG policy: authorization-scoped retrieval precedes model generation. */
public class KnowledgeRagAnswerDomainService {
    private final KnowledgeRagPort rag;
    private final ApplicationAnalysisRoutingDomainService routing;
    private final AiModelExecutor executor;
    private final AiPromptRuntimeResolver promptResolver;
    public KnowledgeRagAnswerDomainService(KnowledgeRagPort rag, ApplicationAnalysisRoutingDomainService routing, AiModelExecutor executor) {
        this(rag, routing, executor, null);
    }
    public KnowledgeRagAnswerDomainService(KnowledgeRagPort rag, ApplicationAnalysisRoutingDomainService routing, AiModelExecutor executor,
                                           AiPromptRuntimeResolver promptResolver) {
        this.rag = rag; this.routing = routing; this.executor = executor; this.promptResolver = promptResolver;
    }
    public Answer answer(Command command) {
        if (command == null || blank(command.tenantKey()) || blank(command.principalKey()) || blank(command.question())) {
            throw new IllegalArgumentException("tenantKey, principalKey and question are required");
        }
        List<KnowledgeRagPort.RetrievedChunk> chunks = rag.search(new KnowledgeRagPort.SearchRequest(
                command.tenantKey().trim(), command.principalKey().trim(), command.question().trim(), Math.max(1, Math.min(12, command.topK())), command.permissionScope()));
        if (chunks.isEmpty()) return new Answer("No authorized knowledge source was found for this question.", List.of());
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) context.append("[Source ").append(i + 1).append("] ").append(chunks.get(i).content()).append("\n\n");
        String system = "Answer the question using only the authorized sources below. Cite sources as [Source N]. If the sources do not contain the answer, say so.";
        String template = "Question:\n{{question}}\n\nSources:\n{{sources}}";
        AiPromptRuntimeResolver.AiPromptRuntime runtime = promptResolver == null
                ? new AiPromptRuntimeResolver.AiPromptRuntime("knowledge_base_rag_answer", "fallback-service-default", system, template)
                : promptResolver.resolve("knowledge_base_rag_answer", "knowledge_base_rag_answer", system, template);
        String prompt = runtime.render(Map.of("question", command.question().trim(), "sources", context.toString()));
        List<AiModelBO> models = routing.routeModels("knowledge_base_rag_answer", command.preferredModelCode());
        if (models == null || models.isEmpty()) throw new IllegalStateException("No model is configured for knowledge RAG answer");
        RuntimeException failure = null;
        for (AiModelBO model : models) try {
            String requestId = "RAG-" + java.util.UUID.randomUUID();
            AiExecutionContext executionContext = new AiExecutionContext(requestId,
                    "knowledge_base_rag_answer", runtime.promptKey(), runtime.promptVersion(),
                    model.getProviderCode(), model.getModelCode(), command.tenantKey(), command.principalKey(), requestId, TraceId.current());
            return new Answer(executor.complete(model, prompt, executionContext).getContent(), chunks);
        }
        catch (RuntimeException ex) { failure = ex; }
        throw new IllegalStateException("Knowledge RAG answer failed", failure);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    public record Command(String tenantKey, String principalKey, String question, int topK, String preferredModelCode, String permissionScope) {
        public Command(String tenantKey, String principalKey, String question, int topK, String preferredModelCode) {
            this(tenantKey, principalKey, question, topK, preferredModelCode, null);
        }
    }
    public record Answer(String content, List<KnowledgeRagPort.RetrievedChunk> sources) { }
}
