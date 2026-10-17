package cn.cccs7.cynexus.ai.infra.ai.spring;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import cn.cccs7.cynexus.ai.infra.observability.LangfuseGenerationReporter;
import java.time.Instant;
import java.time.Duration;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import java.util.List;
import java.net.URI;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

/** Spring AI execution path. Enable after provider compatibility is verified in deployment. */
@Component("springAiModelExecutor")
public class SpringAiModelExecutor implements AiModelExecutor {
    private final SpringAiModelRegistry registry;
    private final LangfuseGenerationReporter reporter;

    public SpringAiModelExecutor(SpringAiModelRegistry registry, LangfuseGenerationReporter reporter) { this.registry = registry; this.reporter = reporter; }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
        Instant started = Instant.now();
        ChatResponse response = null;
        try {
            response = registry.get(model).call(new Prompt(prompt == null ? "" : prompt));
        } catch (RuntimeException ex) {
            reporter.report(model, capability, prompt, null, started, ex);
            throw ex;
        }
        String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                ? "" : response.getResult().getOutput().getText();
        var usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
        AiCompletionResultBO result = AiCompletionResultBO.builder().content(content).providerCode(model.getProviderCode()).modelCode(model.getModelCode())
                .inputTokens(usage == null ? 0 : usage.getPromptTokens()).outputTokens(usage == null ? 0 : usage.getCompletionTokens())
                .totalTokens(usage == null ? 0 : usage.getTotalTokens()).rawResponse(truncate(response == null ? null : response.toString(), 4000))
                .latencyMs(Duration.between(started, Instant.now()).toMillis()).build();
        reporter.report(model, capability, prompt, result, started, null);
        return result;
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        Instant started = Instant.now();
        try {
            ChatResponse response = registry.get(model).call(new Prompt(prompt == null ? "" : prompt));
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null ? "" : response.getResult().getOutput().getText();
            var usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
            AiCompletionResultBO result = AiCompletionResultBO.builder().content(content).providerCode(model.getProviderCode()).modelCode(model.getModelCode())
                    .inputTokens(usage == null ? 0 : usage.getPromptTokens()).outputTokens(usage == null ? 0 : usage.getCompletionTokens()).totalTokens(usage == null ? 0 : usage.getTotalTokens())
                    .rawResponse(response == null ? null : truncate(response.toString(), 4000))
                    .executionNo(context == null ? null : context.executionNo())
                    .latencyMs(Duration.between(started, Instant.now()).toMillis()).build();
            reporter.report(model, context == null ? null : context.capability(), prompt, result, started, null, context);
            return result;
        } catch (RuntimeException ex) {
            reporter.report(model, context == null ? null : context.capability(), prompt, null, started, ex, context);
            throw ex;
        }
    }

    private static String truncate(String value, int max) { return value == null ? null : value.length() <= max ? value : value.substring(0, max); }

    @Override
    public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability) {
        return completeVision(model, prompt, images, AiExecutionContext.of(capability,
                model == null ? null : model.getProviderCode(), model == null ? null : model.getModelCode()));
    }

    @Override
    public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                               AiExecutionContext context) {
        Instant started = Instant.now();
        try {
            var media = (images == null ? List.<AiImageInputBO>of() : images).stream()
                    .filter(image -> image != null && image.getDataUrl() != null && !image.getDataUrl().isBlank())
                    .map(image -> new Media(MimeTypeUtils.parseMimeType(image.getContentType() == null ? "image/jpeg" : image.getContentType()), URI.create(image.getDataUrl())))
                    .toList();
            ChatResponse response = registry.get(model).call(new Prompt(UserMessage.builder().text(prompt == null ? "" : prompt).media(media).build()));
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null ? "" : response.getResult().getOutput().getText();
            var usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
            AiCompletionResultBO result = AiCompletionResultBO.builder().content(content).providerCode(model.getProviderCode()).modelCode(model.getModelCode())
                    .inputTokens(usage == null ? 0 : usage.getPromptTokens()).outputTokens(usage == null ? 0 : usage.getCompletionTokens()).totalTokens(usage == null ? 0 : usage.getTotalTokens())
                    .rawResponse(truncate(response == null ? null : response.toString(), 4000))
                    .executionNo(context == null ? null : context.executionNo())
                    .latencyMs(Duration.between(started, Instant.now()).toMillis()).build();
            reporter.report(model, context == null ? null : context.capability(), prompt, result, started, null, context);
            return result;
        } catch (RuntimeException ex) {
            reporter.report(model, context == null ? null : context.capability(), prompt, null, started, ex, context);
            throw ex;
        }
    }
}
