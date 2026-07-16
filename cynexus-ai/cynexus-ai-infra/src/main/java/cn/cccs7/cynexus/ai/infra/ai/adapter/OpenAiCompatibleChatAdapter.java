package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;

public abstract class OpenAiCompatibleChatAdapter implements AiModelAdapter {

    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String V1_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String RESPONSES_PATH = "/responses";
    private static final String V1_RESPONSES_PATH = "/v1/responses";

    private final ObjectMapper objectMapper;
    private final SecretResolver secretResolver;
    private final Environment environment;
    private final HttpClient httpClient;

    protected OpenAiCompatibleChatAdapter(SecretResolver secretResolver,
                                          Environment environment) {
        this.objectMapper = new ObjectMapper();
        this.secretResolver = secretResolver;
        this.environment = environment;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt) {
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing API key for provider: " + providerCode());
        }

        try {
            boolean responsesApi = "responses".equalsIgnoreCase(extraString(model, "wire_api"));
            boolean jsonOutput = shouldRequestJsonOutput(model, prompt);
            String payload = responsesApi ? buildResponsesPayload(model, prompt, jsonOutput) : buildChatPayload(model, prompt, jsonOutput);
            HttpRequest request = HttpRequest.newBuilder(resolveEndpoint(model, responsesApi))
                    .timeout(resolveRequestTimeout(model))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI provider returned " + response.statusCode() + ": " + response.body());
            }
            String responseBody = response.body();
            String content = responsesApi ? extractResponsesContent(responseBody) : extractChatContent(responseBody);
            return AiCompletionResultBO.builder()
                    .content(content)
                    .providerCode(providerCode())
                    .modelCode(resolveModelName(model))
                    .inputTokens(extractInputTokens(responseBody, prompt))
                    .outputTokens(extractOutputTokens(responseBody, content))
                    .totalTokens(extractTotalTokens(responseBody, prompt, content))
                    .rawResponse(responseBody)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call AI provider: " + providerCode(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI provider call interrupted: " + providerCode(), e);
        }
    }

    protected abstract String baseUrlProperty();

    protected abstract String apiKeyEnvProperty();

    protected abstract String defaultBaseUrl();

    protected abstract String defaultApiKeyEnv();

    protected abstract String defaultModelName();

    private String buildChatPayload(AiModelBO model, String prompt, boolean jsonOutput) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        payload.put("temperature", 0);
        if (jsonOutput) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        payload.put("messages", List.of(
                Map.of("role", "user", "content", prompt == null ? "" : prompt)
        ));
        return objectMapper.writeValueAsString(payload);
    }

    private String buildResponsesPayload(AiModelBO model, String prompt, boolean jsonOutput) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        payload.put("input", prompt == null ? "" : prompt);
        payload.put("store", !extraBoolean(model, "disable_response_storage", true));
        String reasoningEffort = extraString(model, "model_reasoning_effort");
        if (!reasoningEffort.isBlank()) {
            payload.put("reasoning", Map.of("effort", reasoningEffort));
        }
        if (jsonOutput) {
            payload.put("text", Map.of("format", Map.of("type", "json_object")));
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String resolveApiKey(AiModelBO model) {
        if (model != null && model.getApiKeyEnv() != null && !model.getApiKeyEnv().isBlank()) {
            String envValue = secretResolver.resolve("env://" + model.getApiKeyEnv());
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
        }
        if (model != null && model.getApiKey() != null && !model.getApiKey().isBlank()) {
            return model.getApiKey();
        }
        String apiKeyEnv = environment.getProperty(apiKeyEnvProperty(), defaultApiKeyEnv());
        String envValue = secretResolver.resolve("env://" + apiKeyEnv);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return null;
    }

    private String resolveModelName(AiModelBO model) {
        if (model != null && model.getModelName() != null && !model.getModelName().isBlank()) {
            return model.getModelName();
        }
        if (model != null && model.getModelCode() != null && !model.getModelCode().isBlank()) {
            return model.getModelCode();
        }
        return defaultModelName();
    }

    private URI resolveEndpoint(AiModelBO model, boolean responsesApi) {
        String baseUrl = model != null && model.getBaseUrl() != null && !model.getBaseUrl().isBlank()
                ? model.getBaseUrl().trim()
                : environment.getProperty(baseUrlProperty(), defaultBaseUrl()).trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String path = responsesApi
                ? (baseUrl.endsWith("/v1") ? RESPONSES_PATH : V1_RESPONSES_PATH)
                : (baseUrl.endsWith("/v1") ? CHAT_COMPLETIONS_PATH : V1_CHAT_COMPLETIONS_PATH);
        return URI.create(baseUrl + path);
    }

    private Duration resolveRequestTimeout(AiModelBO model) {
        int seconds = extraInt(model, "request_timeout_seconds", (int) DEFAULT_REQUEST_TIMEOUT.toSeconds());
        return Duration.ofSeconds(Math.max(10, Math.min(seconds, 600)));
    }

    private String extractChatContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.at("/choices/0/message/content");
        if (content.isMissingNode() || content.isNull()) {
            throw new IllegalStateException("AI provider response missing choices[0].message.content");
        }
        return content.asText();
    }

    private String extractResponsesContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode outputText = root.get("output_text");
        if (outputText != null && !outputText.isNull() && !outputText.asText("").isBlank()) {
            return outputText.asText();
        }
        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.get("content");
                if (content == null || !content.isArray()) {
                    continue;
                }
                for (JsonNode part : content) {
                    JsonNode text = part.get("text");
                    if (text != null && !text.isNull() && !text.asText("").isBlank()) {
                        return text.asText();
                    }
                }
            }
        }
        throw new IllegalStateException("AI provider response missing output text");
    }

    private Integer extractInputTokens(String responseBody, String prompt) {
        Integer tokens = extractFirstInt(responseBody,
                "/usage/input_tokens",
                "/usage/prompt_tokens");
        return tokens == null ? estimateTokens(prompt) : tokens;
    }

    private Integer extractOutputTokens(String responseBody, String content) {
        Integer tokens = extractFirstInt(responseBody,
                "/usage/output_tokens",
                "/usage/completion_tokens");
        return tokens == null ? estimateTokens(content) : tokens;
    }

    private Integer extractTotalTokens(String responseBody, String prompt, String content) {
        Integer tokens = extractFirstInt(responseBody, "/usage/total_tokens");
        return tokens == null ? estimateTokens(prompt) + estimateTokens(content) : tokens;
    }

    private Integer extractFirstInt(String responseBody, String... paths) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            for (String path : paths) {
                JsonNode value = root.at(path);
                if (!value.isMissingNode() && value.canConvertToInt()) {
                    return value.asInt();
                }
            }
            return null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private int estimateTokens(String value) {
        return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4);
    }

    private String extraString(AiModelBO model, String field) {
        JsonNode extra = extra(model);
        JsonNode value = extra.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private boolean extraBoolean(AiModelBO model, String field, boolean defaultValue) {
        JsonNode extra = extra(model);
        JsonNode value = extra.get(field);
        return value == null || value.isNull() ? defaultValue : value.asBoolean(defaultValue);
    }

    private int extraInt(AiModelBO model, String field, int defaultValue) {
        JsonNode value = extra(model).get(field);
        return value == null || value.isNull() || !value.canConvertToInt() ? defaultValue : value.asInt(defaultValue);
    }

    private boolean shouldRequestJsonOutput(AiModelBO model, String prompt) {
        String responseFormat = extraString(model, "response_format");
        if ("json_object".equalsIgnoreCase(responseFormat) || "json".equalsIgnoreCase(responseFormat)) {
            return true;
        }
        if ("text".equalsIgnoreCase(responseFormat) || "none".equalsIgnoreCase(responseFormat)) {
            return false;
        }
        String text = prompt == null ? "" : prompt;
        if (text.contains("不要返回 JSON") || text.contains("不要返回JSON")) {
            return false;
        }
        return text.contains("JSON") || text.contains("json") || text.contains("合法 JSON");
    }

    private JsonNode extra(AiModelBO model) {
        if (model == null || model.getExtra() == null || model.getExtra().isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(model.getExtra());
        } catch (IOException ignored) {
            return objectMapper.createObjectNode();
        }
    }
}
