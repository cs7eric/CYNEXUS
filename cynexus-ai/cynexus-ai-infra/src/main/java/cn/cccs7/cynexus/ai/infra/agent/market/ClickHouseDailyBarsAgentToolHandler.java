package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClickHouseDailyBarsAgentToolHandler implements AgentToolHandler {
    private static final String CODE = "market.clickhouse_daily_bars";
    private static final String SOURCE = "ClickHouse daily_raw";
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder().code(CODE)
            .name("ClickHouse daily bars").module("market")
            .description("Read bounded historical daily OHLCV bars from the approved ClickHouse market dataset.")
            .inputSchemaJson("{\"type\":\"object\",\"required\":[\"symbol\"],\"properties\":{\"market\":{\"type\":\"string\",\"enum\":[\"SH\",\"SZ\",\"BJ\"]},\"symbol\":{\"type\":\"string\"},\"start\":{\"type\":\"string\",\"format\":\"date\"},\"end\":{\"type\":\"string\",\"format\":\"date\"},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":300}}}")
            .outputSchemaJson("{\"type\":\"array\"}").readOnly(true).requiresConfirmation(false)
            .freshness("historical daily data").source(SOURCE).sourceUrl("/stock-overview").build();

    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper objectMapper;
    private final String url;
    private final String database;
    private final String username;
    private final String password;

    public ClickHouseDailyBarsAgentToolHandler(ObjectMapper objectMapper,
                                               @Value("${cynexus.market.clickhouse.url:http://127.0.0.1:28123}") String url,
                                               @Value("${cynexus.market.clickhouse.database:cynexus}") String database,
                                               @Value("${cynexus.market.clickhouse.username:cynexus}") String username,
                                               @Value("${cynexus.market.clickhouse.password:}") String password) {
        this.objectMapper = objectMapper;
        this.url = url;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String symbol = text(call, "symbol");
        if (symbol.isBlank()) return AgentToolResult.failure(CODE, "symbol is required.", SOURCE, "ARGUMENT_REQUIRED");
        String market = text(call, "market");
        if (market.isBlank()) market = symbol.startsWith("6") ? "SH" : "SZ";
        LocalDate start = date(call, "start");
        LocalDate end = date(call, "end");
        int limit = number(call, "limit", 60);
        StringBuilder where = new StringBuilder("market = '").append(escape(market)).append("' AND symbol = '").append(escape(symbol)).append("'");
        if (start != null) where.append(" AND trade_date >= toDate('").append(start).append("')");
        if (end != null) where.append(" AND trade_date <= toDate('").append(end).append("')");
        String sql = "SELECT toString(trade_date) AS tradeDate, open, high, low, close, volume, amount FROM daily_raw WHERE "
                + where + " ORDER BY trade_date DESC LIMIT " + limit + " FORMAT JSONEachRow";
        try {
            List<Map<String, Object>> rows = new ArrayList<>();
            String body = query(sql);
            for (String line : body.split("\\R")) {
                if (!line.isBlank()) rows.add(objectMapper.convertValue(objectMapper.readTree(line), Map.class));
            }
            return AgentToolResult.success(CODE, objectMapper.writeValueAsString(rows),
                    "Read " + rows.size() + " ClickHouse daily bars.", end == null ? null : end.toString(), SOURCE, List.of());
        } catch (Exception exception) {
            return AgentToolResult.failure(CODE, "ClickHouse daily bars query failed.", SOURCE, "QUERY_FAILED");
        }
    }

    private String query(String sql) throws IOException, InterruptedException {
        String endpoint = url.replaceAll("/+$", "") + "/?database=" + database + "&user=" + username + "&password=" + password;
        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(URI.create(endpoint))
                .header("Content-Type", "text/plain; charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(sql)).build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("ClickHouse HTTP " + response.statusCode());
        return response.body();
    }

    private String text(AgentToolCall call, String key) {
        Object value = call.getArguments() == null ? null : call.getArguments().get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private LocalDate date(AgentToolCall call, String key) {
        String value = text(call, key);
        return value.isBlank() ? null : LocalDate.parse(value);
    }

    private int number(AgentToolCall call, String key, int fallback) {
        String value = text(call, key);
        return value.isBlank() ? fallback : Math.max(1, Math.min(300, Integer.parseInt(value)));
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace("'", "''"); }
}
