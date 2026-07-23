package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

public record KnowledgeBaseGenerationCommand(String sourceText, String preferredModelCode,
                                              String tenantCode, String routingQuality, String maxCostTier) {
}
