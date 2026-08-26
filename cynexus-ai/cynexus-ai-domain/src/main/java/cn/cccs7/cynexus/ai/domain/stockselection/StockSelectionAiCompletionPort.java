package cn.cccs7.cynexus.ai.domain.stockselection;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public interface StockSelectionAiCompletionPort {

    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
}
