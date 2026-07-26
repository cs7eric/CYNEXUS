package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheTelemetry;
import cn.cccs7.cynexus.ai.infra.ai.cache.PromptCacheStrategy;
import cn.cccs7.cynexus.ai.infra.ai.cache.PromptCacheStrategyRegistry;
import cn.cccs7.cynexus.ai.infra.ai.cache.PromptCacheUsage;
import cn.cccs7.cynexus.ai.infra.ai.cache.PromptCacheUsageParser;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import cn.cccs7.cynexus.ai.domain.policy.security.AgentOutboundEndpointPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLHandshakeException;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OpenAiCompatibleChatAdapter implements AiModelAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiCompatibleChatAdapter.class);

    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final int STREAM_TRANSPORT_ATTEMPTS = 2;
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String V1_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String RESPONSES_PATH = "/responses";
    private static final String V1_RESPONSES_PATH = "/v1/responses";
    private static final String DEFAULT_ALLOWED_PROVIDER_ORIGINS =
            "https://api.openai.com,https://api.deepseek.com,https://am.xtyooo.top,https://dashscope.aliyuncs.com";

    private final ObjectMapper objectMapper;
    private final SecretResolver secretResolver;
    private final Environment environment;
    private final HttpClient httpClient;
    private final CloseableHttpClient streamingHttpClient;
    private final PromptCacheStrategyRegistry promptCacheStrategyRegistry = PromptCacheStrategyRegistry.defaultRegistry();
    private final ConcurrentMap<String, ResponsesWebSocketSession> responsesWebSocketSessions = new ConcurrentHashMap<>();

    protected OpenAiCompatibleChatAdapter(SecretResolver secretResolver,
                                          Environment environment) {
        this.objectMapper = new ObjectMapper();
        this.secretResolver = secretResolver;
        this.environment = environment;
        this.httpClient = buildHttpClient(environment);
        this.streamingHttpClient = buildStreamingHttpClient(environment);
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
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
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
        if (responsesApi(model) && responsesWebSocketEnabled(model)) {
            return completeAgentResponsesWebSocket(model, request);
        }
        return completeAgentHttp(model, request, false, false);
    }

    private AgentModelTurnResponse completeAgentHttp(AiModelBO model, AgentModelTurnRequest request,
                                                     boolean forceStateless, boolean stateFallback) {
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing API key for provider: " + providerCode());
        }
        try {
            boolean responsesApi = "responses".equalsIgnoreCase(extraString(model, "wire_api"));
            PromptCacheStrategy cacheStrategy = promptCacheStrategyRegistry.resolve(model);
            boolean stateful = responsesApi && shouldUseResponsesState(model, request, forceStateless);
            String payload = responsesApi ? buildAgentResponsesPayload(model, request, false, cacheStrategy, forceStateless)
                    : buildAgentChatPayload(model, request, cacheStrategy);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(resolveEndpoint(model, responsesApi))
                    .timeout(resolveRequestTimeout(model))
                    .header("Authorization", "Bearer " + apiKey)
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON);
            cacheStrategy.headers(model, promptCacheContext(request)).forEach(requestBuilder::header);
            HttpRequest httpRequest = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                ProviderHttpException failure = providerHttpFailure(response.statusCode(), response.body());
                if (stateful && isResponsesStateFallbackCandidate(failure)) {
                    LOGGER.warn("Retrying Responses request with full transcript after state continuation rejection");
                    return completeAgentHttp(model, request, true, true);
                }
                throw failure;
            }
            AgentModelTurnResponse result = responsesApi
                    ? extractResponsesAgentResponse(response.body(), request, cacheStrategy)
                    : extractChatAgentResponse(response.body(), request, cacheStrategy);
            return attachCacheTelemetry(result, payload, request, stateful, stateFallback,
                    responsesApi && responsesStateEnabled(model));
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
                                              Consumer<String> textDeltaConsumer,
                                              Consumer<String> thinkingDeltaConsumer) {
        if (responsesApi(model) && responsesWebSocketEnabled(model)) {
            return streamAgentResponsesWebSocket(model, request, textDeltaConsumer, thinkingDeltaConsumer);
        }
        return streamAgentHttp(model, request, textDeltaConsumer, thinkingDeltaConsumer, false, false);
    }

    private AgentModelTurnResponse streamAgentHttp(AiModelBO model, AgentModelTurnRequest request,
                                                   Consumer<String> textDeltaConsumer,
                                                   Consumer<String> thinkingDeltaConsumer,
                                                   boolean forceStateless, boolean stateFallback) {
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Missing API key for provider: " + providerCode());
        boolean responsesApi = responsesApi(model);
        String payload;
        PromptCacheStrategy cacheStrategy = promptCacheStrategyRegistry.resolve(model);
        boolean stateful = responsesApi && shouldUseResponsesState(model, request, false);
        try {
            payload = responsesApi ? buildAgentResponsesPayload(model, request, true, cacheStrategy, forceStateless)
                    : buildAgentChatPayload(model, request, true, cacheStrategy);
        } catch (IOException exception) {
            logProviderFailure("agent stream", model, exception);
            throw new IllegalStateException("Failed to prepare AI provider stream: " + providerCode(), exception);
        }
        IOException lastTransportFailure = null;
        for (int attempt = 1; attempt <= STREAM_TRANSPORT_ATTEMPTS; attempt++) {
            try {
                AgentModelTurnResponse result = streamWithApacheHttpClient(model, resolveEndpoint(model, responsesApi), apiKey, payload,
                        resolveRequestTimeout(model), responsesApi, request, cacheStrategy,
                        textDeltaConsumer, thinkingDeltaConsumer);
                return attachCacheTelemetry(result, payload, request, stateful, stateFallback,
                        responsesApi && responsesStateEnabled(model));
            } catch (IOException exception) {
                if (isInterruptedFailure(exception)) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("AI provider stream interrupted: " + providerCode(), exception);
                }
                lastTransportFailure = exception;
                if (attempt < STREAM_TRANSPORT_ATTEMPTS && isTransientStreamTransportFailure(exception)) {
                    LOGGER.warn("Retrying AI provider stream after transport failure (attempt {}/{}): {}",
                            attempt + 1, STREAM_TRANSPORT_ATTEMPTS, rootMessage(exception));
                    continue;
                }
                break;
            } catch (UncheckedIOException exception) {
                if (isInterruptedFailure(exception)) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("AI provider stream interrupted: " + providerCode(), exception);
                }
                lastTransportFailure = exception.getCause() instanceof IOException io
                        ? io
                        : new IOException(exception);
                break;
            } catch (RuntimeException exception) {
                if (stateful && exception instanceof ProviderHttpException failure
                        && isResponsesStateFallbackCandidate(failure)) {
                    LOGGER.warn("Retrying streamed Responses request with full transcript after state continuation rejection");
                    return completeAgentHttp(model, request, true, true);
                }
                logProviderFailure("agent stream", model, exception);
                throw exception;
            }
        }
        if (lastTransportFailure != null && isStreamFallbackCandidate(lastTransportFailure)) {
            LOGGER.warn("Falling back to non-stream agent completion after stream failure for model {} at {}: {}",
                    model.getModelCode(), resolveEndpoint(model, responsesApi), rootMessage(lastTransportFailure));
            try {
                return completeAgentHttp(model, request, forceStateless, stateFallback);
            } catch (RuntimeException fallbackException) {
                logProviderFailure("agent stream fallback", model, fallbackException);
                throw new IllegalStateException(
                        "Failed to stream AI provider: " + providerCode() + ": " + rootMessage(lastTransportFailure),
                        lastTransportFailure);
            }
        }
        if (lastTransportFailure != null) {
            logProviderFailure("agent stream", model, lastTransportFailure);
            throw new IllegalStateException(
                    "Failed to stream AI provider: " + providerCode() + ": " + rootMessage(lastTransportFailure),
                    lastTransportFailure);
        }
        throw new IllegalStateException("Failed to stream AI provider: " + providerCode());
    }


    static boolean isTransientStreamTransportFailure(Throwable failure) {
        if (isTlsHandshakeFailure(failure)) return true;
        Throwable current = failure;
        while (current != null) {
            String name = current.getClass().getSimpleName();
            String message = current.getMessage() == null ? "" : current.getMessage();
            if ("NoHttpResponseException".equals(name)) return true;
            if (message.contains("HTTP/1.1 header parser received no bytes")) return true;
            if (message.contains("Connection reset")) return true;
            if (message.contains("Broken pipe")) return true;
            current = current.getCause();
        }
        return false;
    }

    static boolean isStreamFallbackCandidate(Throwable failure) {
        if (isTransientStreamTransportFailure(failure)) return true;
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SocketTimeoutException) return true;
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (message.contains("read timed out") || message.contains("timeout")
                    || message.contains("unexpected end of stream")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String rootMessage(Throwable failure) {
        String message = failure == null ? "unknown error" : String.valueOf(failure);
        Throwable current = failure;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getClass().getSimpleName() + ": " + current.getMessage();
            }
            current = current.getCause();
        }
        return message;
    }

    static boolean isTlsHandshakeFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SSLHandshakeException) return true;
            current = current.getCause();
        }
        return false;
    }

    static boolean isInterruptedFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof InterruptedIOException && !(current instanceof SocketTimeoutException)) return true;
            current = current.getCause();
        }
        return false;
    }

    @Override
    public AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images) {
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Missing API key for provider: " + providerCode());
        try {
            String payload = buildChatPayload(model, prompt, shouldRequestJsonOutput(model, prompt), images);
            HttpRequest request = HttpRequest.newBuilder(resolveEndpoint(model, false))
                    .timeout(resolveRequestTimeout(model)).header("Authorization", "Bearer " + apiKey)
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON).POST(HttpRequest.BodyPublishers.ofString(payload)).build();
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
        String reasoningEffort = extraString(model, AiConstants.MODEL_REASONING_EFFORT_EXTRA);
        if (!reasoningEffort.isBlank()) {
            payload.put(AiConstants.REASONING_EFFORT_REQUEST_FIELD, reasoningEffort);
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String buildResponsesPayload(AiModelBO model, String prompt, boolean jsonOutput) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        payload.put("input", prompt == null ? "" : prompt);
        payload.put("store", !extraBoolean(model, "disable_response_storage", true));
        String reasoningEffort = extraString(model, AiConstants.MODEL_REASONING_EFFORT_EXTRA);
        if (!reasoningEffort.isBlank()) {
            payload.put("reasoning", Map.of("effort", reasoningEffort));
        }
        if (jsonOutput) {
            payload.put("text", Map.of("format", Map.of("type", "json_object")));
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String buildAgentChatPayload(AiModelBO model, AgentModelTurnRequest request) throws IOException {
        return buildAgentChatPayload(model, request, false, promptCacheStrategyRegistry.resolve(model));
    }

    private String buildAgentChatPayload(AiModelBO model, AgentModelTurnRequest request, boolean stream) throws IOException {
        return buildAgentChatPayload(model, request, stream, promptCacheStrategyRegistry.resolve(model));
    }

    private String buildAgentChatPayload(AiModelBO model, AgentModelTurnRequest request, PromptCacheStrategy cacheStrategy) throws IOException {
        return buildAgentChatPayload(model, request, false, cacheStrategy);
    }

    private String buildAgentChatPayload(AiModelBO model, AgentModelTurnRequest request,
                                         boolean stream, PromptCacheStrategy cacheStrategy) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        payload.put("temperature", 0);
        payload.put("messages", buildChatMessages(request));
        payload.put("tools", buildToolDefinitions(request == null ? List.of() : request.getTools(), false));
        payload.put(AiConstants.TOOL_CHOICE_FIELD,
                request == null || request.isToolsAllowed()
                        ? AiConstants.TOOL_CHOICE_AUTO : AiConstants.TOOL_CHOICE_NONE);
        cacheStrategy.enrichPayload(model, promptCacheContext(request), payload, false);
        String reasoningEffort = extraString(model, AiConstants.MODEL_REASONING_EFFORT_EXTRA);
        if (!reasoningEffort.isBlank()) {
            payload.put(AiConstants.REASONING_EFFORT_REQUEST_FIELD, reasoningEffort);
        }
        if (stream) {
            payload.put("stream", true);
            payload.put("stream_options", Map.of("include_usage", true));
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String buildAgentResponsesPayload(AiModelBO model, AgentModelTurnRequest request) throws IOException {
        return buildAgentResponsesPayload(model, request, false, promptCacheStrategyRegistry.resolve(model));
    }

    private String buildAgentResponsesPayload(AiModelBO model, AgentModelTurnRequest request, boolean stream) throws IOException {
        return buildAgentResponsesPayload(model, request, stream, promptCacheStrategyRegistry.resolve(model));
    }

    private String buildAgentResponsesPayload(AiModelBO model, AgentModelTurnRequest request,
                                              PromptCacheStrategy cacheStrategy) throws IOException {
        return buildAgentResponsesPayload(model, request, false, cacheStrategy);
    }

    private String buildAgentResponsesPayload(AiModelBO model, AgentModelTurnRequest request,
                                              boolean stream, PromptCacheStrategy cacheStrategy) throws IOException {
        return buildAgentResponsesPayload(model, request, stream, cacheStrategy, false);
    }

    private String buildAgentResponsesPayload(AiModelBO model, AgentModelTurnRequest request,
                                              boolean stream, PromptCacheStrategy cacheStrategy,
                                              boolean forceStateless) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModelName(model));
        boolean stateEnabled = responsesStateEnabled(model);
        boolean stateful = shouldUseResponsesState(model, request, forceStateless);
        payload.put(AiConstants.RESPONSES_INPUT_FIELD,
                stateful ? buildResponsesContinuationInput(request) : buildResponsesInput(request));
        if (stateful) {
            payload.put(AiConstants.RESPONSES_PREVIOUS_RESPONSE_ID_FIELD, request.getPreviousResponseId());
        }
        payload.put("tools", buildToolDefinitions(request == null ? List.of() : request.getTools(), true));
        payload.put(AiConstants.TOOL_CHOICE_FIELD,
                request == null || request.isToolsAllowed()
                        ? AiConstants.TOOL_CHOICE_AUTO : AiConstants.TOOL_CHOICE_NONE);
        payload.put(AiConstants.RESPONSES_STORE_FIELD, stateEnabled);
        String cacheRetention = extraString(model, AiConstants.PROMPT_CACHE_RETENTION_EXTRA);
        if (!cacheRetention.isBlank()) {
            payload.put(AiConstants.PROMPT_CACHE_RETENTION_FIELD, cacheRetention);
        }
        cacheStrategy.enrichPayload(model, promptCacheContext(request), payload, true);
        if (stream) payload.put("stream", true);
        Map<String, Object> reasoning = new LinkedHashMap<>();
        String effort = extraString(model, AiConstants.MODEL_REASONING_EFFORT_EXTRA);
        if (!effort.isBlank()) reasoning.put("effort", effort);
        String summary = extraString(model, AiConstants.REASONING_SUMMARY_EXTRA);
        reasoning.put("summary", summary.isBlank() ? "auto" : summary);
        payload.put("reasoning", reasoning);
        return objectMapper.writeValueAsString(payload);
    }

    private AgentModelTurnResponse completeAgentResponsesWebSocket(AiModelBO model,
                                                                    AgentModelTurnRequest request) {
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing API key for provider: " + providerCode());
        }
        PromptCacheStrategy cacheStrategy = promptCacheStrategyRegistry.resolve(model);
        boolean stateful = shouldUseResponsesState(model, request, false);
        String payload;
        try {
            payload = buildAgentResponsesPayload(model, request, false, cacheStrategy, false);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare AI provider Responses WebSocket request: "
                    + providerCode(), exception);
        }
        String sessionKey = responsesWebSocketSessionKey(model, request);
        ResponsesWebSocketSession session = responsesWebSocketSession(model, request, apiKey, cacheStrategy, sessionKey);
        try {
            AgentModelTurnResponse result = session.execute(payload, request, null, null);
            return attachCacheTelemetry(result, payload, request, stateful, false,
                    responsesStateEnabled(model));
        } catch (RuntimeException exception) {
            discardResponsesWebSocketSession(sessionKey, session);
            LOGGER.warn("Falling back to HTTP Responses after WebSocket failure for provider {}: {}",
                    providerCode(), rootMessage(exception));
            return completeAgentHttp(model, request, true, stateful);
        } finally {
            if (sessionKey == null) session.close();
        }
    }

    private AgentModelTurnResponse streamAgentResponsesWebSocket(AiModelBO model,
                                                                  AgentModelTurnRequest request,
                                                                  Consumer<String> textDeltaConsumer,
                                                                  Consumer<String> thinkingDeltaConsumer) {
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing API key for provider: " + providerCode());
        }
        PromptCacheStrategy cacheStrategy = promptCacheStrategyRegistry.resolve(model);
        boolean stateful = shouldUseResponsesState(model, request, false);
        String payload;
        try {
            payload = buildAgentResponsesPayload(model, request, false, cacheStrategy, false);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare AI provider Responses WebSocket request: "
                    + providerCode(), exception);
        }
        AtomicBoolean emitted = new AtomicBoolean();
        Consumer<String> textConsumer = delta -> {
            emitted.set(true);
            if (textDeltaConsumer != null) textDeltaConsumer.accept(delta);
        };
        Consumer<String> thinkingConsumer = delta -> {
            emitted.set(true);
            if (thinkingDeltaConsumer != null) thinkingDeltaConsumer.accept(delta);
        };
        String sessionKey = responsesWebSocketSessionKey(model, request);
        ResponsesWebSocketSession session = responsesWebSocketSession(model, request, apiKey, cacheStrategy, sessionKey);
        try {
            AgentModelTurnResponse result = session.execute(payload, request, textConsumer, thinkingConsumer);
            return attachCacheTelemetry(result, payload, request, stateful, false,
                    responsesStateEnabled(model));
        } catch (RuntimeException exception) {
            discardResponsesWebSocketSession(sessionKey, session);
            if (emitted.get()) throw exception;
            LOGGER.warn("Falling back to HTTP Responses stream after WebSocket failure for provider {}: {}",
                    providerCode(), rootMessage(exception));
            return streamAgentHttp(model, request, textDeltaConsumer, thinkingDeltaConsumer,
                    true, stateful);
        } finally {
            if (sessionKey == null) session.close();
        }
    }

    private ResponsesWebSocketSession responsesWebSocketSession(AiModelBO model,
                                                                 AgentModelTurnRequest request,
                                                                 String apiKey,
                                                                 PromptCacheStrategy cacheStrategy,
                                                                 String sessionKey) {
        URI endpoint = responsesWebSocketEndpoint(model);
        Map<String, String> headers = cacheStrategy.headers(model, promptCacheContext(request));
        if (sessionKey == null) {
            return ResponsesWebSocketSession.connect(httpClient, endpoint, apiKey, headers,
                    resolveRequestTimeout(model), objectMapper);
        }
        return responsesWebSocketSessions.computeIfAbsent(sessionKey, ignored ->
                ResponsesWebSocketSession.connect(httpClient, endpoint, apiKey, headers,
                        resolveRequestTimeout(model), objectMapper));
    }

    private void discardResponsesWebSocketSession(String sessionKey, ResponsesWebSocketSession session) {
        if (sessionKey == null) {
            session.close();
            return;
        }
        if (responsesWebSocketSessions.remove(sessionKey, session)) session.close();
    }

    private String responsesWebSocketSessionKey(AiModelBO model, AgentModelTurnRequest request) {
        PromptCacheContext context = promptCacheContext(request);
        String conversationKey = context == null ? "" : context.getConversationKey();
        if (conversationKey == null || conversationKey.isBlank()) return null;
        return providerCode() + "|" + valueOr(resolveModelName(model), "unknown") + "|" + conversationKey;
    }

    private URI responsesWebSocketEndpoint(AiModelBO model) {
        URI endpoint = resolveEndpoint(model, true);
        String websocketScheme = "https".equalsIgnoreCase(endpoint.getScheme()) ? "wss" : "ws";
        return URI.create(websocketScheme + "://" + endpoint.getRawAuthority()
                + (endpoint.getRawPath() == null ? "" : endpoint.getRawPath())
                + (endpoint.getRawQuery() == null ? "" : "?" + endpoint.getRawQuery()));
    }

    private AgentModelTurnResponse readChatStream(Stream<String> lines, AgentModelTurnRequest request,
                                                   Consumer<String> textDeltaConsumer,
                                                   Consumer<String> thinkingDeltaConsumer) {
        StringBuilder content = new StringBuilder();
        StringBuilder raw = new StringBuilder();
        Map<String, StreamToolAccumulator> tools = new LinkedHashMap<>();
        StreamUsageAccumulator usage = new StreamUsageAccumulator();
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
                    captureStreamUsage(usage, root.get(AiConstants.AI_USAGE_FIELD));
                    JsonNode delta = root.at("/choices/0/delta");
                    String text = textValue(delta.get("content"), "");
                    if (!text.isBlank()) {
                        content.append(text);
                        if (textDeltaConsumer != null) textDeltaConsumer.accept(text);
                    }
                    String reasoning = textValue(delta.get("reasoning_content"), "");
                    if (reasoning.isBlank()) reasoning = textValue(delta.get("reasoning"), "");
                    if (!reasoning.isBlank() && thinkingDeltaConsumer != null) {
                        thinkingDeltaConsumer.accept(reasoning);
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
        return streamResponse(content.toString(), tools, raw.toString(), usage, request);
    }

    private AgentModelTurnResponse readResponsesStream(Stream<String> lines, AgentModelTurnRequest request,
                                                        Consumer<String> textDeltaConsumer,
                                                        Consumer<String> thinkingDeltaConsumer) {
        StringBuilder content = new StringBuilder();
        StringBuilder raw = new StringBuilder();
        Map<String, StreamToolAccumulator> tools = new LinkedHashMap<>();
        StreamUsageAccumulator usage = new StreamUsageAccumulator();
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
                    JsonNode usageNode = root.get(AiConstants.AI_USAGE_FIELD);
                    if (usageNode == null && AiConstants.RESPONSES_COMPLETED_EVENT.equals(type)) {
                        JsonNode response = root.get("response");
                        usageNode = response == null ? null : response.get(AiConstants.AI_USAGE_FIELD);
                        usage.responseId = response == null ? usage.responseId
                                : nonBlank(textValue(response.get(AiConstants.RESPONSES_RESPONSE_ID_FIELD), ""),
                                usage.responseId);
                    }
                    captureStreamUsage(usage, usageNode);
                    if ("response.output_text.delta".equals(type)) {
                        String text = textValue(root.get("delta"), "");
                        content.append(text);
                        if (!text.isBlank() && textDeltaConsumer != null) textDeltaConsumer.accept(text);
                    } else if ("response.reasoning_summary_text.delta".equals(type)
                            || "response.reasoning_text.delta".equals(type)
                            || "response.reasoning.delta".equals(type)) {
                        String thinking = textValue(root.get("delta"), "");
                        if (thinking.isBlank()) thinking = textValue(root.at("/part/text"), "");
                        if (!thinking.isBlank() && thinkingDeltaConsumer != null) {
                            thinkingDeltaConsumer.accept(thinking);
                        }
                    } else if ("response.reasoning_summary_part.added".equals(type)
                            || "response.reasoning_summary_part.done".equals(type)) {
                        String thinking = textValue(root.at("/part/text"), "");
                        if (thinking.isBlank()) thinking = textValue(root.get("delta"), "");
                        if (!thinking.isBlank() && thinkingDeltaConsumer != null) {
                            thinkingDeltaConsumer.accept(thinking);
                        }
                    } else if ("response.output_item.added".equals(type)) {
                        JsonNode item = root.get("item");
                        if (item != null && AiConstants.RESPONSES_FUNCTION_CALL_TYPE.equals(
                                textValue(item.get(AiConstants.RESPONSES_TYPE_FIELD), ""))) {
                            String itemId = textValue(item.get("id"), "");
                            String callId = textValue(item.get("call_id"), "");
                            String key = nonBlank(itemId, nonBlank(callId, String.valueOf(tools.size())));
                            StreamToolAccumulator accumulator = tools.computeIfAbsent(key, ignored -> new StreamToolAccumulator());
                            accumulator.itemId = nonBlank(itemId, accumulator.itemId);
                            accumulator.id = nonBlank(callId, nonBlank(itemId, accumulator.id));
                            accumulator.name = nonBlank(textValue(item.get("name"), ""), accumulator.name);
                            accumulator.arguments.append(textValue(item.get("arguments"), ""));
                        }
                    } else if ("response.function_call_arguments.delta".equals(type)) {
                        StreamToolAccumulator accumulator = findStreamTool(tools,
                                textValue(root.get("item_id"), ""), textValue(root.get("call_id"), ""));
                        accumulator.arguments
                                .append(textValue(root.get("delta"), ""));
                    } else if ("response.function_call_arguments.done".equals(type)) {
                        StreamToolAccumulator accumulator = findStreamTool(tools,
                                textValue(root.get("item_id"), ""), textValue(root.get("call_id"), ""));
                        if (accumulator.arguments.isEmpty()) accumulator.arguments.append(textValue(root.get("arguments"), ""));
                    }
                } catch (IOException ignored) {
                    throw new IllegalStateException("AI provider emitted invalid streaming JSON");
                }
            }
        }
        return streamResponse(content.toString(), tools, raw.toString(), usage, request);
    }

    private StreamToolAccumulator findStreamTool(Map<String, StreamToolAccumulator> tools,
                                                  String itemId, String callId) {
        String itemKey = itemId == null ? "" : itemId;
        String callKey = callId == null ? "" : callId;
        StreamToolAccumulator accumulator = tools.get(itemKey);
        if (accumulator == null) accumulator = tools.get(callKey);
        if (accumulator == null) {
            accumulator = tools.values().stream()
                    .filter(candidate -> (!itemKey.isBlank() && itemKey.equals(candidate.itemId))
                            || (!callKey.isBlank() && callKey.equals(candidate.id)))
                    .findFirst().orElse(null);
        }
        if (accumulator != null) {
            return accumulator;
        }
        String key = nonBlank(itemKey, nonBlank(callKey, String.valueOf(tools.size())));
        accumulator = tools.computeIfAbsent(key, ignored -> new StreamToolAccumulator());
        accumulator.itemId = nonBlank(itemKey, accumulator.itemId);
        accumulator.id = nonBlank(callKey, accumulator.id);
        return accumulator;
    }

    private AgentModelTurnResponse streamResponse(String content, Map<String, StreamToolAccumulator> tools,
                                                  String raw, StreamUsageAccumulator usage,
                                                  AgentModelTurnRequest request) {
        List<AgentModelToolCall> calls = new ArrayList<>();
        for (StreamToolAccumulator accumulator : tools.values()) {
            if (accumulator.name == null || accumulator.name.isBlank()) continue;
            try {
                JsonNode arguments = accumulator.arguments.isEmpty() ? objectMapper.createObjectNode()
                        : objectMapper.readTree(accumulator.arguments.toString());
                if (!arguments.isObject()) throw new IllegalStateException("AI tool arguments must be a JSON object");
                String callId = nonBlank(accumulator.id, "stream-call-" + calls.size());
                calls.add(AgentModelToolCall.builder().itemId(accumulator.itemId).callId(callId)
                        .toolCode(canonicalToolName(accumulator.name)).arguments(objectMapper.convertValue(arguments, Map.class)).build());
            } catch (IOException exception) {
                throw new IllegalStateException("AI provider emitted invalid tool arguments", exception);
            }
        }
        int estimatedInputTokens = estimateTokens(requestPrompt(request));
        int estimatedOutputTokens = estimateTokens(content);
        int inputTokens = usage.inputTokens == null ? estimatedInputTokens : usage.inputTokens;
        int outputTokens = usage.outputTokens == null ? estimatedOutputTokens : usage.outputTokens;
        int totalTokens = usage.totalTokens == null ? inputTokens + outputTokens : usage.totalTokens;
        return AgentModelTurnResponse.builder().type(calls.isEmpty() ? "FINAL" : "TOOL_CALLS")
                .content(content).toolCalls(calls).inputTokens(inputTokens)
                .cachedInputTokens(usage.cachedInputTokens).cacheWriteTokens(usage.cacheWriteTokens)
                .outputTokens(outputTokens).totalTokens(totalTokens)
                .providerResponseId(usage.responseId)
                .rawResponse(formatStreamingRawResponse(raw, usage)).build();
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static final class StreamToolAccumulator {
        private String itemId;
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();
    }

    private static final class StreamUsageAccumulator {
        private Integer inputTokens;
        private Integer cachedInputTokens;
        private Integer cacheWriteTokens;
        private Integer outputTokens;
        private Integer totalTokens;
        private String responseId;

        private boolean hasAny() {
            return inputTokens != null || cachedInputTokens != null || cacheWriteTokens != null
                    || outputTokens != null || totalTokens != null;
        }
    }

    private void captureStreamUsage(StreamUsageAccumulator usage, JsonNode usageNode) {
        if (usageNode == null || !usageNode.isObject()) return;
        Integer input = firstInt(usageNode, AiConstants.AI_USAGE_INPUT_TOKENS_FIELD,
                AiConstants.AI_USAGE_PROMPT_TOKENS_FIELD);
        Integer output = firstInt(usageNode, AiConstants.AI_USAGE_OUTPUT_TOKENS_FIELD,
                AiConstants.AI_USAGE_COMPLETION_TOKENS_FIELD);
        Integer total = firstInt(usageNode, AiConstants.AI_USAGE_TOTAL_TOKENS_FIELD);
        PromptCacheUsage cacheUsage = PromptCacheUsageParser.parse(usageNode);
        Integer cached = cacheUsage.getCachedInputTokens();
        Integer writes = cacheUsage.getCacheWriteTokens();
        if (input != null) usage.inputTokens = input;
        if (cached != null) usage.cachedInputTokens = cached;
        if (writes != null) usage.cacheWriteTokens = writes;
        if (output != null) usage.outputTokens = output;
        if (total != null) usage.totalTokens = total;
    }

    private Integer firstInt(JsonNode node, String... fields) {
        if (node == null) return null;
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.canConvertToInt()) return value.asInt();
        }
        return null;
    }

    private String formatStreamingRawResponse(String raw, StreamUsageAccumulator usage) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("events", raw == null ? "" : raw);
        if (usage != null && usage.hasAny()) {
            ObjectNode usageNode = envelope.putObject(AiConstants.AI_USAGE_FIELD);
            if (usage.inputTokens != null) usageNode.put(AiConstants.AI_USAGE_INPUT_TOKENS_FIELD, usage.inputTokens);
            if (usage.cachedInputTokens != null) usageNode.put(AiConstants.PROMPT_CACHE_USAGE_CACHED_INPUT_TOKENS_FIELD, usage.cachedInputTokens);
            if (usage.cacheWriteTokens != null) usageNode.put(AiConstants.PROMPT_CACHE_USAGE_WRITE_TOKENS_FIELD, usage.cacheWriteTokens);
            if (usage.outputTokens != null) usageNode.put(AiConstants.AI_USAGE_OUTPUT_TOKENS_FIELD, usage.outputTokens);
            if (usage.totalTokens != null) usageNode.put(AiConstants.AI_USAGE_TOTAL_TOKENS_FIELD, usage.totalTokens);
        }
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            return raw == null ? "" : raw;
        }
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
        appendRuntimeContext(messages, request);
        return messages;
    }

    private List<Map<String, Object>> buildResponsesInput(AgentModelTurnRequest request) {
        List<Map<String, Object>> input = new ArrayList<>();
        if (request != null && request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            input.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        appendResponsesMessages(input, request == null ? null : request.getMessages());
        appendRuntimeContext(input, request);
        return input;
    }

    private List<Map<String, Object>> buildResponsesContinuationInput(AgentModelTurnRequest request) {
        List<Map<String, Object>> input = new ArrayList<>();
        appendResponsesMessages(input, request == null ? null : request.getContinuationMessages());
        appendRuntimeContext(input, request);
        return input;
    }

    private void appendResponsesMessages(List<Map<String, Object>> input, List<AgentModelMessage> messages) {
        if (messages == null) return;
        for (AgentModelMessage message : messages) {
            if ("tool".equals(message.getRole())) {
                input.add(Map.of(AiConstants.RESPONSES_TYPE_FIELD,
                        AiConstants.RESPONSES_FUNCTION_CALL_OUTPUT_TYPE,
                        AiConstants.RESPONSES_FUNCTION_CALL_ID_FIELD, message.getToolCallId(),
                        AiConstants.RESPONSES_FUNCTION_CALL_OUTPUT_FIELD,
                        message.getContent() == null ? "" : message.getContent()));
            } else if ("assistant".equals(message.getRole()) && message.getToolCalls() != null
                    && !message.getToolCalls().isEmpty()) {
                for (AgentModelToolCall call : message.getToolCalls()) {
                    input.add(toResponsesFunctionCall(call));
                }
            } else {
                input.add(Map.of("role", message.getRole(),
                        "content", message.getContent() == null ? "" : message.getContent()));
            }
        }
    }

    private void appendRuntimeContext(List<Map<String, Object>> input, AgentModelTurnRequest request) {
        if (request != null && request.getRuntimeContext() != null && !request.getRuntimeContext().isBlank()) {
            input.add(Map.of("role", "system", "content", request.getRuntimeContext()));
        }
    }

    private Map<String, Object> toResponsesFunctionCall(AgentModelToolCall call) {
        try {
            String itemId = nonBlank(call.getItemId(), call.getCallId());
            return Map.of(AiConstants.RESPONSES_TYPE_FIELD, AiConstants.RESPONSES_FUNCTION_CALL_TYPE,
                    AiConstants.RESPONSES_FUNCTION_CALL_ITEM_ID_FIELD, itemId,
                    AiConstants.RESPONSES_FUNCTION_CALL_ID_FIELD, call.getCallId(),
                    AiConstants.RESPONSES_FUNCTION_CALL_NAME_FIELD, providerToolName(call.getToolCode()),
                    AiConstants.RESPONSES_FUNCTION_CALL_ARGUMENTS_FIELD,
                    objectMapper.writeValueAsString(call.getArguments() == null ? Map.of() : call.getArguments()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Agent tool arguments", exception);
        }
    }

    private List<Map<String, Object>> buildToolDefinitions(List<AgentToolDefinition> definitions,
                                                            boolean responsesApi) throws IOException {
        List<Map<String, Object>> tools = new ArrayList<>();
        if (definitions == null) return tools;
        List<AgentToolDefinition> orderedDefinitions = definitions.stream()
                .filter(definition -> definition != null)
                .sorted(Comparator.comparing(definition -> definition.getCode() == null ? "" : definition.getCode()))
                .toList();
        for (AgentToolDefinition definition : orderedDefinitions) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", providerToolName(definition.getCode()));
            function.put("description", AgentToolDescriptionRenderer.render(objectMapper,
                    definition.getDescription(), definition.getGuidanceJson()));
            function.put("parameters", objectMapper.readTree(definition.getInputSchemaJson()));
            if (responsesApi) {
                Map<String, Object> responseTool = new LinkedHashMap<>();
                responseTool.put("type", "function");
                responseTool.put("name", function.get("name"));
                responseTool.put("description", function.get("description"));
                responseTool.put("parameters", function.get("parameters"));
                tools.add(responseTool);
            } else {
                tools.add(Map.of("type", "function", "function", function));
            }
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

    private AgentModelTurnResponse extractChatAgentResponse(String responseBody, AgentModelTurnRequest request,
                                                             PromptCacheStrategy cacheStrategy) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode message = root.at("/choices/0/message");
        List<AgentModelToolCall> calls = new ArrayList<>();
        JsonNode toolCalls = message.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray()) {
            for (JsonNode toolCall : toolCalls) {
                JsonNode function = toolCall.get("function");
                String callId = textValue(toolCall.get("id"), "call_" + calls.size());
                calls.add(AgentModelToolCall.builder().itemId(callId)
                        .callId(callId)
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
                .cachedInputTokens(extractCachedInputTokens(responseBody, cacheStrategy))
                .cacheWriteTokens(extractCacheWriteTokens(responseBody, cacheStrategy))
                .outputTokens(extractOutputTokens(responseBody, content))
                .totalTokens(extractTotalTokens(responseBody, requestPrompt(request), content))
                .rawResponse(responseBody)
                .build();
    }

    private AgentModelTurnResponse extractResponsesAgentResponse(String responseBody, AgentModelTurnRequest request,
                                                                  PromptCacheStrategy cacheStrategy) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        List<AgentModelToolCall> calls = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                if (AiConstants.RESPONSES_FUNCTION_CALL_TYPE.equals(
                        textValue(item.get(AiConstants.RESPONSES_TYPE_FIELD), ""))) {
                    String callId = textValue(item.get(AiConstants.RESPONSES_FUNCTION_CALL_ID_FIELD),
                            textValue(item.get(AiConstants.RESPONSES_FUNCTION_CALL_ITEM_ID_FIELD),
                                    "call_" + calls.size()));
                    calls.add(AgentModelToolCall.builder()
                            .itemId(textValue(item.get(AiConstants.RESPONSES_FUNCTION_CALL_ITEM_ID_FIELD), callId))
                            .callId(callId)
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
                .cachedInputTokens(extractCachedInputTokens(responseBody, cacheStrategy))
                .cacheWriteTokens(extractCacheWriteTokens(responseBody, cacheStrategy))
                .outputTokens(extractOutputTokens(responseBody, content))
                .totalTokens(extractTotalTokens(responseBody, requestPrompt(request), content))
                .providerResponseId(textValue(root.get(AiConstants.RESPONSES_RESPONSE_ID_FIELD), ""))
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
        StringBuilder encoded = new StringBuilder(AiConstants.PROVIDER_TOOL_PREFIX);
        for (byte item : value.getBytes(StandardCharsets.UTF_8)) {
            encoded.append(String.format("%02x", item & 0xff));
        }
        return encoded.toString();
    }

    private String canonicalToolName(String providerToolName) {
        if (providerToolName == null || !providerToolName.startsWith(AiConstants.PROVIDER_TOOL_PREFIX)) return providerToolName;
        String hex = providerToolName.substring(AiConstants.PROVIDER_TOOL_PREFIX.length());
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

    private PromptCacheContext promptCacheContext(AgentModelTurnRequest request) {
        return request == null ? null : request.getPromptCacheContext();
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
        String allowedOrigins = environment.getProperty("cynexus.ai.security.provider-allowed-origins");
        if (allowedOrigins == null || allowedOrigins.isBlank()) allowedOrigins = DEFAULT_ALLOWED_PROVIDER_ORIGINS;
        // Provider endpoints are persisted in ai_providers, so the selected provider's origin
        // is an explicit trust entry in addition to the deployment-level defaults.
        URI allowedBase = AgentOutboundEndpointPolicy.requireAllowed(baseUrl, allowedOrigins + "," + baseUrl);
        return URI.create(allowedBase.toString() + path);
    }

    private Duration resolveRequestTimeout(AiModelBO model) {
        int seconds = extraInt(model, "request_timeout_seconds", (int) DEFAULT_REQUEST_TIMEOUT.toSeconds());
        return Duration.ofSeconds(Math.max(10, Math.min(seconds, 600)));
    }

    static HttpClient buildHttpClient(Environment environment) {
        return baseHttpClientBuilder(environment)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    private static CloseableHttpClient buildStreamingHttpClient(Environment environment) {
        var tlsStrategy = ClientTlsStrategyBuilder.create().setTlsVersions("TLSv1.2", "TLSv1.3").buildClassic();
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy).build();
        var builder = HttpClients.custom().setConnectionManager(connectionManager).disableAutomaticRetries();
        String proxyValue = firstProxyValue(environment);
        if (proxyValue != null && !proxyValue.isBlank()) {
            try {
                URI proxy = URI.create(proxyValue.trim());
                if (proxy.getHost() != null && proxy.getPort() > 0) {
                    builder.setProxy(new org.apache.hc.core5.http.HttpHost(proxy.getScheme(), proxy.getHost(), proxy.getPort()));
                }
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Ignoring invalid AI provider proxy configuration for stream client");
            }
        }
        return builder.build();
    }

    private AgentModelTurnResponse streamWithApacheHttpClient(AiModelBO model, URI endpoint, String apiKey, String payload,
                                                               Duration requestTimeout, boolean responsesApi,
                                                               AgentModelTurnRequest request,
                                                               PromptCacheStrategy cacheStrategy,
                                                               Consumer<String> textDeltaConsumer,
                                                               Consumer<String> thinkingDeltaConsumer) throws IOException {
        HttpPost post = new HttpPost(endpoint);
        post.setConfig(buildStreamingRequestConfig(requestTimeout));
        post.setHeader("Authorization", "Bearer " + apiKey);
        post.setHeader(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON);
        post.setHeader(CommonConstants.HTTP_HEADER_ACCEPT, CommonConstants.MEDIA_TYPE_TEXT_EVENT_STREAM);
        cacheStrategy.headers(model, promptCacheContext(request)).forEach(post::setHeader);
        post.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = streamingHttpClient.execute(post)) {
            int statusCode = response.getCode();
            if (statusCode < 200 || statusCode >= 300) {
                String responseBody = response.getEntity() == null
                        ? ""
                        : new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                throw providerHttpFailure(statusCode, responseBody);
            }
            if (response.getEntity() == null) {
                throw new IllegalStateException("AI provider stream response missing body");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent(), StandardCharsets.UTF_8));
                 Stream<String> lines = reader.lines()) {
                return responsesApi ? readResponsesStream(lines, request, textDeltaConsumer, thinkingDeltaConsumer)
                        : readChatStream(lines, request, textDeltaConsumer, thinkingDeltaConsumer);
            }
        }
    }

    static RequestConfig buildStreamingRequestConfig(Duration requestTimeout) {
        return RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.of(requestTimeout))
                .build();
    }

    private static HttpClient.Builder baseHttpClientBuilder(Environment environment) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
        String proxyValue = firstProxyValue(environment);
        if (proxyValue == null || proxyValue.isBlank()) return builder.version(HttpClient.Version.HTTP_1_1);
        try {
            URI proxy = URI.create(proxyValue.trim());
            if (proxy.getHost() == null || proxy.getPort() <= 0) return builder.version(HttpClient.Version.HTTP_1_1);
            LOGGER.info("AI provider outbound proxy configured at {}:{} using HTTP/1.1", proxy.getHost(), proxy.getPort());
            return builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())))
                    .version(HttpClient.Version.HTTP_1_1);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Ignoring invalid AI provider proxy configuration");
            return builder.version(HttpClient.Version.HTTP_1_1);
        }
    }

    private static String firstProxyValue(Environment environment) {
        String proxyValue = environment.getProperty("HTTPS_PROXY");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = environment.getProperty("HTTP_PROXY");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = System.getenv("HTTPS_PROXY");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = System.getenv("HTTP_PROXY");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = System.getenv("https_proxy");
        if (proxyValue == null || proxyValue.isBlank()) proxyValue = System.getenv("http_proxy");
        return proxyValue;
    }

    @PreDestroy
    void closeStreamingHttpClient() throws IOException {
        responsesWebSocketSessions.values().forEach(ResponsesWebSocketSession::close);
        responsesWebSocketSessions.clear();
        streamingHttpClient.close();
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

    private Integer extractCachedInputTokens(String responseBody, PromptCacheStrategy strategy) {
        return extractPromptCacheUsage(responseBody, strategy).getCachedInputTokens();
    }

    private Integer extractCacheWriteTokens(String responseBody, PromptCacheStrategy strategy) {
        return extractPromptCacheUsage(responseBody, strategy).getCacheWriteTokens();
    }

    private PromptCacheUsage extractPromptCacheUsage(String responseBody, PromptCacheStrategy strategy) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usage = root.get(AiConstants.AI_USAGE_FIELD);
            return strategy.extractUsage(usage);
        } catch (IOException ignored) {
            return PromptCacheUsage.builder().build();
        }
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

    private boolean responsesApi(AiModelBO model) {
        return AiConstants.WIRE_API_RESPONSES.equalsIgnoreCase(extraString(model, AiConstants.MODEL_WIRE_API_EXTRA));
    }

    private boolean responsesWebSocketEnabled(AiModelBO model) {
        return AiConstants.RESPONSES_TRANSPORT_WEBSOCKET.equalsIgnoreCase(
                extraString(model, AiConstants.RESPONSES_TRANSPORT_EXTRA));
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean responsesStateEnabled(AiModelBO model) {
        return AiConstants.RESPONSES_STATE_MODE_PREVIOUS_RESPONSE_ID.equalsIgnoreCase(
                extraString(model, AiConstants.RESPONSES_STATE_MODE_EXTRA))
                && !extraBoolean(model, AiConstants.RESPONSES_DISABLE_STORAGE_EXTRA, false);
    }

    private boolean shouldUseResponsesState(AiModelBO model, AgentModelTurnRequest request,
                                            boolean forceStateless) {
        return !forceStateless && responsesStateEnabled(model) && request != null
                && request.getPreviousResponseId() != null && !request.getPreviousResponseId().isBlank();
    }

    private AgentModelTurnResponse attachCacheTelemetry(AgentModelTurnResponse response, String payload,
                                                         AgentModelTurnRequest request, boolean stateful,
                                                         boolean stateFallback, boolean storageEnabled) {
        if (response == null) return null;
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode input = root.has(AiConstants.RESPONSES_INPUT_FIELD)
                    ? root.get(AiConstants.RESPONSES_INPUT_FIELD) : root.get("messages");
            String cacheKey = textValue(root.get(AiConstants.PROMPT_CACHE_KEY_FIELD), "");
            PromptCacheContext context = promptCacheContext(request);
            String mode = stateFallback ? AiConstants.AGENT_CACHE_MODE_STATELESS_FALLBACK
                    : stateful ? AiConstants.AGENT_CACHE_MODE_STATEFUL : AiConstants.AGENT_CACHE_MODE_STATELESS;
            PromptCacheTelemetry telemetry = PromptCacheTelemetry.builder()
                    .cacheKeyHash(digestTelemetry(cacheKey))
                    .stablePrefixHash(digestTelemetry(context == null ? null : context.getStablePrefixKey()))
                    .requestInputHash(digestTelemetry(input == null ? null : input.toString()))
                    .inputItemCount(input == null ? 0 : input.isArray() ? input.size() : 1)
                    .continuationMode(mode)
                    .previousResponseIdHash(digestTelemetry(stateful && request != null
                            ? request.getPreviousResponseId() : null))
                    .providerResponseIdHash(digestTelemetry(response.getProviderResponseId()))
                    .responseStorageEnabled(storageEnabled)
                    .stateFallback(stateFallback)
                    .build();
            return response.toBuilder().cacheTelemetry(telemetry).build();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build Agent cache telemetry", exception);
        }
    }

    private String digestTelemetry(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean isResponsesStateFallbackCandidate(ProviderHttpException failure) {
        return failure != null && (failure.statusCode == 400 || failure.statusCode == 404
                || failure.statusCode == 409 || failure.statusCode == 422);
    }

    private ProviderHttpException providerHttpFailure(int statusCode, String responseBody) {
        String detail = responseBody == null ? "" : responseBody.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (detail.length() > 240) detail = detail.substring(0, 240);
        LOGGER.warn("AI provider {} returned HTTP {} with safe detail: {}", providerCode(), statusCode, detail);
        return new ProviderHttpException(statusCode);
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

    private static final class ProviderHttpException extends IllegalStateException {
        private final int statusCode;

        private ProviderHttpException(int statusCode) {
            super("AI provider returned HTTP " + statusCode);
            this.statusCode = statusCode;
        }
    }
}
