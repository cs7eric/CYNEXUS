package cn.cccs7.cynexus.ai.domain.marketfactor;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public interface MarketEventFactorAnalysisCompletionPort {
    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
}
