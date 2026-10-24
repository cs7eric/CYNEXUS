package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionAiPort;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.Identity;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.Image;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Signed server-to-server adapter; only fixed AI queue paths can be called. */
@Component
public class HttpFoodRecognitionAiClient implements FoodRecognitionAiPort {
    private static final String PATH = "/api/ai/executions/vision/tasks";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String secret;

    public HttpFoodRecognitionAiClient(
            @Value("${cynexus.food.recognition.ai.base-url:${cynexus.fridge.ai.base-url:${cynexus.finance.ai.base-url:http://127.0.0.1:28081}}}") String baseUrl,
            @Value("${cynexus.core.food.trusted-identity-secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String secret) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.secret = secret;
    }

    @Override
    public String enqueue(Identity identity, String idempotencyKey, List<Image> images) {
        try {
            String body = mapper.writeValueAsString(Map.of("scenarioCode", "food_nutrition_label_parse", "images", images));
            JsonNode data = send(request(identity, PATH).header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey).POST(HttpRequest.BodyPublishers.ofString(body)).build());
            String id = data.path("processId").asText("");
            if (!id.matches("[0-9]{1,19}")) throw new Failure("Nutrition queue returned an invalid task identifier", false);
            return id;
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new Failure("Nutrition request could not be prepared", false);
        }
    }

    @Override
    public Snapshot get(Identity identity, String aiTaskId) {
        if (aiTaskId == null || !aiTaskId.matches("[0-9]{1,19}")) throw new Failure("Nutrition task identifier is invalid", false);
        JsonNode data = send(request(identity, PATH + "/" + aiTaskId).GET().build());
        return new Snapshot(data.path("status").asText(null), data.path("content").asText(null), data.path("errorMessage").asText(null));
    }

    private HttpRequest.Builder request(Identity identity, String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(30))
                .header("X-Cynexus-Tenant", identity.tenant()).header("X-Cynexus-Principal", identity.principal())
                .header("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(identity.principal(), identity.tenant(), secret));
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream input = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    int status = response.statusCode();
                    throw new Failure("Nutrition queue request failed: HTTP " + status,
                            status == 408 || status == 429 || status >= 500);
                }
                byte[] bytes = input.readNBytes(1024 * 1024 + 1);
                if (bytes.length > 1024 * 1024) throw new Failure("Nutrition queue returned an oversized response", false);
                JsonNode root = mapper.readTree(bytes);
                if (root == null || !"0".equals(root.path("code").asText("")) || !root.path("data").isObject()) {
                    throw new Failure("Nutrition queue returned an invalid response", false);
                }
                return root.path("data");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new Failure("Nutrition queue request was interrupted", true);
        } catch (Failure exception) {
            throw exception;
        } catch (Exception exception) {
            throw new Failure("Nutrition queue is temporarily unavailable", true);
        }
    }
}
