package cn.cccs7.cynexus.ai.domain.gateway.modelturn;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import java.util.function.Consumer;

/** Cancellation-aware provider call supplied by the application adapter. */
@FunctionalInterface
public interface AgentProviderTurnPort {

    AgentModelTurnResponse complete(AiModelBO model, AgentModelTurnRequest request,
                                    Consumer<String> textDeltaConsumer);
}
