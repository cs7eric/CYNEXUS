package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.MarketInformationAiTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
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
    private static final String EVENT_PROMPT_VERSION = "v4";
    private static final String BRIEF_SCHEMA_VERSION = "v2";
    private static final String BRIEF_PROMPT_VERSION = "v6";
    private static final String DEVELOPMENT_USAGE_SCOPE = "development-live-evaluation";

    private final MarketInformationAiTaskRepository taskRepository;
    private final AiProviderRepository providerRepository;
    private final AiPromptRepository promptRepository;
    private final AiUsageBindingRepository bindingRepository;
    private final AiExecutionLogRepository executionLogRepository;
    private final AgentExecutor agentExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MarketInformationAnalysisDomainPolicy analysisPolicy = new MarketInformationAnalysisDomainPolicy();
    private final int deepSeekLiveLimit;
    private final int taskTimeoutMinutes;

    public MarketInformationAiWorker(MarketInformationAiTaskRepository taskRepository,
                                     AiProviderRepository providerRepository,
                                     AiPromptRepository promptRepository,
                                     AiUsageBindingRepository bindingRepository,
                                     AiExecutionLogRepository executionLogRepository,
                                     AgentExecutor agentExecutor,
                                     @Value("${cynexus.ai.market-information.deepseek-live-limit:0}") int deepSeekLiveLimit,
                                     @Value("${cynexus.ai.market-information.task-timeout-minutes:10}") int taskTimeoutMinutes) {
        this.taskRepository = taskRepository;
        this.providerRepository = providerRepository;
        this.promptRepository = promptRepository;
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
        AiUsageBindingBO binding = requiredBinding(EVENT_CAPABILITY);
        PromptRuntime promptRuntime = resolvePrompt(binding, EVENT_PROMPT_VERSION);
        String prompt = eventPrompt(promptRuntime, task);
        ParsedCompletion completion = completeAndParse(EVENT_CAPABILITY, promptRuntime, prompt, task,
                (model, result) -> parseEventOrRepair(model, EVENT_CAPABILITY, promptRuntime, prompt, result, task));
        taskRepository.saveEventAnalysis(task, EVENT_SCHEMA_VERSION, promptRuntime.promptVersion(),
                providerIdentity(completion.model()), completion.model().getModelCode(), completion.value().toString(),
                analysisPolicy.truncate(completion.result().getContent(), 4000), completion.fallbackUsed());
        taskRepository.enqueueDailyBrief(EVENT_SCHEMA_VERSION, promptRuntime.promptVersion());
    }

    private void processBrief(MarketInformationAiTaskBO task) throws Exception {
        String eventPromptVersion = promptVersion(requiredBinding(EVENT_CAPABILITY), EVENT_PROMPT_VERSION);
        String input = taskRepository.loadBriefInput(EVENT_SCHEMA_VERSION, eventPromptVersion);
        JsonNode briefInput = analysisPolicy.readJson(input);
        if (!briefInput.path("events").isArray() || briefInput.path("events").isEmpty()) {
            throw new IllegalStateException("No current successful event analyses are available for the brief");
        }
        if (isStaleBriefTask(task, input)) return;
        AiUsageBindingBO binding = requiredBinding(BRIEF_CAPABILITY);
        PromptRuntime promptRuntime = resolvePrompt(binding, BRIEF_PROMPT_VERSION);
        String prompt = briefPrompt(promptRuntime, input);
        ParsedCompletion completion = completeAndParse(BRIEF_CAPABILITY, promptRuntime, prompt, task,
                (model, result) -> parseBriefOrRepair(model, BRIEF_CAPABILITY, promptRuntime, prompt, result, task, briefInput));
        taskRepository.saveDailyBrief(task, BRIEF_SCHEMA_VERSION, promptRuntime.promptVersion(),
                providerIdentity(completion.model()), completion.model().getModelCode(), completion.value().toString(), completion.fallbackUsed());
    }

    private ParsedCompletion completeAndParse(String capability, PromptRuntime promptRuntime, String prompt, MarketInformationAiTaskBO task,
                                              CompletionParser parser) throws Exception {
        AiUsageBindingBO binding = requiredBinding(capability);
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
                AiCompletionResultBO result = completeOnce(capability, promptRuntime, model, prompt, task);
                return new ParsedCompletion(model, result, parser.parse(model, result), index > 0);
            } catch (Exception exception) {
                last = exception;
            }
        }
        throw last == null ? new IllegalStateException("No enabled AI provider for " + capability) : last;
    }

    private AiCompletionResultBO completeOnce(String capability, PromptRuntime promptRuntime, AiModelBO model, String prompt, MarketInformationAiTaskBO task) {
        if ("deepseek".equalsIgnoreCase(model.getProviderCode()) && !allowDeepSeekCall()) {
            throw new IllegalStateException("DeepSeek live-call development budget exhausted");
        }
        Instant startedAt = Instant.now();
        try {
            AiCompletionResultBO result = agentExecutor.completeDetailed(model, prompt, capability);
            recordExecution(capability, promptRuntime, model, result, task, startedAt, null);
            return result;
        } catch (Exception exception) {
            recordExecution(capability, promptRuntime, model, null, task, startedAt, exception);
            throw exception;
        }
    }

    private void recordExecution(String capability, PromptRuntime promptRuntime, AiModelBO model, AiCompletionResultBO completion,
                                 MarketInformationAiTaskBO task, Instant startedAt, Exception error) {
        try {
            executionLogRepository.create(AiExecutionLogBO.builder()
                    .executionNo("MI-" + UUID.randomUUID().toString().replace("-", ""))
                    .capability(capability).promptKey(promptRuntime.promptKey())
                    .promptVersion(promptRuntime.promptVersion())
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

    private JsonNode parseEventOrRepair(AiModelBO model, String capability, PromptRuntime promptRuntime, String prompt,
                                        AiCompletionResultBO result, MarketInformationAiTaskBO task) throws Exception {
        try {
            return validateEvent(analysisPolicy.enrichEvent(analysisPolicy.extractObject(result.getContent()), task), task);
        } catch (Exception first) {
            AiCompletionResultBO repaired = repair(model, capability, promptRuntime, prompt, result.getContent(), first.getMessage(), task);
            return validateEvent(analysisPolicy.enrichEvent(analysisPolicy.extractObject(repaired.getContent()), task), task);
        }
    }

    private JsonNode parseBriefOrRepair(AiModelBO model, String capability, PromptRuntime promptRuntime, String prompt,
                                        AiCompletionResultBO result, MarketInformationAiTaskBO task,
                                        JsonNode input) throws Exception {
        try {
            return validateBrief(analysisPolicy.enrichBrief(analysisPolicy.extractObject(result.getContent()), input), input);
        } catch (Exception first) {
            AiCompletionResultBO repaired = repair(model, capability, promptRuntime, prompt, result.getContent(), first.getMessage(), task);
            return validateBrief(analysisPolicy.enrichBrief(analysisPolicy.extractObject(repaired.getContent()), input), input);
        }
    }

    private AiCompletionResultBO repair(AiModelBO model, String capability, PromptRuntime promptRuntime, String prompt,
                                        String invalid, String error, MarketInformationAiTaskBO task) {
        return completeOnce(capability, promptRuntime, model,
                prompt + "\n\nReturn corrected JSON only. Error: " + error + "\nInvalid output:\n"
                        + analysisPolicy.truncate(invalid, 12000), task);
    }

    String eventPrompt(MarketInformationAiTaskBO task) throws Exception {
        return eventPrompt(fallbackPrompt(EVENT_CAPABILITY, EVENT_PROMPT_VERSION), task);
    }

    private String eventPrompt(PromptRuntime promptRuntime, MarketInformationAiTaskBO task) throws Exception {
        return render(promptRuntime, Map.of("event_title", value(task.getTitle()), "category", value(task.getCategory()),
                "sources", objectMapper.writeValueAsString(task.getSources())));
    }

    private String briefPrompt(PromptRuntime promptRuntime, String input) {
        return render(promptRuntime, Map.of("analysis_input", input));
    }

    PromptRuntime resolvePrompt(AiUsageBindingBO binding, String fallbackVersion) {
        if (binding != null && binding.getPromptId() != null && promptRepository != null) {
            AiPromptBO prompt = promptRepository.findById(binding.getPromptId());
            if (prompt != null && "active".equalsIgnoreCase(prompt.getStatus())) {
                return new PromptRuntime(prompt.getPromptKey(), prompt.getPromptVersion(), prompt.getSystemPrompt(), prompt.getUserPromptTemplate());
            }
        }
        String capability = binding == null ? EVENT_CAPABILITY : binding.getCapability();
        return fallbackPrompt(capability, fallbackVersion);
    }

    private String promptVersion(AiUsageBindingBO binding, String fallbackVersion) {
        if (binding != null && binding.getPromptId() != null && promptRepository != null) {
            AiPromptBO prompt = promptRepository.findById(binding.getPromptId());
            if (prompt != null && "active".equalsIgnoreCase(prompt.getStatus())) {
                return prompt.getPromptVersion();
            }
        }
        return fallbackVersion;
    }

    private AiUsageBindingBO requiredBinding(String capability) {
        return bindingRepository.listAll().stream()
                .filter(item -> capability.equals(item.getCapability()) && Boolean.TRUE.equals(item.getEnabled()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing AI usage binding: " + capability));
    }

    private PromptRuntime fallbackPrompt(String capability, String version) {
        if (EVENT_CAPABILITY.equals(capability)) {
            return new PromptRuntime(capability, version,
                    "You are the Cynexus Chinese A-share market information event analyzer. "
                + "Turn one event and its source materials into a structured event analysis for later market briefs and research review. "
                + "You are not a news summarizer, market forecaster, or investment adviser. Use only the input; do not add external facts, prices, trading conclusions, or unsupported causal claims. "
                + "First establish the event background: the title and category describe the topic, sources are the available materials, sourceId is the evidence reference, and contentQuality describes completeness. "
                + "Separate source-confirmed facts, limited conditional impact judgments, and unknowns. summary must explain what happened and its context, not repeat the title. "
                + "eventFacts contain only directly supported facts; informationDelta explains what is newly known; whyItMatters explains the reason for attention; transmissionPaths must remain conditional and input-supported. "
                + "For summary_only sources, do not infer article details, policy rules, amounts, schedules, beneficiaries, or market reactions. Evidence quotes must be exact contiguous substrings from the matching source. "
                + "Return one valid JSON object only, in Simplified Chinese, with no Markdown or commentary. confidence must be a finite JSON number from 0 to 1. "
                + "Required fields: summary,eventFacts,informationDelta,whyItMatters,transmissionPaths,marketExpectationGap,eventType,affectedScopes,affectedIndustries,"
                + "affectedCompanies,entities,marketDirection,materiality,timeHorizon,novelty,catalysts,scenarios,watchItems,confidence,contentQuality,evidence,risks,uncertainties,disclaimer.",
                    "The following event input is the only factual source. Explain the background first, then separate facts, information change, impact judgment, evidence, and uncertainty.\n\nEvent title: {{event_title}}\nEvent category: {{category}}\nSource materials:\n{{sources}}");
        }
        return new PromptRuntime(capability, version,
                "You are the Cynexus Chinese A-share market information brief generator. "
                + "Turn the completed event analyses in the current input snapshot into a market-level brief for research and review. "
                + "You are not a news searcher, market terminal, or investment adviser. The input is only a selected snapshot within a time window, not the complete market record. "
                + "Use only the input: do not add external facts, prices, index movements, investment advice, trading conclusions, or unsupported causal claims. "
                + "First understand the background: analysisWindow is the time range and event count, sourceCoverage is the source-count summary, and each event's clusterId is its reference. "
                + "The event title, category, timestamps, and analysis fields describe the available background. Preserve summary_only, unknown, missing evidence, and uncertainty instead of filling gaps with general knowledge. "
                + "The brief must first explain what this batch of information is about, then identify the important events, why they matter, possible transmission paths, and what remains unknown. "
                + "Background must be derived from the input, not invented macro context. Do not turn a title into a slogan or an inference into a fact. "
                + "Return one valid JSON object only, in Simplified Chinese, with no Markdown or commentary. "
                + "headline must accurately summarize the main thread, not be a generic label. executiveSummary must be 3 to 5 complete sentences covering the time and information scope, main events or themes, why they matter, and the limits of the evidence. "
                + "Each keyEvents.summary must explain the event background, known information change, and reason for attention, not merely repeat its title. "
                + "Signals must be supported by eventReferences. eventReferences may contain only clusterId values from the input and must cover every event actually cited in the brief. "
                + "Copy sourceCoverage and analysisWindow exactly from the input. Required fields: headline,executiveSummary,marketTone,keyEvents,policySignals,industrySignals,"
                + "companySignals,riskSignals,uncertainties,sourceCoverage,analysisWindow,eventReferences,disclaimer.",
                "This is the only factual input. Understand its background and information thread before producing the structured brief.\n\nCurrent market information snapshot: {{analysis_input}}");
    }

    private String render(PromptRuntime promptRuntime, Map<String, String> variables) {
        String system = value(promptRuntime.systemPrompt());
        String user = value(promptRuntime.userPromptTemplate());
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String marker = "{{" + entry.getKey() + "}}";
            system = system.replace(marker, entry.getValue());
            user = user.replace(marker, entry.getValue());
        }
        return system + System.lineSeparator() + user;
    }

    private String value(String value) {
        return value == null ? "" : value;
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

    record PromptRuntime(String promptKey, String promptVersion, String systemPrompt, String userPromptTemplate) { }
}
