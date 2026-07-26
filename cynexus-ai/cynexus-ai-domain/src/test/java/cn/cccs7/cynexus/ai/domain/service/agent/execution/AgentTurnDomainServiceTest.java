package cn.cccs7.cynexus.ai.domain.service.agent.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolExecutionPort;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AgentProviderTurnPort;
import cn.cccs7.cynexus.ai.domain.gateway.observability.AgentObservabilityPort;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentCircuitBreakerPort;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AgentTurnDomainServiceTest {

    @Test
    void retriesTransientProviderFailureAndNotifiesTheApplicationAdapter() {
        FakeCircuitBreaker circuit = new FakeCircuitBreaker();
        AgentTurnDomainService service = new AgentTurnDomainService(circuit, new NoopObservability(), 2);
        AtomicInteger calls = new AtomicInteger();
        List<Integer> retries = new ArrayList<>();
        AgentProviderTurnPort provider = (model, request, delta) -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("HTTP 503 from provider");
            return AgentModelTurnResponse.builder().type("FINAL").content("done").build();
        };

        AgentModelTurnResponse response = service.completeWithRetry(AiModelBO.builder().providerCode("custom")
                        .modelCode("model").build(), AgentModelTurnRequest.builder().build(), "owner", "run",
                provider, null, retries::add);

        assertEquals("done", response.getContent());
        assertEquals(2, calls.get());
        assertEquals(List.of(2), retries);
    }

    @Test
    void clearsResponsesContinuationStateBeforeRetryingATransientFailure() {
        FakeCircuitBreaker circuit = new FakeCircuitBreaker();
        AgentTurnDomainService service = new AgentTurnDomainService(circuit, new NoopObservability(), 3);
        AtomicInteger calls = new AtomicInteger();
        List<Integer> retries = new ArrayList<>();
        AgentProviderTurnPort provider = (model, request, delta) -> {
            int attempt = calls.incrementAndGet();
            if (attempt == 1) {
                assertEquals("response-1", request.getPreviousResponseId());
                throw new IllegalStateException("HTTP 502 from provider");
            }
            assertNull(request.getPreviousResponseId());
            if (attempt == 2) throw new IllegalStateException("HTTP 503 from provider");
            return AgentModelTurnResponse.builder().type("FINAL").content("done").build();
        };

        AgentModelTurnResponse response = service.completeWithRetry(AiModelBO.builder().providerCode("custom")
                        .modelCode("model").build(), AgentModelTurnRequest.builder()
                        .previousResponseId("response-1").build(), "owner", "run", provider, null, retries::add);

        assertEquals("done", response.getContent());
        assertEquals(3, calls.get());
        assertEquals(List.of(2, 3), retries);
    }

    @Test
    void retriesTransientToolFailureAndReturnsAttemptCount() {
        FakeCircuitBreaker circuit = new FakeCircuitBreaker();
        AgentTurnDomainService service = new AgentTurnDomainService(circuit, new NoopObservability(), 2);
        AtomicInteger calls = new AtomicInteger();
        AgentToolExecutionPort executor = (call, context) -> calls.incrementAndGet() == 1
                ? AgentToolResult.failure("market.overview", "temporary", "market", "timeout")
                : AgentToolResult.success("market.overview", "{}", "ok", "2026-07-23", "market", List.of());

        var outcome = service.executeToolWithRetry(AgentToolCall.builder().callId("call-1")
                        .toolCode("market.overview").arguments(Map.of()).build(), AgentExecutionContext.builder().build(),
                executor, ignored -> { });

        assertEquals(2, outcome.getAttempts());
        assertEquals("SUCCEEDED", outcome.getResult().getStatus());
        assertEquals(2, calls.get());
        assertTrue(circuit.successes.contains("tool:market.overview"));
    }

    private static final class FakeCircuitBreaker implements AgentCircuitBreakerPort {
        private final List<String> successes = new ArrayList<>();

        @Override
        public boolean allow(String dependencyKey) {
            return true;
        }

        @Override
        public void recordSuccess(String dependencyKey) {
            successes.add(dependencyKey);
        }

        @Override
        public void recordFailure(String dependencyKey, int failureThreshold, int openSeconds) {
        }
    }

    private static final class NoopObservability implements AgentObservabilityPort {
        @Override public void runStarted(String traceId) { }
        @Override public void runCompleted(String status, long durationMs) { }
        @Override public void providerCompleted(String provider, String model, boolean success, long durationMs) { }
        @Override public void toolCompleted(String toolCode, boolean success, long durationMs) { }
    }
}
