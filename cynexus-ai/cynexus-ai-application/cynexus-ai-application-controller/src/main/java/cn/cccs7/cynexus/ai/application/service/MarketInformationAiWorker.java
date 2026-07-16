package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.MarketInformationAiTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.repository.MarketInformationAiTaskRepository;
import cn.cccs7.cynexus.ai.domain.marketinformation.MarketInformationAnalysisDomainPolicy;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Application orchestration for the market-information AI worker. */
@Service
public class MarketInformationAiWorker {

    private static final String EVENT_CAPABILITY = "market_information_event_analysis";
    private static final String BRIEF_CAPABILITY = "market_information_daily_brief";
    private static final String EVENT_SCHEMA_VERSION = "v2";
    private static final String EVENT_PROMPT_VERSION = "v3";
    private static final String BRIEF_SCHEMA_VERSION = "v2";
    private static final String BRIEF_PROMPT_VERSION = "v4";
    private static final String DEVELOPMENT_USAGE_SCOPE = "development-live-evaluation";

    private final MarketInformationAiTaskRepository taskRepository;
    private final AiProviderRepository providerRepository;
    private final AiUsageBindingRepository bindingRepository;
    private final AiExecutionLogRepository executionLogRepository;
    private final AgentExecutor agentExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MarketInformationAnalysisDomainPolicy analysisPolicy = new MarketInformationAnalysisDomainPolicy();
    private final int deepSeekLiveLimit;
    private final int taskTimeoutMinutes;

    public MarketInformationAiWorker(MarketInformationAiTaskRepository taskRepository,
                                     AiProviderRepository providerRepository,
                                     AiUsageBindingRepository bindingRepository,
                                     AiExecutionLogRepository executionLogRepository,
                                     AgentExecutor agentExecutor,
                                     @Value("${cynexus.ai.market-information.deepseek-live-limit:0}") int deepSeekLiveLimit,
                                     @Value("${cynexus.ai.market-information.task-timeout-minutes:10}") int taskTimeoutMinutes) {
        this.taskRepository = taskRepository;
        this.providerRepository = providerRepository;
        this.bindingRepository = bindingRepository;
        this.executionLogRepository = executionLogRepository;
        this.agentExecutor = agentExecutor;
        this.deepSeekLiveLimit = Math.max(-1, deepSeekLiveLimit);
        this.taskTimeoutMinutes = Math.max(1, taskTimeoutMinutes);
    }

    @Scheduled(initialDelayString = "${cynexus.ai.market-information.recovery-initial-delay-ms:10000}",
            fixedDelayString = "${cynexus.ai.market-information.recovery-delay-ms:60000}")
    public void recoverStaleTasks() {
        taskRepository.recoverStaleTasks(taskTimeoutMinutes);
    }

    @Scheduled(fixedDelayString = "${cynexus.ai.market-information.worker-delay-ms:5000}")
    public void processNext() {
        MarketInformationAiTaskBO task = taskRepository.claimNext("ai-" + ProcessHandle.current().pid());
        if (task == null) return;
        try {
            if ("EVENT_ANALYSIS".equals(task.getTaskType())) processEvent(taskRepository.loadInput(task));
            else if ("DAILY_BRIEF".equals(task.getTaskType())) processBrief(task);
            else throw new IllegalArgumentException("Unsupported Market Information AI task type: " + task.getTaskType());
            taskRepository.markSucceeded(task.getId());
        } catch (Exception exception) {
            taskRepository.markFailed(task, exception.getMessage());
        }
    }

    private void processEvent(MarketInformationAiTaskBO task) throws Exception {
        if (task.getSources() == null || task.getSources().isEmpty()) throw new IllegalStateException("Event has no source content");
        String prompt = eventPrompt(task);
        ParsedCompletion completion = completeAndParse(EVENT_CAPABILITY, prompt, task,
                (model, result) -> parseEventOrRepair(model, EVENT_CAPABILITY, prompt, result, task));
        taskRepository.saveEventAnalysis(task, EVENT_SCHEMA_VERSION, EVENT_PROMPT_VERSION,
                providerIdentity(completion.model()), completion.model().getModelCode(), completion.value().toString(),
                analysisPolicy.truncate(completion.result().getContent(), 4000), completion.fallbackUsed());
        taskRepository.enqueueDailyBrief(EVENT_SCHEMA_VERSION, EVENT_PROMPT_VERSION);
    }

