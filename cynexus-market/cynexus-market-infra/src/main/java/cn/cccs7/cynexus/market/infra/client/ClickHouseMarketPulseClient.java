package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.market.domain.entity.IndexMinuteBarBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ClickHouseMarketPulseClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clickHouseUrl;
    private final String clickHouseDatabase;
    private final String clickHouseUser;
    private final String clickHousePassword;
    private final String latestIndexMinuteDateSql;
    private final String listIndexMinuteBarsSql;
    private final String indexPreviousCloseSql;
    private final String listDailyBreadthSql;

    public ClickHouseMarketPulseClient(
            @Value("${cynexus.market.clickhouse.url:http://127.0.0.1:28123}") String clickHouseUrl,
            @Value("${cynexus.market.clickhouse.database:cynexus}") String clickHouseDatabase,
            @Value("${cynexus.market.clickhouse.username:cynexus}") String clickHouseUser,
            @Value("${cynexus.market.clickhouse.password:}") String clickHousePassword
    ) {
        this.clickHouseUrl = clickHouseUrl;
        this.clickHouseDatabase = clickHouseDatabase;
        this.clickHouseUser = clickHouseUser;
        this.clickHousePassword = clickHousePassword;
        this.latestIndexMinuteDateSql = loadSql("clickhouse/market-pulse/find-latest-index-minute-date.sql");
        this.listIndexMinuteBarsSql = loadSql("clickhouse/market-pulse/list-index-minute-bars.sql");
        this.indexPreviousCloseSql = loadSql("clickhouse/market-pulse/find-index-previous-close.sql");
        this.listDailyBreadthSql = loadSql("clickhouse/market-pulse/list-daily-breadth.sql");
    }

    public Optional<LocalDate> findLatestIndexMinuteDate(String code, LocalDate requestedDate) {
        IndexSymbol indexSymbol = indexSymbol(code);
        if (indexSymbol == null || requestedDate == null) {
            return Optional.empty();
        }
        String sql = latestIndexMinuteDateSql.formatted(indexSymbol.market(), indexSymbol.symbol(), requestedDate);
        List<LatestTradeDateRow> rows = query(sql, LatestTradeDateRow.class);
        if (rows.isEmpty() || rows.getFirst().latestTradeDate() == null) {
            return Optional.empty();
        }
        String value = rows.getFirst().latestTradeDate();
        if (value.isBlank() || "1970-01-01".equals(value)) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.parse(value));
    }

    public List<IndexMinuteBarBO> listIndexMinuteBars(String code, LocalDate tradeDate) {
        IndexSymbol indexSymbol = indexSymbol(code);
        if (indexSymbol == null || tradeDate == null) {
            return List.of();
        }
        String sql = listIndexMinuteBarsSql.formatted(indexSymbol.market(), indexSymbol.symbol(), tradeDate);
        return query(sql, IndexMinuteBarRow.class).stream()
                .map(row -> IndexMinuteBarBO.builder()
                        .time(row.time())
                        .timestamp(row.timestamp())
                        .open(row.open())
                        .high(row.high())
                        .low(row.low())
                        .close(row.close())
                        .volume(row.volume())
                        .turnover(row.turnover())
                        .build())
                .toList();
    }

    public Optional<BigDecimal> findIndexPreviousClose(String code, LocalDate tradeDate) {
        IndexSymbol indexSymbol = indexSymbol(code);
        if (indexSymbol == null || tradeDate == null) {
            return Optional.empty();
        }
        String indexCode = indexSymbol.market().toLowerCase() + indexSymbol.symbol();
        String sql = indexPreviousCloseSql.formatted(indexCode, tradeDate);
        return query(sql, PreviousCloseRow.class).stream()
                .map(PreviousCloseRow::close)
                .findFirst();
    }

    public Optional<MarketOverviewBO> findDailyBreadth(LocalDate tradeDate) {
        if (tradeDate == null) {
            return Optional.empty();
        }
        List<MarketOverviewBO> rows = listDailyBreadth(tradeDate, tradeDate);
        return rows.stream().findFirst();
    }

    public List<MarketOverviewBO> listDailyBreadth(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }
        String sql = listDailyBreadthSql.formatted(startDate, endDate, startDate, endDate);
        try {
            return query(sql, DailyBreadthRow.class).stream()
                    .map(row -> MarketOverviewBO.builder()
                            .tradeDate(LocalDate.parse(row.tradeDate()))
                            .stockCount(row.stockCount())
                            .risingCount(row.risingCount())
                            .fallingCount(row.fallingCount())
                            .flatCount(row.flatCount())
                            .limitUpCount(row.limitUpCount())
                            .limitDownCount(row.limitDownCount())
                            .source("clickhouse.daily_qfq")
                            .build())
                    .toList();
        } catch (IllegalStateException ex) {
            return List.of();
        }
    }

    private <T> List<T> query(String sql, Class<T> rowType) {
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

    private IndexSymbol indexSymbol(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.replaceAll("\\D", "");
        return switch (normalized) {
            case "000001" -> new IndexSymbol("SH", "000001");
            case "399001" -> new IndexSymbol("SZ", "399001");
            case "399006" -> new IndexSymbol("SZ", "399006");
            default -> null;
        };
    }

    private record IndexSymbol(String market, String symbol) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LatestTradeDateRow(String latestTradeDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PreviousCloseRow(BigDecimal close) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record IndexMinuteBarRow(
            String time,
            Long timestamp,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            BigDecimal turnover
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DailyBreadthRow(
            String tradeDate,
            Integer stockCount,
            Integer risingCount,
            Integer fallingCount,
            Integer flatCount,
            Integer limitUpCount,
            Integer limitDownCount
    ) {
    }
}
