package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

public record KnowledgeBaseGenerationPrompt(String promptKey, String promptVersion,
                                            String systemPrompt, String userPromptTemplate) {
}
