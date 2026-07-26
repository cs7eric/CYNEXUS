package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/** One serialized Responses WebSocket v2 conversation. */
final class ResponsesWebSocketSession implements WebSocket.Listener, AutoCloseable {

    private final ObjectMapper objectMapper;
    private final Object monitor = new Object();
    private final Object executionMonitor = new Object();
    private final Duration timeout;
    private final CompletableFuture<WebSocket> opened;
    private volatile WebSocket socket;
    private CompletableFuture<AgentModelTurnResponse> activeResponse;
    private ResponsesWebSocketEventAccumulator activeAccumulator;
    private AgentModelTurnRequest activeRequest;
    private StringBuilder fragmentedMessage;

    private ResponsesWebSocketSession(HttpClient client, URI endpoint, String apiKey,
                                      Map<String, String> headers, Duration timeout,
                                      ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        WebSocket.Builder builder = client.newWebSocketBuilder().header("Authorization", "Bearer " + apiKey);
        headers.forEach(builder::header);
        this.opened = builder.buildAsync(endpoint, this);
    }

    static ResponsesWebSocketSession connect(HttpClient client, URI endpoint, String apiKey,
                                             Map<String, String> headers, Duration timeout,
                                             ObjectMapper objectMapper) {
        return new ResponsesWebSocketSession(client, endpoint, apiKey, headers, timeout, objectMapper);
    }

    AgentModelTurnResponse execute(String payload, AgentModelTurnRequest request,
                                   Consumer<String> textDeltaConsumer,
                                   Consumer<String> thinkingDeltaConsumer) {
        synchronized (executionMonitor) {
            CompletableFuture<AgentModelTurnResponse> responseFuture;
            try {
                awaitOpen();
                synchronized (monitor) {
                    activeResponse = responseFuture = new CompletableFuture<>();
                    activeAccumulator = new ResponsesWebSocketEventAccumulator(objectMapper);
                    activeRequest = request;
                    activeTextConsumer = textDeltaConsumer;
                    activeThinkingConsumer = thinkingDeltaConsumer;
                    fragmentedMessage = new StringBuilder();
                }
                JsonNode event = objectMapper.readTree(payload);
                if (event == null || !event.isObject()) {
                    throw new IllegalStateException("Responses WebSocket payload must be an object");
                }
                ((ObjectNode) event).put(
                        AiConstants.RESPONSES_TYPE_FIELD,
                        AiConstants.RESPONSES_CREATE_EVENT);
                socket.sendText(objectMapper.writeValueAsString(event), true).join();
                try {
                    return responseFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException exception) {
                    throw new IllegalStateException("AI provider Responses WebSocket timed out", exception);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("AI provider Responses WebSocket interrupted", exception);
                } catch (CompletionException exception) {
                    throw new IllegalStateException("AI provider Responses WebSocket send failed", exception.getCause());
                } catch (Exception exception) {
                    if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                    throw new IllegalStateException("AI provider Responses WebSocket failed", exception);
                }
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to encode AI provider Responses WebSocket request", exception);
            } finally {
                synchronized (monitor) {
                    activeResponse = null;
                    activeAccumulator = null;
                    activeRequest = null;
                    fragmentedMessage = null;
                    activeTextConsumer = null;
                    activeThinkingConsumer = null;
                }
            }
        }
    }

    private void awaitOpen() {
        try {
            socket = opened.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("AI provider Responses WebSocket connection failed", exception);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        socket = webSocket;
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String eventJson = null;
        ResponsesWebSocketEventAccumulator accumulator = null;
        CompletableFuture<AgentModelTurnResponse> response = null;
        AgentModelTurnRequest request = null;
        Consumer<String> textConsumer = null;
        Consumer<String> thinkingConsumer = null;
        synchronized (monitor) {
            if (fragmentedMessage == null) fragmentedMessage = new StringBuilder();
            fragmentedMessage.append(data);
            if (last && activeAccumulator != null) {
                eventJson = fragmentedMessage.toString();
                fragmentedMessage.setLength(0);
                accumulator = activeAccumulator;
                response = activeResponse;
                request = activeRequest;
                textConsumer = activeTextConsumer;
                thinkingConsumer = activeThinkingConsumer;
            }
        }
        if (eventJson != null && accumulator != null && response != null) {
            try {
                String type = accumulator.accept(eventJson, textConsumer, thinkingConsumer);
                if (AiConstants.RESPONSES_COMPLETED_EVENT.equals(type)) {
                    response.complete(accumulator.toResponse(request));
                } else if (AiConstants.RESPONSES_ERROR_EVENT.equals(type)
                        || AiConstants.RESPONSES_FAILED_EVENT.equals(type)) {
                    response.completeExceptionally(new IllegalStateException(
                            "AI provider Responses WebSocket returned " + safeError(eventJson)));
                }
            } catch (Exception exception) {
                response.completeExceptionally(exception);
            }
        }
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    private Consumer<String> activeTextConsumer;
    private Consumer<String> activeThinkingConsumer;

    private String safeError(String eventJson) {
        try {
            JsonNode root = objectMapper.readTree(eventJson);
            return root.path(AiConstants.RESPONSES_ERROR_FIELD).toString();
        } catch (Exception ignored) {
            return "error";
        }
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        synchronized (monitor) {
            if (!opened.isDone()) opened.completeExceptionally(error);
            if (activeResponse != null) activeResponse.completeExceptionally(error);
        }
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        synchronized (monitor) {
            if (activeResponse != null && !activeResponse.isDone()) {
                activeResponse.completeExceptionally(new IllegalStateException(
                        "AI provider Responses WebSocket closed: " + statusCode + " " + reason));
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
        WebSocket current = socket;
        if (current != null) {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }
}
