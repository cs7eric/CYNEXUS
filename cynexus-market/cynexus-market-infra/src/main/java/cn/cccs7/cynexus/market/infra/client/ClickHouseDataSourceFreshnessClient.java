package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClickHouseDataSourceFreshnessClient {

    private static final ZoneId STATUS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<String> DATE_COLUMN_PRIORITY = List.of(
            "trade_date",
            "trading_date",
            "snapshot_date",
            "capture_date",
            "data_date",
            "stat_date",
            "report_date",
            "target_date",
            "biz_date",
            "last_trade_date",
            "last_seen",
            "ex_date",
            "date",
            "dt",
            "trade_time",
            "quote_time",
            "updated_at_result",
            "captured_at",
            "ingested_at",
            "fetched_at",
            "source_updated_at",
            "updated_at",
            "created_at"
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clickHouseUrl;
    private final String clickHouseDatabase;
    private final String clickHouseUser;
    private final String clickHousePassword;

    public ClickHouseDataSourceFreshnessClient(
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

    public List<DataSourceFreshnessBO.ItemBO> snapshot() {
        try {
            return query("""
                            SELECT
                                database AS databaseName,
                                name AS tableName
                            FROM system.tables
                            WHERE database = currentDatabase()
                              AND engine NOT IN ('View', 'MaterializedView')
                            ORDER BY name
                            FORMAT JSONEachRow
                            """, TableRow.class)
                    .stream()
                    .map(this::inspectTable)
                    .toList();
        } catch (Exception ex) {
            return List.of(errorItem(null, ex));
        }
    }

    private DataSourceFreshnessBO.ItemBO inspectTable(TableRow table) {
        try {
            List<ColumnRow> columns = query("""
                            SELECT
                                name,
                                type
                            FROM system.columns
                            WHERE database = '%s'
                              AND table = '%s'
                            FORMAT JSONEachRow
                            """.formatted(escapeSql(table.databaseName()), escapeSql(table.tableName())), ColumnRow.class);
            String effectiveDateColumn = chooseEffectiveDateColumn(columns).orElse(null);
            String latestUpdatedColumn = columns.stream().anyMatch(column -> "updated_at".equals(column.name())) ? "updated_at" : null;
            if (effectiveDateColumn == null) {
                Long rowCount = rowCount(table);
                return base(table)
                        .rowCount(rowCount)
                        .status(rowCount != null && rowCount == 0 ? "empty" : "untracked")
                        .build();
            }
            MetricRow metrics = query(metricsSql(table, effectiveDateColumn, latestUpdatedColumn), MetricRow.class)
                    .stream()
                    .findFirst()
                    .orElse(new MetricRow(0L, null, null));
            long rowCount = metrics.rowCount() == null ? 0L : metrics.rowCount();
            Long lagDays = lagDays(metrics.latestEffectiveDate()).orElse(null);
            return base(table)
                    .effectiveDateColumn(effectiveDateColumn)
                    .latestEffectiveDate(rowCount == 0 ? null : metrics.latestEffectiveDate())
                    .latestUpdatedAt(metrics.latestUpdatedAt())
                    .rowCount(rowCount)
                    .lagDays(rowCount == 0 ? null : lagDays)
                    .status(status(rowCount, lagDays))
                    .build();
        } catch (Exception ex) {
            return errorItem(table, ex);
        }
    }

    private Optional<String> chooseEffectiveDateColumn(List<ColumnRow> columns) {
        return columns.stream()
                .filter(column -> isDateLikeType(column.type()))
                .min(Comparator.comparingInt(column -> priority(column.name())))
                .map(ColumnRow::name);
    }

    private boolean isDateLikeType(String type) {
        String value = type == null ? "" : type.toLowerCase(Locale.ROOT);
        return value.startsWith("date") || value.startsWith("datetime");
    }

    private int priority(String columnName) {
        int index = DATE_COLUMN_PRIORITY.indexOf(columnName);
        if (index >= 0) {
            return index;
        }
        if (columnName != null && columnName.endsWith("_date")) {
            return DATE_COLUMN_PRIORITY.size();
        }
        if (columnName != null && columnName.endsWith("_at")) {
            return DATE_COLUMN_PRIORITY.size() + 10;
        }
        return DATE_COLUMN_PRIORITY.size() + 20;
    }

    private Long rowCount(TableRow table) {
        return query("SELECT count() AS rowCount FROM " + qualified(table) + " FORMAT JSONEachRow", MetricRow.class)
                .stream()
                .findFirst()
                .map(MetricRow::rowCount)
                .orElse(0L);
    }

    private String metricsSql(TableRow table, String effectiveDateColumn, String latestUpdatedColumn) {
        String latestUpdatedExpr = latestUpdatedColumn == null ? "NULL" : "toString(max(" + quote(latestUpdatedColumn) + "))";
        return "SELECT count() AS rowCount, toString(max(" + quote(effectiveDateColumn)
                + ")) AS latestEffectiveDate, " + latestUpdatedExpr
                + " AS latestUpdatedAt FROM " + qualified(table) + " FORMAT JSONEachRow";
    }

    private DataSourceFreshnessBO.ItemBO.ItemBOBuilder base(TableRow table) {
        return DataSourceFreshnessBO.ItemBO.builder()
                .sourceType("clickhouse")
                .databaseName(table.databaseName())
                .schemaName(table.databaseName())
                .tableName(table.tableName());
    }

    private DataSourceFreshnessBO.ItemBO errorItem(TableRow table, Exception ex) {
        return DataSourceFreshnessBO.ItemBO.builder()
                .sourceType("clickhouse")
                .databaseName(table == null ? clickHouseDatabase : table.databaseName())
                .schemaName(table == null ? clickHouseDatabase : table.databaseName())
                .tableName(table == null ? "(connection)" : table.tableName())
                .status("error")
                .error(ex.getMessage())
                .build();
    }

    private String qualified(TableRow table) {
        return quote(table.databaseName()) + "." + quote(table.tableName());
    }

    private String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private <T> List<T> query(String sql, Class<T> rowType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(clickHouseEndpoint()))
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(sql, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("ClickHouse query failed: HTTP " + response.statusCode() + " " + response.body());
            }
            return parseJsonEachRow(response.body(), rowType);
        } catch (IOException ex) {
            throw new IllegalStateException("ClickHouse query failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ClickHouse query interrupted", ex);
        }
    }

    private <T> List<T> parseJsonEachRow(String body, Class<T> rowType) throws JsonProcessingException {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        return body.lines()
                .filter(line -> !line.isBlank())
                .map(line -> readRow(line, rowType))
                .toList();
    }

    private <T> T readRow(String line, Class<T> rowType) {
        try {
            return objectMapper.readValue(line, rowType);
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

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "''");
    }

    private Optional<Long> lagDays(String value) {
        if (value == null || value.length() < 10 || "1970-01-01".equals(value.substring(0, 10))) {
            return Optional.empty();
        }
        try {
            LocalDate date = LocalDate.parse(value.substring(0, 10));
            return Optional.of(ChronoUnit.DAYS.between(date, LocalDate.now(STATUS_ZONE)));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String status(long rowCount, Long lagDays) {
        if (rowCount == 0) {
            return "empty";
        }
        if (lagDays == null) {
            return "untracked";
        }
        if (lagDays <= 3) {
            return "fresh";
        }
        if (lagDays <= 7) {
            return "lagging";
        }
        return "stale";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TableRow(String databaseName, String tableName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ColumnRow(String name, String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MetricRow(Long rowCount, String latestEffectiveDate, String latestUpdatedAt) {
    }
}
