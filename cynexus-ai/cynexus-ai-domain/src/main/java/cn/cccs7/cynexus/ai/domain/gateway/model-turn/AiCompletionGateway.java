package cn.cccs7.cynexus.ai.domain.gateway.modelturn;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import java.util.List;
import java.util.function.Consumer;

public interface AiCompletionGateway {

    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);

    /** Context-preserving overload used by framework fallback paths. */
    default AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        return complete(model, prompt, context == null ? null : context.capability());
    }

    AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability);

    /** Context-preserving overload used by framework fallback paths. */
    default AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                 AiExecutionContext context) {
        return completeVision(model, prompt, images, context == null ? null : context.capability());
    }

    AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request, String capability);

    /** Context-aware Agent overload without changing ownership of the native Agent state machine. */
    default AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request,
                                                 AiExecutionContext context) {
        return completeAgent(model, request, context == null ? null : context.capability());
    }

    default AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request,
                                               String capability, Consumer<String> textDeltaConsumer) {
        return streamAgent(model, request, capability, textDeltaConsumer, null);
    }

    default AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request,
                                               String capability, Consumer<String> textDeltaConsumer,
                                               Consumer<String> thinkingDeltaConsumer) {
        return completeAgent(model, request, capability);
    }

    default AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request,
                                               AiExecutionContext context, Consumer<String> textDeltaConsumer,
                                               Consumer<String> thinkingDeltaConsumer) {
        return streamAgent(model, request, context == null ? null : context.capability(), textDeltaConsumer,
                thinkingDeltaConsumer);
    }
}
