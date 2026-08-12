package cn.cccs7.cynexus.ai.domain.marketindicator;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public interface MarketIndicatorReplayAiCompletionPort {
    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
}
