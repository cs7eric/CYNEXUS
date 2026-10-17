package cn.cccs7.cynexus.ai.infra.ai.langchain4j;
import dev.langchain4j.service.UserMessage;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationParsedDocument;
public interface KnowledgeDocumentGenerator {
    @UserMessage("{{text}}") KnowledgeBaseGenerationParsedDocument generate(String text);
}
