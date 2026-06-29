package cn.cccs7.cynexus.market.infra.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ClickHouseStockChartClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clickHouseUrl;
    private final String clickHouseDatabase;
    private final String clickHouseUser;
    private final String clickHousePassword;
    private final String latestMinuteDateSql;
    private final String listMinuteBarsSql;
    private final String listTimeshareSql;

    public ClickHouseStockChartClient(
            @Value("${cynexus.market.clickhouse.url:http://127.0.0.1:28123}") String clickHouseUrl,
            @Value("${cynexus.market.clickhouse.database:cynexus}") String clickHouseDatabase,
            @Value("${cynexus.market.clickhouse.username:cynexus}") String clickHouseUser,
            @Value("${cynexus.market.clickhouse.password:}") String clickHousePassword
    ) {
        this.clickHouseUrl = clickHouseUrl;
        this.clickHouseDatabase = clickHouseDatabase;
        this.clickHouseUser = clickHouseUser;
        this.clickHousePassword = clickHousePassword;
        this.latestMinuteDateSql = loadSql("clickhouse/stock-chart/find-latest-minute-date.sql");
        this.listMinuteBarsSql = loadSql("clickhouse/stock-chart/list-minute-bars.sql");
        this.listTimeshareSql = loadSql("clickhouse/stock-chart/list-timeshare.sql");
    }

    public Optional<LocalDate> findLatestMinuteDate(String market, String symbol, LocalDate requestedDate) {
        if (market == null || symbol == null || requestedDate == null) {
            return Optional.empty();
        }
        List<LatestTradeDateRow> rows = query(latestMinuteDateSql.formatted(market, symbol, requestedDate), LatestTradeDateRow.class);
        if (rows.isEmpty() || rows.getFirst().latestTradeDate() == null || rows.getFirst().latestTradeDate().isBlank()) {
            return Optional.empty();
        }
        String value = rows.getFirst().latestTradeDate();
        if ("1970-01-01".equals(value)) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.parse(value));
    }

    public List<MinuteBar> listMinuteBars(String market, String symbol, LocalDate tradeDate, int periodMinutes) {
        if (market == null || symbol == null || tradeDate == null || periodMinutes <= 0) {
            return List.of();
        }
        return query(listMinuteBarsSql.formatted(periodMinutes, market, symbol, tradeDate), MinuteBarRow.class).stream()
                .map(row -> MinuteBar.builder()
                        .timestamp(row.timestamp())
                        .tradeDate(row.tradeDate() == null ? tradeDate : LocalDate.parse(row.tradeDate()))
                        .open(row.open())
                        .high(row.high())
                        .low(row.low())
                        .close(row.close())
                        .volume(row.volume())
                        .turnover(row.turnover())
                        .build())
                .toList();
    }

    public List<TimesharePoint> listTimeshare(String market, String symbol, LocalDate tradeDate) {
        if (market == null || symbol == null || tradeDate == null) {
            return List.of();
        }
        return query(listTimeshareSql.formatted(market, symbol, tradeDate), TimeshareRow.class).stream()
                .map(row -> TimesharePoint.builder()
                        .timestamp(row.timestamp())
                        .tradeDate(row.tradeDate() == null ? tradeDate : LocalDate.parse(row.tradeDate()))
                        .timeLabel(row.timeLabel())
                        .price(row.price())
                        .open(row.open())
                        .avgPrice(row.avgPrice())
                        .volume(row.volume())
                        .turnover(row.turnover())
                        .build())
                .toList();
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
        List<T> rows = new ArrayList<>();
        for (String line : body.split("\\R")) {
            if (!line.isBlank()) {
                rows.add(objectMapper.readValue(line, rowType));
            }
        }
        return rows;
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

    private String loadSql(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("ClickHouse SQL template not found: " + path, ex);
        }
    }

    @Data
    @Builder
    public static class MinuteBar {
        private Long timestamp;
        private LocalDate tradeDate;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
        private BigDecimal turnover;
    }

    @Data
    @Builder
    public static class TimesharePoint {
        private Long timestamp;
        private LocalDate tradeDate;
        private String timeLabel;
        private BigDecimal price;
        private BigDecimal open;
        private BigDecimal avgPrice;
        private BigDecimal volume;
        private BigDecimal turnover;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LatestTradeDateRow(String latestTradeDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MinuteBarRow(
            Long timestamp,
            String tradeDate,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            BigDecimal turnover
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TimeshareRow(
            Long timestamp,
            String tradeDate,
            String timeLabel,
            BigDecimal price,
            BigDecimal open,
            BigDecimal avgPrice,
            BigDecimal volume,
            BigDecimal turnover
    ) {
    }
}
