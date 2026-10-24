package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import cn.cccs7.cynexus.core.domain.food.FridgeInventoryAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Calls the shared AI execution boundary; the domain owns response validation. */
@Component
public class HttpFridgeInventoryAiClient implements FridgeInventoryAiClient {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;
    private final String trustedIdentitySecret;

    public HttpFridgeInventoryAiClient(
            @Value("${cynexus.fridge.ai.base-url:${cynexus.finance.ai.base-url:http://127.0.0.1:28081}}") String baseUrl,
            @Value("${cynexus.core.food.trusted-identity-secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String trustedIdentitySecret) {
        this.baseUrl = trim(baseUrl);
        this.trustedIdentitySecret = trustedIdentitySecret;
    }

    @Override
    public Completion preview(String input, String inputMode) {
        throw new IllegalStateException("A trusted identity is required for fridge AI preview");
    }

    @Override
    public Completion preview(String input, String inputMode, String tenant, String principal) {
        return execute("food_fridge_inventory", input, Map.of("input_mode", inputMode), tenant, principal);
    }

    @Override
    public Completion matchNutrition(String candidateContext, String tenant, String principal) {
        if (candidateContext == null || candidateContext.length() > 160000) {
            throw new IllegalArgumentException("Nutrition candidate context is too large");
        }
        return execute("food_fridge_nutrition_match", "Rank the supplied food items against their supplied candidates.",
                Map.of("candidate_input", candidateContext), tenant, principal);
    }

    private Completion execute(String scenarioCode, String input, Map<String, String> variables,
                               String tenant, String principal) {
        try {
            String signature = TrustedPrincipalSigner.sign(principal, tenant, trustedIdentitySecret);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("scenarioCode", scenarioCode);
            payload.put("userInput", input);
            payload.put("variables", variables);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/ai/executions"))
                    .timeout(Duration.ofMinutes(3))
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON_UTF8)
                    .header("X-Cynexus-Tenant", tenant)
                    .header("X-Cynexus-Principal", principal)
                    .header("X-Cynexus-Identity-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI preview request failed: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!"0".equals(root.path("code").asText("0")) || data.isMissingNode() || data.isNull()) {
                throw new IllegalStateException("AI preview response is invalid");
            }
            if ("FAILED".equalsIgnoreCase(data.path("status").asText())) {
                throw new IllegalStateException("AI preview execution failed");
            }
            String content = data.path("content").asText("");
            if (content.isBlank()) throw new IllegalStateException("AI preview returned no content");
            String providerCode = data.path("providerCode").asText("").trim();
            String modelCode = data.path("modelCode").asText("").trim();
            if (providerCode.isBlank() || modelCode.isBlank()) {
                throw new IllegalStateException("AI preview response omitted the resolved provider or model");
            }
            return new Completion(content, providerCode, modelCode);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI preview request interrupted", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("AI preview request failed", exception);
        }
    }

    private static String trim(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
