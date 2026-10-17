package cn.cccs7.cynexus.ai.infra.agent;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentScopeOrchestratorTest {
    @Test
    void disabledByDefaultAndDoesNotAcceptCommands() {
        var adapter = new AgentScopeOrchestrator(false, "", "", "");
        assertThrows(IllegalStateException.class, () -> adapter.execute(
                new cn.cccs7.cynexus.ai.domain.port.AgentOrchestratorPort.AgentRunCommand(
                        "run", "market_research", "input", "tenant", "principal")));
    }

    @Test
    void enabledEndpointMustBeAllowListed() {
        assertThrows(IllegalArgumentException.class, () -> new AgentScopeOrchestrator(
                true, "https://agentscope.example.test/run", "token", "https://other.example.test"));
    }

    @Test
    void boundedMarketResearchWorkflowMapsGatewayResult() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/run", exchange -> {
            byte[] body = "{\"runNo\":\"scope-run\",\"status\":\"SUCCEEDED\",\"content\":\"bounded result\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            var adapter = new AgentScopeOrchestrator(true, "http://localhost:" + port + "/run", "token",
                    "http://localhost:" + port);
            var result = adapter.execute(new cn.cccs7.cynexus.ai.domain.port.AgentOrchestratorPort.AgentRunCommand(
                    "run", "market_research", "input", "tenant", "principal"));
            assertEquals("scope-run", result.runNo());
            assertEquals("SUCCEEDED", result.status());
            assertEquals("bounded result", result.content());
        } finally {
            server.stop(0);
        }
    }
}
