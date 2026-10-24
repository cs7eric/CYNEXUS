package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpFridgeInventoryAiClientTest {
    private static final String SECRET = "test-trusted-identity-secret";

    @Test
    void forwardsSignedIdentityToAiExecution() throws Exception {
        AtomicReference<String> principal = new AtomicReference<>();
        AtomicReference<String> tenant = new AtomicReference<>();
        AtomicReference<String> signature = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/ai/executions", exchange -> {
            principal.set(exchange.getRequestHeaders().getFirst("X-Cynexus-Principal"));
            tenant.set(exchange.getRequestHeaders().getFirst("X-Cynexus-Tenant"));
            signature.set(exchange.getRequestHeaders().getFirst("X-Cynexus-Identity-Signature"));
            byte[] body = "{\"code\":\"0\",\"data\":{\"status\":\"SUCCESS\",\"content\":\"{}\",\"providerCode\":\"xtyoo\",\"modelCode\":\"gpt-6.1-sol\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            HttpFridgeInventoryAiClient client = new HttpFridgeInventoryAiClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), SECRET);

            var completion = client.preview("milk", "text", "tenant-a", "user-a");

            assertEquals("xtyoo", completion.providerCode());
            assertEquals("user-a", principal.get());
            assertEquals("tenant-a", tenant.get());
            assertTrue(TrustedPrincipalSigner.verify(principal.get(), tenant.get(), signature.get(), SECRET));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sendsNutritionCandidateContextThroughDedicatedBoundScenario() throws Exception {
        AtomicReference<String> scenario = new AtomicReference<>();
        AtomicReference<String> candidateInput = new AtomicReference<>();
        ObjectMapper mapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/ai/executions", exchange -> {
            var payload = mapper.readTree(exchange.getRequestBody());
            scenario.set(payload.path("scenarioCode").asText());
            candidateInput.set(payload.path("variables").path("candidate_input").asText());
            byte[] body = "{\"code\":\"0\",\"data\":{\"status\":\"SUCCESS\",\"content\":\"{\\\"matches\\\":[]}\",\"providerCode\":\"xtyoo\",\"modelCode\":\"gpt-6.1-sol\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            HttpFridgeInventoryAiClient client = new HttpFridgeInventoryAiClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), SECRET);
            client.matchNutrition("{\"items\":[{\"itemIndex\":0}]}", "tenant-a", "user-a");
            assertEquals("food_fridge_nutrition_match", scenario.get());
            assertEquals("{\"items\":[{\"itemIndex\":0}]}", candidateInput.get());
        } finally {
            server.stop(0);
        }
    }
}
