package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

public interface KnowledgeBaseDocumentGenerationDomainService {

    KnowledgeBaseGenerationOutcome generate(KnowledgeBaseGenerationCommand command);

    void recordExecutionLogs(KnowledgeBaseGenerationOutcome outcome);
}
