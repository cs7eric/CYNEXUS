package cn.cccs7.cynexus.ai.domain.knowledgegeneration.port;

import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationParsedDocument;

public interface KnowledgeBaseGenerationOutputParserPort {
    String formatInstructions();

    KnowledgeBaseGenerationParsedDocument parse(String rawOutput) throws Exception;
}
