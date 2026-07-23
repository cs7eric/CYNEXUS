package cn.cccs7.cynexus.ai.domain.knowledgegeneration.port;

import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationRoutingParameters;

public interface KnowledgeBaseGenerationRoutingParametersPort {
    KnowledgeBaseGenerationRoutingParameters parse(String rawParams);
}
