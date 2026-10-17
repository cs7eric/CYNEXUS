package cn.cccs7.cynexus.ai.domain.port;

import java.util.List;

public interface KnowledgeDocumentChunker {
    List<String> chunk(String content, int maxCharacters, int overlapCharacters);
}
