package cn.cccs7.cynexus.ai.domain.futuresposition;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;

public interface FuturesPositionAiCompletionPort extends AiModelExecutor {
    @Override AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
    @Override default AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        return complete(model, prompt, context == null ? null : context.capability());
    }
}
