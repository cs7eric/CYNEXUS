package cn.cccs7.cynexus.ai.domain.knowledgegeneration.port;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public interface KnowledgeBaseGenerationFallbackModelPort {
    AiModelBO route(String preferredModelCode);
}
