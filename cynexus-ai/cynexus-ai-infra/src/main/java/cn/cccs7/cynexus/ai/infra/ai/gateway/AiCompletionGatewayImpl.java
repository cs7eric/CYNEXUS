package cn.cccs7.cynexus.ai.infra.ai.gateway;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import java.util.List;
import cn.cccs7.cynexus.ai.infra.ai.adapter.AiModelAdapter;
import cn.cccs7.cynexus.ai.infra.ai.factory.DynamicChatClientFactory;
import cn.cccs7.cynexus.ai.infra.observability.LangfuseGenerationReporter;
import java.time.Instant;
import org.springframework.stereotype.Component;
import java.util.function.Consumer;

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
        String providerCode = providerCode(model);
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

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, cn.cccs7.cynexus.ai.domain.port.AiExecutionContext context) {
        String capability = context == null ? null : context.capability();
        String providerCode = providerCode(model);
        AiModelAdapter adapter = factory.resolve(providerCode);
        if (adapter == null) throw new IllegalStateException("No AI model adapter for provider: " + providerCode);
        Instant startedAt = Instant.now();
        try {
            AiCompletionResultBO raw = adapter.complete(model, prompt);
            AiCompletionResultBO result = raw == null ? null : AiCompletionResultBO.builder()
                    .content(raw.getContent()).providerCode(raw.getProviderCode()).modelCode(raw.getModelCode())
                    .inputTokens(raw.getInputTokens()).outputTokens(raw.getOutputTokens()).totalTokens(raw.getTotalTokens())
                    .rawResponse(raw.getRawResponse()).executionNo(context == null ? null : context.executionNo())
                    .latencyMs(raw.getLatencyMs()).errorType(raw.getErrorType()).build();
            reporter.report(model, capability, prompt, result, startedAt, null, context);
            return result;
        } catch (RuntimeException exception) {
            reporter.report(model, capability, prompt, null, startedAt, exception, context);
            throw exception;
        }
    }

    @Override
    public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability) {
        String providerCode = providerCode(model);
        AiModelAdapter adapter = factory.resolve(providerCode);
        if (adapter == null) throw new IllegalStateException("No AI model adapter for provider: " + providerCode);
        Instant startedAt = Instant.now();
        try {
            AiCompletionResultBO result = adapter.completeVision(model, prompt, images);
            reporter.report(model, capability, prompt, result, startedAt, null);
            return result;
        } catch (RuntimeException exception) {
            reporter.report(model, capability, prompt, null, startedAt, exception);
            throw exception;
        }
    }

    @Override
    public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                cn.cccs7.cynexus.ai.domain.port.AiExecutionContext context) {
        String capability = context == null ? null : context.capability();
        String providerCode = providerCode(model);
        AiModelAdapter adapter = factory.resolve(providerCode);
        if (adapter == null) throw new IllegalStateException("No AI model adapter for provider: " + providerCode);
        Instant startedAt = Instant.now();
        try {
            AiCompletionResultBO raw = adapter.completeVision(model, prompt, images);
            AiCompletionResultBO result = raw == null ? null : AiCompletionResultBO.builder()
                    .content(raw.getContent()).providerCode(raw.getProviderCode()).modelCode(raw.getModelCode())
                    .inputTokens(raw.getInputTokens()).outputTokens(raw.getOutputTokens()).totalTokens(raw.getTotalTokens())
                    .rawResponse(raw.getRawResponse()).executionNo(context == null ? null : context.executionNo())
                    .latencyMs(raw.getLatencyMs()).errorType(raw.getErrorType()).build();
            reporter.report(model, capability, prompt, result, startedAt, null, context);
            return result;
        } catch (RuntimeException exception) {
            reporter.report(model, capability, prompt, null, startedAt, exception, context);
            throw exception;
        }
    }

    @Override
    public AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request, String capability) {
        String providerCode = providerCode(model);
        AiModelAdapter adapter = factory.resolve(providerCode);
        if (adapter == null) {
            throw new IllegalStateException("No AI model adapter for provider: " + providerCode);
        }
        Instant startedAt = Instant.now();
        try {
            AgentModelTurnResponse result = adapter.completeAgent(model, request);
            reporter.report(model, capability, request == null ? "" : request.getSystemPrompt(),
                    null, startedAt, null);
            return result;
        } catch (RuntimeException exception) {
            reporter.report(model, capability, request == null ? "" : request.getSystemPrompt(), null, startedAt, exception);
            throw exception;
        }
    }

    @Override
    public AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request,
                                                AiExecutionContext context) {
        String providerCode = providerCode(model);
        AiModelAdapter adapter = factory.resolve(providerCode);
        if (adapter == null) throw new IllegalStateException("No AI model adapter for provider: " + providerCode);
        Instant startedAt = Instant.now();
        try {
            AgentModelTurnResponse result = adapter.completeAgent(model, request);
            reporter.report(model, context == null ? null : context.capability(),
                    request == null ? "" : request.getSystemPrompt(), null, startedAt, null, context);
            return result;
        } catch (RuntimeException exception) {
            reporter.report(model, context == null ? null : context.capability(),
                    request == null ? "" : request.getSystemPrompt(), null, startedAt, exception, context);
            throw exception;
        }
    }

    @Override
    public AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request, String capability,
                                              Consumer<String> textDeltaConsumer) {
        return streamAgent(model, request, capability, textDeltaConsumer, null);
    }

    @Override
    public AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request, String capability,
                                              Consumer<String> textDeltaConsumer,
                                              Consumer<String> thinkingDeltaConsumer) {
        String providerCode = providerCode(model);
        AiModelAdapter adapter = factory.resolve(providerCode);
        if (adapter == null) throw new IllegalStateException("No AI model adapter for provider: " + providerCode);
        return adapter.streamAgent(model, request, textDeltaConsumer, thinkingDeltaConsumer);
    }

    @Override
    public AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request,
                                              AiExecutionContext context, Consumer<String> textDeltaConsumer,
                                              Consumer<String> thinkingDeltaConsumer) {
        String providerCode = providerCode(model);
        AiModelAdapter adapter = factory.resolve(providerCode);
        if (adapter == null) throw new IllegalStateException("No AI model adapter for provider: " + providerCode);
        Instant startedAt = Instant.now();
        try {
            AgentModelTurnResponse result = adapter.streamAgent(model, request, textDeltaConsumer, thinkingDeltaConsumer);
            reporter.report(model, context == null ? null : context.capability(),
                    request == null ? "" : request.getSystemPrompt(), null, startedAt, null, context);
            return result;
        } catch (RuntimeException exception) {
            reporter.report(model, context == null ? null : context.capability(),
                    request == null ? "" : request.getSystemPrompt(), null, startedAt, exception, context);
            throw exception;
        }
    }

    private String providerCode(AiModelBO model) {
        if (model == null || model.getProviderCode() == null || model.getProviderCode().isBlank()) {
            throw new IllegalStateException("AI provider routing is missing; refusing synthetic fallback.");
        }
        return model.getProviderCode().trim();
    }
}
