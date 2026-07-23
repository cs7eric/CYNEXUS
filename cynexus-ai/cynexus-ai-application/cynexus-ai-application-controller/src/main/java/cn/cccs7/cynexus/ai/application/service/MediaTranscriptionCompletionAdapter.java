package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.service.agent.execution.AgentExecutor;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionCompletionPort;
import org.springframework.stereotype.Component;

@Component
public class MediaTranscriptionCompletionAdapter implements MediaTranscriptionCompletionPort {

    private final AgentExecutor agentExecutor;

    public MediaTranscriptionCompletionAdapter(AgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
        return agentExecutor.completeDetailed(model, prompt, capability);
    }
}
