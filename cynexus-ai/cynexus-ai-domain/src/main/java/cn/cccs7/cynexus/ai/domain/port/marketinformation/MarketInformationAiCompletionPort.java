package cn.cccs7.cynexus.ai.domain.port.marketinformation;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

/** Provider-neutral completion capability required by Market Information analysis. */
public interface MarketInformationAiCompletionPort {
    AiCompletionResultBO completeDetailed(AiModelBO model, String prompt, String capability);
}
