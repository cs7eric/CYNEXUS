package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionAiPort;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.Identity;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.Image;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HttpFoodRecognitionAiClientTest {
    private static final String SECRET = "food-recognition-test-secret";
    private static final Identity IDENTITY = new Identity("owner", "tenant", "principal");

    @Test void enqueuesOriginalsAndPollsUsingSignedOwnerIdentityAndStableKey() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<JsonNode> payload = new AtomicReference<>();
        AtomicReference<String> idempotency = new AtomicReference<>();
        AtomicReference<Boolean> signed = new AtomicReference<>();
        ObjectMapper mapper = new ObjectMapper();
        server.createContext("/api/ai/executions/vision/tasks", exchange -> {
            signed.set(TrustedPrincipalSigner.verify(exchange.getRequestHeaders().getFirst("X-Cynexus-Principal"),
                    exchange.getRequestHeaders().getFirst("X-Cynexus-Tenant"),
                    exchange.getRequestHeaders().getFirst("X-Cynexus-Identity-Signature"), SECRET));
            String body;
            if ("POST".equals(exchange.getRequestMethod())) {
                payload.set(mapper.readTree(exchange.getRequestBody()));
                idempotency.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
                body = "{\"code\":\"0\",\"data\":{\"processId\":\"123\",\"status\":\"queued\"}}";
            } else {
                body = "{\"code\":\"0\",\"data\":{\"status\":\"succeeded\",\"content\":\"{}\"}}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            HttpFoodRecognitionAiClient client = new HttpFoodRecognitionAiClient("http://127.0.0.1:" + server.getAddress().getPort(), SECRET);
            String id = client.enqueue(IDENTITY, "food-recognition:12:0", List.of(
                    new Image("food.jpg", "image/jpeg", "original-food"), new Image("label.jpg", "image/jpeg", "original-label")));
            assertEquals("123", id);
            assertEquals("food_nutrition_label_parse", payload.get().path("scenarioCode").asText());
            assertEquals("original-food", payload.get().path("images").get(0).path("dataUrl").asText());
            assertEquals("original-label", payload.get().path("images").get(1).path("dataUrl").asText());
            assertEquals("food-recognition:12:0", idempotency.get());
            assertTrue(signed.get());
            assertEquals("succeeded", client.get(IDENTITY, id).status());
            assertTrue(signed.get());
        } finally { server.stop(0); }
    }

    @Test void badTaskIdentifiersCannotBecomeArbitraryRequestPaths() {
        HttpFoodRecognitionAiClient client = new HttpFoodRecognitionAiClient("http://127.0.0.1:1", SECRET);
        FoodRecognitionAiPort.Failure failure = assertThrows(FoodRecognitionAiPort.Failure.class, () -> client.get(IDENTITY, "../../other"));
        assertFalse(failure.retryable());
    }
}
