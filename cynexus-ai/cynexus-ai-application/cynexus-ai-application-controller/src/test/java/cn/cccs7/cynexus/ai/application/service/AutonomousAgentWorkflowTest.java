package cn.cccs7.cynexus.ai.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.application.service.agent.execution.AutonomousAgentAppService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRateLimitPort;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentCircuitBreakerPort;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.gateway.quota.AgentQuotaPort;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolExecutor;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.policy.sensitivedata.AgentSourceUrlPolicy;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.gateway.observability.AgentObservabilityPort;
import cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).freshness("daily").source("Market API")
                .sourceUrl("/stock-overview").build();
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
                .thenReturn(AgentToolResult.success("market.overview",
                        "{\"apiKey\":\"secret-value\",\"email\":\"person@example.com\",\"payload\":\""
                                + "x".repeat(25_000) + "\"}", "Market data", "2026-07-22", "Market API", List.of()));
        when(completion.completeAgent(any(), any(), eq("autonomous_agent")))
                .thenReturn(AgentModelTurnResponse.builder().type("TOOL_CALLS").toolCalls(List.of(
                        AgentModelToolCall.builder().callId("call-1").toolCode("market.overview").arguments(Map.of()).build())).build())
                .thenReturn(AgentModelTurnResponse.builder().type("FINAL").content("市场成交额为 123。").build());

        AutonomousAgentAppService service = newService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        var result = service.execute(AgentRunRequest.builder().userInput("今天市场怎么样？").idempotencyKey("idem-1").build(),
                AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build(), ignored -> { });

        assertEquals("SUCCEEDED", result.getStatus());
        assertEquals("gpt-5.4-mini", result.getModelCode());
        assertEquals(1, result.getToolCallCount());
        assertNotNull(result.getEvidenceJson());
        ArgumentCaptor<cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO> stepCaptor = ArgumentCaptor.forClass(cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO.class);
        verify(repository, times(2)).saveStep(stepCaptor.capture());
        stepCaptor.getAllValues().forEach(step -> {
            try {
                new ObjectMapper().readTree(step.getOutputJson());
            } catch (Exception exception) {
                throw new AssertionError("Agent step output must remain valid JSON", exception);
            }
        });
        assertTrue(stepCaptor.getAllValues().get(0).getOutputJson().contains("truncated"));
        assertTrue(stepCaptor.getAllValues().get(0).getOutputJson().contains("[REDACTED]"));
        assertTrue(!stepCaptor.getAllValues().get(0).getOutputJson().contains("secret-value"));
        assertTrue(!stepCaptor.getAllValues().get(0).getOutputJson().contains("person@example.com"));
        assertEquals("/stock-overview", stepCaptor.getAllValues().get(0).getSourceUrl());
        assertTrue(stepCaptor.getAllValues().get(0).getCitationJson().contains("cit-"));
        assertTrue(stepCaptor.getAllValues().get(0).getCitationJson().contains("AVAILABLE"));
        assertTrue(result.getEvidenceJson().contains("citations"));
        verify(repository).findRunByIdempotencyKey("tenant-1::user-1", "idem-1");
        verify(lease).release(eq("tenant-1::user-1"), any());
        ArgumentCaptor<AgentRunEventBO> eventCaptor = ArgumentCaptor.forClass(AgentRunEventBO.class);
        verify(repository, atLeast(4)).saveEvent(eq("tenant-1::user-1"), eventCaptor.capture());
        assertEquals("RUN_STARTED", eventCaptor.getAllValues().get(0).getType());
        assertEquals("RUN_COMPLETED", eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1).getType());
        for (int index = 0; index < eventCaptor.getAllValues().size(); index++) {
            assertEquals(index + 1, eventCaptor.getAllValues().get(index).getSequenceNo());
        }
    }

    @Test
    void structuredClarificationIsPersistedAndDoesNotBecomeSuccessfulAnswer() {
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
        when(catalog.enabledTools()).thenReturn(List.of());
        when(repository.findConversation(any(), any())).thenReturn(Optional.empty());
        when(repository.findMessages(any(), any())).thenReturn(List.of());
        when(lease.tryAcquire(any(), any(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimiter.allow(any(), anyInt(), anyInt())).thenReturn(true);
        when(lease.isCancellationRequested(any())).thenReturn(false);
        when(bindings.listAll()).thenReturn(List.of(AiUsageBindingBO.builder().capability("autonomous_agent")
                .providerId(1L).modelOverride("gpt-5.4-mini").enabled(true).build()));
        when(providers.findById(1L)).thenReturn(AiProviderBO.builder().id(1L).code("custom")
                .providerType("custom").baseUrl("http://mock").defaultModel("gpt-5.4-mini").enabled(true).build());
        when(completion.completeAgent(any(), any(), eq("autonomous_agent")))
                .thenReturn(AgentModelTurnResponse.builder().type("FINAL").content(
                        "{\"type\":\"clarification\",\"summary\":\"需要明确分析范围。\","
                                + "\"questions\":[\"请提供股票代码。\"],\"missingFields\":[\"symbol\"]}").build());

        AutonomousAgentAppService service = newService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        var result = service.execute(AgentRunRequest.builder().userInput("分析这只股票").build(),
                AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build(), ignored -> { });

        assertEquals("NEEDS_CLARIFICATION", result.getStatus());
        assertTrue(result.getAnswer().contains("请提供股票代码"));
        assertTrue(result.getClarificationJson().contains("symbol"));
        ArgumentCaptor<cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO> stepCaptor = ArgumentCaptor.forClass(cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO.class);
        verify(repository).saveStep(stepCaptor.capture());
        assertEquals("CLARIFICATION", stepCaptor.getValue().getKind());
        assertEquals("NEEDS_CLARIFICATION", stepCaptor.getValue().getStatus());
    }

    @Test
    void structuredClarificationAfterProviderPreambleIsStillPersistedAsClarification() {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentToolCatalog catalog = mock(AgentToolCatalog.class);
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AiCompletionGateway completion = mock(AiCompletionGateway.class);
        AiProviderRepository providers = mock(AiProviderRepository.class);
        AiUsageBindingRepository bindings = mock(AiUsageBindingRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRateLimitPort rateLimiter = mock(AgentRateLimitPort.class);
        AgentCircuitBreakerPort circuitBreaker = mock(AgentCircuitBreakerPort.class);
        when(catalog.enabledTools()).thenReturn(List.of());
        when(repository.findConversation(any(), any())).thenReturn(Optional.empty());
        when(repository.findMessages(any(), any())).thenReturn(List.of());
        when(lease.tryAcquire(any(), any(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimiter.allow(any(), anyInt(), anyInt())).thenReturn(true);
        when(lease.isCancellationRequested(any())).thenReturn(false);
        when(circuitBreaker.allow(any())).thenReturn(true);
        when(bindings.listAll()).thenReturn(List.of(AiUsageBindingBO.builder().capability("autonomous_agent")
                .providerId(1L).modelOverride("gpt-5.4-mini").enabled(true).build()));
        when(providers.findById(1L)).thenReturn(AiProviderBO.builder().id(1L).code("custom")
                .providerType("custom").baseUrl("http://mock").defaultModel("gpt-5.4-mini").enabled(true).build());
        when(completion.completeAgent(any(), any(), eq("autonomous_agent")))
                .thenReturn(AgentModelTurnResponse.builder().type("FINAL").content(
                        "需要补充参数，下面是结构化结果： {\"type\":\"clarification\",\"summary\":\"请明确分析范围。\","
                                + "\"questions\":[\"请提供股票代码。\"],\"missingFields\":[\"symbol\"]}").build());

        AutonomousAgentAppService service = newService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        var result = service.execute(AgentRunRequest.builder().userInput("分析这只股票").build(),
                AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build(), ignored -> { });

        assertEquals("NEEDS_CLARIFICATION", result.getStatus());
        assertTrue(result.getClarificationJson().contains("symbol"));
    }

    @Test
    void transientProviderRetryPersistsRetryingStateAndEventBeforeRecovery() {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentToolCatalog catalog = mock(AgentToolCatalog.class);
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AiCompletionGateway completion = mock(AiCompletionGateway.class);
        AiProviderRepository providers = mock(AiProviderRepository.class);
        AiUsageBindingRepository bindings = mock(AiUsageBindingRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRateLimitPort rateLimiter = mock(AgentRateLimitPort.class);
        AgentCircuitBreakerPort circuitBreaker = mock(AgentCircuitBreakerPort.class);
        List<String> persistedStatuses = new ArrayList<>();
        when(catalog.enabledTools()).thenReturn(List.of());
        when(repository.findConversation(any(), any())).thenReturn(Optional.empty());
        when(repository.findMessages(any(), any())).thenReturn(List.of());
        when(lease.tryAcquire(any(), any(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimiter.allow(any(), anyInt(), anyInt())).thenReturn(true);
        when(lease.isCancellationRequested(any())).thenReturn(false);
        when(circuitBreaker.allow(any())).thenReturn(true);
        doAnswer(invocation -> {
            persistedStatuses.add(invocation.getArgument(0, AgentRunBO.class).getStatus());
            return null;
        }).when(repository).updateRun(any(AgentRunBO.class));
        when(bindings.listAll()).thenReturn(List.of(AiUsageBindingBO.builder().capability("autonomous_agent")
                .providerId(1L).modelOverride("gpt-5.4-mini").enabled(true).build()));
        when(providers.findById(1L)).thenReturn(AiProviderBO.builder().id(1L).code("custom")
                .providerType("custom").baseUrl("http://mock").defaultModel("gpt-5.4-mini").enabled(true).build());
        when(completion.completeAgent(any(), any(), eq("autonomous_agent")))
                .thenThrow(new IllegalStateException("HTTP 503 from AI provider"))
                .thenReturn(AgentModelTurnResponse.builder().type("FINAL").content("重试后完成").build());

        AutonomousAgentAppService service = newService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        var result = service.execute(AgentRunRequest.builder().userInput("验证 Provider 重试").build(),
                AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build(), ignored -> { });

        assertEquals("SUCCEEDED", result.getStatus());
        verify(repository, atLeast(3)).updateRun(any(AgentRunBO.class));
        assertTrue(persistedStatuses.contains("RETRYING"));
        assertTrue(persistedStatuses.contains("RUNNING"));
        ArgumentCaptor<AgentRunEventBO> eventCaptor = ArgumentCaptor.forClass(AgentRunEventBO.class);
        verify(repository, atLeast(3)).saveEvent(eq("tenant-1::user-1"), eventCaptor.capture());
        assertTrue(eventCaptor.getAllValues().stream().anyMatch(event -> "RETRYING".equals(event.getType())
                && "RETRYING".equals(event.getStatus())));
    }

    @Test
    void streamingProviderFailurePersistsFailedTerminalStateAndReleasesLease() {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentToolCatalog catalog = mock(AgentToolCatalog.class);
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AiCompletionGateway completion = mock(AiCompletionGateway.class);
        AiProviderRepository providers = mock(AiProviderRepository.class);
        AiUsageBindingRepository bindings = mock(AiUsageBindingRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRateLimitPort rateLimiter = mock(AgentRateLimitPort.class);
        AgentCircuitBreakerPort circuitBreaker = mock(AgentCircuitBreakerPort.class);
        List<String> statuses = new ArrayList<>();
        when(catalog.enabledTools()).thenReturn(List.of());
        when(repository.findConversation(any(), any())).thenReturn(Optional.empty());
        when(repository.findMessages(any(), any())).thenReturn(List.of());
        when(lease.tryAcquire(any(), any(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimiter.allow(any(), anyInt(), anyInt())).thenReturn(true);
        when(lease.isCancellationRequested(any())).thenReturn(false);
        when(circuitBreaker.allow(any())).thenReturn(true);
        doAnswer(invocation -> {
            statuses.add(invocation.getArgument(0, AgentRunBO.class).getStatus());
            return null;
        }).when(repository).updateRun(any(AgentRunBO.class));
        when(bindings.listAll()).thenReturn(List.of(AiUsageBindingBO.builder().capability("autonomous_agent")
                .providerId(1L).modelOverride("gpt-5.4-mini").enabled(true).build()));
        when(providers.findById(1L)).thenReturn(AiProviderBO.builder().id(1L).code("custom")
                .providerType("custom").baseUrl("http://mock").defaultModel("gpt-5.4-mini").enabled(true).build());
        when(completion.streamAgent(any(), any(), eq("autonomous_agent"), any()))
                .thenThrow(new IllegalStateException("Failed to stream AI provider: custom"));

        AutonomousAgentAppService service = newService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        var result = service.stream(AgentRunRequest.builder().userInput("stream failure").build(),
                AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build(), ignored -> { });

        assertEquals("FAILED", result.getStatus());
        assertTrue(statuses.contains("FAILED"));
        verify(lease).release(eq("tenant-1::user-1"), any());
        ArgumentCaptor<AgentRunEventBO> eventCaptor = ArgumentCaptor.forClass(AgentRunEventBO.class);
        verify(repository, atLeast(2)).saveEvent(eq("tenant-1::user-1"), eventCaptor.capture());
        assertEquals("RUN_COMPLETED", eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1).getType());
        assertEquals("FAILED", eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1).getStatus());
    }

    @Test
    void structuredProviderAnswerIsRenderedWithoutLeakingRawJsonEnvelope() {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentToolCatalog catalog = mock(AgentToolCatalog.class);
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AiCompletionGateway completion = mock(AiCompletionGateway.class);
        AiProviderRepository providers = mock(AiProviderRepository.class);
        AiUsageBindingRepository bindings = mock(AiUsageBindingRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRateLimitPort rateLimiter = mock(AgentRateLimitPort.class);
        AgentCircuitBreakerPort circuitBreaker = mock(AgentCircuitBreakerPort.class);
        when(catalog.enabledTools()).thenReturn(List.of());
        when(repository.findConversation(any(), any())).thenReturn(Optional.empty());
        when(repository.findMessages(any(), any())).thenReturn(List.of());
        when(lease.tryAcquire(any(), any(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimiter.allow(any(), anyInt(), anyInt())).thenReturn(true);
        when(lease.isCancellationRequested(any())).thenReturn(false);
        when(circuitBreaker.allow(any())).thenReturn(true);
        when(bindings.listAll()).thenReturn(List.of(AiUsageBindingBO.builder().capability("autonomous_agent")
                .providerId(1L).modelOverride("gpt-5.4-mini").enabled(true).build()));
        when(providers.findById(1L)).thenReturn(AiProviderBO.builder().id(1L).code("custom")
                .providerType("custom").baseUrl("http://mock").defaultModel("gpt-5.4-mini").enabled(true).build());
        when(completion.completeAgent(any(), any(), eq("autonomous_agent"))).thenReturn(
                AgentModelTurnResponse.builder().type("FINAL").content(
                        "{\"type\":\"analysis\",\"summary\":\"数据已核验\",\"dataDate\":\"2026-07-22\","
                                + "\"conclusion\":\"市场保持稳定\",\"usedModules\":[\"market.overview\"],"
                                + "\"limitations\":[\"部分数据缺失\"]}").build());

        AutonomousAgentAppService service = newService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        var result = service.execute(AgentRunRequest.builder().userInput("输出结构化分析").build(),
                AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build(), ignored -> { });

        assertEquals("SUCCEEDED", result.getStatus());
        assertTrue(result.getAnswer().contains("## 结论"));
        assertTrue(result.getAnswer().contains("市场保持稳定"));
        assertTrue(result.getAnswer().contains("## 使用的数据模块"));
        assertTrue(!result.getAnswer().startsWith("{\"type\""));
    }

    @Test
    void cancellationInterruptsAnInFlightProviderWaitAndPersistsCancelledStatus() throws Exception {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentToolCatalog catalog = mock(AgentToolCatalog.class);
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AiCompletionGateway completion = mock(AiCompletionGateway.class);
        AiProviderRepository providers = mock(AiProviderRepository.class);
        AiUsageBindingRepository bindings = mock(AiUsageBindingRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRateLimitPort rateLimiter = mock(AgentRateLimitPort.class);
        AgentCircuitBreakerPort circuitBreaker = mock(AgentCircuitBreakerPort.class);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicBoolean providerInterrupted = new AtomicBoolean(false);
        CountDownLatch providerStarted = new CountDownLatch(1);
        CountDownLatch providerStopped = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        when(catalog.enabledTools()).thenReturn(List.of());
        when(repository.findConversation(any(), any())).thenReturn(Optional.empty());
        when(repository.findMessages(any(), any())).thenReturn(List.of());
        when(repository.findRun(any(), any())).thenReturn(Optional.of(AgentRunBO.builder().status("RUNNING").build()));
        when(lease.tryAcquire(any(), any(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimiter.allow(any(), anyInt(), anyInt())).thenReturn(true);
        when(lease.isCancellationRequested(any())).thenAnswer(invocation -> cancelled.get());
        when(lease.requestCancellation(any(), any(), anyInt())).thenAnswer(invocation -> {
            cancelled.set(true);
            return true;
        });
        when(bindings.listAll()).thenReturn(List.of(AiUsageBindingBO.builder().capability("autonomous_agent")
                .providerId(1L).modelOverride("gpt-5.4-mini").enabled(true).build()));
        when(providers.findById(1L)).thenReturn(AiProviderBO.builder().id(1L).code("custom")
                .providerType("custom").baseUrl("http://mock").defaultModel("gpt-5.4-mini").enabled(true).build());
        when(circuitBreaker.allow(any())).thenReturn(true);
        when(completion.completeAgent(any(), any(), eq("autonomous_agent"))).thenAnswer(invocation -> {
            providerStarted.countDown();
            try {
                releaseProvider.await(5, TimeUnit.SECONDS);
                return AgentModelTurnResponse.builder().type("FINAL").content("late answer").build();
            } catch (InterruptedException exception) {
                providerInterrupted.set(true);
                throw new IllegalStateException("provider worker interrupted", exception);
            } finally {
                providerStopped.countDown();
            }
        });

        AutonomousAgentAppService service = newService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        CompletableFuture<cn.cccs7.cynexus.ai.api.dto.AgentRunDTO> execution = CompletableFuture.supplyAsync(() ->
                service.execute(AgentRunRequest.builder().userInput("cancel this request").build(),
                        AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build(), ignored -> { }));
        assertTrue(providerStarted.await(2, TimeUnit.SECONDS));
        ArgumentCaptor<cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO> runCaptor = ArgumentCaptor.forClass(cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO.class);
        verify(repository).saveRun(runCaptor.capture());
        String runNo = runCaptor.getValue().getRunNo();
        when(repository.requestCancellation(any(), eq(runNo), any()))
                .thenReturn(Optional.of(AgentRunBO.builder().runNo(runNo).status("RUNNING").build()));
        var cancellation = service.cancel(AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build(), runNo);
        assertEquals("CANCEL_REQUESTED", cancellation.getStatus());
        assertEquals("CANCELLED", execution.get(5, TimeUnit.SECONDS).getStatus());
        assertTrue(providerStopped.await(2, TimeUnit.SECONDS));
        assertTrue(providerInterrupted.get());
    }

    @Test
    void replayStreamStartsStrictlyAfterLastEventIdAndStopsAfterTerminalEvent() {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentToolCatalog catalog = mock(AgentToolCatalog.class);
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AiCompletionGateway completion = mock(AiCompletionGateway.class);
        AiProviderRepository providers = mock(AiProviderRepository.class);
        AiUsageBindingRepository bindings = mock(AiUsageBindingRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRateLimitPort rateLimiter = mock(AgentRateLimitPort.class);
        AgentCircuitBreakerPort circuitBreaker = mock(AgentCircuitBreakerPort.class);
        AgentIdentity identity = AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build();
        AgentRunEventBO second = AgentRunEventBO.builder().runNo("AGR-1").sequenceNo(2).type("TOKEN_DELTA")
                .delta("answer").build();
        AgentRunEventBO completed = AgentRunEventBO.builder().runNo("AGR-1").sequenceNo(3).type("RUN_COMPLETED")
                .status("SUCCEEDED").build();
        when(repository.findEvents("tenant-1::user-1", "AGR-1", 1, 200)).thenReturn(List.of(second, completed));
        when(repository.findRun("tenant-1::user-1", "AGR-1"))
                .thenReturn(Optional.of(AgentRunBO.builder().runNo("AGR-1").status("SUCCEEDED").build()));

        AutonomousAgentAppService service = newService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        List<AgentRunEventBO> replayed = new ArrayList<>();
        service.replayStream(identity, "AGR-1", 1, replayed::add);

        assertEquals(List.of(2, 3), replayed.stream().map(AgentRunEventBO::getSequenceNo).toList());
        verify(repository).findEvents("tenant-1::user-1", "AGR-1", 1, 200);
    }

    @Test
    void replayStreamReturnsOnThreadInterruptionWithoutConvertingItToAProviderFailure() throws Exception {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentToolCatalog catalog = mock(AgentToolCatalog.class);
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AiCompletionGateway completion = mock(AiCompletionGateway.class);
        AiProviderRepository providers = mock(AiProviderRepository.class);
        AiUsageBindingRepository bindings = mock(AiUsageBindingRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRateLimitPort rateLimiter = mock(AgentRateLimitPort.class);
        AgentCircuitBreakerPort circuitBreaker = mock(AgentCircuitBreakerPort.class);
        AgentIdentity identity = AgentIdentity.builder().principalKey("user-1").tenantKey("tenant-1").build();
        CountDownLatch replayStarted = new CountDownLatch(1);
        when(repository.findEvents(any(), eq("AGR-1"), anyInt(), anyInt())).thenAnswer(invocation -> {
            replayStarted.countDown();
            return List.of();
        });
        when(repository.findRun(any(), eq("AGR-1")))
                .thenReturn(Optional.of(AgentRunBO.builder().runNo("AGR-1").status("RUNNING").build()));

        AutonomousAgentAppService service = newService(repository, catalog, executor, completion,
                providers, bindings, lease, rateLimiter, circuitBreaker, new ObjectMapper());
        AtomicBoolean interrupted = new AtomicBoolean(false);
        Thread replayThread = new Thread(() -> {
            service.replayStream(identity, "AGR-1", 0, ignored -> { });
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        replayThread.start();
        assertTrue(replayStarted.await(2, TimeUnit.SECONDS));
        replayThread.interrupt();
        replayThread.join(2_000);

        assertTrue(interrupted.get());
    }

    @Test
    void sourceLinksRejectUnsafeOriginsAndAllowOnlyConfiguredHttpsOrigins() {
        assertEquals("/stock-overview", AgentSourceUrlPolicy.sanitize("/stock-overview", Set.of()));
        assertNull(AgentSourceUrlPolicy.sanitize("javascript:alert(1)", Set.of()));
        assertNull(AgentSourceUrlPolicy.sanitize("https://untrusted.example/report", Set.of("https://trusted.example")));
        assertEquals("https://trusted.example/report", AgentSourceUrlPolicy.sanitize(
                "https://trusted.example/report", Set.of("https://trusted.example")));
    }

    private AutonomousAgentAppService newService(AgentRunRepository repository,
                                                   AgentToolCatalog catalog,
                                                   AgentToolExecutor executor,
                                                   AiCompletionGateway completion,
                                                   AiProviderRepository providers,
                                                   AiUsageBindingRepository bindings,
                                                   AgentRuntimeLeasePort lease,
                                                   AgentRateLimitPort rateLimiter,
                                                   AgentCircuitBreakerPort circuitBreaker,
                                                   ObjectMapper objectMapper) {
        AgentQuotaPort quota = mock(AgentQuotaPort.class);
        when(quota.reserveRequest(any(), any(), any(), anyInt())).thenReturn(true);
        return new AutonomousAgentAppService(repository, catalog, executor, completion, providers, bindings,
                lease, rateLimiter, circuitBreaker, objectMapper, quota,
                mock(AgentObservabilityPort.class), mock(AgentCapabilityRepository.class));
    }
}
