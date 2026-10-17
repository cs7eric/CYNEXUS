package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import cn.cccs7.cynexus.ai.domain.entity.*;
import cn.cccs7.cynexus.ai.domain.port.LangChain4jCompletionPort;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.infra.observability.LangfuseGenerationReporter;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class LangChain4jCompletionAdapter implements LangChain4jCompletionPort {
    private final LangChain4jServiceFactory factory;
    private final LangfuseGenerationReporter reporter;
    private final AiExecutionLogRepository executionLogs;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    public LangChain4jCompletionAdapter(LangChain4jServiceFactory factory, LangfuseGenerationReporter reporter) {
        this(factory, reporter, null);
    }
    @Autowired
    public LangChain4jCompletionAdapter(LangChain4jServiceFactory factory, LangfuseGenerationReporter reporter,
                                        AiExecutionLogRepository executionLogs) {
        this.factory = factory; this.reporter = reporter; this.executionLogs = executionLogs;
    }
    @Override public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
        return completeInternal(model, prompt, capability, null);
    }
    @Override public AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        return completeInternal(model, prompt, context == null ? null : context.capability(), context);
    }
    private AiCompletionResultBO completeInternal(AiModelBO model, String prompt, String capability, AiExecutionContext context) {
        Instant started = Instant.now();
        try {
        String input = prompt == null ? "" : prompt;
        Object typed = switch (capability == null ? "" : capability) {
            case "media_transcription_enrich" -> factory.mediaTranscriptAnalyzer(model).analyze(input);
            case "knowledge_base_document_generation", "knowledge_base_document_generate" -> factory.knowledgeDocumentGenerator(model).generate(input);
            case "market_event_factor_analysis" -> factory.marketEventAnalyzer(model).analyze(input);
            case "market_factor_study_synthesis" -> factory.factorStudySynthesizer(model).synthesize(input);
            case "futures_position_analysis", "market.futures_position_analysis" -> factory.futuresPositionAnalyzer(model).analyze(input);
            case "market_information_event_analysis", "market_information_daily_brief" -> factory.marketInformationAnalyzer(model).analyze(input);
            case "application_recent30" -> factory.applicationAnalysisAnalyzer(model).analyze(input);
            case "stock_selection_ai_screening", "stock_selection_ai_intent_planning" -> factory.stockSelectionAnalyzer(model).analyze(input);
            case "market_indicator_replay_explanation" -> factory.marketIndicatorReplayAnalyzer(model).analyze(input);
            default -> factory.genericTextAnalyzer(model).complete(input);
        };
        var typedUsage = SpringBackedLangChainChatModel.consumeLastUsage();
        String content = serialize(typed);
        AiCompletionResultBO result = AiCompletionResultBO.builder().content(content).providerCode(model.getProviderCode()).modelCode(model.getModelCode())
                .inputTokens(typedUsage == null ? 0 : typedUsage.inputTokenCount())
                .outputTokens(typedUsage == null ? 0 : typedUsage.outputTokenCount())
                .totalTokens(typedUsage == null ? 0 : typedUsage.totalTokenCount())
                .rawResponse(truncate(content, 4000))
                .executionNo(context == null ? null : context.executionNo())
                .latencyMs(java.time.Duration.between(started, Instant.now()).toMillis()).build();
        reporter.report(model, capability, prompt, result, started, null, context);
        return result;
        } catch (RuntimeException ex) {
            SpringBackedLangChainChatModel.consumeLastUsage();
            reporter.report(model, capability, prompt, null, started, ex, context);
            recordFailure(model, capability, prompt, context, started, ex);
            throw ex;
        }
    }

    private String serialize(Object value) {
        try { return value instanceof String text ? text : mapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalArgumentException("LangChain4j typed output could not be serialized", exception); }
    }
    private String truncate(String value, int max) {
        return value == null ? null : value.length() <= max ? value : value.substring(0, max);
    }
    @Override public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability) {
        return completeVisionInternal(model, prompt, images, AiExecutionContext.of(capability,
                model == null ? null : model.getProviderCode(), model == null ? null : model.getModelCode()));
    }

    @Override public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                         AiExecutionContext context) {
        return completeVisionInternal(model, prompt, images, context);
    }

    private AiCompletionResultBO completeVisionInternal(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                         AiExecutionContext context) {
        Instant started = Instant.now();
        try {
        var contents = new java.util.ArrayList<dev.langchain4j.data.message.Content>();
        contents.add(dev.langchain4j.data.message.TextContent.from(prompt == null ? "" : prompt));
        if (images != null) images.stream().filter(i -> i != null && i.getDataUrl() != null && !i.getDataUrl().isBlank())
                .forEach(i -> contents.add(dev.langchain4j.data.message.ImageContent.from(i.getDataUrl(), i.getContentType() == null ? "image/jpeg" : i.getContentType())));
        var userMessage = dev.langchain4j.data.message.UserMessage.from(contents);
        String capability = context == null ? null : context.capability();
        Object typed = switch (capability == null ? "" : capability) {
            case "accounting_receipt_parse" -> factory.receiptVisionAnalyzer(model).analyze(userMessage);
            case "portfolio_trade_screenshot_parse" -> factory.tradeScreenshotAnalyzer(model).analyze(userMessage);
            case "portfolio_trade_screenshot_review" -> factory.tradeScreenshotReviewAnalyzer(model).analyze(userMessage);
            case "wechat_image_route" -> factory.wechatImageRouteAnalyzer(model).analyze(userMessage);
            default -> null;
        };
        var response = typed == null ? factory.chatModel(model).doChat(dev.langchain4j.model.chat.request.ChatRequest.builder()
                .messages(userMessage).build()) : null;
        // Typed AI Services invoke the same Spring-backed model, but LangChain4j
        // does not expose its ChatResponse here. Consume the bridge usage so
        // vision calls retain the same metering contract as text calls.
        var usage = typed == null ? (response == null ? null : response.tokenUsage())
                : SpringBackedLangChainChatModel.consumeLastUsage();
        String content = typed == null ? (response == null || response.aiMessage() == null ? "" : response.aiMessage().text()) : serialize(typed);
        AiCompletionResultBO result = AiCompletionResultBO.builder().content(content)
                .providerCode(model.getProviderCode()).modelCode(model.getModelCode())
                .inputTokens(usage == null ? 0 : usage.inputTokenCount())
                .outputTokens(usage == null ? 0 : usage.outputTokenCount())
                .totalTokens(usage == null ? 0 : usage.totalTokenCount())
                .rawResponse(truncate(content, 4000))
                .executionNo(context == null ? null : context.executionNo())
                .latencyMs(java.time.Duration.between(started, Instant.now()).toMillis()).build();
        reporter.report(model, context == null ? null : context.capability(), prompt, result, started, null, context);
        return result;
        } catch (RuntimeException ex) {
        reporter.report(model, context == null ? null : context.capability(), prompt, null, started, ex, context);
            recordFailure(model, context == null ? null : context.capability(), prompt, context, started, ex);
            throw ex;
        }
    }

    private void recordFailure(AiModelBO model, String capability, String prompt,
                               AiExecutionContext context, Instant started, RuntimeException error) {
        if (executionLogs == null) return;
        try {
            String base = context == null || context.executionNo() == null || context.executionNo().isBlank()
                    ? "LC4J-" + java.util.UUID.randomUUID().toString().replace("-", "")
                    : context.executionNo();
            executionLogs.create(AiExecutionLogBO.builder()
                    .executionNo(base + "-lc4j")
                    .capability(capability)
                    .promptKey(context == null ? null : context.promptKey())
                    .promptVersion(context == null ? null : context.promptVersion())
                    .providerCode(model == null ? null : model.getProviderCode())
                    .modelCode(model == null ? null : model.getModelCode())
                    .status("FAILED")
                    .latencyMs(Duration.between(started, Instant.now()).toMillis())
                    .requestJson(preview(prompt))
                    .responseJson("{\"contentPreview\":\"\"}")
                    .errorMessage(truncate(error == null ? null : error.getMessage(), 2000))
                    .extra("{\"runtime\":\"langchain4j\",\"fallbackEligible\":true}")
                    .build());
        } catch (RuntimeException ignored) {
            // Observability must not change fallback behavior.
        }
    }

    private String preview(String prompt) {
        String value = prompt == null ? "" : prompt;
        if (value.length() > 4000) value = value.substring(0, 4000);
        try { return mapper.writeValueAsString(java.util.Map.of("promptPreview", value)); }
        catch (Exception ignored) { return "{\"promptPreview\":\"\"}"; }
    }
}
