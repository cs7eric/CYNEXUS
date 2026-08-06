package cn.cccs7.cynexus.ai.domain.futuresposition;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public interface FuturesPositionAiCompletionPort {

    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
}
