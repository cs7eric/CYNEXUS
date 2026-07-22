package cn.cccs7.cynexus.ai.infra.agent.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ClickHouseDailyBarsAgentToolHandlerSecurityTest {

    @Test
    void rejectsCredentialBearingOrNonHttpClickHouseEndpoints() {
        assertThatIllegalArgumentException().isThrownBy(() -> handler("file:///etc/passwd"));
        assertThatIllegalArgumentException().isThrownBy(() -> handler("http://user:password@127.0.0.1:8123"));
        assertThatIllegalArgumentException().isThrownBy(() -> handler("http://127.0.0.1:8123/?password=secret"));
    }

    @Test
    void keepsTheClickHousePasswordOutOfTheRequestUri() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestUri = new AtomicReference<>();
        AtomicReference<String> requestKey = new AtomicReference<>();
        server.createContext("/", exchange -> {
            requestUri.set(exchange.getRequestURI().toString());
            requestKey.set(exchange.getRequestHeaders().getFirst("X-ClickHouse-Key"));
            byte[] body = "{\"tradeDate\":\"2026-07-22\",\"close\":1}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            ClickHouseDailyBarsAgentToolHandler tool = new ClickHouseDailyBarsAgentToolHandler(new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), "cynexus", "cynexus", "secret",
                    HttpClient.newHttpClient());
            var result = tool.execute(cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall.builder()
                    .toolCode("market.clickhouse_daily_bars").arguments(java.util.Map.of("symbol", "600000")).build(), null);

            assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
            assertThat(requestUri).hasValueSatisfying(uri -> assertThat(uri).doesNotContain("secret", "password"));
            assertThat(requestKey).hasValue("secret");
        } finally {
            server.stop(0);
        }
    }

    private ClickHouseDailyBarsAgentToolHandler handler(String endpoint) {
        return new ClickHouseDailyBarsAgentToolHandler(new ObjectMapper(), endpoint, "cynexus", "cynexus", "secret");
    }
}
