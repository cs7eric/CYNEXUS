package cn.cccs7.cynexus.ai.domain.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KnowledgeRagAnswerDomainServiceTest {
    @Test
    void rejectsMissingIdentityOrQuestion() {
        var service = new KnowledgeRagAnswerDomainService(new Rag(List.of()), new Routing(), (model, prompt, capability) -> AiCompletionResultBO.builder().build());
        assertThrows(IllegalArgumentException.class, () -> service.answer(new KnowledgeRagAnswerDomainService.Command("tenant", "", "q", 5, null)));
    }

    @Test
    void returnsNoSourceWithoutCallingModel() {
        var rag = new Rag(List.of());
        var routing = new Routing();
        var executor = (AiModelExecutor) (model, prompt, capability) -> { throw new AssertionError("model must not be called"); };
        var answer = new KnowledgeRagAnswerDomainService(rag, routing, executor)
                .answer(new KnowledgeRagAnswerDomainService.Command("tenant", "principal", "question", 5, null));
        assertEquals(List.of(), answer.sources());
    }

    @Test
    void retrievesInScopeSourcesAndCitesThemInPrompt() {
        var rag = new Rag(List.of(new KnowledgeRagPort.RetrievedChunk("doc-1", "source text", 0.9d, Map.of("tenantKey", "tenant"))));
        var routing = new Routing();
        var executor = (AiModelExecutor) (model, prompt, capability) -> {
            if (!prompt.contains("[Source 1]")) throw new AssertionError("source citation context missing");
            return AiCompletionResultBO.builder().content("answer [Source 1]").build();
        };
        var model = AiModelBO.builder().providerCode("xhhai-grok").modelCode("grok-4.6").build();
        routing.models = List.of(model);
        var answer = new KnowledgeRagAnswerDomainService(rag, routing, executor)
                .answer(new KnowledgeRagAnswerDomainService.Command("tenant", "principal", "question", 5, null));
        assertEquals("answer [Source 1]", answer.content());
        assertEquals(1, answer.sources().size());
    }

    private static final class Routing implements ApplicationAnalysisRoutingDomainService {
        private List<AiModelBO> models = List.of();
        @Override public cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO findBinding(String capability) { return null; }
        @Override public List<AiModelBO> routeModels(String capability, String preferredModelCode) { return models; }
        @Override public String promptVersion(cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO binding) { return "v1"; }
    }

    private record Rag(List<KnowledgeRagPort.RetrievedChunk> chunks) implements KnowledgeRagPort {
        @Override public void index(IndexDocument document) { }
        @Override public List<RetrievedChunk> search(SearchRequest request) { return chunks; }
    }
}
