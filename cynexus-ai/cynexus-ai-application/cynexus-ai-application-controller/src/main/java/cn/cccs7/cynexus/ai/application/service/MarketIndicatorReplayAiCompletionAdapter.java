package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.service.agent.execution.AgentExecutor;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.marketindicator.MarketIndicatorReplayAiCompletionPort;
import org.springframework.stereotype.Service;

@Service
public class MarketIndicatorReplayAiCompletionAdapter implements MarketIndicatorReplayAiCompletionPort {
    private final AgentExecutor agentExecutor;

    public MarketIndicatorReplayAiCompletionAdapter(AgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
        return agentExecutor.completeDetailed(model, prompt, capability);
    }
}
