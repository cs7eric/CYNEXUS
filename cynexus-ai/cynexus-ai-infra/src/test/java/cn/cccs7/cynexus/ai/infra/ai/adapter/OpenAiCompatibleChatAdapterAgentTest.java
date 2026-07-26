package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheContext;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Map;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class OpenAiCompatibleChatAdapterAgentTest {

    @Test
    void identifiesOnlyTlsHandshakeFailuresForStreamTransportRetry() {
        assertThat(OpenAiCompatibleChatAdapter.isTlsHandshakeFailure(
                new IOException("wrapper", new SSLHandshakeException("terminated")))).isTrue();
        assertThat(OpenAiCompatibleChatAdapter.isTlsHandshakeFailure(new IOException("timeout"))).isFalse();
    }

    @Test
    void preservesStreamingTimeoutAndInterruptionClassification() {
        var config = OpenAiCompatibleChatAdapter.buildStreamingRequestConfig(java.time.Duration.ofSeconds(37));

        assertThat(config.getConnectTimeout().toSeconds()).isEqualTo(10);
        assertThat(config.getConnectionRequestTimeout().toSeconds()).isEqualTo(10);
        assertThat(config.getResponseTimeout().toSeconds()).isEqualTo(37);
        assertThat(OpenAiCompatibleChatAdapter.isInterruptedFailure(
                new IOException("wrapper", new InterruptedIOException("cancelled")))).isTrue();
        assertThat(OpenAiCompatibleChatAdapter.isInterruptedFailure(
                new IOException("wrapper", new SocketTimeoutException("timed out")))).isFalse();
        assertThat(OpenAiCompatibleChatAdapter.isInterruptedFailure(new IOException("timeout"))).isFalse();
    }

    @Test
    void configuresHttpProxyWithHttp11ForProviderTlsCompatibility() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("HTTPS_PROXY")).thenReturn("http://proxy.example:7890");

        var client = OpenAiCompatibleChatAdapter.buildHttpClient(environment);

        assertThat(client.version()).isEqualTo(java.net.http.HttpClient.Version.HTTP_1_1);
        assertThat(client.proxy()).isPresent().get().satisfies(proxy -> {
            assertThat(proxy.select(URI.create("https://am.xtyooo.top/v1/chat/completions")))
                    .containsExactly(new java.net.Proxy(java.net.Proxy.Type.HTTP,
                            new InetSocketAddress("proxy.example", 7890)));
        });
    }

    @Test
    void ignoresMalformedProxyWithoutChangingTlsDefaults() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("HTTPS_PROXY")).thenReturn("not a uri");

        var client = OpenAiCompatibleChatAdapter.buildHttpClient(environment);

        assertThat(client.proxy()).isEmpty();
    }

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesChatCompletionsNativeToolCallAndSendsSchemas() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                {"choices":[{"message":{"role":"assistant","content":"","tool_calls":[{"id":"call-1","type":"function","function":{"name":"market.overview","arguments":"{\\"tradeDate\\":\\"2026-07-22\\"}"}}]}}],"usage":{"prompt_tokens":12,"completion_tokens":8,"total_tokens":20}}
                """);

        AgentModelTurnResponse response = adapter().completeAgent(model(""), request(List.of(toolDefinition())));

        assertThat(response.getType()).isEqualTo("TOOL_CALLS");
        assertThat(response.getToolCalls()).singleElement().satisfies(call -> {
            assertThat(call.getCallId()).isEqualTo("call-1");
            assertThat(call.getToolCode()).isEqualTo("market.overview");
            assertThat(call.getArguments()).containsEntry("tradeDate", "2026-07-22");
        });
        assertThat(requestBody.get()).contains("cynexus_tool_6d61726b65742e6f76657276696577")
                .doesNotContain("\"name\":\"market.overview\"").contains("tradeDate").contains("tool_choice");
    }

    @Test
    void sendsReasoningEffortToChatCompletions() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}");

        adapter().completeAgent(model("{\"model_reasoning_effort\":\"low\"}"), request(List.of()));

        assertThat(new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get())
                .path("reasoning_effort").asText()).isEqualTo("low");
    }

    @Test
    void sendsReasoningEffortToResponses() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"ok\"}]}]}");

        adapter().completeAgent(model("{\"wire_api\":\"responses\",\"model_reasoning_effort\":\"high\"}"), request(List.of()));

        assertThat(new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get())
                .path("reasoning").path("effort").asText()).isEqualTo("high");
    }

    @Test
    void keepsToolDefinitionsStableWhenFinalTurnDisablesToolCalls() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}");

        AgentModelTurnRequest turn = AgentModelTurnRequest.builder()
                .systemPrompt("Use tools when needed.")
                .messages(List.of(AgentModelMessage.builder().role("user").content("大盘怎么样？").build()))
                .tools(List.of(toolDefinition()))
                .toolsAllowed(false)
                .build();

        adapter().completeAgent(model(""), turn);

        var payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get());
        assertThat(payload.path("tools")).hasSize(1);
        assertThat(payload.path("tool_choice").asText()).isEqualTo("none");
    }

    @Test
    void placesRuntimeContextAfterConversationHistory() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}");

        AgentModelTurnRequest turn = AgentModelTurnRequest.builder()
                .systemPrompt("stable system prompt")
                .runtimeContext("dynamic runtime context")
                .messages(List.of(AgentModelMessage.builder().role("user").content("question").build()))
                .tools(List.of())
                .build();
        adapter().completeAgent(model(""), turn);

        var messages = new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get()).path("messages");
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
        assertThat(messages.get(0).path("content").asText()).isEqualTo("stable system prompt");
        assertThat(messages.get(1).path("role").asText()).isEqualTo("user");
        assertThat(messages.get(1).path("content").asText()).isEqualTo("question");
        assertThat(messages.get(2).path("role").asText()).isEqualTo("system");
        assertThat(messages.get(2).path("content").asText()).isEqualTo("dynamic runtime context");
    }

    @Test
    void parsesResponsesFunctionCallItemsWithUsage() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                {"output":[{"type":"function_call","id":"fc-resp-1","call_id":"resp-1","name":"market.overview","arguments":"{}"}],"usage":{"input_tokens":10,"output_tokens":6,"total_tokens":16}}
                """);

        AgentModelTurnResponse response = adapter().completeAgent(model("{\"wire_api\":\"responses\"}"), request(List.of(toolDefinition())));

        assertThat(response.getType()).isEqualTo("TOOL_CALLS");
        assertThat(response.getToolCalls()).singleElement().satisfies(call -> {
            assertThat(call.getCallId()).isEqualTo("resp-1");
            assertThat(call.getItemId()).isEqualTo("fc-resp-1");
            assertThat(call.getToolCode()).isEqualTo("market.overview");
            assertThat(call.getArguments()).isEmpty();
        });
        assertThat(response.getInputTokens()).isEqualTo(10);
        assertThat(response.getOutputTokens()).isEqualTo(6);
        assertThat(response.getTotalTokens()).isEqualTo(16);
        assertThat(response.getProviderOutputJson()).contains("fc-resp-1", "function_call");

        var payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get());
        var tool = payload.path("tools").get(0);
        assertThat(tool.path("type").asText()).isEqualTo("function");
        assertThat(tool.path("name").asText()).isEqualTo("cynexus_tool_6d61726b65742e6f76657276696577");
        assertThat(tool.path("description").asText()).contains("Purpose: Read one bounded market snapshot.");
        assertThat(tool.path("parameters").path("type").asText()).isEqualTo("object");
        assertThat(tool.path("function").isMissingNode()).isTrue();
    }

    @Test
    void storesFirstResponsesTurnAndCapturesProviderStateAndTelemetry() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                {"id":"resp-first","output":[{"type":"message","content":[{"type":"output_text","text":"done"}]}],"usage":{"input_tokens":20,"input_tokens_details":{"cached_tokens":8},"output_tokens":2,"total_tokens":22}}
                """);

        AgentModelTurnResponse response = adapter().completeAgent(statefulModel(), statefulRequest(null));

        var payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get());
        assertThat(payload.path("store").asBoolean()).isTrue();
        assertThat(payload.has("previous_response_id")).isFalse();
        assertThat(payload.path("prompt_cache_retention").asText()).isEqualTo("24h");
        assertThat(payload.path("prompt_cache_key").asText()).isEqualTo("cynexus-agent-routing-test");
        assertThat(response.getProviderResponseId()).isEqualTo("resp-first");
        assertThat(response.getCachedInputTokens()).isEqualTo(8);
        assertThat(response.getCacheTelemetry().isResponseStorageEnabled()).isTrue();
        assertThat(response.getCacheTelemetry().getProviderResponseIdHash())
                .isNotBlank().isNotEqualTo("resp-first");
    }

    @Test
    void sendsOnlyDeltaInputWhenContinuingAStoredResponsesConversation() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                {"id":"resp-next","output":[{"type":"message","content":[{"type":"output_text","text":"done"}]}],"usage":{"input_tokens":12,"output_tokens":2,"total_tokens":14}}
                """);

        AgentModelTurnResponse response = adapter().completeAgent(statefulModel(), statefulRequest("resp-first"));

        var payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get());
        assertThat(payload.path("previous_response_id").asText()).isEqualTo("resp-first");
        assertThat(payload.path("input").toString())
                .contains("function_call_output")
                .contains("fresh runtime")
                .doesNotContain("old history")
                .doesNotContain("stable system prompt");
        assertThat(response.getProviderResponseId()).isEqualTo("resp-next");
        assertThat(response.getCacheTelemetry().getContinuationMode()).isEqualTo("STATEFUL");
        assertThat(response.getCacheTelemetry().getPreviousResponseIdHash())
                .isNotBlank().isNotEqualTo("resp-first");
    }

    @Test
    void retriesFullTranscriptWhenGatewayRejectsResponsesState() throws Exception {
        List<String> requestBodies = new ArrayList<>();
        AtomicInteger requests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int requestNo = requests.incrementAndGet();
            String body = requestNo == 1 ? "{\"error\":\"previous response not found\"}" : """
                    {"id":"resp-recovered","output":[{"type":"message","content":[{"type":"output_text","text":"recovered"}]}],"usage":{"input_tokens":30,"output_tokens":2,"total_tokens":32}}
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON);
            exchange.sendResponseHeaders(requestNo == 1 ? 404 : 200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        AgentModelTurnResponse response = adapter().completeAgent(statefulModel(), statefulRequest("resp-expired"));

        assertThat(requestBodies).hasSize(2);
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        assertThat(mapper.readTree(requestBodies.get(0)).has("previous_response_id")).isTrue();
        var fallback = mapper.readTree(requestBodies.get(1));
        assertThat(fallback.has("previous_response_id")).isFalse();
        assertThat(fallback.path("input").toString())
                .contains("stable system prompt")
                .contains("old history");
        assertThat(response.getProviderResponseId()).isEqualTo("resp-recovered");
        assertThat(response.getCacheTelemetry().isStateFallback()).isTrue();
        assertThat(response.getCacheTelemetry().getContinuationMode()).isEqualTo("STATELESS_FALLBACK");
    }

    @Test
    void allowsTheSelectedProviderOriginEvenWhenItIsAbsentFromDeploymentDefaults() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}");

        AgentModelTurnResponse response = adapterWithDeploymentOrigins("https://api.openai.com")
                .completeAgent(model(""), request(List.of()));

        assertThat(response.getType()).isEqualTo("FINAL");
        assertThat(response.getContent()).isEqualTo("ok");
    }

    @Test
    void responsesToolContinuationPreservesItemIdAndUsesStandardFunctionOutput() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                {"output":[{"type":"message","content":[{"type":"output_text","text":"done"}]}],"usage":{"input_tokens":4,"output_tokens":1,"total_tokens":5}}
                """);

        AgentModelTurnRequest continuation = AgentModelTurnRequest.builder()
                .systemPrompt("Use tools when needed.")
                .messages(List.of(
                        AgentModelMessage.builder().role("user").content("大盘怎么样？").build(),
                        AgentModelMessage.builder().role("assistant").content("")
                                .toolCalls(List.of(AgentModelToolCall.builder().itemId("fc-market-1")
                                        .callId("call-market-1")
                                        .toolCode("market.overview")
                                        .arguments(Map.of("tradeDate", "2026-07-24"))
                                        .build()))
                                .build(),
                        AgentModelMessage.builder().role("tool").toolCallId("call-market-1")
                                .content("{\"up\":1000,\"down\":2000}").build()
                ))
                .tools(List.of(toolDefinition()))
                .build();

        AgentModelTurnResponse response = adapter().completeAgent(model("{\"wire_api\":\"responses\"}"), continuation);
        assertThat(response.getType()).isEqualTo("FINAL");
        assertThat(response.getContent()).isEqualTo("done");

        var payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get());
        assertThat(payload.path("input").toString())
                .contains("\"type\":\"function_call\"")
                .contains("\"call_id\":\"call-market-1\"")
                .contains("\"id\":\"fc-market-1\"")
                .contains("\"type\":\"function_call_output\"")
                .doesNotContain("item_reference");
    }

    @Test
    void replaysNativeResponsesOutputInExactConversationOrder() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                {"id":"resp-next","output":[{"type":"message","phase":"final_answer","content":[{"type":"output_text","text":"done"}]}],"usage":{"input_tokens":40,"input_tokens_details":{"cached_tokens":32},"output_tokens":2,"total_tokens":42}}
                """);
        String nativeOutput = "[{\"type\":\"reasoning\",\"encrypted_content\":\"opaque-state\"},"
                + "{\"id\":\"fc-1\",\"type\":\"function_call\",\"call_id\":\"call-1\","
                + "\"name\":\"cynexus_tool_6d61726b65742e6f76657276696577\",\"arguments\":\"{}\"}]";
        AgentModelTurnRequest turn = AgentModelTurnRequest.builder()
                .systemPrompt("stable system prompt")
                .messages(List.of(
                        AgentModelMessage.builder().role("user").content("old question").build(),
                        AgentModelMessage.builder().role("assistant").content("")
                                .runtimeContext("runtime-1")
                                .providerOutputJson(nativeOutput)
                                .toolCalls(List.of(AgentModelToolCall.builder()
                                        .itemId("fc-1").callId("call-1").toolCode("market.overview")
                                        .arguments(Map.of()).build()))
                                .build(),
                        AgentModelMessage.builder().role("tool").toolCallId("call-1")
                                .content("{\"up\":1000}").build()))
                .runtimeContext("runtime-2")
                .promptCacheContext(PromptCacheContext.builder()
                        .routingKey("shared-routing-key")
                        .stablePrefixKey("stable-prefix-key")
                        .build())
                .tools(List.of(toolDefinition()))
                .build();

        AgentModelTurnResponse response = adapter().completeAgent(
                model("{\"wire_api\":\"responses\",\"disable_response_storage\":true}"), turn);

        var input = new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get()).path("input");
        assertThat(input).hasSize(7);
        assertThat(input.get(0).path("content").asText()).isEqualTo("stable system prompt");
        assertThat(input.get(1).path("content").asText()).isEqualTo("old question");
        assertThat(input.get(2).path("content").asText()).isEqualTo("runtime-1");
        assertThat(input.get(3).path("type").asText()).isEqualTo("reasoning");
        assertThat(input.get(3).path("encrypted_content").asText()).isEqualTo("opaque-state");
        assertThat(input.get(4).path("type").asText()).isEqualTo("function_call");
        assertThat(input.get(5).path("type").asText()).isEqualTo("function_call_output");
        assertThat(input.get(6).path("content").asText()).isEqualTo("runtime-2");
        assertThat(response.getCacheTelemetry().getContinuationMode()).isEqualTo("STATELESS_REPLAY");
        assertThat(response.getProviderOutputJson()).contains("final_answer");
    }

    @Test
    void parsesChatStreamingTextAndToolDeltas() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                data: {"choices":[{"delta":{"content":"实时"}}]}

                data: {"choices":[{"delta":{"content":"答案"}}]}

                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"stream-call","function":{"name":"market.overview","arguments":"{}"}}]}}]}

                data: {"choices":[],"usage":{"prompt_tokens":12,"completion_tokens":8,"total_tokens":20}}

                data: [DONE]

                """);
        List<String> deltas = new ArrayList<>();

        AgentModelTurnResponse response = adapter().streamAgent(model(""), request(List.of(toolDefinition())), deltas::add);

        assertThat(deltas).containsExactly("实时", "答案");
        assertThat(response.getContent()).isEqualTo("实时答案");
        assertThat(response.getInputTokens()).isEqualTo(12);
        assertThat(response.getOutputTokens()).isEqualTo(8);
        assertThat(response.getTotalTokens()).isEqualTo(20);
        assertThat(new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getRawResponse())
                .path("usage").path("total_tokens").asInt()).isEqualTo(20);
        assertThat(response.getToolCalls()).singleElement().satisfies(call -> {
            assertThat(call.getCallId()).isEqualTo("stream-call");
            assertThat(call.getToolCode()).isEqualTo("market.overview");
            assertThat(call.getArguments()).isEmpty();
        });

        var requestPayload = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                requestBody.get());
        assertThat(requestPayload.path("stream_options").path("include_usage").asBoolean()).isTrue();
    }

    @Test
    void correlatesResponsesStreamingArgumentsByItemIdAndPreservesCallId() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                data: {"type":"response.output_item.added","item":{"id":"fc-item-1","type":"function_call","call_id":"call-1","name":"market.stock_research","arguments":""}}

                data: {"type":"response.function_call_arguments.delta","item_id":"fc-item-1","delta":"{\\"operation\\":\\"metadata\\",\\"symbol\\":\\"600519\\"}"}

                data: {"type":"response.function_call_arguments.done","item_id":"fc-item-1","arguments":"{\\"operation\\":\\"metadata\\",\\"symbol\\":\\"600519\\"}"}

                data: {"type":"response.completed","response":{"id":"resp-stream-1","output":[{"type":"reasoning","encrypted_content":"opaque-stream"},{"id":"fc-item-1","type":"function_call","call_id":"call-1","name":"market.stock_research","arguments":"{\\"operation\\":\\"metadata\\",\\"symbol\\":\\"600519\\"}"}],"usage":{"input_tokens":14,"input_tokens_details":{"cached_tokens":8},"output_tokens":3,"total_tokens":17}}}

                data: [DONE]

                """);

        AgentModelTurnResponse response = adapter().streamAgent(model("{\"wire_api\":\"responses\"}"),
                request(List.of(toolDefinition())), ignored -> { });

        assertThat(response.getToolCalls()).singleElement().satisfies(call -> {
            assertThat(call.getCallId()).isEqualTo("call-1");
            assertThat(call.getToolCode()).isEqualTo("market.stock_research");
            assertThat(call.getArguments()).containsEntry("operation", "metadata")
                    .containsEntry("symbol", "600519");
        });
        assertThat(response.getProviderResponseId()).isEqualTo("resp-stream-1");
        assertThat(response.getCachedInputTokens()).isEqualTo(8);
        assertThat(response.getProviderOutputJson()).contains("opaque-stream", "fc-item-1");
    }

    @Test
    void fallsBackToHttpResponsesWhenWebSocketHandshakeFails() throws Exception {
        server = server(new AtomicReference<>(),
                "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"ok\"}]}]}" );

        AgentModelTurnResponse response = adapter().completeAgent(
                model("{\"wire_api\":\"responses\",\"responses_transport\":\"websocket\"}"),
                request(List.of()));

        assertThat(response.getContent()).isEqualTo("ok");
    }

    private HttpServer server(AtomicReference<String> requestBody, String responseBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1/responses", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private CustomModelAdapter adapter() {
        return adapterWithDeploymentOrigins("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private CustomModelAdapter adapterWithDeploymentOrigins(String deploymentOrigins) {
        SecretResolver secrets = secretRef -> "test-key";
        Environment environment = mock(Environment.class);
        when(environment.getProperty("cynexus.ai.platform.providers.custom.base-url", "https://am.xtyooo.top"))
                .thenReturn("http://127.0.0.1:" + server.getAddress().getPort());
        when(environment.getProperty("cynexus.ai.platform.providers.custom.api-key-env", "CYNEXUS_AI_CUSTOM_API_KEY"))
                .thenReturn("CYNEXUS_AI_CUSTOM_API_KEY");
        when(environment.getProperty("cynexus.ai.security.provider-allowed-origins"))
                .thenReturn(deploymentOrigins);
        return new CustomModelAdapter(secrets, environment);
    }

    private AiModelBO model(String extra) {
        return AiModelBO.builder().modelCode("gpt-5.4-mini").modelName("gpt-5.4-mini")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("test-key").extra(extra).enabled(true).build();
    }

    private AgentModelTurnRequest request(List<AgentToolDefinition> tools) {
        return AgentModelTurnRequest.builder().systemPrompt("Use tools when needed.")
                .messages(List.of(AgentModelMessage.builder().role("user").content("大盘怎么样？").build()))
                .tools(tools).build();
    }

    private AiModelBO statefulModel() {
        return model("{\"wire_api\":\"responses\",\"responses_state_mode\":\"previous_response_id\","
                + "\"disable_response_storage\":false,\"prompt_cache_retention\":\"24h\"}");
    }

    private AgentModelTurnRequest statefulRequest(String previousResponseId) {
        AgentModelMessage toolOutput = AgentModelMessage.builder().role("tool")
                .toolCallId("call-market-1").content("{\"up\":1000}").build();
        return AgentModelTurnRequest.builder()
                .systemPrompt("stable system prompt")
                .runtimeContext("fresh runtime")
                .messages(List.of(
                        AgentModelMessage.builder().role("user").content("old history").build(),
                        toolOutput))
                .continuationMessages(List.of(toolOutput))
                .previousResponseId(previousResponseId)
                .promptCacheContext(PromptCacheContext.builder()
                        .conversationKey("cynexus-conversation-test")
                        .stablePrefixKey("cynexus-prefix-test")
                        .routingKey("cynexus-agent-routing-test")
                        .stablePrefixVersion("v4")
                        .build())
                .tools(List.of(toolDefinition()))
                .build();
    }

    private AgentToolDefinition toolDefinition() {
        return AgentToolDefinition.builder().code("market.overview").name("Market overview")
                .description("Read market overview.").inputSchemaJson("{\"type\":\"object\",\"properties\":{\"tradeDate\":{\"type\":\"string\"}}}")
                .guidanceJson("{\"purpose\":\"Read one bounded market snapshot.\"}")
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).source("test").build();
    }
}
