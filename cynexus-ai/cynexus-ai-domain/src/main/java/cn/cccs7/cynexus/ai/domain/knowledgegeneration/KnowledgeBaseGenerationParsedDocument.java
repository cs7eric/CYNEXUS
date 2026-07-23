package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

import java.util.List;

public record KnowledgeBaseGenerationParsedDocument(String title, String description, List<String> tags,
                                                    List<String> categories, String content) {
}
