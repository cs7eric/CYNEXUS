package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import cn.cccs7.cynexus.ai.domain.rag.KnowledgeRagAnswerDomainService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Configuration
@ConditionalOnProperty(prefix = "cynexus.ai.rag", name = "enabled", havingValue = "true")
public class KnowledgeRagAnswerDomainServiceConfig {
    @Bean
    KnowledgeRagAnswerDomainService knowledgeRagAnswerDomainService(ObjectProvider<KnowledgeRagPort> ragProvider, ApplicationAnalysisRoutingDomainService routing, AiModelExecutor executor,
                                                                     AiPromptRuntimeResolver promptResolver) {
        KnowledgeRagPort rag = ragProvider.getIfAvailable(() -> new KnowledgeRagPort() {
            public java.util.List<RetrievedChunk> search(SearchRequest request) { throw new IllegalStateException("Knowledge RAG vector store is not configured"); }
            public void index(IndexDocument document) { throw new IllegalStateException("Knowledge RAG vector store is not configured"); }
            public void delete(String documentId) { throw new IllegalStateException("Knowledge RAG vector store is not configured"); }
        });
        return new KnowledgeRagAnswerDomainService(rag, routing, executor, promptResolver);
    }
}
