package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;

/** Port for completing an application-analysis model turn. */
public interface ApplicationAnalysisCompletionPort extends AiModelExecutor {

    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);

    default AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        return complete(model, prompt, context == null ? null : context.capability());
    }
}
