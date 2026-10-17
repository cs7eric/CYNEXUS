package cn.cccs7.cynexus.ai.infra.observability;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.policy.security.AgentOutboundEndpointPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LangfuseGenerationReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LangfuseGenerationReporter.class);
    private static final String DEFAULT_ALLOWED_ORIGINS = "http://127.0.0.1:33000,http://cynexus-langfuse-web:3000";
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final boolean enabled;
    private final URI ingestionUri;
    private final String authorization;

    public LangfuseGenerationReporter(
            @Value("${cynexus.ai.langfuse.enabled:false}") boolean enabled,
            @Value("${cynexus.ai.langfuse.base-url:http://127.0.0.1:33000}") String baseUrl,
            @Value("${cynexus.ai.langfuse.public-key:}") String publicKey,
            @Value("${cynexus.ai.langfuse.secret-key:}") String secretKey,
            @Value("${cynexus.ai.security.langfuse-allowed-origins:}") String allowedOrigins) {
        boolean configured = enabled && !blank(publicKey) && !blank(secretKey);
        URI validatedBase;
        try {
            String origins = blank(allowedOrigins) ? DEFAULT_ALLOWED_ORIGINS : allowedOrigins;
            validatedBase = AgentOutboundEndpointPolicy.requireAllowed(trimUrl(baseUrl), origins);
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Langfuse endpoint rejected by outbound allow-list: {}", exception.getMessage());
            configured = false;
            validatedBase = URI.create("http://127.0.0.1:33000");
        }
        this.enabled = configured;
        this.ingestionUri = URI.create(validatedBase + "/api/public/ingestion");
        this.authorization = "Basic " + Base64.getEncoder().encodeToString(
                (safe(publicKey) + ":" + safe(secretKey)).getBytes(StandardCharsets.UTF_8));
    }

    public void report(AiModelBO model, String capability, String prompt,
                       AiCompletionResultBO result, Instant startedAt, Throwable error) {
        if (!enabled) return;
        String id = UUID.randomUUID().toString();
        Instant end = Instant.now();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("providerCode", model == null ? "" : safe(model.getProviderCode()));
        metadata.put("pricingCurrency", model == null || blank(model.getPricingCurrency()) ? "USD" : model.getPricingCurrency());
        metadata.put("inputPricePerMillion", model == null || model.getInputPricePerMillion() == null ? BigDecimal.ZERO : model.getInputPricePerMillion());
        metadata.put("outputPricePerMillion", model == null || model.getOutputPricePerMillion() == null ? BigDecimal.ZERO : model.getOutputPricePerMillion());
        metadata.put("capability", safe(capability));
        metadata.put("status", error == null ? "SUCCEEDED" : "FAILED");
        metadata.put("latencyMs", Math.max(0, Duration.between(startedAt, end).toMillis()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("traceId", id);
        body.put("name", blank(capability) ? "ai.completion" : capability);
        body.put("startTime", startedAt.toString());
        body.put("endTime", end.toString());
        body.put("model", result != null && !blank(result.getModelCode()) ? result.getModelCode() : modelName(model));
        body.put("input", null);
        body.put("output", null);
        Map<String, Object> usage = Map.of(
                "input", nonNegative(result == null ? null : result.getInputTokens()),
                "output", nonNegative(result == null ? null : result.getOutputTokens()),
                "total", nonNegative(result == null ? null : result.getTotalTokens()));
        // Langfuse v3 reads usageDetails; usage is retained for older compatible deployments.
        body.put("usageDetails", usage);
        body.put("usage", usage);
        BigDecimal estimatedCost = estimateCost(model, result);
        if (estimatedCost.signum() > 0) {
            body.put("costDetails", Map.of("total", estimatedCost));
        }
        body.put("metadata", metadata);
        if (error != null) body.put("statusMessage", truncate(error.getMessage(), 800));
        Map<String, Object> event = Map.of("id", id, "type", "generation-create", "timestamp", end.toString(), "body", body);
        try {
            String payload = objectMapper.writeValueAsString(Map.of("batch", new Object[]{event}));
            HttpRequest request = HttpRequest.newBuilder(ingestionUri)
                    .timeout(Duration.ofSeconds(3)).header("Authorization", authorization)
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> { if (response.statusCode() >= 300) LOGGER.debug("Langfuse returned {}", response.statusCode()); })
                    .exceptionally(exception -> { LOGGER.warn("Langfuse unavailable at {}: {}", ingestionUri, exception.getMessage()); return null; });
        } catch (Exception exception) {
            LOGGER.debug("Unable to build Langfuse event: {}", exception.getMessage());
        }
    }

    public void report(AiModelBO model, String capability, String prompt,
                       AiCompletionResultBO result, Instant startedAt, Throwable error,
                       AiExecutionContext context) {
        if (!enabled) return;
        reportInternal(model, capability, result, startedAt, error, context);
    }

    private void reportInternal(AiModelBO model, String capability, AiCompletionResultBO result,
                                Instant startedAt, Throwable error, AiExecutionContext context) {
        // Keep the legacy overload behavior while making correlation fields explicit for new callers.
        String id = context == null || blank(context.traceId()) ? UUID.randomUUID().toString() : context.traceId();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("capability", safe(capability));
        if (context != null) {
            metadata.put("executionNo", safe(context.executionNo()));
            metadata.put("promptKey", safe(context.promptKey()));
            metadata.put("promptVersion", safe(context.promptVersion()));
            metadata.put("providerCode", safe(context.providerCode()));
            metadata.put("modelCode", safe(context.modelCode()));
            metadata.put("tenantKey", safe(context.tenantKey()));
            metadata.put("principalKey", safe(context.principalKey()));
            metadata.put("requestId", safe(context.requestId()));
            metadata.put("traceId", id);
        }
        // The existing reporter is intentionally bounded; context-aware calls use the same transport contract.
        reportWithMetadata(model, capability, result, startedAt, error, id, metadata);
    }

    private void reportWithMetadata(AiModelBO model, String capability, AiCompletionResultBO result,
                                    Instant startedAt, Throwable error, String id,
                                    Map<String, Object> contextMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>(contextMetadata);
        metadata.putIfAbsent("providerCode", model == null ? "" : safe(model.getProviderCode()));
        metadata.put("status", error == null ? "SUCCEEDED" : "FAILED");
        metadata.put("latencyMs", Math.max(0, Duration.between(startedAt, Instant.now()).toMillis()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id); body.put("traceId", id);
        body.put("name", blank(capability) ? "ai.completion" : capability);
        body.put("startTime", startedAt.toString()); body.put("endTime", Instant.now().toString());
        body.put("model", result != null && !blank(result.getModelCode()) ? result.getModelCode() : modelName(model));
        body.put("usageDetails", Map.of("input", nonNegative(result == null ? null : result.getInputTokens()),
                "output", nonNegative(result == null ? null : result.getOutputTokens()),
                "total", nonNegative(result == null ? null : result.getTotalTokens())));
        body.put("metadata", metadata);
        if (error != null) body.put("statusMessage", truncate(error.getMessage(), 800));
        try {
            String payload = objectMapper.writeValueAsString(Map.of("batch", new Object[]{Map.of("id", id, "type", "generation-create", "timestamp", Instant.now().toString(), "body", body)}));
            httpClient.sendAsync(HttpRequest.newBuilder(ingestionUri).timeout(Duration.ofSeconds(3)).header("Authorization", authorization)
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.discarding());
        } catch (Exception exception) { LOGGER.debug("Unable to build Langfuse event: {}", exception.getMessage()); }
    }

    private static int nonNegative(Integer value) { return value == null ? 0 : Math.max(0, value); }
    private static BigDecimal estimateCost(AiModelBO model, AiCompletionResultBO result) {
        if (model == null || result == null) return BigDecimal.ZERO;
        BigDecimal input = model.getInputPricePerMillion() == null ? BigDecimal.ZERO : model.getInputPricePerMillion();
        BigDecimal output = model.getOutputPricePerMillion() == null ? BigDecimal.ZERO : model.getOutputPricePerMillion();
        return BigDecimal.valueOf(nonNegative(result.getInputTokens())).multiply(input)
                .add(BigDecimal.valueOf(nonNegative(result.getOutputTokens())).multiply(output))
                .divide(BigDecimal.valueOf(1_000_000L), 12, java.math.RoundingMode.HALF_UP);
    }
    private static String modelName(AiModelBO model) { return model == null ? "unknown" : blank(model.getModelName()) ? safe(model.getModelCode()) : model.getModelName(); }
    private static String truncate(String value, int max) { String text = safe(value); return text.length() > max ? text.substring(0, max) : text; }
    private static String safe(String value) { return value == null ? "" : value; }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String trimUrl(String value) { String result = blank(value) ? "http://127.0.0.1:33000" : value.trim(); while (result.endsWith("/")) result = result.substring(0, result.length() - 1); return result; }
}
