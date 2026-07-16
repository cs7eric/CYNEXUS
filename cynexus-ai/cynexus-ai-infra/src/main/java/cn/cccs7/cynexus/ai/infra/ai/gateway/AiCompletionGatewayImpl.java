package cn.cccs7.cynexus.ai.infra.ai.gateway;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiCompletionGateway;
import cn.cccs7.cynexus.ai.infra.ai.adapter.AiModelAdapter;
import cn.cccs7.cynexus.ai.infra.ai.factory.DynamicChatClientFactory;
import cn.cccs7.cynexus.ai.infra.observability.LangfuseGenerationReporter;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AiCompletionGatewayImpl implements AiCompletionGateway {

    private final DynamicChatClientFactory factory;
    private final LangfuseGenerationReporter reporter;

    public AiCompletionGatewayImpl(DynamicChatClientFactory factory, LangfuseGenerationReporter reporter) {
        this.factory = factory;
        this.reporter = reporter;
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
        String providerCode = model == null || model.getProviderCode() == null || model.getProviderCode().isBlank()
                ? "mock" : model.getProviderCode();
        AiModelAdapter adapter = factory.resolve(providerCode);
        if (adapter == null) {
            throw new IllegalStateException("No AI model adapter for provider: " + providerCode);
        }
        Instant startedAt = Instant.now();
        try {
            AiCompletionResultBO result = adapter.complete(model, prompt);
            reporter.report(model, capability, prompt, result, startedAt, null);
            return result;
        } catch (RuntimeException exception) {
            reporter.report(model, capability, prompt, null, startedAt, exception);
            throw exception;
        }
    }
}
