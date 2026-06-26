package cn.cccs7.cynexus.ai.api.dto;

public record AiModelDTO(
        String modelCode,
        String providerCode,
        String modelName,
        boolean toolCallSupported,
        boolean structuredOutputSupported,
        boolean enabled
) {
}
