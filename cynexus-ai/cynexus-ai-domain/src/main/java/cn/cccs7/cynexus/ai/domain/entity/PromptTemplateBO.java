package cn.cccs7.cynexus.ai.domain.entity;

public record PromptTemplateBO(
        String scenarioCode,
        String promptVersion,
        String systemPrompt,
        String userPromptTemplate,
        String outputSchemaJson,
        boolean enabled
) {
}
