package cn.cccs7.cynexus.ai.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.application.service.AutonomousAgentAppService;
import cn.cccs7.cynexus.ai.domain.agent.AgentCircuitBreakerPort;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.agent.AgentRateLimitPort;
import cn.cccs7.cynexus.ai.domain.agent.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolExecutor;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.port.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.repository.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

class AgentEvaluationDatasetTest {

    @Test
    void executesVersionedAcceptanceDatasetThroughTheModelDirectedWorkflow() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("ai-agent-evaluation.json")) {
            JsonNode root = mapper.readTree(input);
            List<JsonNode> cases = new ArrayList<>();
            root.path("cases").forEach(cases::add);
            assertEquals(5, cases.size());
            List<Map<String, Object>> results = new ArrayList<>();
            for (JsonNode evaluationCase : cases) {
                results.add(executeCase(evaluationCase));
            }
            Files.createDirectories(Path.of("target"));
            Files.writeString(Path.of("target", "agent-evaluation-results.json"), mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(Map.of("version", root.path("version").asText(), "passed", results.size(),
                            "total", cases.size(), "cases", results)));
            int passed = results.size();
            System.out.println("AGENT_EVALUATION version=" + root.path("version").asText() + " passed=" + passed + " total=" + cases.size());
        }
    }

    private Map<String, Object> executeCase(JsonNode evaluationCase) {
        String input = evaluationCase.path("input").asText();
        List<String> expectedTools = new ArrayList<>();
        evaluationCase.path("expected_tools").forEach(item -> expectedTools.add(item.asText()));
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentToolCatalog catalog = mock(AgentToolCatalog.class);
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AiCompletionGateway completion = mock(AiCompletionGateway.class);
        AiProviderRepository providers = mock(AiProviderRepository.class);
        AiUsageBindingRepository bindings = mock(AiUsageBindingRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRateLimitPort limiter = mock(AgentRateLimitPort.class);
        AgentCircuitBreakerPort circuit = mock(AgentCircuitBreakerPort.class);
        List<String> actualTools = new ArrayList<>();
        AtomicInteger turns = new AtomicInteger();
        when(repository.findConversation(any(), any())).thenReturn(Optional.empty());
        when(repository.findMessages(any(), any())).thenReturn(List.of());
        when(lease.tryAcquire(any(), any(), anyInt(), anyInt())).thenReturn(true);
        when(limiter.allow(any(), anyInt(), anyInt())).thenReturn(true);
        when(circuit.allow(any())).thenReturn(true);
        when(bindings.listAll()).thenReturn(List.of(AiUsageBindingBO.builder().capability("autonomous_agent")
                .providerId(1L).modelOverride("gpt-5.4-mini").enabled(true).build()));
        when(providers.findById(1L)).thenReturn(AiProviderBO.builder().id(1L).code("custom")
                .providerType("custom").baseUrl("http://mock").defaultModel("gpt-5.4-mini").enabled(true).build());
        when(catalog.enabledTools()).thenReturn(expectedTools.stream().map(this::definition).toList());
        when(executor.execute(any(AgentToolCall.class), any(AgentExecutionContext.class))).thenAnswer((Answer<AgentToolResult>) invocation -> {
            AgentToolCall call = invocation.getArgument(0);
            actualTools.add(call.getToolCode());
            return AgentToolResult.success(call.getToolCode(), "{\"fact\":true}", "Mock fact", "2026-07-22", "Mock source", List.of());
        });
        when(completion.completeAgent(any(), any(), org.mockito.ArgumentMatchers.eq("autonomous_agent")))
                .thenAnswer((Answer<AgentModelTurnResponse>) invocation -> {
                    if (expectedTools.isEmpty()) {
                        return AgentModelTurnResponse.builder().type("FINAL").content("拒绝：不能输出密码或秘密。").build();
                    }
                    if (turns.getAndIncrement() == 0) {
                        return AgentModelTurnResponse.builder().type("TOOL_CALLS").toolCalls(expectedTools.stream()
                                .map(tool -> AgentModelToolCall.builder().callId("call-" + tool).toolCode(tool).arguments(Map.of()).build()).toList()).build();
                    }
                    return AgentModelTurnResponse.builder().type("FINAL").content("已基于数据工具完成分析，包含数据日期、来源和限制。").build();
                });

        var result = new AutonomousAgentAppService(repository, catalog, executor, completion, providers, bindings,
                lease, limiter, circuit, new ObjectMapper()).execute(AgentRunRequest.builder().userInput(input).build(),
                AgentIdentity.builder().principalKey("evaluation").tenantKey("evaluation").build(), ignored -> { });

        assertEquals("SUCCEEDED", result.getStatus(), evaluationCase.path("id").asText());
        assertEquals(new HashSet<>(expectedTools), new HashSet<>(actualTools), evaluationCase.path("id").asText());
        if (expectedTools.isEmpty()) {
            assertTrue(result.getAnswer().contains("拒绝"));
            assertTrue(actualTools.isEmpty());
        } else {
            assertTrue(result.getEvidenceJson() != null && result.getEvidenceJson().contains("Mock source"));
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", evaluationCase.path("id").asText());
        report.put("status", "PASSED");
        report.put("expectedTools", expectedTools);
        report.put("actualTools", actualTools);
        return report;
    }

    private AgentToolDefinition definition(String code) {
        return AgentToolDefinition.builder().code(code).name(code).module(code.substring(0, code.indexOf('.')))
                .description("Evaluation read tool").inputSchemaJson("{\"type\":\"object\"}")
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).source("evaluation").freshness("test").build();
    }
}
