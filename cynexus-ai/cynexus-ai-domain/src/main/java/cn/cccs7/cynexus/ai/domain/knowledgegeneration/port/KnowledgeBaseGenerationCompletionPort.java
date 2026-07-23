package cn.cccs7.cynexus.ai.domain.knowledgegeneration.port;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public interface KnowledgeBaseGenerationCompletionPort {
    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
}
