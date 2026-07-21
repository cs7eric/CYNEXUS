package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AgentRunDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentRunStepDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentToolDefinitionDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentConversationDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentMessageDTO;
import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.port.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.repository.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class AutonomousAgentAppService {

    private static final int MAX_TURNS = 8;
    private static final int MAX_TOOL_CALLS = 12;
    private static final int MAX_PARALLEL_READ_CALLS = 4;

    private final AgentRunRepository agentRunRepository;
    private final AgentToolCatalogFacade toolCatalog;
    private final AgentToolExecutorFacade toolExecutor;
    private final AiCompletionGateway completionGateway;
    private final AiProviderRepository providerRepository;
    private final AiUsageBindingRepository usageBindingRepository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, AtomicBoolean> cancellations = new ConcurrentHashMap<>();

    public AutonomousAgentAppService(AgentRunRepository agentRunRepository,
                                     cn.cccs7.cynexus.ai.domain.agent.AgentToolCatalog toolCatalog,
                                     cn.cccs7.cynexus.ai.domain.agent.AgentToolExecutor toolExecutor,
                                     AiCompletionGateway completionGateway,
                                     AiProviderRepository providerRepository,
                                     AiUsageBindingRepository usageBindingRepository,
                                     ObjectMapper objectMapper) {
        this.agentRunRepository = agentRunRepository;
        this.toolCatalog = new AgentToolCatalogFacade(toolCatalog);
        this.toolExecutor = new AgentToolExecutorFacade(toolExecutor);
        this.completionGateway = completionGateway;
        this.providerRepository = providerRepository;
        this.usageBindingRepository = usageBindingRepository;
        this.objectMapper = objectMapper;
    }

    public AgentRunDTO execute(AgentRunRequest request) {
        return execute(request, ignored -> { });
    }

    public AgentRunDTO execute(AgentRunRequest request, Consumer<AgentRunEventBO> eventConsumer) {
        AgentRunRequest safeRequest = request == null ? new AgentRunRequest() : request;
        String ownerKey = valueOr(safeRequest.getOwnerKey(), "anonymous");
        String conversationNo = valueOr(safeRequest.getConversationNo(), "AGC-" + shortId());
        String runNo = "AGR-" + shortId();
        String userInput = valueOr(safeRequest.getUserInput(), "");
        OffsetDateTime startedAt = OffsetDateTime.now();
        AgentConversationBO conversation = agentRunRepository.findConversation(ownerKey, conversationNo)
                .orElseGet(() -> AgentConversationBO.builder().conversationNo(conversationNo).ownerKey(ownerKey)
                        .title("Cynexus Agent").status("ACTIVE").metadataJson("{}").build());
        AgentRunBO run = AgentRunBO.builder()
                .runNo(runNo).conversationNo(conversationNo).ownerKey(ownerKey).status("RUNNING")
                .userInput(userInput).turnCount(0).toolCallCount(0).startedAt(startedAt).build();
        if (conversation.getId() == null) agentRunRepository.saveConversation(conversation);
        agentRunRepository.saveRun(run);
        cancellations.put(runNo, new AtomicBoolean(false));
        emit(eventConsumer, AgentRunEventBO.builder().type("RUN_STARTED").runNo(runNo).status("RUNNING").build());

        List<AgentRunStepBO> steps = new ArrayList<>();
        try {
            AiModelBO model = resolveModel(safeRequest.getPreferredModelCode());
            run.setModelCode(model.getModelCode());
            List<AgentToolDefinition> definitions = toolCatalog.enabledTools();
            List<AgentModelMessage> messages = new ArrayList<>(agentRunRepository.findMessages(ownerKey, conversationNo).stream()
                    .map(this::toModelMessage).toList());
            AgentModelMessage userMessage = AgentModelMessage.builder().role("user").content(userInput).build();
            messages.add(userMessage);
            saveMessage(conversationNo, runNo, messages.size(), userMessage);
            AgentExecutionContext context = AgentExecutionContext.builder()
                    .ownerKey(ownerKey).conversationNo(conversationNo).runNo(runNo)
                    .workspace(safeRequest.getWorkspace()).locale(safeRequest.getLocale())
                    .tradeDate(safeRequest.getTradeDate()).build();

            String answer = null;
            for (int turn = 1; turn <= MAX_TURNS; turn++) {
                checkCancelled(runNo);
                run.setTurnCount(turn);
                AgentModelTurnResponse response = completionGateway.completeAgent(model,
                        AgentModelTurnRequest.builder()
                                .systemPrompt(systemPrompt())
                                .messages(List.copyOf(messages))
                                .tools(definitions).build(), "autonomous_agent");
                if (response == null) throw new IllegalStateException("Agent model returned no response");
                if (!response.hasToolCalls()) {
                    List<cn.cccs7.cynexus.ai.domain.agent.AgentModelToolCall> fallbackCalls = parseFallbackToolCalls(response.getContent());
                    if (!fallbackCalls.isEmpty()) {
                        response = AgentModelTurnResponse.builder().type("TOOL_CALLS").content("")
                                .toolCalls(fallbackCalls).inputTokens(response.getInputTokens())
                                .outputTokens(response.getOutputTokens()).totalTokens(response.getTotalTokens())
                                .rawResponse(response.getRawResponse()).build();
                    }
                }
                if (!response.hasToolCalls()) {
                    answer = valueOr(response.getContent(), "The Agent did not produce an answer.");
                    AgentModelMessage finalMessage = AgentModelMessage.builder().role("assistant").content(answer).build();
                    messages.add(finalMessage);
                    saveMessage(conversationNo, runNo, messages.size(), finalMessage);
                    AgentRunStepBO answerStep = saveAnswerStep(runNo, steps.size() + 1, answer);
                    steps.add(answerStep);
                    emit(eventConsumer, AgentRunEventBO.builder().type("ANSWER").runNo(runNo).status("SUCCEEDED")
                            .stepNo(answerStep.getStepNo()).answer(answer).build());
                    break;
                }
                if (response.getToolCalls().size() > MAX_PARALLEL_READ_CALLS) {
                    throw new IllegalStateException("The Agent requested too many tools in one turn");
                }
                AgentModelMessage assistantMessage = AgentModelMessage.builder().role("assistant")
                        .content(valueOr(response.getContent(), "")).toolCalls(response.getToolCalls()).build();
                messages.add(assistantMessage);
                saveMessage(conversationNo, runNo, messages.size(), assistantMessage);
                for (var modelCall : response.getToolCalls()) {
                    checkCancelled(runNo);
                    if (intValue(run, "toolCallCount") > MAX_TOOL_CALLS) {
                        throw new IllegalStateException("The Agent exceeded the tool call limit");
                    }
                    AgentToolCall call = AgentToolCall.builder().callId(modelCall.getCallId())
                            .toolCode(modelCall.getToolCode()).arguments(modelCall.getArguments()).build();
                    AgentToolResult result = toolExecutor.execute(call, context);
                    AgentRunStepBO step = saveToolStep(runNo, steps.size() + 1, call, result);
                    steps.add(step);
                    emit(eventConsumer, AgentRunEventBO.builder().type("TOOL_STEP").runNo(runNo).status(result.getStatus())
                            .stepNo(step.getStepNo()).toolCode(step.getToolCode()).summary(step.getSummary()).build());
                    AgentModelMessage toolMessage = AgentModelMessage.builder().role("tool").toolCallId(call.getCallId())
                            .toolName(call.getToolCode()).content(result.getOutputJson()).build();
                    messages.add(toolMessage);
                    saveMessage(conversationNo, runNo, messages.size(), toolMessage);
                }
            }
            if (answer == null) throw new IllegalStateException("The Agent exceeded the turn limit");
            run.setStatus("SUCCEEDED");
            run.setAnswer(answer);
            run.setPlanJson(planJson(steps));
            run.setFinishedAt(OffsetDateTime.now());
            agentRunRepository.updateRun(run);
            conversation.setLastRunNo(runNo);
            conversation.setLastMessage(answer);
            conversation.setUpdatedAt(OffsetDateTime.now());
            agentRunRepository.updateConversation(conversation);
            emit(eventConsumer, AgentRunEventBO.builder().type("RUN_COMPLETED").runNo(runNo).status(run.getStatus())
                    .answer(answer).build());
        } catch (RuntimeException exception) {
            run.setStatus(cancellations.getOrDefault(runNo, new AtomicBoolean(false)).get() ? "CANCELLED" : "FAILED");
            run.setErrorMessage(exception.getMessage());
            run.setFinishedAt(OffsetDateTime.now());
            agentRunRepository.updateRun(run);
            emit(eventConsumer, AgentRunEventBO.builder().type("RUN_COMPLETED").runNo(runNo).status(run.getStatus())
                    .errorMessage(exception.getMessage()).build());
        } finally {
            cancellations.remove(runNo);
        }
        return toDTO(run, steps);
    }

    public boolean cancel(String ownerKey, String runNo) {
        if (agentRunRepository.findRun(valueOr(ownerKey, "anonymous"), runNo).isEmpty()) return false;
        AtomicBoolean token = cancellations.get(runNo);
        return token != null && token.compareAndSet(false, true);
    }

    private void checkCancelled(String runNo) {
        AtomicBoolean token = cancellations.get(runNo);
        if (token != null && token.get()) throw new IllegalStateException("Agent run cancelled by user");
    }

    private void emit(Consumer<AgentRunEventBO> consumer, AgentRunEventBO event) {
        if (consumer != null) consumer.accept(event);
    }

    public AgentRunDTO getRun(String ownerKey, String runNo) {
        AgentRunBO run = agentRunRepository.findRun(valueOr(ownerKey, "anonymous"), runNo)
                .orElseThrow(() -> new IllegalArgumentException("Agent run was not found"));
        return toDTO(run, agentRunRepository.findSteps(run.getOwnerKey(), runNo));
    }

    public List<AgentToolDefinitionDTO> enabledTools() {
        return toolCatalog.enabledTools().stream().map(definition -> AgentToolDefinitionDTO.builder()
                .code(definition.getCode()).name(definition.getName()).module(definition.getModule())
                .description(definition.getDescription()).inputSchemaJson(definition.getInputSchemaJson())
                .outputSchemaJson(definition.getOutputSchemaJson()).readOnly(definition.isReadOnly())
                .requiresConfirmation(definition.isRequiresConfirmation()).freshness(definition.getFreshness())
                .source(definition.getSource()).build()).toList();
    }

    public List<AgentConversationDTO> conversations(String ownerKey, int limit) {
        return agentRunRepository.listConversations(valueOr(ownerKey, "anonymous"), limit).stream()
                .map(item -> AgentConversationDTO.builder().conversationNo(item.getConversationNo()).title(item.getTitle())
                        .status(item.getStatus()).lastRunNo(item.getLastRunNo()).lastMessage(item.getLastMessage())
                        .createdAt(item.getCreatedAt()).updatedAt(item.getUpdatedAt()).build()).toList();
    }

    public List<AgentMessageDTO> messages(String ownerKey, String conversationNo) {
        return agentRunRepository.findMessages(valueOr(ownerKey, "anonymous"), conversationNo).stream()
                .map(item -> AgentMessageDTO.builder().sequenceNo(item.getSequenceNo()).role(item.getRole())
                        .content(item.getContent()).toolCallId(item.getToolCallId()).toolName(item.getToolName())
                        .toolCallsJson(item.getToolCallsJson()).createdAt(item.getCreatedAt()).build()).toList();
    }

    private AiModelBO resolveModel(String preferredModelCode) {
        List<AiUsageBindingBO> enabledBindings = usageBindingRepository.listAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .toList();
        AiUsageBindingBO binding = firstBinding(enabledBindings, "autonomous_agent")
                .or(() -> firstBinding(enabledBindings, "workspace_chat"))
                .or(() -> firstBinding(enabledBindings, "text_summary"))
                .orElseThrow(() -> new IllegalStateException("No AI provider is bound to autonomous_agent"));
        AiProviderBO provider = providerRepository.findById(binding.getProviderId());
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            throw new IllegalStateException("The AI provider bound to autonomous_agent is unavailable");
        }
        String modelCode = valueOr(preferredModelCode, valueOr(binding.getModelOverride(), provider.getDefaultModel()));
        return AiModelBO.builder().modelCode(modelCode).modelName(modelCode)
                .providerCode(valueOr(provider.getProviderType(), provider.getCode()))
                .baseUrl(provider.getBaseUrl()).apiKey(provider.getApiKey()).apiKeyEnv(provider.getApiKeyEnv())
                .extra(provider.getExtra()).enabled(true)
                .capability(ModelCapability.builder().structuredOutputSupported(true).toolCallSupported(true).build()).build();
    }

    private java.util.Optional<AiUsageBindingBO> firstBinding(List<AiUsageBindingBO> bindings, String capability) {
        return bindings.stream().filter(item -> capability.equals(item.getCapability())).findFirst();
    }

    private String systemPrompt() {
        return "你是 Cynexus 的自主数据分析 Agent。根据用户请求自主选择并按需调用提供的数据工具，先获取事实再分析。最终回答要包含：结论、使用的数据模块、关键输入输出、数据日期、限制和下一步建议。不要展示隐藏思维链，只展示简洁的工具用途、状态、来源和结果摘要。只能调用只读工具，不得编造工具结果；工具没有数据时明确说明。";
    }

    private AgentRunStepBO saveToolStep(String runNo, int stepNo, AgentToolCall call, AgentToolResult result) {
        AgentRunStepBO step = AgentRunStepBO.builder().runNo(runNo).stepNo(stepNo).kind("TOOL")
                .toolCode(call.getToolCode()).inputJson(json(call.getArguments())).outputJson(result.getOutputJson())
                .status(result.getStatus()).summary(result.getSummary()).source(result.getSource())
                .dataDate(result.getDataDate()).errorMessage(result.getWarnings().isEmpty() ? null : String.join(",", result.getWarnings()))
                .startedAt(OffsetDateTime.now()).finishedAt(OffsetDateTime.now()).build();
        agentRunRepository.saveStep(step);
        return step;
    }

    private AgentRunStepBO saveAnswerStep(String runNo, int stepNo, String answer) {
        AgentRunStepBO step = AgentRunStepBO.builder().runNo(runNo).stepNo(stepNo).kind("ANSWER")
                .status("SUCCEEDED").summary("Generated the final Agent answer.").outputJson(json(Map.of("content", answer)))
                .startedAt(OffsetDateTime.now()).finishedAt(OffsetDateTime.now()).build();
        agentRunRepository.saveStep(step);
        return step;
    }

    private String planJson(List<AgentRunStepBO> steps) {
        List<Map<String, Object>> plan = steps.stream().filter(step -> "TOOL".equals(step.getKind()))
                .map(step -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("toolCode", step.getToolCode());
                    item.put("summary", valueOr(step.getSummary(), ""));
                    item.put("status", valueOr(step.getStatus(), ""));
                    return item;
                }).toList();
        return json(Map.of("steps", plan));
    }

    private AgentRunDTO toDTO(AgentRunBO run, List<AgentRunStepBO> steps) {
        return AgentRunDTO.builder().runNo(run.getRunNo()).conversationNo(run.getConversationNo()).status(run.getStatus())
                .userInput(run.getUserInput()).answer(run.getAnswer()).modelCode(run.getModelCode()).planJson(run.getPlanJson())
                .turnCount(run.getTurnCount()).toolCallCount(run.getToolCallCount()).errorMessage(run.getErrorMessage())
                .startedAt(run.getStartedAt()).finishedAt(run.getFinishedAt())
                .steps(steps == null ? List.of() : steps.stream().map(step -> AgentRunStepDTO.builder()
                        .stepNo(step.getStepNo()).kind(step.getKind()).toolCode(step.getToolCode()).status(step.getStatus())
                        .summary(step.getSummary()).source(step.getSource()).dataDate(step.getDataDate())
                        .inputJson(step.getInputJson()).outputJson(step.getOutputJson()).errorMessage(step.getErrorMessage())
                        .startedAt(step.getStartedAt()).finishedAt(step.getFinishedAt()).build()).toList()).build();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private void saveMessage(String conversationNo, String runNo, int sequenceNo, AgentModelMessage message) {
        agentRunRepository.saveMessage(AgentMessageBO.builder().conversationNo(conversationNo).runNo(runNo)
                .sequenceNo(sequenceNo).role(message.getRole()).content(message.getContent())
                .toolCallId(message.getToolCallId()).toolName(message.getToolName())
                .toolCallsJson(json(message.getToolCalls() == null ? List.of() : message.getToolCalls())).build());
    }

    private AgentModelMessage toModelMessage(AgentMessageBO message) {
        List<cn.cccs7.cynexus.ai.domain.agent.AgentModelToolCall> calls = List.of();
        if (message.getToolCallsJson() != null && !message.getToolCallsJson().isBlank()) {
            try {
                calls = objectMapper.readValue(message.getToolCallsJson(), objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, cn.cccs7.cynexus.ai.domain.agent.AgentModelToolCall.class));
            } catch (Exception ignored) {
                calls = List.of();
            }
        }
        return AgentModelMessage.builder().role(message.getRole()).content(valueOr(message.getContent(), ""))
                .toolCallId(message.getToolCallId()).toolName(message.getToolName()).toolCalls(calls).build();
    }

    private List<cn.cccs7.cynexus.ai.domain.agent.AgentModelToolCall> parseFallbackToolCalls(String content) {
        if (content == null || content.isBlank()) return List.of();
        try {
            var root = objectMapper.readTree(content);
            String type = root.path("type").asText("");
            if (!"tool_calls".equals(type) && !"action".equals(type)) return List.of();
            var calls = root.get("calls");
            if (calls == null || !calls.isArray() || calls.isEmpty() || calls.size() > MAX_PARALLEL_READ_CALLS) return List.of();
            List<cn.cccs7.cynexus.ai.domain.agent.AgentModelToolCall> parsed = new ArrayList<>();
            for (var item : calls) {
                String toolCode = item.path("toolCode").asText(item.path("tool").asText(""));
                if (toolCode.isBlank() || !item.has("arguments") || !item.get("arguments").isObject()) return List.of();
                parsed.add(cn.cccs7.cynexus.ai.domain.agent.AgentModelToolCall.builder()
                        .callId(item.path("callId").asText("fallback-" + parsed.size()))
                        .toolCode(toolCode).arguments(objectMapper.convertValue(item.get("arguments"), Map.class)).build());
            }
            return parsed;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private int intValue(AgentRunBO run, String field) {
        int value = (run.getToolCallCount() == null ? 0 : run.getToolCallCount()) + 1;
        run.setToolCallCount(value);
        return value;
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    private record AgentToolCatalogFacade(cn.cccs7.cynexus.ai.domain.agent.AgentToolCatalog delegate) {
        List<AgentToolDefinition> enabledTools() { return delegate.enabledTools(); }
    }

    private record AgentToolExecutorFacade(cn.cccs7.cynexus.ai.domain.agent.AgentToolExecutor delegate) {
        AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) { return delegate.execute(call, context); }
    }
}
