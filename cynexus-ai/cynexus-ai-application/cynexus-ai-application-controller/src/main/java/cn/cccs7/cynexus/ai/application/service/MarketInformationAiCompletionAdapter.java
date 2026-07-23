package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.service.agent.execution.AgentExecutor;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.marketinformation.MarketInformationAiCompletionPort;
import org.springframework.stereotype.Service;

/** Adapts the application-owned Agent completion boundary to the domain port. */
@Service
public class MarketInformationAiCompletionAdapter implements MarketInformationAiCompletionPort {
    private final AgentExecutor agentExecutor;

    public MarketInformationAiCompletionAdapter(AgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    @Override
    public AiCompletionResultBO completeDetailed(AiModelBO model, String prompt, String capability) {
        return agentExecutor.completeDetailed(model, prompt, capability);
    }
}
