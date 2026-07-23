package cn.cccs7.cynexus.ai.domain.service.agent.execution;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolExecutionPort;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AgentProviderTurnPort;
import cn.cccs7.cynexus.ai.domain.gateway.observability.AgentObservabilityPort;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentCircuitBreakerPort;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolExecutionOutcome;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import java.util.function.Consumer;

/**
 * Coordinates one model turn or one tool call. Blocking, cancellation-aware
 * adapters remain outside the domain; retry and circuit policy stays here.
 */
public class AgentTurnDomainService {

    private final AgentCircuitBreakerPort circuitBreaker;
    private final AgentObservabilityPort observability;
    private final int maxRetryAttempts;

    public AgentTurnDomainService(AgentCircuitBreakerPort circuitBreaker,
                                  AgentObservabilityPort observability,
                                  int maxRetryAttempts) {
        this.circuitBreaker = circuitBreaker;
        this.observability = observability;
        this.maxRetryAttempts = Math.max(1, maxRetryAttempts);
    }

    public AgentModelTurnResponse completeWithRetry(AiModelBO model,
                                                    AgentModelTurnRequest request,
                                                    String ownerKey,
                                                    String runNo,
                                                    AgentProviderTurnPort provider,
                                                    Consumer<String> textDeltaConsumer,
                                                    Consumer<Integer> retryConsumer) {
        RuntimeException last = null;
        String circuitKey = "provider:" + valueOr(model == null ? null : model.getProviderCode(), "unknown");
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            long startedNanos = System.nanoTime();
            try {
                if (!circuitBreaker.allow(circuitKey)) {
                    throw new IllegalStateException("Agent provider circuit is open");
                }
                AgentModelTurnResponse response = provider.complete(model, request, textDeltaConsumer);
                observability.providerCompleted(model == null ? null : model.getProviderCode(),
                        model == null ? null : model.getModelCode(), true, elapsedMs(startedNanos));
                circuitBreaker.recordSuccess(circuitKey);
                return response;
            } catch (RuntimeException exception) {
                observability.providerCompleted(model == null ? null : model.getProviderCode(),
                        model == null ? null : model.getModelCode(), false, elapsedMs(startedNanos));
                last = exception;
                if (!isTransientProviderFailure(exception) || attempt >= maxRetryAttempts) throw exception;
                circuitBreaker.recordFailure(circuitKey, 3, 30);
                if (retryConsumer != null) retryConsumer.accept(attempt + 1);
            }
        }
        throw last == null ? new IllegalStateException("Agent provider retry failed") : last;
    }

    public AgentToolExecutionOutcome executeToolWithRetry(AgentToolCall call,
                                                          AgentExecutionContext context,
                                                          AgentToolExecutionPort executor,
                                                          Consumer<Integer> retryConsumer) {
        String circuitKey = "tool:" + call.getToolCode();
        if (!circuitBreaker.allow(circuitKey)) {
            throw new IllegalStateException("Agent tool circuit is open: " + call.getToolCode());
        }
        AgentToolResult result = null;
        int attempts = 0;
        while (attempts < maxRetryAttempts) {
            attempts++;
            long startedNanos = System.nanoTime();
            result = executor.execute(call, context);
            observability.toolCompleted(call.getToolCode(), result != null && "SUCCEEDED".equals(result.getStatus()),
                    elapsedMs(startedNanos));
            if (!isTransientToolFailure(result) || attempts >= maxRetryAttempts) break;
            circuitBreaker.recordFailure(circuitKey, 3, 30);
            if (retryConsumer != null) retryConsumer.accept(attempts + 1);
        }
        if (result != null && "SUCCEEDED".equals(result.getStatus())) {
            circuitBreaker.recordSuccess(circuitKey);
        } else if (isTransientToolFailure(result)) {
            circuitBreaker.recordFailure(circuitKey, 3, 30);
        }
        return AgentToolExecutionOutcome.builder().result(result).attempts(attempts).build();
    }

    public boolean isTransientProviderFailure(RuntimeException exception) {
        String message = exception == null || exception.getMessage() == null ? ""
                : exception.getMessage().toLowerCase();
        return message.contains("timeout") || message.contains("timed out") || message.contains("interrupted")
                || message.contains("http 429") || message.contains("http 500") || message.contains("http 502")
                || message.contains("http 503") || message.contains("http 504")
                || message.contains("failed to call ai provider");
    }

    public boolean isTransientToolFailure(AgentToolResult result) {
        if (result == null || !"FAILED".equals(result.getStatus()) || result.getWarnings() == null) return false;
        return result.getWarnings().stream().map(value -> value == null ? "" : value.toLowerCase())
                .anyMatch(value -> value.contains("timeout") || value.contains("http_5") || value.contains("upstream")
                        || value.contains("temporarily") || value.contains("connection"));
    }

    private long elapsedMs(long startedNanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                Math.max(0, System.nanoTime() - startedNanos));
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