    private void processBrief(MarketInformationAiTaskBO task) throws Exception {
        String input = taskRepository.loadBriefInput(EVENT_SCHEMA_VERSION, EVENT_PROMPT_VERSION);
        JsonNode briefInput = analysisPolicy.readJson(input);
        if (!briefInput.path("events").isArray() || briefInput.path("events").isEmpty()) {
            throw new IllegalStateException("No current successful event analyses are available for the brief");
        }
        if (isStaleBriefTask(task, input)) return;
        String prompt = briefPrompt(input);
        ParsedCompletion completion = completeAndParse(BRIEF_CAPABILITY, prompt, task,
                (model, result) -> parseBriefOrRepair(model, BRIEF_CAPABILITY, prompt, result, task, briefInput));
        taskRepository.saveDailyBrief(task, BRIEF_SCHEMA_VERSION, BRIEF_PROMPT_VERSION,
                providerIdentity(completion.model()), completion.model().getModelCode(), completion.value().toString(), completion.fallbackUsed());
    }

    private ParsedCompletion completeAndParse(String capability, String prompt, MarketInformationAiTaskBO task,
                                              CompletionParser parser) throws Exception {
        AiUsageBindingBO binding = bindingRepository.listAll().stream()
                .filter(item -> capability.equals(item.getCapability()) && Boolean.TRUE.equals(item.getEnabled()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing AI usage binding: " + capability));
        JsonNode params = analysisPolicy.readJson(binding.getParams());
        AiProviderBO primary = providerRepository.findById(binding.getProviderId());
        List<AiProviderBO> providers = new ArrayList<>();
        if (primary != null && Boolean.TRUE.equals(primary.getEnabled())) providers.add(primary);
        String fallbackCode = params.path("fallback_provider_code").asText("");
        providerRepository.listAll().stream().filter(item -> fallbackCode.equals(item.getCode()) && Boolean.TRUE.equals(item.getEnabled()))
                .findFirst().filter(item -> providers.stream().noneMatch(existing -> existing.getId().equals(item.getId())))
                .ifPresent(providers::add);
        Exception last = null;
        for (int index = 0; index < providers.size(); index++) {
            AiProviderBO provider = providers.get(index);
            String modelName = index == 0 ? analysisPolicy.normalize(binding.getModelOverride(), provider.getDefaultModel())
                    : params.path("fallback_model").asText(provider.getDefaultModel());
            AiModelBO model = model(provider, modelName);
            try {
                AiCompletionResultBO result = completeOnce(capability, model, prompt, task);
                return new ParsedCompletion(model, result, parser.parse(model, result), index > 0);
            } catch (Exception exception) {
                last = exception;
            }
        }
        throw last == null ? new IllegalStateException("No enabled AI provider for " + capability) : last;
    }

    private AiCompletionResultBO completeOnce(String capability, AiModelBO model, String prompt, MarketInformationAiTaskBO task) {
        if ("deepseek".equalsIgnoreCase(model.getProviderCode()) && !allowDeepSeekCall()) {
            throw new IllegalStateException("DeepSeek live-call development budget exhausted");
        }
        Instant startedAt = Instant.now();
        try {
            AiCompletionResultBO result = agentExecutor.completeDetailed(model, prompt, capability);
            recordExecution(capability, model, result, task, startedAt, null);
            return result;
        } catch (Exception exception) {
            recordExecution(capability, model, null, task, startedAt, exception);
            throw exception;
        }
    }

    private void recordExecution(String capability, AiModelBO model, AiCompletionResultBO completion,
                                 MarketInformationAiTaskBO task, Instant startedAt, Exception error) {
        try {
            executionLogRepository.create(AiExecutionLogBO.builder()
                    .executionNo("MI-" + UUID.randomUUID().toString().replace("-", ""))
                    .capability(capability).promptKey(capability)
                    .promptVersion(BRIEF_CAPABILITY.equals(capability) ? BRIEF_PROMPT_VERSION : EVENT_PROMPT_VERSION)
                    .providerCode(providerIdentity(model)).modelCode(model.getModelCode())
                    .status(error == null ? "SUCCEEDED" : "FAILED")
                    .inputTokens(completion == null ? 0 : completion.getInputTokens())
                    .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? 0 : completion.getTotalTokens())
                    .latencyMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .requestJson(objectMapper.writeValueAsString(Map.of("taskId", task.getId())))
                    .responseJson(objectMapper.writeValueAsString(Map.of("contentPreview", completion == null ? "" : analysisPolicy.truncate(completion.getContent(), 4000))))
                    .errorMessage(error == null ? null : analysisPolicy.truncate(error.getMessage(), 2000))
                    .extra("{\"rawResponseStored\":false}").build());
        } catch (Exception ignored) {
            // Observability is fail-open and cannot change the completion outcome.
        }
    }

    private JsonNode parseEventOrRepair(AiModelBO model, String capability, String prompt,
                                        AiCompletionResultBO result, MarketInformationAiTaskBO task) throws Exception {
        try {
            return validateEvent(analysisPolicy.enrichEvent(analysisPolicy.extractObject(result.getContent()), task), task);
        } catch (Exception first) {
            AiCompletionResultBO repaired = repair(model, capability, prompt, result.getContent(), first.getMessage(), task);
            return validateEvent(analysisPolicy.enrichEvent(analysisPolicy.extractObject(repaired.getContent()), task), task);
        }
    }

    private JsonNode parseBriefOrRepair(AiModelBO model, String capability, String prompt,
                                        AiCompletionResultBO result, MarketInformationAiTaskBO task,
                                        JsonNode input) throws Exception {
        try {
            return validateBrief(analysisPolicy.enrichBrief(analysisPolicy.extractObject(result.getContent()), input), input);
        } catch (Exception first) {
            AiCompletionResultBO repaired = repair(model, capability, prompt, result.getContent(), first.getMessage(), task);
            return validateBrief(analysisPolicy.enrichBrief(analysisPolicy.extractObject(repaired.getContent()), input), input);
        }
    }

    private AiCompletionResultBO repair(AiModelBO model, String capability, String prompt,
                                        String invalid, String error, MarketInformationAiTaskBO task) {
        return completeOnce(capability, model,
                prompt + "\n\nReturn corrected JSON only. Error: " + error + "\nInvalid output:\n"
                        + analysisPolicy.truncate(invalid, 12000), task);
    }

    String eventPrompt(MarketInformationAiTaskBO task) throws Exception {
        return ("You are a Chinese A-share market event analyst. Return one valid JSON object only. "
                + "Use only supplied sources and do not provide investment advice. Write analysis in Simplified Chinese. "
                + "Evidence quotes must be exact contiguous substring copied from the source. A summary_only source may contain only a title; "
                + "do not infer article details. Required fields include summary,eventFacts,informationDelta,whyItMatters,"
                + "transmissionPaths,marketExpectationGap,eventType,marketDirection,materiality,timeHorizon,novelty,scenarios,confidence,"
                + "contentQuality,evidence,risks,uncertainties,disclaimer. Event title: %s Category: %s Sources: %s")
                .formatted(task.getTitle(), task.getCategory(), objectMapper.writeValueAsString(task.getSources()));
    }

    private String briefPrompt(String input) {
        return ("Create a market-level information brief from the supplied current event analyses. Return valid JSON only, "
                + "use Simplified Chinese, preserve uncertainty, and keep every event reference in the supplied input. "
                + "Required fields: headline,executiveSummary,marketTone,keyEvents,policySignals,industrySignals,"
                + "companySignals,riskSignals,uncertainties,sourceCoverage,analysisWindow,eventReferences,disclaimer. Input: %s")
                .formatted(input);
    }

    AiModelBO model(AiProviderBO provider, String modelName) {
        String adapterCode = analysisPolicy.normalize(provider.getProviderType(), provider.getCode());
        String providerIdentity = analysisPolicy.normalize(provider.getCode(), adapterCode);
        return AiModelBO.builder().modelCode(modelName).modelName(modelName).providerCode(adapterCode)
                .providerIdentity(providerIdentity).baseUrl(provider.getBaseUrl()).apiKey(provider.getApiKey())
                .apiKeyEnv(provider.getApiKeyEnv()).extra(provider.getExtra())
                .inputPricePerMillion(provider.getInputPricePerMillion()).outputPricePerMillion(provider.getOutputPricePerMillion())
                .pricingCurrency(provider.getPricingCurrency())
                .capability(ModelCapability.builder().structuredOutputSupported(true).build()).enabled(true).build();
    }

    boolean allowDeepSeekCall() {
        if (deepSeekLiveLimit == 0) return false;
        return deepSeekLiveLimit < 0 || taskRepository.tryReserveProviderRequest("deepseek", DEVELOPMENT_USAGE_SCOPE, deepSeekLiveLimit);
    }

    boolean isStaleBriefTask(MarketInformationAiTaskBO task, String input) {
        return task != null && analysisPolicy.isStale(task.getClusterContentHash(), input);
    }

    JsonNode validateEvent(JsonNode value, MarketInformationAiTaskBO task) { return analysisPolicy.validateEvent(value, task); }

    JsonNode validateBrief(JsonNode value, JsonNode input) { return analysisPolicy.validateBrief(value, input); }

    private String providerIdentity(AiModelBO model) {
        return analysisPolicy.normalize(model.getProviderIdentity(), model.getProviderCode());
    }

    @FunctionalInterface
    private interface CompletionParser {
        JsonNode parse(AiModelBO model, AiCompletionResultBO result) throws Exception;
    }

    private record ParsedCompletion(AiModelBO model, AiCompletionResultBO result, JsonNode value, boolean fallbackUsed) { }
}
