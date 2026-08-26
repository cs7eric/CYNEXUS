package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.service.agent.execution.AgentExecutor;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.stockselection.StockSelectionAiCompletionPort;
import org.springframework.stereotype.Component;

@Component
public class StockSelectionAiCompletionAdapter implements StockSelectionAiCompletionPort {

    private final AgentExecutor agentExecutor;

    public StockSelectionAiCompletionAdapter(AgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
        return agentExecutor.completeDetailed(model, prompt, capability);
    }
}
