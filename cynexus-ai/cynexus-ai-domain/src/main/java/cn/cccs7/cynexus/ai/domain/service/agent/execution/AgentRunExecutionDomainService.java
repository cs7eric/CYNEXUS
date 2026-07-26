package cn.cccs7.cynexus.ai.domain.service.agent.execution;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentClarificationBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.domain.exception.quota.AgentQuotaExceededException;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolExecutionPort;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolExecutor;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AgentProviderTurnPort;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.gateway.observability.AgentObservabilityPort;
import cn.cccs7.cynexus.ai.domain.gateway.quota.AgentQuotaPort;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRateLimitPort;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentHistoryCollapsePolicy;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentPromptCacheKeyPolicy;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentRunArtifactPolicy;
import cn.cccs7.cynexus.ai.domain.policy.sensitivedata.AgentSensitiveDataPolicy;
import cn.cccs7.cynexus.ai.domain.service.agent.prompt.AgentSystemPromptDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentRunCancellationOutcome;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentRunExecutionCommand;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentRunExecutionResult;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCallAdmission;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolExecutionOutcome;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheTelemetry;
import cn.cccs7.cynexus.common.core.constant.TraceId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AgentRunExecutionDomainService {

    private static final int MAX_RUNS_PER_MINUTE = 30;

    private final AgentRunDomainService runDomainService;
    private final AgentToolCatalog toolCatalog;
    private final AgentToolExecutor toolExecutor;
    private final AiCompletionGateway completionGateway;
    private final ObjectMapper objectMapper;
    private final AgentRuntimeLeasePort runtimeLease;
    private final AgentRateLimitPort rateLimiter;
    private final AgentQuotaPort quota;
    private final AgentObservabilityPort observability;
    private final AgentExecutionDomainService executionDomainService;
    private final AgentTurnDomainService turnDomainService;
    private final AgentSystemPromptDomainService systemPromptDomainService;

    public AgentRunExecutionDomainService(AgentRunDomainService runDomainService,
                                          AgentToolCatalog toolCatalog,
                                          AgentToolExecutor toolExecutor,
                                          AiCompletionGateway completionGateway,
                                          ObjectMapper objectMapper,
                                          AgentRuntimeLeasePort runtimeLease,
                                          AgentRateLimitPort rateLimiter,
                                          AgentQuotaPort quota,
                                          AgentObservabilityPort observability,
                                          AgentExecutionDomainService executionDomainService,
                                          AgentTurnDomainService turnDomainService,
                                          AgentSystemPromptDomainService systemPromptDomainService) {
        this.runDomainService = runDomainService;
        this.toolCatalog = toolCatalog;
        this.toolExecutor = toolExecutor;
        this.completionGateway = completionGateway;
        this.objectMapper = objectMapper;
        this.runtimeLease = runtimeLease;
        this.rateLimiter = rateLimiter;
        this.quota = quota;
        this.observability = observability;
        this.executionDomainService = executionDomainService;
        this.turnDomainService = turnDomainService;
        this.systemPromptDomainService = systemPromptDomainService;
    }

    public AgentRunExecutionResult execute(AgentRunExecutionCommand command,
                                           Consumer<AgentRunEventBO> eventConsumer) {
        AgentRunExecutionCommand safeCommand = command == null ? AgentRunExecutionCommand.builder().build() : command;
        AgentIdentity safeIdentity = safeCommand.getIdentity() == null
                ? AgentIdentity.singleUser() : safeCommand.getIdentity();
        String ownerKey = safeIdentity.storageOwnerKey();
        String conversationNo = valueOr(safeCommand.getConversationNo(), "AGC-" + shortId());
        String runNo = valueOr(safeCommand.getRunNo(), "AGR-" + shortId());
        String userInput = executionDomainService.validateUserInput(safeCommand.getUserInput());
        String idempotencyKey = executionDomainService.normalizeIdempotencyKey(safeCommand.getIdempotencyKey());
        if (idempotencyKey != null) {
            var existing = runDomainService.findRunByIdempotencyKey(ownerKey, idempotencyKey);
            if (existing.isPresent()) {
                AgentRunBO existingRun = existing.get();
                return AgentRunExecutionResult.builder()
                        .run(existingRun)
                        .steps(runDomainService.findSteps(existingRun.getOwnerKey(), existingRun.getRunNo()))
                        .build();
            }
        }
        if (!rateLimiter.allow(ownerKey, MAX_RUNS_PER_MINUTE, 60)) {
            throw new IllegalStateException("Agent request rate limit exceeded for this owner");
        }
        if (!runtimeLease.tryAcquire(ownerKey, runNo, 2, executionDomainService.limits().getMaxRunSeconds() + 60)) {
            throw new IllegalStateException("Too many active Agent runs for this owner");
        }
        int quotaReservationTokens = Math.max(1_024, Math.min(16_000, userInput.length() / 4 + 1_024));
        if (!quota.reserveRequest(ownerKey, safeIdentity.getTenantKey(), safeIdentity.getPrincipalKey(),
                quotaReservationTokens)) {
            runtimeLease.release(ownerKey, runNo);
            throw new AgentQuotaExceededException();
        }
        OffsetDateTime startedAt = OffsetDateTime.now();
        AgentConversationBO conversation = runDomainService.findConversation(ownerKey, conversationNo)
                .orElseGet(() -> AgentConversationBO.builder().conversationNo(conversationNo).ownerKey(ownerKey)
                        .title("Cynexus Agent").status("ACTIVE").metadataJson("{}").build());
        AgentRunBO run = AgentRunBO.builder()
                .runNo(runNo)
                .conversationNo(conversationNo)
                .ownerKey(ownerKey)
                .idempotencyKey(idempotencyKey)
                .status("RUNNING")
                .userInput(redactText(userInput))
                .contextJson(json(Map.of(
                        "workspace", valueOr(safeCommand.getWorkspace(), ""),
                        "locale", valueOr(safeCommand.getLocale(), "zh-CN"),
                        "tradeDate", safeCommand.getTradeDate() == null ? "" : safeCommand.getTradeDate().toString())))
                .assumptionsJson("[]")
                .clarificationJson("null")
                .evidenceJson("[]")
                .warningsJson("[]")
                .turnCount(0)
                .toolCallCount(0)
                .startedAt(startedAt)
                .build();
        try {
            if (conversation.getId() == null) runDomainService.saveConversation(conversation);
            runDomainService.saveRun(run);
        } catch (RuntimeException exception) {
            runtimeLease.release(ownerKey, runNo);
            if (idempotencyKey != null) {
                var existing = runDomainService.findRunByIdempotencyKey(ownerKey, idempotencyKey);
                if (existing.isPresent()) {
                    AgentRunBO existingRun = existing.get();
                    return AgentRunExecutionResult.builder()
                            .run(existingRun)
                            .steps(runDomainService.findSteps(existingRun.getOwnerKey(), existingRun.getRunNo()))
                            .build();
                }
            }
            throw exception;
        }
        AtomicInteger eventSequence = new AtomicInteger(0);
        String traceId = TraceId.current();
        long runStartedNanos = System.nanoTime();
        observability.runStarted(traceId);
        Consumer<AgentRunEventBO> publisher = event -> {
            AgentRunEventBO sequenced = event.toBuilder()
                    .traceId(traceId)
                    .summary(redactText(event.getSummary()))
                    .answer(redactText(event.getAnswer()))
                    .delta(redactText(event.getDelta()))
                    .errorMessage(redactText(event.getErrorMessage()))
                    .detailsJson(AgentSensitiveDataPolicy.redactJson(event.getDetailsJson()))
                    .sequenceNo(eventSequence.incrementAndGet())
                    .occurredAt(OffsetDateTime.now())
                    .build();
            runDomainService.saveEvent(ownerKey, sequenced);
            emit(eventConsumer, sequenced);
        };
        publisher.accept(AgentRunEventBO.builder().type("RUN_STARTED").runNo(runNo).status("RUNNING").build());

        List<AgentRunStepBO> steps = new ArrayList<>();
        try {
            AiModelBO model = runDomainService.resolveModel(
                    safeCommand.getPreferredProviderCode(), safeCommand.getPreferredModelCode(),
                    safeCommand.getPreferredReasoningEffort());
            run.setProviderCode(model.getProviderIdentity());
            run.setModelCode(model.getModelCode());
            List<AgentToolDefinition> definitions = toolCatalog.enabledTools();
            String stableSystemPrompt = systemPromptDomainService.resolveStable(executionDomainService.limits());
            List<AgentMessageBO> persistedMessages = runDomainService.findMessages(ownerKey, conversationNo);
            AtomicInteger messageSequence = new AtomicInteger(AgentRunDomainService.nextMessageSequence(persistedMessages));
            List<AgentModelMessage> messages = new ArrayList<>(AgentHistoryCollapsePolicy.collapseForProvider(
                    persistedMessages.stream().map(runDomainService::toModelMessage).toList()));
            AgentModelMessage userMessage = AgentModelMessage.builder().role("user").content(userInput).build();
            messages.add(userMessage);
            runDomainService.saveMessage(conversationNo, runNo, messageSequence.incrementAndGet(), userMessage);
            PromptCacheContext promptCacheContext = AgentPromptCacheKeyPolicy.context(
                    ownerKey, conversationNo, stableSystemPrompt, model, definitions);
            List<AgentModelMessage> continuationMessages = new ArrayList<>();
            continuationMessages.add(userMessage);
            String providerResponseId = reusableProviderResponseId(conversation, model, promptCacheContext);
            AgentExecutionContext context = AgentExecutionContext.builder()
                    .ownerKey(ownerKey)
                    .tenantKey(safeIdentity.getTenantKey())
                    .conversationNo(conversationNo)
                    .runNo(runNo)
                    .workspace(safeCommand.getWorkspace())
                    .locale(safeCommand.getLocale())
                    .tradeDate(safeCommand.getTradeDate())
                    .build();

            String answer = null;
            AgentClarificationBO clarification = null;
            int inputTokens = 0;
            int cachedInputTokens = 0;
            int cacheWriteTokens = 0;
            int outputTokens = 0;
            int totalTokens = 0;
            boolean estimatedUsage = false;
            int toolBudget = executionDomainService.toolBudgetFor(userInput);
            boolean forceFinalAnswer = false;
            boolean budgetExhaustedNotified = false;
            for (int turn = 1; turn <= executionDomainService.limits().getMaxTurns(); turn++) {
                checkCancelled(ownerKey, runNo);
                executionDomainService.checkTurn(turn, startedAt, OffsetDateTime.now());
                run.setTurnCount(turn);
                int usedTools = run.getToolCallCount() == null ? 0 : run.getToolCallCount();
                if (usedTools >= toolBudget) forceFinalAnswer = true;
                String runtimeContext = systemPromptDomainService.resolveRuntimeContext(
                        currentSystemDate(), toolBudget, usedTools, forceFinalAnswer,
                        executionDomainService.limits());
                AgentModelTurnRequest modelRequest = AgentModelTurnRequest.builder()
                        .systemPrompt(stableSystemPrompt)
                        .runtimeContext(runtimeContext)
                        .messages(List.copyOf(messages))
                        .continuationMessages(List.copyOf(continuationMessages))
                        .previousResponseId(providerResponseId)
                        .tools(definitions)
                        .toolsAllowed(!forceFinalAnswer)
                        .promptCacheContext(promptCacheContext)
                        .build();
                StringBuilder turnThinking = new StringBuilder();
                AgentModelTurnResponse response = completeAgentWithRetry(model, modelRequest, ownerKey, runNo,
                        safeCommand.isStreamTokens() ? delta -> publisher.accept(AgentRunEventBO.builder()
                                .type("TOKEN_DELTA").runNo(runNo).status("STREAMING").delta(delta).build()) : null,
                        safeCommand.isStreamTokens() ? thinking -> {
                            if (thinking == null || thinking.isBlank()) return;
                            turnThinking.append(thinking);
                            publisher.accept(AgentRunEventBO.builder().type("THINKING_DELTA")
                                    .runNo(runNo).status("STREAMING").delta(thinking).build());
                        } : null,
                        attempt -> {
                            run.setStatus("RETRYING");
                            runDomainService.updateRun(run);
                            publisher.accept(AgentRunEventBO.builder().type("RETRYING").runNo(runNo)
                                    .status("RETRYING")
                                    .summary("Retrying the AI provider request (attempt " + attempt + ").")
                                    .build());
                        });
                if (!turnThinking.isEmpty()) {
                    AgentRunStepBO thinkingStep = runDomainService.saveThinkingStep(
                            ownerKey, runNo, steps.size() + 1, turnThinking.toString());
                    steps.add(thinkingStep);
                    publisher.accept(AgentRunEventBO.builder().type("THINKING").runNo(runNo).status("SUCCEEDED")
                            .stepNo(thinkingStep.getStepNo()).summary(thinkingStep.getSummary()).build());
                }
                run.setStatus("RUNNING");
                runDomainService.updateRun(run);
                if (response == null) throw new IllegalStateException("Agent model returned no response");
                providerResponseId = valueOrNull(response.getProviderResponseId());
                updateProviderState(conversation, model, promptCacheContext, providerResponseId);
                runDomainService.updateConversation(conversation);
                continuationMessages.clear();
                // Keep the previous turn's runtime suffix in the next request so the full
                // conversation prefix remains byte-for-byte cacheable.
                messages.add(AgentModelMessage.builder().role("system").content(runtimeContext).build());
                inputTokens += nonNegative(response.getInputTokens());
                cachedInputTokens += nonNegative(response.getCachedInputTokens());
                cacheWriteTokens += nonNegative(response.getCacheWriteTokens());
                outputTokens += nonNegative(response.getOutputTokens());
                totalTokens += nonNegative(response.getTotalTokens());
                estimatedUsage |= !hasReportedUsage(response);
                run.setInputTokens(inputTokens);
                run.setCachedInputTokens(cachedInputTokens);
                run.setCacheWriteTokens(cacheWriteTokens);
                run.setOutputTokens(outputTokens);
                run.setTotalTokens(totalTokens);
                run.setUsageSource(estimatedUsage ? "ESTIMATED" : "REPORTED");
                run.setEstimatedCost(executionDomainService.estimateCost(model, inputTokens, outputTokens));
                publisher.accept(AgentRunEventBO.builder()
                        .type(AiConstants.AGENT_EVENT_CACHE_USAGE)
                        .runNo(runNo)
                        .status("SUCCEEDED")
                        .summary(cacheUsageSummary(turn, response))
                        .detailsJson(cacheUsageDetails(turn, response))
                        .build());
                if (!response.hasToolCalls()) {
                    List<AgentModelToolCall> fallbackCalls =
                            executionDomainService.parseFallbackToolCalls(response.getContent());
                    if (!fallbackCalls.isEmpty()) {
                        providerResponseId = null;
                        updateProviderState(conversation, model, promptCacheContext, null);
                        runDomainService.updateConversation(conversation);
                        response = response.toBuilder()
                                .type("TOOL_CALLS")
                                .content("")
                                .toolCalls(fallbackCalls)
                                .build();
                    }
                }
                if (!response.hasToolCalls()) {
                    clarification = executionDomainService.parseClarification(response.getContent());
                    answer = clarification == null
                            ? executionDomainService.normalizeFinalAnswer(response.getContent())
                            : executionDomainService.clarificationAnswer(clarification);
                    answer = redactText(answer);
                    AgentModelMessage finalMessage = AgentModelMessage.builder().role("assistant").content(answer).build();
                    messages.add(finalMessage);
                    runDomainService.saveMessage(conversationNo, runNo, messageSequence.incrementAndGet(), finalMessage);
                    AgentRunStepBO answerStep = clarification == null
                            ? runDomainService.saveAnswerStep(ownerKey, runNo, steps.size() + 1, answer)
                            : runDomainService.saveClarificationStep(ownerKey, runNo, steps.size() + 1, clarification);
                    steps.add(answerStep);
                    publisher.accept(AgentRunEventBO.builder().type(clarification == null ? "ANSWER" : "CLARIFICATION")
                            .runNo(runNo)
                            .status(clarification == null ? "SUCCEEDED" : "NEEDS_CLARIFICATION")
                            .stepNo(answerStep.getStepNo())
                            .answer(answer)
                            .build());
                    break;
                }
                int persistedToolCalls = run.getToolCallCount() == null ? 0 : run.getToolCallCount();
                AgentToolCallAdmission admission = executionDomainService.admitToolCalls(
                        response.getToolCalls(), persistedToolCalls, toolBudget);
                if (forceFinalAnswer) {
                    answer = executionDomainService.budgetExhaustedAnswer(steps, response.getContent());
                    AgentModelMessage finalMessage = AgentModelMessage.builder().role("assistant").content(answer).build();
                    messages.add(finalMessage);
                    runDomainService.saveMessage(conversationNo, runNo, messageSequence.incrementAndGet(), finalMessage);
                    AgentRunStepBO answerStep = runDomainService.saveAnswerStep(
                            ownerKey, runNo, steps.size() + 1, answer);
                    steps.add(answerStep);
                    publisher.accept(AgentRunEventBO.builder().type("ANSWER").runNo(runNo).status("SUCCEEDED")
                            .stepNo(answerStep.getStepNo()).answer(answer)
                            .summary("Stopped further tool calls after the soft tool budget was reached.").build());
                    break;
                }
                AgentModelMessage assistantMessage = AgentModelMessage.builder().role("assistant")
                        .content(valueOr(response.getContent(), ""))
                        .toolCalls(response.getToolCalls())
                        .build();
                messages.add(assistantMessage);
                runDomainService.saveMessage(conversationNo, runNo, messageSequence.incrementAndGet(), assistantMessage);
                java.util.Set<String> acceptedIds = admission.getAccepted().stream()
                        .map(AgentModelToolCall::getCallId)
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
                for (var modelCall : response.getToolCalls()) {
                    checkCancelled(ownerKey, runNo);
                    AgentToolCall call = AgentToolCall.builder()
                            .callId(modelCall.getCallId())
                            .toolCode(modelCall.getToolCode())
                            .arguments(modelCall.getArguments())
                            .build();
                    if (!acceptedIds.contains(modelCall.getCallId())) {
                        AgentToolResult skipped = AgentToolResult.builder()
                                .toolCode(call.getToolCode())
                                .status("SKIPPED")
                                .summary("Skipped because the soft tool budget was exhausted.")
                                .outputJson("{\"status\":\"SKIPPED\",\"reason\":\"tool_budget_exhausted\",\"toolCode\":\""
                                        + (modelCall.getToolCode() == null ? "" : modelCall.getToolCode()) + "\"}")
                                .warnings(List.of("tool_budget_exhausted"))
                                .citations(List.of())
                                .build();
                        AgentRunStepBO step = runDomainService.saveToolStep(ownerKey, runNo, steps.size() + 1,
                                call, skipped, 1, null, toolFreshness(definitions, call.getToolCode()),
                                executionDomainService.classifyToolFailure(skipped));
                        steps.add(step);
                        publisher.accept(AgentRunEventBO.builder().type("TOOL_STEP").runNo(runNo).status("SKIPPED")
                                .stepNo(step.getStepNo()).toolCode(step.getToolCode()).summary(step.getSummary()).build());
                        AgentModelMessage toolMessage = AgentModelMessage.builder()
                                .role("tool")
                                .toolCallId(call.getCallId())
                                .toolName(call.getToolCode())
                                .content(skipped.getOutputJson())
                                .build();
                        messages.add(toolMessage);
                        continuationMessages.add(toolMessage);
                        runDomainService.saveMessage(conversationNo, runNo, messageSequence.incrementAndGet(), toolMessage);
                        continue;
                    }
                    executionDomainService.incrementToolCallCount(run);
                    AgentToolDefinition definition = definitions.stream()
                            .filter(item -> call.getToolCode().equals(item.getCode()))
                            .findFirst()
                            .orElse(null);
                    AgentToolExecutionOutcome outcome = turnDomainService.executeToolWithRetry(call, context,
                            (AgentToolExecutionPort) (toolCall, toolContext) ->
                                    executeToolWithCancellation(toolCall, toolContext, ownerKey, runNo),
                            attempt -> {
                                run.setStatus("RETRYING");
                                runDomainService.updateRun(run);
                                publisher.accept(AgentRunEventBO.builder().type("RETRYING").runNo(runNo)
                                        .status("RETRYING")
                                        .toolCode(call.getToolCode())
                                        .summary("Retrying the data tool (attempt " + attempt + ").")
                                        .build());
                                pauseBeforeRetry(attempt);
                            });
                    int attempts = outcome.getAttempts();
                    AgentToolResult result = executionDomainService.sanitizeToolResult(outcome.getResult());
                    run.setStatus("RUNNING");
                    runDomainService.updateRun(run);
                    AgentRunStepBO step = runDomainService.saveToolStep(ownerKey, runNo, steps.size() + 1,
                            call, result, attempts, definition == null ? null : definition.getSourceUrl(),
                            toolFreshness(definitions, call.getToolCode()),
                            executionDomainService.classifyToolFailure(result));
                    steps.add(step);
                    publisher.accept(AgentRunEventBO.builder().type("TOOL_STEP").runNo(runNo).status(result.getStatus())
                            .stepNo(step.getStepNo()).toolCode(step.getToolCode()).summary(step.getSummary()).build());
                    AgentModelMessage toolMessage = AgentModelMessage.builder()
                            .role("tool")
                            .toolCallId(call.getCallId())
                            .toolName(call.getToolCode())
                            .content(result.getOutputJson())
                            .build();
                    messages.add(toolMessage);
                    continuationMessages.add(toolMessage);
                    runDomainService.saveMessage(conversationNo, runNo, messageSequence.incrementAndGet(), toolMessage);
                }
                int usedAfter = run.getToolCallCount() == null ? 0 : run.getToolCallCount();
                if (admission.isBudgetExhausted() || usedAfter >= toolBudget) {
                    forceFinalAnswer = true;
                    if (!budgetExhaustedNotified) {
                        AgentModelMessage budgetMessage = AgentModelMessage.builder().role("user").content(
                                "系统提示：本轮工具预算已用尽（上限 " + toolBudget + " 次）。"
                                        + "请停止调用工具，仅基于已有工具结果给出最终回答；"
                                        + "如数据不足请明确说明缺口，不要再发起 tool calls。").build();
                        messages.add(budgetMessage);
                        continuationMessages.add(budgetMessage);
                        budgetExhaustedNotified = true;
                        publisher.accept(AgentRunEventBO.builder().type("RETRYING").runNo(runNo).status("RUNNING")
                                .summary("Tool budget reached; forcing a final answer from collected evidence.").build());
                    }
                }
            }
            if (answer == null) throw new IllegalStateException("The Agent exceeded the turn limit");
            run.setStatus(clarification == null ? "SUCCEEDED" : "NEEDS_CLARIFICATION");
            answer = redactText(answer);
            run.setAnswer(answer);
            run.setClarificationJson(clarification == null ? "null" : json(clarification));
            run.setPlanJson(AgentRunArtifactPolicy.planJson(steps));
            run.setEvidenceJson(AgentRunArtifactPolicy.evidenceJson(steps));
            run.setWarningsJson(AgentRunArtifactPolicy.warningsJson(steps));
            run.setFinishedAt(OffsetDateTime.now());
            runDomainService.updateRun(run);
            conversation.setLastRunNo(runNo);
            conversation.setLastMessage(answer);
            conversation.setUpdatedAt(OffsetDateTime.now());
            runDomainService.updateConversation(conversation);
            publisher.accept(AgentRunEventBO.builder().type("RUN_COMPLETED").runNo(runNo).status(run.getStatus())
                    .answer(answer).build());
        } catch (RuntimeException exception) {
            boolean cancelled = isCancellationRequested(ownerKey, runNo);
            run.setStatus(cancelled ? "CANCELLED" : "FAILED");
            run.setErrorMessage(redactText(exception.getMessage()));
            run.setFailureCategory(cancelled ? "CANCELLED" : executionDomainService.classifyFailure(exception));
            run.setFinishedAt(OffsetDateTime.now());
            runDomainService.updateRun(run);
            publisher.accept(AgentRunEventBO.builder().type("RUN_COMPLETED").runNo(runNo).status(run.getStatus())
                    .errorMessage(exception.getMessage()).build());
        } finally {
            try {
                quota.recordUsage(ownerKey, safeIdentity.getTenantKey(), safeIdentity.getPrincipalKey(),
                        quotaReservationTokens, nonNegative(run.getInputTokens()), nonNegative(run.getOutputTokens()),
                        nonNegative(run.getTotalTokens()), run.getEstimatedCost(),
                        valueOr(run.getUsageSource(), "ESTIMATED"));
            } catch (RuntimeException ignored) {
                // Usage accounting must not turn a completed Agent answer into a false failure.
            }
            runtimeLease.release(ownerKey, runNo);
            observability.runCompleted(run.getStatus(), elapsedMs(runStartedNanos));
        }
        return AgentRunExecutionResult.builder().run(run).steps(steps).build();
    }

    public AgentRunCancellationOutcome cancel(String ownerKey, String runNo) {
        String safeOwnerKey = valueOr(ownerKey, AgentIdentity.singleUser().storageOwnerKey());
        AgentRunBO current = runDomainService.findRun(safeOwnerKey, runNo).orElse(null);
        if (current == null) return AgentRunCancellationOutcome.notFound(runNo);
        if (isTerminal(current.getStatus())) {
            return AgentRunCancellationOutcome.alreadyTerminal(runNo, current.getStatus());
        }
        AgentRunBO requested = runDomainService.requestCancellation(safeOwnerKey, runNo, safeOwnerKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Agent cancellation request could not be persisted: " + runNo));
        runtimeLease.requestCancellation(safeOwnerKey, runNo, executionDomainService.limits().getMaxRunSeconds() + 60);
        return AgentRunCancellationOutcome.requested(runNo, requested.getStatus());
    }

    public boolean isTerminal(String status) {
        return "SUCCEEDED".equals(status) || "NEEDS_CLARIFICATION".equals(status)
                || "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    private AgentModelTurnResponse completeAgentWithRetry(AiModelBO model, AgentModelTurnRequest request,
                                                          String ownerKey, String runNo,
                                                          Consumer<String> textDeltaConsumer,
                                                          Consumer<String> thinkingDeltaConsumer,
                                                          Consumer<Integer> retryConsumer) {
        AgentProviderTurnPort provider = (resolvedModel, turnRequest, deltaConsumer) ->
                awaitCancellable(() -> deltaConsumer == null
                        ? completionGateway.completeAgent(resolvedModel, turnRequest, "autonomous_agent")
                        : completionGateway.streamAgent(resolvedModel, turnRequest, "autonomous_agent",
                        deltaConsumer, thinkingDeltaConsumer), ownerKey, runNo);
        return turnDomainService.completeWithRetry(model, request, ownerKey, runNo, provider,
                textDeltaConsumer, attempt -> {
                    if (retryConsumer != null) retryConsumer.accept(attempt);
                    pauseBeforeRetry(attempt);
                });
    }

    private AgentToolResult executeToolWithCancellation(AgentToolCall call, AgentExecutionContext context,
                                                        String ownerKey, String runNo) {
        return awaitCancellable(() -> toolExecutor.execute(call, context), ownerKey, runNo);
    }

    private <T> T awaitCancellable(Supplier<T> supplier, String ownerKey, String runNo) {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Thread worker = new Thread(task, "cynexus-agent-worker-" + shortId());
        worker.setDaemon(true);
        worker.start();
        try {
            while (true) {
                checkCancelled(ownerKey, runNo);
                try {
                    return task.get(200, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignored) {
                    // poll cancellation
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cancelWorker(task, worker);
            throw new IllegalStateException("Agent execution was interrupted", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException(cause == null ? "Agent execution failed" : cause.getMessage(), cause);
        } catch (RuntimeException exception) {
            cancelWorker(task, worker);
            throw exception;
        }
    }

    private void checkCancelled(String ownerKey, String runNo) {
        if (isCancellationRequested(ownerKey, runNo)) {
            throw new IllegalStateException("Agent run was cancelled");
        }
    }

    private boolean isCancellationRequested(String ownerKey, String runNo) {
        return executionDomainService.isCancellationRequested(
                runtimeLease.isCancellationRequested(runNo),
                runDomainService.isCancellationRequested(ownerKey, runNo));
    }

    private void cancelWorker(FutureTask<?> task, Thread worker) {
        task.cancel(true);
        if (worker != null) worker.interrupt();
    }

    private void pauseBeforeRetry(int attempt) {
        try {
            Thread.sleep(Math.min(1_500L, 200L * Math.max(1, attempt)));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Agent retry was interrupted", exception);
        }
    }

    private boolean hasReportedUsage(AgentModelTurnResponse response) {
        String raw = response.getRawResponse();
        if (raw == null || raw.isBlank()) return false;
        try {
            var node = objectMapper.readTree(raw);
            return node.has("usage") || node.has("usageDetails");
        } catch (JsonProcessingException ignored) {
            return false;
        }
    }

    private String reusableProviderResponseId(AgentConversationBO conversation, AiModelBO model,
                                              PromptCacheContext promptCacheContext) {
        if (conversation == null || model == null || promptCacheContext == null) return null;
        boolean sameProvider = Objects.equals(valueOrNull(conversation.getProviderStateProviderCode()),
                valueOrNull(model.getProviderIdentity()));
        boolean sameModel = Objects.equals(valueOrNull(conversation.getProviderStateModelCode()),
                valueOrNull(model.getModelCode()));
        boolean sameSignature = Objects.equals(valueOrNull(conversation.getProviderStateSignature()),
                valueOrNull(promptCacheContext.getRoutingKey()));
        return sameProvider && sameModel && sameSignature
                ? valueOrNull(conversation.getProviderResponseId()) : null;
    }

    private void updateProviderState(AgentConversationBO conversation, AiModelBO model,
                                     PromptCacheContext promptCacheContext, String providerResponseId) {
        String responseId = valueOrNull(providerResponseId);
        conversation.setProviderResponseId(responseId);
        conversation.setProviderStateProviderCode(responseId == null || model == null
                ? null : valueOrNull(model.getProviderIdentity()));
        conversation.setProviderStateModelCode(responseId == null || model == null
                ? null : valueOrNull(model.getModelCode()));
        conversation.setProviderStateSignature(responseId == null || promptCacheContext == null
                ? null : valueOrNull(promptCacheContext.getRoutingKey()));
        conversation.setProviderStateUpdatedAt(responseId == null ? null : OffsetDateTime.now());
        conversation.setUpdatedAt(OffsetDateTime.now());
    }

    private String cacheUsageSummary(int turn, AgentModelTurnResponse response) {
        int input = nonNegative(response == null ? null : response.getInputTokens());
        int cached = nonNegative(response == null ? null : response.getCachedInputTokens());
        double ratio = input == 0 ? 0D : (double) cached / input;
        return "Turn " + turn + " prompt cache: " + cached + "/" + input
                + " input tokens (" + Math.round(ratio * 1_000D) / 10D + "%).";
    }

    private String cacheUsageDetails(int turn, AgentModelTurnResponse response) {
        PromptCacheTelemetry telemetry = response == null ? null : response.getCacheTelemetry();
        int input = nonNegative(response == null ? null : response.getInputTokens());
        int cached = nonNegative(response == null ? null : response.getCachedInputTokens());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_TURN_FIELD, turn);
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_INPUT_TOKENS_FIELD, input);
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_CACHED_INPUT_TOKENS_FIELD, cached);
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_WRITE_TOKENS_FIELD,
                nonNegative(response == null ? null : response.getCacheWriteTokens()));
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_HIT_RATIO_FIELD,
                input == 0 ? 0D : (double) cached / input);
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_KEY_HASH_FIELD,
                telemetry == null ? null : telemetry.getCacheKeyHash());
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_STABLE_PREFIX_HASH_FIELD,
                telemetry == null ? null : telemetry.getStablePrefixHash());
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_REQUEST_INPUT_HASH_FIELD,
                telemetry == null ? null : telemetry.getRequestInputHash());
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_INPUT_ITEM_COUNT_FIELD,
                telemetry == null ? null : telemetry.getInputItemCount());
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_CONTINUATION_MODE_FIELD,
                telemetry == null ? null : telemetry.getContinuationMode());
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_STORAGE_ENABLED_FIELD,
                telemetry != null && telemetry.isResponseStorageEnabled());
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_STATE_FALLBACK_FIELD,
                telemetry != null && telemetry.isStateFallback());
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_PREVIOUS_RESPONSE_HASH_FIELD,
                telemetry == null ? null : telemetry.getPreviousResponseIdHash());
        details.put(AiConstants.AGENT_CACHE_TELEMETRY_PROVIDER_RESPONSE_HASH_FIELD,
                telemetry == null ? null : telemetry.getProviderResponseIdHash());
        return json(details);
    }

    private String currentSystemDate() {
        return java.time.LocalDate.now(java.time.ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID)).toString();
    }

    private String toolFreshness(List<AgentToolDefinition> definitions, String toolCode) {
        return definitions.stream()
                .filter(definition -> definition.getCode().equals(toolCode))
                .map(AgentToolDefinition::getFreshness)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void emit(Consumer<AgentRunEventBO> consumer, AgentRunEventBO event) {
        if (consumer != null) consumer.accept(event);
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String redactText(String value) {
        return AgentSensitiveDataPolicy.redactText(value);
    }

    private long elapsedMs(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0, System.nanoTime() - startedNanos));
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String valueOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
