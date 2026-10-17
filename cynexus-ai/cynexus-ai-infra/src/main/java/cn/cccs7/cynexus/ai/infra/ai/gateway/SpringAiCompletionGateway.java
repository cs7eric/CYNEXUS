package cn.cccs7.cynexus.ai.infra.ai.gateway;

import cn.cccs7.cynexus.ai.domain.entity.*;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.*;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Primary completion route; Agent turns remain on the rollback adapter until Agent migration. */
@Component("springAiCompletionGateway")
@Primary
public class SpringAiCompletionGateway implements AiCompletionGateway {
    private static final Logger log = LoggerFactory.getLogger(SpringAiCompletionGateway.class);
    private final AiModelExecutor executor;
    private final AiCompletionGateway legacy;

    public SpringAiCompletionGateway(@Qualifier("springAiModelExecutor") AiModelExecutor executor,
                                     @Qualifier("aiCompletionGatewayImpl") AiCompletionGateway legacy) {
        this.executor = executor;
        this.legacy = legacy;
    }

    @Override public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
        try {
            return executor.complete(model, prompt, capability);
        } catch (RuntimeException primaryFailure) {
            if (!AiProviderFallbackPolicy.isEligible(primaryFailure)) throw primaryFailure;
            log.warn("Spring AI completion failed; using legacy adapter fallback execution, capability={}, executionNo={}, reason={}",
                    capability, null, primaryFailure.getMessage());
            return legacy.complete(model, prompt, capability);
        }
    }

    @Override public AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        try {
            return executor.complete(model, prompt, context);
        } catch (RuntimeException primaryFailure) {
            if (!AiProviderFallbackPolicy.isEligible(primaryFailure)) throw primaryFailure;
            log.warn("Spring AI context completion failed; using legacy adapter fallback execution, capability={}, executionNo={}, reason={}",
                    context == null ? null : context.capability(), context == null ? null : context.executionNo(), primaryFailure.getMessage());
            return legacy.complete(model, prompt, context);
        }
    }

    @Override public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability) {
        try {
            return executor.completeVision(model, prompt, images, capability);
        } catch (RuntimeException primaryFailure) {
            if (!AiProviderFallbackPolicy.isEligible(primaryFailure)) throw primaryFailure;
            log.warn("Spring AI vision completion failed; using legacy adapter fallback execution, capability={}, reason={}",
                    capability, primaryFailure.getMessage());
            return legacy.completeVision(model, prompt, images, capability);
        }
    }

    @Override public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                          AiExecutionContext context) {
        try {
            return executor.completeVision(model, prompt, images, context);
        } catch (RuntimeException primaryFailure) {
            if (!AiProviderFallbackPolicy.isEligible(primaryFailure)) throw primaryFailure;
            log.warn("Spring AI context vision completion failed; using legacy adapter fallback execution, capability={}, executionNo={}, reason={}",
                    context == null ? null : context.capability(), context == null ? null : context.executionNo(), primaryFailure.getMessage());
            return legacy.completeVision(model, prompt, images, context);
        }
    }

    @Override public AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request, String capability) {
        return legacy.completeAgent(model, request, capability);
    }

    @Override public AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request,
                                                          AiExecutionContext context) {
        return legacy.completeAgent(model, request, context);
    }

    @Override public AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request, String capability,
                                                        Consumer<String> textDeltaConsumer, Consumer<String> thinkingDeltaConsumer) {
        return legacy.streamAgent(model, request, capability, textDeltaConsumer, thinkingDeltaConsumer);
    }

    @Override public AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request,
                                                        AiExecutionContext context, Consumer<String> textDeltaConsumer,
                                                        Consumer<String> thinkingDeltaConsumer) {
        return legacy.streamAgent(model, request, context, textDeltaConsumer, thinkingDeltaConsumer);
    }

}
