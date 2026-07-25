package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClickHouseAStockSkillDataClient {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clickHouseUrl;
    private final String clickHouseDatabase;
    private final String clickHouseUser;
    private final String clickHousePassword;

    public ClickHouseAStockSkillDataClient(
            @Value("${cynexus.market.clickhouse.url:http://127.0.0.1:28123}") String clickHouseUrl,
            @Value("${cynexus.market.clickhouse.database:cynexus}") String clickHouseDatabase,
            @Value("${cynexus.market.clickhouse.username:cynexus}") String clickHouseUser,
            @Value("${cynexus.market.clickhouse.password:}") String clickHousePassword
    ) {
        this.clickHouseUrl = clickHouseUrl;
        this.clickHouseDatabase = clickHouseDatabase;
        this.clickHouseUser = clickHouseUser;
        this.clickHousePassword = clickHousePassword;
    }

    public TableStats stats(String databaseName, String tableName, String dateColumn) {
        String latestExpr = dateColumn == null || dateColumn.isBlank()
                ? "NULL"
                : "toString(max(" + quote(dateColumn) + "))";
        String sql = "SELECT count() AS rowCount, " + latestExpr + " AS latestAt FROM "
                + qualified(databaseName, tableName) + " FORMAT JSONEachRow";
        Map<String, Object> row = query(sql).stream().findFirst().orElse(Map.of());
        return new TableStats(number(row.get("rowCount")), string(row.get("latestAt")));
    }

    public List<Map<String, Object>> rows(String databaseName,
                                          String tableName,
                                          List<String> columns,
                                          String sourceColumn,
                                          String codeColumn,
                                          String sortClause,
                                          int limit,
                                          String code,
                                          String source) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectColumns(columns))
                .append(" FROM ").append(qualified(databaseName, tableName))
                .append(" WHERE 1 = 1");
        if (sourceColumn != null && source != null && !source.isBlank()) {
            sql.append(" AND ").append(quote(sourceColumn)).append(" = '").append(escapeSql(source.trim())).append("'");
        }
        if (codeColumn != null && code != null && !code.isBlank()) {
            sql.append(" AND ").append(quote(codeColumn)).append(" = '").append(escapeSql(code.trim())).append("'");
        }
        sql.append(" ORDER BY ").append(sortClause)
                .append(" LIMIT ").append(Math.max(1, Math.min(limit, 500)))
                .append(" FORMAT JSONEachRow");
        return query(sql.toString());
    }

    private String selectColumns(List<String> columns) {
        return columns.stream().map(this::quote).reduce((left, right) -> left + ", " + right).orElse("*");
    }

    private List<Map<String, Object>> query(String sql) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(clickHouseEndpoint()))
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_TEXT_PLAIN_UTF8)
                    .POST(HttpRequest.BodyPublishers.ofString(sql, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("ClickHouse query failed: HTTP " + response.statusCode() + " " + response.body());
            }
            return parseJsonEachRow(response.body());
        } catch (IOException ex) {
            throw new IllegalStateException("ClickHouse query failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ClickHouse query interrupted", ex);
        }
    }

    private List<Map<String, Object>> parseJsonEachRow(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        return body.lines()
                .filter(line -> !line.isBlank())
                .map(this::readRow)
                .toList();
    }

    private Map<String, Object> readRow(String line) {
        try {
            return objectMapper.readValue(line, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("ClickHouse row parse failed", ex);
        }
    }

    private String clickHouseEndpoint() {
        return clickHouseUrl
                + "/?database=" + urlEncode(clickHouseDatabase)
                + "&user=" + urlEncode(clickHouseUser)
                + "&password=" + urlEncode(clickHousePassword);
    }

    private String qualified(String databaseName, String tableName) {
        return quote(databaseName) + "." + quote(tableName);
    }

    private String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "''");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    public record TableStats(Long rowCount, String latestAt) {
    }
}
