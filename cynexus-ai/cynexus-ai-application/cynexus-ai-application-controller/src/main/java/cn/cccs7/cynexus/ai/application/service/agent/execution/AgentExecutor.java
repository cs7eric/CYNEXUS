package cn.cccs7.cynexus.ai.application.service.agent.execution;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AiCompletionGateway;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentExecutor {

    private final AiCompletionGateway completionGateway;

    public AgentExecutor(AiCompletionGateway completionGateway) {
        this.completionGateway = completionGateway;
    }

    public String execute(AiScenarioBO scenario, AiModelBO model, String prompt, List<String> toolCodes) {
        if (toolCodes == null || toolCodes.isEmpty()) {
            return complete(model, prompt);
        }
        throw new IllegalStateException("Tool-enabled generic execution is unsupported; use the autonomous Agent workflow.");
    }

    public String complete(AiModelBO model, String prompt) {
        return completeDetailed(model, prompt).getContent();
    }

    public AiCompletionResultBO completeDetailed(AiModelBO model, String prompt) {
        return completeDetailed(model, prompt, "");
    }

    public AiCompletionResultBO completeDetailed(AiModelBO model, String prompt, String capability) {
        return completionGateway.complete(model, prompt, capability);
    }

    public AiCompletionResultBO completeVisionDetailed(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability) {
        return completionGateway.completeVision(model, prompt, images, capability);
    }

    public AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request, String capability) {
        return completionGateway.completeAgent(model, request, capability);
    }
}
