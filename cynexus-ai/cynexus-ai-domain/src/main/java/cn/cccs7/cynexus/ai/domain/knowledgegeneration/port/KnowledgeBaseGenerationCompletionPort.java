package cn.cccs7.cynexus.ai.domain.knowledgegeneration.port;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;

/** Capability-specific alias over the shared model execution contract. */
public interface KnowledgeBaseGenerationCompletionPort extends AiModelExecutor {

    default AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        return complete(model, prompt, context == null ? null : context.capability());
    }
}
