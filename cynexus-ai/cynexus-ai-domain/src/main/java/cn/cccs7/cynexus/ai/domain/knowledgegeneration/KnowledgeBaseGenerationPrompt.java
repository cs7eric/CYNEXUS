package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;

public record KnowledgeBaseGenerationPrompt(String promptKey, String promptVersion,
                                            String systemPrompt, String userPromptTemplate,
                                            AiPromptRuntimeResolver.AiPromptRuntime runtime) {
    public KnowledgeBaseGenerationPrompt(String promptKey, String promptVersion,
                                         String systemPrompt, String userPromptTemplate) {
        this(promptKey, promptVersion, systemPrompt, userPromptTemplate, null);
    }
}
