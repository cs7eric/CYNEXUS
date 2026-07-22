package cn.cccs7.cynexus.ai.domain.gateway.modelturn;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import java.util.List;
import java.util.function.Consumer;

public interface AiCompletionGateway {

    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);

    AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability);

    AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request, String capability);

    default AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request,
                                               String capability, Consumer<String> textDeltaConsumer) {
        return completeAgent(model, request, capability);
    }
}
