package cn.cccs7.cynexus.ai.application.service.agent.execution;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.api.dto.AgentCancellationResult;
import cn.cccs7.cynexus.ai.api.dto.AgentConversationDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentMessageDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentRunDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentRunExportDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentRunStepDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentToolDefinitionDTO;
import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentCapabilityRuntimeCatalog;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentHistoryCollapsePolicy;
import cn.cccs7.cynexus.ai.domain.service.agent.execution.AgentExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.service.agent.execution.AgentRunDomainService;
import cn.cccs7.cynexus.ai.domain.service.agent.execution.AgentRunExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentRunCancellationOutcome;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentRunExecutionCommand;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentRunExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class AutonomousAgentAppService {

    private final AgentRunDomainService runDomainService;
    private final AgentRunExecutionDomainService runExecutionDomainService;
    private final AgentExecutionDomainService executionDomainService;
    private final AgentToolCatalog toolCatalog;
    private final AgentCapabilityRuntimeCatalog runtimeCatalog;
    private final ObjectMapper objectMapper;

    public AutonomousAgentAppService(AgentRunDomainService runDomainService,
                                     AgentRunExecutionDomainService runExecutionDomainService,
                                     AgentExecutionDomainService executionDomainService,
                                     AgentToolCatalog toolCatalog,
                                     ObjectMapper objectMapper) {
        this.runDomainService = runDomainService;
        this.runExecutionDomainService = runExecutionDomainService;
        this.executionDomainService = executionDomainService;
        this.toolCatalog = toolCatalog;
        this.objectMapper = objectMapper;
        this.runtimeCatalog = toolCatalog instanceof AgentCapabilityRuntimeCatalog runtime
                ? runtime : descriptor -> { throw new IllegalStateException("Dynamic capability runtime catalog is unavailable"); };
    }

    public AgentRunDTO execute(AgentRunRequest request) {
        return executeInternal(request, AgentIdentity.singleUser(), ignored -> { }, false);
    }

    public AgentRunDTO execute(AgentRunRequest request, Consumer<AgentRunEventBO> eventConsumer) {
        return executeInternal(request, AgentIdentity.singleUser(), eventConsumer, false);
    }

    public AgentRunDTO execute(AgentRunRequest request, AgentIdentity identity, Consumer<AgentRunEventBO> eventConsumer) {
        return executeInternal(request, identity, eventConsumer, false);
    }

    public AgentRunDTO stream(AgentRunRequest request, AgentIdentity identity, Consumer<AgentRunEventBO> eventConsumer) {
        return executeInternal(request, identity, eventConsumer, true);
    }

    private AgentRunDTO executeInternal(AgentRunRequest request, AgentIdentity identity,
                                        Consumer<AgentRunEventBO> eventConsumer, boolean streamTokens) {
        AgentRunRequest safeRequest = request == null ? new AgentRunRequest() : request;
        AgentRunExecutionCommand command = AgentRunExecutionCommand.builder()
                .runNo(safeRequest.getRunNo())
                .conversationNo(safeRequest.getConversationNo())
                .idempotencyKey(safeRequest.getIdempotencyKey())
                .userInput(safeRequest.getUserInput())
                .preferredProviderCode(safeRequest.getPreferredProviderCode())
                .preferredModelCode(safeRequest.getPreferredModelCode())
                .preferredReasoningEffort(safeRequest.getPreferredReasoningEffort())
                .workspace(safeRequest.getWorkspace())
                .locale(safeRequest.getLocale())
                .tradeDate(safeRequest.getTradeDate())
                .identity(identity == null ? AgentIdentity.singleUser() : identity)
                .streamTokens(streamTokens)
                .build();
        AgentRunExecutionResult result = runExecutionDomainService.execute(command, eventConsumer);
        return toDTO(result.getRun(), result.getSteps());
    }

    public AgentCancellationResult cancel(String ownerKey, String runNo) {
        return toCancellationResult(runExecutionDomainService.cancel(ownerKey, runNo));
    }

    public AgentCancellationResult cancel(AgentIdentity identity, String runNo) {
        return cancel(identity == null ? null : identity.storageOwnerKey(), runNo);
    }

    public void replayStream(AgentIdentity identity, String runNo, int afterSequenceNo,
                             Consumer<AgentRunEventBO> eventConsumer) {
        AgentIdentity safeIdentity = identity == null ? AgentIdentity.singleUser() : identity;
        String ownerKey = safeIdentity.storageOwnerKey();
        int cursor = Math.max(0, afterSequenceNo);
        long deadline = System.currentTimeMillis() + (executionDomainService.limits().getMaxRunSeconds() + 60L) * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<AgentRunEventBO> events = runDomainService.findEvents(ownerKey, runNo, cursor, 200);
            for (AgentRunEventBO event : events) {
                if (event.getSequenceNo() != null) cursor = event.getSequenceNo();
                emit(eventConsumer, event);
            }
            AgentRunBO run = runDomainService.findRun(ownerKey, runNo)
                    .orElseThrow(() -> new IllegalArgumentException("Agent run was not found"));
            if (runExecutionDomainService.isTerminal(run.getStatus())) return;
            try {
                Thread.sleep(300L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new IllegalStateException("Agent event replay timed out");
    }

    public AgentRunDTO getRun(String ownerKey, String runNo) {
        AgentRunBO run = runDomainService.findRun(valueOr(ownerKey, AgentIdentity.singleUser().storageOwnerKey()), runNo)
                .orElseThrow(() -> new IllegalArgumentException("Agent run was not found"));
        return toDTO(run, runDomainService.findSteps(run.getOwnerKey(), runNo));
    }

    public AgentRunDTO getRun(AgentIdentity identity, String runNo) {
        return getRun(identity == null ? null : identity.storageOwnerKey(), runNo);
    }

    public List<AgentToolDefinitionDTO> enabledTools() {
        return toolCatalog.enabledTools().stream().map(definition -> AgentToolDefinitionDTO.builder()
                .code(definition.getCode()).name(definition.getName()).module(definition.getModule())
                .description(definition.getDescription()).guidanceJson(definition.getGuidanceJson()).inputSchemaJson(definition.getInputSchemaJson())
                .outputSchemaJson(definition.getOutputSchemaJson()).readOnly(definition.isReadOnly())
                .requiresConfirmation(definition.isRequiresConfirmation()).freshness(definition.getFreshness())
                .source(definition.getSource()).sourceUrl(definition.getSourceUrl()).version(definition.getVersion())
                .tenantScope(definition.getTenantScope()).availability(definition.getAvailability())
                .priority(definition.getPriority()).promptKey(definition.getPromptKey())
                .runtimeEntry(definition.getRuntimeEntry()).routeGroup(definition.getRouteGroup()).build()).toList();
    }

    public List<AgentCapabilityDescriptor> capabilities() {
        return runDomainService.listCapabilities();
    }

    public boolean updateCapability(String code, boolean enabled, String availability, int priority) {
        return runDomainService.updateCapability(code, enabled, availability, priority);
    }

    public AgentCapabilityDescriptor registerCapability(AgentCapabilityDescriptor descriptor) {
        runtimeCatalog.register(descriptor);
        return runDomainService.register(descriptor);
    }

    public List<AgentConversationDTO> conversations(String ownerKey, int limit) {
        return conversations(ownerKey, limit, null);
    }

    public List<AgentConversationDTO> conversations(String ownerKey, int limit, String query) {
        return runDomainService.listConversations(valueOr(ownerKey, AgentIdentity.singleUser().storageOwnerKey()), limit, query).stream()
                .map(item -> AgentConversationDTO.builder().conversationNo(item.getConversationNo()).title(item.getTitle())
                        .status(item.getStatus()).lastRunNo(item.getLastRunNo()).lastMessage(item.getLastMessage())
                        .createdAt(item.getCreatedAt()).updatedAt(item.getUpdatedAt()).build()).toList();
    }

    public List<AgentConversationDTO> conversations(AgentIdentity identity, int limit) {
        return conversations(identity == null ? null : identity.storageOwnerKey(), limit);
    }

    public List<AgentConversationDTO> conversations(AgentIdentity identity, int limit, String query) {
        return conversations(identity == null ? null : identity.storageOwnerKey(), limit, query);
    }

    public boolean archiveConversation(AgentIdentity identity, String conversationNo) {
        return runDomainService.archiveConversation(
                identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey(),
                conversationNo);
    }

    public AgentRunDTO retry(AgentIdentity identity, String runNo) {
        AgentRunBO original = runDomainService.findRun(identity == null ? null : identity.storageOwnerKey(), runNo)
                .orElseThrow(() -> new IllegalArgumentException("Agent run was not found"));
        return execute(AgentRunRequest.builder().conversationNo(original.getConversationNo()).userInput(original.getUserInput())
                .preferredProviderCode(original.getProviderCode()).preferredModelCode(original.getModelCode())
                .workspace("agent").locale("zh-CN").build(), identity, ignored -> { });
    }

    public AgentRunDTO regenerate(AgentIdentity identity, String runNo) {
        return retry(identity, runNo);
    }

    public List<AgentMessageDTO> messages(String ownerKey, String conversationNo) {
        return runDomainService.findMessages(valueOr(ownerKey, AgentIdentity.singleUser().storageOwnerKey()), conversationNo).stream()
                .map(item -> AgentMessageDTO.builder().sequenceNo(item.getSequenceNo()).role(item.getRole())
                        .content(item.getContent()).toolCallId(item.getToolCallId()).toolName(item.getToolName())
                        .toolCallsJson(item.getToolCallsJson()).createdAt(item.getCreatedAt()).build()).toList();
    }

    public List<AgentMessageDTO> messages(AgentIdentity identity, String conversationNo) {
        return messages(identity == null ? null : identity.storageOwnerKey(), conversationNo);
    }

    public List<AgentRunEventBO> events(AgentIdentity identity, String runNo, int afterSequenceNo, int limit) {
        AgentIdentity safeIdentity = identity == null ? AgentIdentity.singleUser() : identity;
        String ownerKey = safeIdentity.storageOwnerKey();
        if (runDomainService.findRun(ownerKey, runNo).isEmpty()) {
            throw new IllegalArgumentException("Agent run was not found");
        }
        return runDomainService.findEvents(ownerKey, runNo, Math.max(0, afterSequenceNo), Math.min(200, Math.max(1, limit)));
    }

    public AgentRunExportDTO export(AgentIdentity identity, String runNo, String format) {
        AgentRunDTO run = getRun(identity, runNo);
        String normalizedFormat = AiConstants.FORMAT_JSON.equalsIgnoreCase(format)
                ? AiConstants.FORMAT_JSON : AiConstants.FORMAT_MARKDOWN;
        String content;
        String contentType;
        String extension;
        if (AiConstants.FORMAT_JSON.equals(normalizedFormat)) {
            content = json(run);
            contentType = "application/json;charset=UTF-8";
            extension = AiConstants.FORMAT_JSON;
        } else {
            StringBuilder markdown = new StringBuilder();
            markdown.append("# Cynexus Agent Result\n\n")
                    .append("- Run: ").append(valueOr(run.getRunNo(), "")).append("\n")
                    .append("- Conversation: ").append(valueOr(run.getConversationNo(), "")).append("\n")
                    .append("- Status: ").append(valueOr(run.getStatus(), "")).append("\n")
                    .append("- Model: ").append(valueOr(run.getModelCode(), "")).append("\n\n")
                    .append("## Request\n\n").append(valueOr(run.getUserInput(), "")).append("\n\n")
                    .append("## Answer\n\n").append(valueOr(run.getAnswer(), run.getErrorMessage())).append("\n\n")
                    .append("## Evidence\n\n");
            for (AgentRunStepDTO step : run.getSteps() == null ? List.<AgentRunStepDTO>of() : run.getSteps()) {
                if (!"TOOL".equals(step.getKind())) continue;
                markdown.append("- `").append(valueOr(step.getToolCode(), "tool")).append("` ")
                        .append(valueOr(step.getSummary(), "")).append("; date: ")
                        .append(valueOr(step.getDataDate(), "unknown"));
                if (step.getSourceUrl() != null && !step.getSourceUrl().isBlank()) {
                    markdown.append("; source: ").append(step.getSourceUrl());
                }
                markdown.append("\n");
            }
            content = markdown.toString();
            contentType = "text/markdown;charset=UTF-8";
            extension = "md";
        }
        return AgentRunExportDTO.builder().format(normalizedFormat).filename("cynexus-agent-" + runNo + "." + extension)
                .contentType(contentType).content(content).build();
    }

    public static List<AgentModelMessage> collapsePersistedHistoryForProvider(List<AgentModelMessage> history) {
        return AgentHistoryCollapsePolicy.collapseForProvider(history);
    }

    private AgentRunDTO toDTO(AgentRunBO run, List<AgentRunStepBO> steps) {
        return AgentRunDTO.builder().runNo(run.getRunNo()).conversationNo(run.getConversationNo()).status(run.getStatus())
                .userInput(run.getUserInput()).answer(run.getAnswer()).providerCode(run.getProviderCode()).modelCode(run.getModelCode()).planJson(run.getPlanJson())
                .contextJson(run.getContextJson()).assumptionsJson(run.getAssumptionsJson()).evidenceJson(run.getEvidenceJson())
                .clarificationJson(run.getClarificationJson())
                .warningsJson(run.getWarningsJson())
                .turnCount(run.getTurnCount()).toolCallCount(run.getToolCallCount()).inputTokens(run.getInputTokens())
                .cachedInputTokens(run.getCachedInputTokens()).cacheWriteTokens(run.getCacheWriteTokens())
                .outputTokens(run.getOutputTokens()).totalTokens(run.getTotalTokens()).estimatedCost(run.getEstimatedCost())
                .usageSource(run.getUsageSource()).errorMessage(run.getErrorMessage())
                .failureCategory(run.getFailureCategory())
                .startedAt(run.getStartedAt()).finishedAt(run.getFinishedAt())
                .steps(steps == null ? List.of() : steps.stream().map(step -> AgentRunStepDTO.builder()
                        .stepNo(step.getStepNo()).attemptCount(step.getAttemptCount()).kind(step.getKind()).toolCode(step.getToolCode()).status(step.getStatus())
                        .summary(step.getSummary()).source(step.getSource()).sourceUrl(step.getSourceUrl()).citationJson(step.getCitationJson()).dataDate(step.getDataDate())
                        .failureCategory(step.getFailureCategory())
                        .inputJson(step.getInputJson()).outputJson(step.getOutputJson()).errorMessage(step.getErrorMessage())
                        .startedAt(step.getStartedAt()).finishedAt(step.getFinishedAt()).build()).toList()).build();
    }

    private AgentCancellationResult toCancellationResult(AgentRunCancellationOutcome outcome) {
        if ("CANCEL_REQUESTED".equals(outcome.getStatus())) {
            return AgentCancellationResult.requested(outcome.getRunNo(), outcome.getRunStatus());
        }
        if ("ALREADY_TERMINAL".equals(outcome.getStatus())) {
            return AgentCancellationResult.alreadyTerminal(outcome.getRunNo(), outcome.getRunStatus());
        }
        return AgentCancellationResult.notFound(outcome.getRunNo());
    }

    private void emit(Consumer<AgentRunEventBO> consumer, AgentRunEventBO event) {
        if (consumer == null) return;
        try {
            consumer.accept(event);
        } catch (RuntimeException ignored) {
            // A disconnected SSE subscriber must not cancel the persisted Agent run.
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
