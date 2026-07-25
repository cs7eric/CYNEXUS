package cn.cccs7.cynexus.ai.infra.ai.adapter;

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
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
    void parsesResponsesFunctionCallItemsWithUsage() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                {"output":[{"type":"function_call","call_id":"resp-1","name":"market.overview","arguments":"{}"}],"usage":{"input_tokens":10,"output_tokens":6,"total_tokens":16}}
                """);

        AgentModelTurnResponse response = adapter().completeAgent(model("{\"wire_api\":\"responses\"}"), request(List.of(toolDefinition())));

        assertThat(response.getType()).isEqualTo("TOOL_CALLS");
        assertThat(response.getToolCalls()).singleElement().satisfies(call -> {
            assertThat(call.getCallId()).isEqualTo("resp-1");
            assertThat(call.getToolCode()).isEqualTo("market.overview");
            assertThat(call.getArguments()).isEmpty();
        });
        assertThat(response.getInputTokens()).isEqualTo(10);
        assertThat(response.getOutputTokens()).isEqualTo(6);
        assertThat(response.getTotalTokens()).isEqualTo(16);

        var payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get());
        var tool = payload.path("tools").get(0);
        assertThat(tool.path("type").asText()).isEqualTo("function");
        assertThat(tool.path("name").asText()).isEqualTo("cynexus_tool_6d61726b65742e6f76657276696577");
        assertThat(tool.path("description").asText()).contains("Purpose: Read one bounded market snapshot.");
        assertThat(tool.path("parameters").path("type").asText()).isEqualTo("object");
        assertThat(tool.path("function").isMissingNode()).isTrue();
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
    void responsesToolContinuationIncludesItemReferenceMatchingCallId() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(requestBody, """
                {"output":[{"type":"message","content":[{"type":"output_text","text":"done"}]}],"usage":{"input_tokens":4,"output_tokens":1,"total_tokens":5}}
                """);

        AgentModelTurnRequest continuation = AgentModelTurnRequest.builder()
                .systemPrompt("Use tools when needed.")
                .messages(List.of(
                        AgentModelMessage.builder().role("user").content("大盘怎么样？").build(),
                        AgentModelMessage.builder().role("assistant").content("")
                                .toolCalls(List.of(AgentModelToolCall.builder()
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
                .contains("\"id\":\"call-market-1\"")
                .contains("\"type\":\"function_call_output\"")
                .contains("\"item_reference\":\"call-market-1\"");
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

    private HttpServer server(AtomicReference<String> requestBody, String responseBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1/responses", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
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

    private AgentToolDefinition toolDefinition() {
        return AgentToolDefinition.builder().code("market.overview").name("Market overview")
                .description("Read market overview.").inputSchemaJson("{\"type\":\"object\",\"properties\":{\"tradeDate\":{\"type\":\"string\"}}}")
                .guidanceJson("{\"purpose\":\"Read one bounded market snapshot.\"}")
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).source("test").build();
    }
}
