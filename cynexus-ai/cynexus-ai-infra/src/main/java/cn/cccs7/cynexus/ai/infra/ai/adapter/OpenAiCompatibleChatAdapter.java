package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OpenAiCompatibleChatAdapter implements AiModelAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiCompatibleChatAdapter.class);

    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String V1_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String RESPONSES_PATH = "/responses";
    private static final String V1_RESPONSES_PATH = "/v1/responses";
    private static final String PROVIDER_TOOL_PREFIX = "cynexus_tool_";

    private final ObjectMapper objectMapper;
    private final SecretResolver secretResolver;
    private final Environment environment;
    private final HttpClient httpClient;

    protected OpenAiCompatibleChatAdapter(SecretResolver secretResolver,
                                          Environment environment) {
        this.objectMapper = new ObjectMapper();
        this.secretResolver = secretResolver;
        this.environment = environment;
        this.httpClient = buildHttpClient(environment);
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
                throw providerHttpFailure(response.statusCode(), response.body());
            }
            return responsesApi ? extractResponsesAgentResponse(response.body(), request) : extractChatAgentResponse(response.body(), request);
        } catch (IOException exception) {
            logProviderFailure("agent", model, exception);
            throw new IllegalStateException("Failed to call AI provider: " + providerCode(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI provider call interrupted: " + providerCode(), exception);
        }
    }

    @Override
    public AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request,
                                              Consumer<String> textDeltaConsumer) {
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Missing API key for provider: " + providerCode());
        boolean responsesApi = "responses".equalsIgnoreCase(extraString(model, "wire_api"));
        try {
            String payload = responsesApi ? buildAgentResponsesPayload(model, request, true)
                    : buildAgentChatPayload(model, request, true);
            HttpRequest httpRequest = HttpRequest.newBuilder(resolveEndpoint(model, responsesApi))
                    .timeout(resolveRequestTimeout(model)).header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<Stream<String>> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (Stream<String> lines = response.body()) {
                    throw providerHttpFailure(response.statusCode(), lines.reduce("", (left, right) -> left + right));
                }
            }
            return responsesApi ? readResponsesStream(response.body(), request, textDeltaConsumer)
                    : readChatStream(response.body(), request, textDeltaConsumer);
        } catch (IOException exception) {
            logProviderFailure("agent stream", model, exception);
            throw new IllegalStateException("Failed to stream AI provider: " + providerCode(), exception);
        } catch (UncheckedIOException exception) {
            logProviderFailure("agent stream", model, exception);
            throw new IllegalStateException("Failed to stream AI provider: " + providerCode(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI provider stream interrupted: " + providerCode(), exception);
        } catch (RuntimeException exception) {
            logProviderFailure("agent stream", model, exception);
            throw exception;
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
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw providerHttpFailure(response.statusCode(), response.body());
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
        return buildAgentChatPayload(model, request, false);
    }

    private String buildAgentChatPayload(AiModelBO model, AgentModelTurnRequest request, boolean stream) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        payload.put("temperature", 0);
        payload.put("messages", buildChatMessages(request));
        payload.put("tools", buildToolDefinitions(request == null ? List.of() : request.getTools()));
        payload.put("tool_choice", "auto");
        if (stream) payload.put("stream", true);
        return objectMapper.writeValueAsString(payload);
    }

    private String buildAgentResponsesPayload(AiModelBO model, AgentModelTurnRequest request) throws IOException {
        return buildAgentResponsesPayload(model, request, false);
    }

    private String buildAgentResponsesPayload(AiModelBO model, AgentModelTurnRequest request, boolean stream) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        payload.put("input", buildResponsesInput(request));
        payload.put("tools", buildToolDefinitions(request == null ? List.of() : request.getTools()));
        payload.put("store", !extraBoolean(model, "disable_response_storage", true));
        if (stream) payload.put("stream", true);
        return objectMapper.writeValueAsString(payload);
    }

    private AgentModelTurnResponse readChatStream(Stream<String> lines, AgentModelTurnRequest request,
                                                   Consumer<String> textDeltaConsumer) {
        StringBuilder content = new StringBuilder();
        StringBuilder raw = new StringBuilder();
        Map<String, StreamToolAccumulator> tools = new LinkedHashMap<>();
        try (lines) {
            var iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line == null || !line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isBlank() || "[DONE]".equals(data)) continue;
                raw.append(data).append('\n');
                try {
                    JsonNode delta = objectMapper.readTree(data).at("/choices/0/delta");
                    String text = textValue(delta.get("content"), "");
                    if (!text.isBlank()) {
                        content.append(text);
                        if (textDeltaConsumer != null) textDeltaConsumer.accept(text);
                    }
                    JsonNode toolCalls = delta.get("tool_calls");
                    if (toolCalls != null && toolCalls.isArray()) {
                        for (JsonNode item : toolCalls) {
                            String key = textValue(item.get("index"), String.valueOf(tools.size()));
                            StreamToolAccumulator accumulator = tools.computeIfAbsent(key, ignored -> new StreamToolAccumulator());
                            accumulator.id = nonBlank(textValue(item.get("id"), ""), accumulator.id);
                            JsonNode function = item.get("function");
                            if (function != null) {
                                accumulator.name = nonBlank(textValue(function.get("name"), ""), accumulator.name);
                                accumulator.arguments.append(textValue(function.get("arguments"), ""));
                            }
                        }
                    }
                } catch (IOException ignored) {
                    throw new IllegalStateException("AI provider emitted invalid streaming JSON");
                }
            }
        }
        return streamResponse(content.toString(), tools, raw.toString(), request);
    }

    private AgentModelTurnResponse readResponsesStream(Stream<String> lines, AgentModelTurnRequest request,
                                                        Consumer<String> textDeltaConsumer) {
        StringBuilder content = new StringBuilder();
        StringBuilder raw = new StringBuilder();
        Map<String, StreamToolAccumulator> tools = new LinkedHashMap<>();
        try (lines) {
            var iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line == null || !line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isBlank() || "[DONE]".equals(data)) continue;
                raw.append(data).append('\n');
                try {
                    JsonNode root = objectMapper.readTree(data);
                    String type = textValue(root.get("type"), "");
                    if ("response.output_text.delta".equals(type)) {
                        String text = textValue(root.get("delta"), "");
                        content.append(text);
                        if (!text.isBlank() && textDeltaConsumer != null) textDeltaConsumer.accept(text);
                    } else if ("response.output_item.added".equals(type)) {
                        JsonNode item = root.get("item");
                        if (item != null && "function_call".equals(textValue(item.get("type"), ""))) {
                            String key = nonBlank(textValue(item.get("call_id"), ""), textValue(item.get("id"), String.valueOf(tools.size())));
                            StreamToolAccumulator accumulator = tools.computeIfAbsent(key, ignored -> new StreamToolAccumulator());
                            accumulator.id = key;
                            accumulator.name = textValue(item.get("name"), "");
                            accumulator.arguments.append(textValue(item.get("arguments"), ""));
                        }
                    } else if ("response.function_call_arguments.delta".equals(type)) {
                        String key = nonBlank(textValue(root.get("call_id"), ""), textValue(root.get("item_id"), String.valueOf(tools.size())));
                        tools.computeIfAbsent(key, ignored -> new StreamToolAccumulator()).arguments
                                .append(textValue(root.get("delta"), ""));
                    } else if ("response.function_call_arguments.done".equals(type)) {
                        String key = nonBlank(textValue(root.get("call_id"), ""), textValue(root.get("item_id"), String.valueOf(tools.size())));
                        StreamToolAccumulator accumulator = tools.computeIfAbsent(key, ignored -> new StreamToolAccumulator());
                        if (accumulator.arguments.isEmpty()) accumulator.arguments.append(textValue(root.get("arguments"), ""));
                    }
                } catch (IOException ignored) {
                    throw new IllegalStateException("AI provider emitted invalid streaming JSON");
                }
            }
        }
        return streamResponse(content.toString(), tools, raw.toString(), request);
    }

    private AgentModelTurnResponse streamResponse(String content, Map<String, StreamToolAccumulator> tools,
                                                  String raw, AgentModelTurnRequest request) {
        List<AgentModelToolCall> calls = new ArrayList<>();
        for (StreamToolAccumulator accumulator : tools.values()) {
            if (accumulator.name == null || accumulator.name.isBlank()) continue;
            try {
                JsonNode arguments = accumulator.arguments.isEmpty() ? objectMapper.createObjectNode()
                        : objectMapper.readTree(accumulator.arguments.toString());
                if (!arguments.isObject()) throw new IllegalStateException("AI tool arguments must be a JSON object");
                calls.add(AgentModelToolCall.builder().callId(nonBlank(accumulator.id, "stream-call-" + calls.size()))
                        .toolCode(canonicalToolName(accumulator.name)).arguments(objectMapper.convertValue(arguments, Map.class)).build());
            } catch (IOException exception) {
                throw new IllegalStateException("AI provider emitted invalid tool arguments", exception);
            }
        }
        return AgentModelTurnResponse.builder().type(calls.isEmpty() ? "FINAL" : "TOOL_CALLS")
                .content(content).toolCalls(calls).inputTokens(estimateTokens(requestPrompt(request)))
                .outputTokens(estimateTokens(content)).totalTokens(estimateTokens(requestPrompt(request)) + estimateTokens(content))
                .rawResponse(raw).build();
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static final class StreamToolAccumulator {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();
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
            return Map.of("type", "function_call", "call_id", call.getCallId(), "name", providerToolName(call.getToolCode()),
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
            function.put("name", providerToolName(definition.getCode()));
            function.put("description", definition.getDescription());
            function.put("parameters", objectMapper.readTree(definition.getInputSchemaJson()));
            tools.add(Map.of("type", "function", "function", function));
        }
        return tools;
    }

    private Map<String, Object> toChatToolCall(AgentModelToolCall call) {
        try {
            return Map.of("id", call.getCallId(), "type", "function",
                    "function", Map.of("name", providerToolName(call.getToolCode()),
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
                        .toolCode(canonicalToolName(textValue(function == null ? null : function.get("name"), "")))
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
                            .toolCode(canonicalToolName(textValue(item.get("name"), "")))
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

    private String providerToolName(String canonicalToolCode) {
        String value = canonicalToolCode == null ? "" : canonicalToolCode;
        StringBuilder encoded = new StringBuilder(PROVIDER_TOOL_PREFIX);
        for (byte item : value.getBytes(StandardCharsets.UTF_8)) {
            encoded.append(String.format("%02x", item & 0xff));
        }
        return encoded.toString();
    }

    private String canonicalToolName(String providerToolName) {
        if (providerToolName == null || !providerToolName.startsWith(PROVIDER_TOOL_PREFIX)) return providerToolName;
        String hex = providerToolName.substring(PROVIDER_TOOL_PREFIX.length());
        if (hex.isBlank() || (hex.length() % 2) != 0 || !hex.matches("[0-9a-fA-F]+")) return providerToolName;
        try {
            byte[] decoded = new byte[hex.length() / 2];
            for (int index = 0; index < hex.length(); index += 2) {
                decoded[index / 2] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
            }
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            return providerToolName;
        }
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

    static HttpClient buildHttpClient(Environment environment) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
        String proxyValue = environment.getProperty("HTTPS_PROXY");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = environment.getProperty("HTTP_PROXY");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = System.getenv("HTTPS_PROXY");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = System.getenv("HTTP_PROXY");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = System.getenv("https_proxy");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = System.getenv("http_proxy");
        if (proxyValue == null || proxyValue.isBlank()) return builder.build();
        try {
            URI proxy = URI.create(proxyValue.trim());
            if (proxy.getHost() == null || proxy.getPort() <= 0) return builder.build();
            LOGGER.info("AI provider outbound proxy configured at {}:{} using HTTP/1.1", proxy.getHost(), proxy.getPort());
            return builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())))
                    // Keep the proxy tunnel on HTTP/1.1. The configured proxy path is
                    // validated by curl with HTTP/1.1, while Java HTTP/2 proxy negotiation
                    // can fail before the provider's TLS handshake completes.
                    .version(HttpClient.Version.HTTP_1_1).build();
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Ignoring invalid AI provider proxy configuration");
            return builder.build();
        }
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

    private IllegalStateException providerHttpFailure(int statusCode, String responseBody) {
        String detail = responseBody == null ? "" : responseBody.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (detail.length() > 240) detail = detail.substring(0, 240);
        LOGGER.warn("AI provider {} returned HTTP {} with safe detail: {}", providerCode(), statusCode, detail);
        return new IllegalStateException("AI provider returned HTTP " + statusCode);
    }

    private void logProviderFailure(String operation, AiModelBO model, Exception exception) {
        LOGGER.warn("AI provider {} {} failed for model {} at {}: {}", providerCode(), operation,
                resolveModelName(model), safeEndpoint(model), exception.getClass().getSimpleName(), exception);
    }

    private String safeEndpoint(AiModelBO model) {
        try {
            return resolveEndpoint(model, "responses".equalsIgnoreCase(extraString(model, "wire_api"))).toString();
        } catch (RuntimeException ignored) {
            return "unresolved";
        }
    }
}
