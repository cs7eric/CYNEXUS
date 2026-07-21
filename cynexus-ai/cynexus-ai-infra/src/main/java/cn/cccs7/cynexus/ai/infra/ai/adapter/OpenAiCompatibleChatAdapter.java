package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
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

    @Override
    public AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request) {
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing API key for provider: " + providerCode());
        }
        try {
            boolean responsesApi = "responses".equalsIgnoreCase(extraString(model, "wire_api"));
            String payload = responsesApi ? buildAgentResponsesPayload(model, request) : buildAgentChatPayload(model, request);
            HttpRequest httpRequest = HttpRequest.newBuilder(resolveEndpoint(model, responsesApi))
                    .timeout(resolveRequestTimeout(model))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI provider returned " + response.statusCode() + ": " + response.body());
            }
            return responsesApi ? extractResponsesAgentResponse(response.body(), request) : extractChatAgentResponse(response.body(), request);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call AI provider: " + providerCode(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI provider call interrupted: " + providerCode(), exception);
        }
    }

    @Override
    public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images) {
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Missing API key for provider: " + providerCode());
        try {
            String payload = buildChatPayload(model, prompt, shouldRequestJsonOutput(model, prompt), images);
            HttpRequest request = HttpRequest.newBuilder(resolveEndpoint(model, false))
                    .timeout(resolveRequestTimeout(model)).header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("AI provider returned " + response.statusCode() + ": " + response.body());
            String content = extractChatContent(response.body());
            return AiCompletionResultBO.builder().content(content).providerCode(providerCode()).modelCode(resolveModelName(model))
                    .inputTokens(extractInputTokens(response.body(), prompt)).outputTokens(extractOutputTokens(response.body(), content))
                    .totalTokens(extractTotalTokens(response.body(), prompt, content)).rawResponse(response.body()).build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call AI vision provider: " + providerCode(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI vision provider call interrupted: " + providerCode(), e);
        }
    }

    protected abstract String baseUrlProperty();

    protected abstract String apiKeyEnvProperty();

    protected abstract String defaultBaseUrl();

    protected abstract String defaultApiKeyEnv();

    protected abstract String defaultModelName();

    private String buildChatPayload(AiModelBO model, String prompt, boolean jsonOutput) throws IOException {
        return buildChatPayload(model, prompt, jsonOutput, List.of());
    }

    private String buildChatPayload(AiModelBO model, String prompt, boolean jsonOutput, List<AiImageInputBO> images) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        payload.put("temperature", 0);
        if (jsonOutput) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        if (images == null || images.isEmpty()) {
            payload.put("messages", List.of(Map.of("role", "user", "content", prompt == null ? "" : prompt)));
        } else {
            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", prompt == null ? "" : prompt));
            for (AiImageInputBO image : images) {
                if (image != null && image.getDataUrl() != null && !image.getDataUrl().isBlank()) {
                    content.add(Map.of("type", "image_url", "image_url", Map.of("url", image.getDataUrl())));
                }
            }
            payload.put("messages", List.of(Map.of("role", "user", "content", content)));
        }
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

    private String buildAgentChatPayload(AiModelBO model, AgentModelTurnRequest request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        payload.put("temperature", 0);
        payload.put("messages", buildChatMessages(request));
        payload.put("tools", buildToolDefinitions(request == null ? List.of() : request.getTools()));
        payload.put("tool_choice", "auto");
        return objectMapper.writeValueAsString(payload);
    }

    private String buildAgentResponsesPayload(AiModelBO model, AgentModelTurnRequest request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        payload.put("input", buildResponsesInput(request));
        payload.put("tools", buildToolDefinitions(request == null ? List.of() : request.getTools()));
        payload.put("store", !extraBoolean(model, "disable_response_storage", true));
        return objectMapper.writeValueAsString(payload);
    }

    private List<Map<String, Object>> buildChatMessages(AgentModelTurnRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (request != null && request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        if (request != null && request.getMessages() != null) {
            for (AgentModelMessage message : request.getMessages()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("role", message.getRole());
                item.put("content", message.getContent() == null ? "" : message.getContent());
                if ("tool".equals(message.getRole()) && message.getToolCallId() != null) {
                    item.put("tool_call_id", message.getToolCallId());
                }
                if ("assistant".equals(message.getRole()) && message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                    item.put("tool_calls", message.getToolCalls().stream().map(this::toChatToolCall).toList());
                    item.put("content", message.getContent() == null ? "" : message.getContent());
                }
                messages.add(item);
            }
        }
        return messages;
    }

    private List<Map<String, Object>> buildResponsesInput(AgentModelTurnRequest request) {
        List<Map<String, Object>> input = new ArrayList<>();
        if (request != null && request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            input.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        if (request != null && request.getMessages() != null) {
            for (AgentModelMessage message : request.getMessages()) {
                if ("tool".equals(message.getRole())) {
                    input.add(Map.of("type", "function_call_output", "call_id", message.getToolCallId(),
                            "output", message.getContent() == null ? "" : message.getContent()));
                } else if ("assistant".equals(message.getRole()) && message.getToolCalls() != null
                        && !message.getToolCalls().isEmpty()) {
                    for (AgentModelToolCall call : message.getToolCalls()) {
                        input.add(toResponsesFunctionCall(call));
                    }
                } else {
                    input.add(Map.of("role", message.getRole(), "content", message.getContent() == null ? "" : message.getContent()));
                }
            }
        }
        return input;
    }

    private Map<String, Object> toResponsesFunctionCall(AgentModelToolCall call) {
        try {
            return Map.of("type", "function_call", "call_id", call.getCallId(), "name", call.getToolCode(),
                    "arguments", objectMapper.writeValueAsString(call.getArguments() == null ? Map.of() : call.getArguments()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Agent tool arguments", exception);
        }
    }

    private List<Map<String, Object>> buildToolDefinitions(List<AgentToolDefinition> definitions) throws IOException {
        List<Map<String, Object>> tools = new ArrayList<>();
        if (definitions == null) return tools;
        for (AgentToolDefinition definition : definitions) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", definition.getCode());
            function.put("description", definition.getDescription());
            function.put("parameters", objectMapper.readTree(definition.getInputSchemaJson()));
            tools.add(Map.of("type", "function", "function", function));
        }
        return tools;
    }

    private Map<String, Object> toChatToolCall(AgentModelToolCall call) {
        try {
            return Map.of("id", call.getCallId(), "type", "function",
                    "function", Map.of("name", call.getToolCode(),
                            "arguments", objectMapper.writeValueAsString(call.getArguments() == null ? Map.of() : call.getArguments())));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Agent tool arguments", exception);
        }
    }

    private AgentModelTurnResponse extractChatAgentResponse(String responseBody, AgentModelTurnRequest request) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode message = root.at("/choices/0/message");
        List<AgentModelToolCall> calls = new ArrayList<>();
        JsonNode toolCalls = message.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray()) {
            for (JsonNode toolCall : toolCalls) {
                JsonNode function = toolCall.get("function");
                calls.add(AgentModelToolCall.builder()
                        .callId(textValue(toolCall.get("id"), "call_" + calls.size()))
                        .toolCode(textValue(function == null ? null : function.get("name"), ""))
                        .arguments(parseArguments(function == null ? null : function.get("arguments")))
                        .build());
            }
        }
        String content = textValue(message.get("content"), "");
        return AgentModelTurnResponse.builder()
                .type(calls.isEmpty() ? "FINAL" : "TOOL_CALLS")
                .content(content)
                .toolCalls(calls)
                .inputTokens(extractInputTokens(responseBody, requestPrompt(request)))
                .outputTokens(extractOutputTokens(responseBody, content))
                .totalTokens(extractTotalTokens(responseBody, requestPrompt(request), content))
                .rawResponse(responseBody)
                .build();
    }

    private AgentModelTurnResponse extractResponsesAgentResponse(String responseBody, AgentModelTurnRequest request) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        List<AgentModelToolCall> calls = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                if ("function_call".equals(textValue(item.get("type"), ""))) {
                    calls.add(AgentModelToolCall.builder()
                            .callId(textValue(item.get("call_id"), textValue(item.get("id"), "call_" + calls.size())))
                            .toolCode(textValue(item.get("name"), ""))
                            .arguments(parseArguments(item.get("arguments")))
                            .build());
                }
                JsonNode content = item.get("content");
                if (content != null && content.isArray()) {
                    for (JsonNode part : content) {
                        String partText = textValue(part.get("text"), "");
                        if (!partText.isBlank()) text.append(partText);
                    }
                }
            }
        }
        String content = root.hasNonNull("output_text") ? root.get("output_text").asText("") : text.toString();
        return AgentModelTurnResponse.builder()
                .type(calls.isEmpty() ? "FINAL" : "TOOL_CALLS")
                .content(content)
                .toolCalls(calls)
                .inputTokens(extractInputTokens(responseBody, requestPrompt(request)))
                .outputTokens(extractOutputTokens(responseBody, content))
                .totalTokens(extractTotalTokens(responseBody, requestPrompt(request), content))
                .rawResponse(responseBody)
                .build();
    }

    private Map<String, Object> parseArguments(JsonNode value) throws IOException {
        if (value == null || value.isNull() || value.asText("").isBlank()) return Map.of();
        JsonNode parsed = value.isTextual() ? objectMapper.readTree(value.asText()) : value;
        if (!parsed.isObject()) throw new IllegalStateException("AI tool arguments must be a JSON object");
        return objectMapper.convertValue(parsed, Map.class);
    }

    private String textValue(JsonNode node, String fallback) {
        return node == null || node.isNull() ? fallback : node.asText(fallback);
    }

    private String requestPrompt(AgentModelTurnRequest request) {
        return request == null || request.getMessages() == null ? "" : request.getMessages().stream()
                .map(AgentModelMessage::getContent).filter(value -> value != null).reduce("", (left, right) -> left + right);
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
