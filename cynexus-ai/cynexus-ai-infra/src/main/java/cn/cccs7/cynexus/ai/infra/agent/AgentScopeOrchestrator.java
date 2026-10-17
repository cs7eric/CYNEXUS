package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.domain.port.AgentOrchestratorPort;
import cn.cccs7.cynexus.ai.domain.policy.security.AgentOutboundEndpointPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Isolated HTTP adapter for an AgentScope gateway. AgentScope never receives a
 * repository, business writer, or Cynexus run-state handle; it returns only a
 * bounded result which the caller maps into its own run state machine.
 */
@Component("agentScopeOrchestrator")
public class AgentScopeOrchestrator implements AgentOrchestratorPort {
    private final boolean enabled;
    private final URI endpoint;
    private final String token;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public AgentScopeOrchestrator(
            @Value("${cynexus.ai.agentscope.enabled:false}") boolean enabled,
            @Value("${cynexus.ai.agentscope.endpoint:}") String endpoint,
            @Value("${cynexus.ai.agentscope.token:}") String token,
            @Value("${cynexus.ai.security.provider-allowed-origins:}") String allowedOrigins) {
        this.enabled = enabled;
        this.endpoint = enabled ? AgentOutboundEndpointPolicy.requireAllowed(endpoint, allowedOrigins) : null;
        this.token = token == null ? "" : token.trim();
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public AgentRunResult execute(AgentRunCommand command) {
        if (!enabled) throw new IllegalStateException("AgentScope orchestration is disabled");
        if (command == null || blank(command.runNo()) || blank(command.capability())
                || blank(command.input()) || blank(command.tenantKey()) || blank(command.principalKey())) {
            throw new IllegalArgumentException("AgentScope command and identity are required");
        }
        if (!command.capability().startsWith("market_research")) {
            throw new IllegalArgumentException("AgentScope is restricted to the market_research workflow");
        }
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "runNo", command.runNo(), "capability", command.capability(),
                    "input", bounded(command.input(), 32_000), "tenantKey", command.tenantKey(),
                    "principalKey", command.principalKey()));
            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(90)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (!token.isBlank()) request.header("Authorization", "Bearer " + token);
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AgentScope gateway returned HTTP " + response.statusCode());
            }
            JsonNode result = mapper.readTree(bounded(response.body(), 64_000));
            String runNo = text(result, "runNo", command.runNo());
            String status = text(result, "status", "SUCCEEDED");
            String content = bounded(text(result, "content", ""), 32_000);
            return new AgentRunResult(runNo, status, content);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AgentScope request interrupted", interrupted);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("AgentScope request failed", exception);
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String bounded(String value, int max) { return value == null ? "" : value.length() <= max ? value : value.substring(0, max); }
    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isValueNode() ? value.asText(fallback) : fallback;
    }
}
