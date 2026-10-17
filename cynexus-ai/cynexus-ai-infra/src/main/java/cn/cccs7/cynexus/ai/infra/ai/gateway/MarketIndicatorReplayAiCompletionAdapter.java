package cn.cccs7.cynexus.ai.infra.ai.gateway;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.marketindicator.MarketIndicatorReplayAiCompletionPort;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import org.springframework.stereotype.Service;

@Service
public class MarketIndicatorReplayAiCompletionAdapter implements MarketIndicatorReplayAiCompletionPort {
    private final AiModelExecutor executor;

    public MarketIndicatorReplayAiCompletionAdapter(AiModelExecutor executor) {
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
