package cn.cccs7.cynexus.ai.domain.service.agent.execution;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Agent execution invariants shared by the application adapter and turn service. */
public class AgentExecutionDomainService {

    private final AgentExecutionLimits limits;
    private final AgentTurnDomainService turnService;

    public AgentExecutionDomainService(AgentExecutionLimits limits, AgentTurnDomainService turnService) {
        this.limits = limits == null ? AgentExecutionLimits.defaults() : limits;
        this.turnService = turnService;
    }

    public AgentExecutionLimits limits() {
        return limits;
    }

    public String validateUserInput(String userInput) {
        if (userInput == null || userInput.isBlank()) throw new IllegalArgumentException("Agent userInput cannot be blank");
        String normalized = userInput.trim();
        if (normalized.length() > limits.getMaxUserInputChars()) {
            throw new IllegalArgumentException("Agent userInput is too long");
        }
        return normalized;
    }

    public String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 128) throw new IllegalArgumentException("Agent idempotencyKey is too long");
        return normalized;
    }

    public void checkTurn(int turn, OffsetDateTime startedAt, OffsetDateTime now) {
        if (turn < 1 || turn > limits.getMaxTurns()) throw new IllegalStateException("The Agent exceeded the turn limit");
        if (startedAt != null && now != null
                && java.time.Duration.between(startedAt, now).getSeconds() > limits.getMaxRunSeconds()) {
            throw new IllegalStateException("Agent run exceeded the time limit");
        }
    }

    public void validateToolCalls(List<AgentModelToolCall> calls, int existingCount) {
        if (calls == null || calls.isEmpty()) return;
        if (existingCount < 0 || calls.size() > limits.getMaxToolCalls() - existingCount) {
            throw new IllegalStateException("The Agent exceeded the total tool call limit");
        }
        Set<String> callIds = new HashSet<>();
        for (AgentModelToolCall call : calls) {
            if (call == null || call.getCallId() == null || !callIds.add(call.getCallId())) {
                throw new IllegalStateException("The Agent returned duplicate or missing tool call ids");
            }
        }
    }

    public int incrementToolCallCount(AgentRunBO run) {
        int next = run.getToolCallCount() == null ? 1 : run.getToolCallCount() + 1;
        if (next > limits.getMaxToolCalls()) throw new IllegalStateException("The Agent exceeded the tool call limit");
        run.setToolCallCount(next);
        return next;
    }

    public boolean isCancellationRequested(boolean runtimeRequested, boolean repositoryRequested) {
        return runtimeRequested || repositoryRequested;
    }

    public String classifyFailure(RuntimeException exception) {
        String message = exception == null || exception.getMessage() == null ? ""
                : exception.getMessage().toLowerCase();
        if (message.contains("rate limit") || message.contains("http 429")) return "RATE_LIMITED";
        if (message.contains("timeout") || message.contains("timed out")) return "TIMEOUT";
        if (message.contains("provider")) return "PROVIDER";
        if (message.contains("tool")) return "TOOL";
        if (message.contains("policy") || message.contains("unregistered")) return "POLICY";
        if (message.contains("authorization") || message.contains("identity")) return "AUTHORIZATION";
        return "INTERNAL";
    }

    public String classifyToolFailure(AgentToolResult result) {
        String warning = result == null || result.getWarnings() == null ? ""
                : String.join(" ", result.getWarnings()).toLowerCase();
        if (warning.contains("timeout")) return "TIMEOUT";
        if (warning.contains("schema") || warning.contains("argument")) return "VALIDATION";
        if (warning.contains("unauthorized") || warning.contains("owner") || warning.contains("identity")) return "AUTHORIZATION";
        if (turnService != null && turnService.isTransientToolFailure(result)) return "UPSTREAM";
        return "TOOL";
    }
}
