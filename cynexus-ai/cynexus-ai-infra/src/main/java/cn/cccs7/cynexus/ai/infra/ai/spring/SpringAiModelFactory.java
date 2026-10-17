package cn.cccs7.cynexus.ai.infra.ai.spring;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import com.openai.client.OpenAIClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.cccs7.cynexus.ai.domain.policy.security.AgentOutboundEndpointPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.stereotype.Component;

/** Builds and caches Spring AI models from the database-routed model descriptor. */
@Component
public class SpringAiModelFactory {
    private final SecretResolver secrets;
    private final String allowedOrigins;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, OpenAiChatModel> cache = new ConcurrentHashMap<>();

    public SpringAiModelFactory(SecretResolver secrets,
            @Value("${cynexus.ai.security.provider-allowed-origins:}") String allowedOrigins) {
        this.secrets = secrets;
        this.allowedOrigins = allowedOrigins;
    }

    public OpenAiChatModel resolve(AiModelBO model) {
        if (model == null) throw new IllegalArgumentException("AI model is required");
        String key = key(model);
        return cache.computeIfAbsent(key, ignored -> create(model));
    }

    /** Invalidates cached clients after a provider/model configuration update. */
    public void invalidate(AiModelBO model) {
        if (model != null) cache.remove(key(model));
    }

    public void invalidateAll() {
        cache.clear();
    }

    private OpenAiChatModel create(AiModelBO model) {
        String apiKey = model.getApiKey();
        if ((apiKey == null || apiKey.isBlank()) && model.getApiKeyEnv() != null && !model.getApiKeyEnv().isBlank()) {
            apiKey = secrets.resolve("env://" + model.getApiKeyEnv());
        }
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Missing API key for provider: " + model.getProviderCode());
        String configuredBaseUrl = trim(model.getBaseUrl());
        AgentOutboundEndpointPolicy.requireAllowed(configuredBaseUrl, allowedOrigins);
        String baseUrl = SpringAiOpenAiClientSupport.normalizeBaseUrl(configuredBaseUrl);
        String modelName = model.getModelName() == null || model.getModelName().isBlank() ? model.getModelCode() : model.getModelName();
        JsonNode extra = parseExtra(model.getExtra());
        int timeoutSeconds = intValue(extra, "request_timeout_seconds", 90);
        int timeoutMs = intValue(extra, "request_timeout_ms", 0);
        if (timeoutMs > 0) timeoutSeconds = Math.max(1, timeoutMs / 1000);
        if (model.getRuntimeOptions() != null && model.getRuntimeOptions().getTimeoutMs() != null) {
            timeoutSeconds = Math.max(1, model.getRuntimeOptions().getTimeoutMs() / 1000);
        }
        Duration timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        var builder = OpenAiChatOptions.builder().apiKey(apiKey).baseUrl(baseUrl).model(modelName)
                .timeout(timeout).maxRetries(intValue(extra, "max_retries", 0));
        Integer maxTokens = integer(extra, "max_completion_tokens", integer(extra, "max_tokens", null));
        if (maxTokens != null) builder.maxTokens(maxTokens);
        if (model.getRuntimeOptions() != null && model.getRuntimeOptions().getTemperature() != null) {
            builder.temperature(model.getRuntimeOptions().getTemperature().doubleValue());
        } else if (extra.has("temperature") && extra.get("temperature").isNumber()) {
            builder.temperature(extra.get("temperature").doubleValue());
        }
        String reasoning = text(extra, "reasoning_effort", text(extra, "model_reasoning_effort", null));
        if (reasoning != null) builder.reasoningEffort(reasoning);
        String responseFormat = text(extra, "response_format", null);
        if (responseFormat == null && extra.path("response_format").isObject()) {
            responseFormat = text(extra.path("response_format"), "type", null);
        }
        // Capability support only describes what the model can do. It must not
        // force every request routed to that model into JSON mode: plain-text
        // capabilities may intentionally share the same model. JSON mode is
        // enabled only when the binding/runtime explicitly requests it.
        if ("json_object".equalsIgnoreCase(responseFormat) || "json".equalsIgnoreCase(responseFormat)) {
            builder.responseFormat(OpenAiChatModel.ResponseFormat.builder()
                    .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT).build());
        }
        OpenAiChatOptions options = builder.build();
        OpenAIClient client = OpenAiSetup.setupSyncClient(baseUrl, apiKey, null, null, null, null, false, false, null,
                timeout, 0, null, Map.of(), io.micrometer.observation.ObservationRegistry.NOOP,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), java.util.List.of());
        return OpenAiChatModel.builder().openAiClient(client).options(options).build();
    }

    private String key(AiModelBO model) {
        // Include a one-way key fingerprint so rotating a DB-supplied secret cannot reuse an old client.
        String configuredSecret = model.getApiKey();
        if ((configuredSecret == null || configuredSecret.isBlank())
                && model.getApiKeyEnv() != null && !model.getApiKeyEnv().isBlank()) {
            configuredSecret = secrets.resolve("env://" + model.getApiKeyEnv());
        }
        String secretFingerprint = Integer.toHexString(java.util.Objects.toString(configuredSecret, "").hashCode());
        return String.join("|", trim(model.getProviderCode()), trim(model.getBaseUrl()), trim(model.getModelCode()), trim(model.getModelName()), trim(model.getApiKeyEnv()), secretFingerprint, trim(model.getExtra()), String.valueOf(model.getRuntimeOptions()));
    }

    private String trim(String value) { return value == null ? "" : value.trim(); }
    private JsonNode parseExtra(String value) { try { return value == null || value.isBlank() ? mapper.createObjectNode() : mapper.readTree(value); } catch (Exception ignored) { return mapper.createObjectNode(); } }
    private static String text(JsonNode node, String key, String fallback) { JsonNode value = node.get(key); return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : fallback; }
    private static int intValue(JsonNode node, String key, int fallback) { Integer value = integer(node, key, null); return value == null ? fallback : value; }
    private static Integer integer(JsonNode node, String key, Integer fallback) {
        JsonNode value = node.get(key);
        // Keep this boxed: a ternary containing intValue() otherwise unboxes a
        // null fallback before it can be returned to optional runtime options.
        return value != null && value.isIntegralNumber() ? Integer.valueOf(value.intValue()) : fallback;
    }
}
