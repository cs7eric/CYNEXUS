package cn.cccs7.cynexus.ai.infra.ai.gateway;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Single model-execution boundary for normal capabilities. Spring AI is the
 * primary provider client; the legacy adapter remains an explicit rollback
 * implementation. LangChain4j is intentionally kept behind its own typed
 * integration and is not allowed to create a second normal-call route here.
 */
@Component("aiModelExecutor")
@Primary
public final class UnifiedAiModelExecutor implements AiModelExecutor {
    private final AiModelExecutor springExecutor;
    private final AiCompletionGatewayImpl legacyGateway;

    public UnifiedAiModelExecutor(
            @Qualifier("springAiModelExecutor") AiModelExecutor springExecutor,
            @Qualifier("aiCompletionGatewayImpl") AiCompletionGatewayImpl legacyGateway) {
        this.springExecutor = springExecutor;
        this.legacyGateway = legacyGateway;
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
        return complete(model, prompt, AiExecutionContext.of(capability,
                model == null ? null : model.getProviderCode(), model == null ? null : model.getModelCode()));
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        try {
            return springExecutor.complete(model, prompt, context);
        } catch (RuntimeException typedFailure) {
            if (!AiProviderFallbackPolicy.isEligible(typedFailure)) throw typedFailure;
            return legacyGateway.complete(model, prompt, context);
        }
    }

    @Override
    public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                String capability) {
        return completeVision(model, prompt, images, AiExecutionContext.of(capability,
                model == null ? null : model.getProviderCode(), model == null ? null : model.getModelCode()));
    }

    @Override
    public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                AiExecutionContext context) {
        try {
            return springExecutor.completeVision(model, prompt, images, context);
        } catch (RuntimeException typedFailure) {
            if (!AiProviderFallbackPolicy.isEligible(typedFailure)) throw typedFailure;
            return legacyGateway.completeVision(model, prompt, images, context);
        }
    }
}
