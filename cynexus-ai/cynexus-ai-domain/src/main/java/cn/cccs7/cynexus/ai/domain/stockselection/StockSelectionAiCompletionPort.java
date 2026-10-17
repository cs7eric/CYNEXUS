package cn.cccs7.cynexus.ai.domain.stockselection;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;

public interface StockSelectionAiCompletionPort extends AiModelExecutor {
    @Override AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
    @Override default AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        return complete(model, prompt, context == null ? null : context.capability());
    }
}
