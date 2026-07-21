package cn.cccs7.cynexus.ai.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.agent.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.agent.AgentRateLimitPort;
import cn.cccs7.cynexus.ai.domain.agent.AgentCircuitBreakerPort;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolExecutor;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.port.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.repository.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AutonomousAgentWorkflowTest {

    @Test
    void mockProviderSelectsToolPersistsTraceAndReturnsFinalAnswer() {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentToolCatalog catalog = mock(AgentToolCatalog.class);
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AiCompletionGateway completion = mock(AiCompletionGateway.class);
        AiProviderRepository providers = mock(AiProviderRepository.class);
        AiUsageBindingRepository bindings = mock(AiUsageBindingRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRateLimitPort rateLimiter = mock(AgentRateLimitPort.class);
        AgentCircuitBreakerPort circuitBreaker = mock(AgentCircuitBreakerPort.class);
        when(circuitBreaker.allow(any())).thenReturn(true);

        AgentToolDefinition definition = AgentToolDefinition.builder().code("market.overview").name("Market overview")
                .module("market").description("Read market overview").inputSchemaJson("{\"type\":\"object\"}")
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).freshness("daily").source("Market API").build();
        when(catalog.enabledTools()).thenReturn(List.of(definition));
        when(repository.findConversation(any(), any())).thenReturn(Optional.empty());
        when(repository.findMessages(any(), any())).thenReturn(List.of());
        when(lease.tryAcquire(any(), any(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimiter.allow(any(), anyInt(), anyInt())).thenReturn(true);
        when(lease.isCancellationRequested(any())).thenReturn(false);
        when(bindings.listAll()).thenReturn(List.of(AiUsageBindingBO.builder().capability("autonomous_agent")
                .providerId(1L).modelOverride("gpt-5.4-mini").enabled(true).build()));
        when(providers.findById(1L)).thenReturn(AiProviderBO.builder().id(1L).code("custom")
                .providerType("custom").baseUrl("http://mock").defaultModel("gpt-5.4-mini").enabled(true).build());
        when(executor.execute(any(AgentToolCall.class), any(AgentExecutionContext.class)))
                .thenReturn(AgentToolResult.success("market.overview", "{broken" + "x".repeat(25_000), "Market data", "2026-07-22", "Market API", List.of()));
        when(completion.completeAgent(any(), any(), eq("autonomous_agent")))
                .thenReturn(AgentModelTurnResponse.builder().type("TOOL_CALLS").toolCalls(List.of(
                        AgentModelToolCall.builder().callId("call-1").toolCode("market.overview").arguments(Map.of()).build())).build())
                .thenReturn(AgentModelTurnResponse.builder().type("FINAL").content("市场成交额为 123。").build());

        AutonomousAgentAppService service = new AutonomousAgentAppService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        var result = service.execute(AgentRunRequest.builder().userInput("今天市场怎么样？").idempotencyKey("idem-1").build(),
                AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build(), ignored -> { });

        assertEquals("SUCCEEDED", result.getStatus());
        assertEquals("gpt-5.4-mini", result.getModelCode());
        assertEquals(1, result.getToolCallCount());
        assertNotNull(result.getEvidenceJson());
        ArgumentCaptor<cn.cccs7.cynexus.ai.domain.agent.AgentRunStepBO> stepCaptor = ArgumentCaptor.forClass(cn.cccs7.cynexus.ai.domain.agent.AgentRunStepBO.class);
        verify(repository, times(2)).saveStep(stepCaptor.capture());
        stepCaptor.getAllValues().forEach(step -> {
            try {
                new ObjectMapper().readTree(step.getOutputJson());
            } catch (Exception exception) {
                throw new AssertionError("Agent step output must remain valid JSON", exception);
            }
        });
        assertTrue(stepCaptor.getAllValues().get(0).getOutputJson().contains("truncated"));
        verify(repository).findRunByIdempotencyKey("tenant-1::user-1", "idem-1");
        verify(lease).release(eq("tenant-1::user-1"), any());
    }
}
