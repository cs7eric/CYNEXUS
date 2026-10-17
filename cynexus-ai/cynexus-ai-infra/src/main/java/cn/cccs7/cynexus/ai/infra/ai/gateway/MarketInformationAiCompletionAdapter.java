package cn.cccs7.cynexus.ai.infra.ai.gateway;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.marketinformation.MarketInformationAiCompletionPort;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import org.springframework.stereotype.Service;

/** Adapts the model executor to the market-information completion port. */
@Service
public class MarketInformationAiCompletionAdapter implements MarketInformationAiCompletionPort {
    private final AiModelExecutor executor;

    public MarketInformationAiCompletionAdapter(AiModelExecutor executor) {
        this.executor = executor;
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
        return executor.complete(model, prompt, capability);
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        return executor.complete(model, prompt, context);
    }
}
