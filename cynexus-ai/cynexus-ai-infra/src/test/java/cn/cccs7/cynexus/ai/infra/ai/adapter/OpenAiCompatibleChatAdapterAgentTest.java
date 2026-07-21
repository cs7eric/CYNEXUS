package cn.cccs7.cynexus.ai.infra.ai.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class OpenAiCompatibleChatAdapterAgentTest {

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
        assertThat(requestBody.get()).contains("market.overview").contains("tradeDate").contains("tool_choice");
    }

    @Test
    void parsesResponsesFunctionCallItemsWithUsage() throws Exception {
        server = server(new AtomicReference<>(), """
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
        SecretResolver secrets = secretRef -> "test-key";
        Environment environment = mock(Environment.class);
        when(environment.getProperty("cynexus.ai.platform.providers.custom.base-url", "https://am.xtyooo.top"))
                .thenReturn("http://127.0.0.1:" + server.getAddress().getPort());
        when(environment.getProperty("cynexus.ai.platform.providers.custom.api-key-env", "CYNEXUS_AI_CUSTOM_API_KEY"))
                .thenReturn("CYNEXUS_AI_CUSTOM_API_KEY");
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
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).source("test").build();
    }
}
