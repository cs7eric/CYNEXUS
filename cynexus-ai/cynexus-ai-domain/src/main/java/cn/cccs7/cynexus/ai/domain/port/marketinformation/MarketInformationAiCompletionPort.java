package cn.cccs7.cynexus.ai.domain.port.marketinformation;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;

/** Provider-neutral completion capability required by Market Information analysis. */
public interface MarketInformationAiCompletionPort extends AiModelExecutor {
    /** Compatibility alias retained for existing market-information callers. */
    default AiCompletionResultBO completeDetailed(AiModelBO model, String prompt, String capability) {
        return complete(model, prompt, capability);
    }

    default AiCompletionResultBO completeDetailed(AiModelBO model, String prompt, AiExecutionContext context) {
        return complete(model, prompt, context);
    }
}
