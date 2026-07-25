package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
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
import java.util.Locale;
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
    private final String latestAuctionDateSql;
    private final String listAuctionPointsSql;

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
        this.latestAuctionDateSql = loadSql("clickhouse/stock-chart/find-latest-auction-date.sql");
        this.listAuctionPointsSql = loadSql("clickhouse/stock-chart/list-auction-points.sql");
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

    public Optional<LocalDate> findLatestAuctionDate(String market, String symbol, LocalDate requestedDate) {
        if (market == null || symbol == null || requestedDate == null) {
            return Optional.empty();
        }
        List<LatestTradeDateRow> rows = query(latestAuctionDateSql.formatted(market, symbol, requestedDate), LatestTradeDateRow.class);
        if (rows.isEmpty() || rows.getFirst().latestTradeDate() == null || rows.getFirst().latestTradeDate().isBlank()) {
            return Optional.empty();
        }
        String value = rows.getFirst().latestTradeDate();
        if ("1970-01-01".equals(value)) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.parse(value));
    }

    public List<LocalDate> listAuctionDates(String market, String symbol, int limit) {
        if (market == null || symbol == null) {
            return List.of();
        }
        int resolvedLimit = Math.max(1, Math.min(limit, 60));
        String sql = """
                SELECT toString(trade_date) AS tradeDate
                  FROM ch_ods_stock_auction_point FINAL
                 WHERE market = '%s'
                   AND symbol = '%s'
                 GROUP BY trade_date
                 ORDER BY trade_date DESC
                 LIMIT %d
                FORMAT JSONEachRow
                """.formatted(
                escapeSql(market.toUpperCase(Locale.ROOT)),
                escapeSql(symbol),
                resolvedLimit
        );
        return query(sql, TradeDateRow.class).stream()
                .map(TradeDateRow::tradeDate)
                .filter(value -> value != null && !value.isBlank() && !"1970-01-01".equals(value))
                .map(LocalDate::parse)
                .toList();
    }

    public List<AuctionPoint> listAuctionPoints(String market, String symbol, LocalDate tradeDate) {
        if (market == null || symbol == null || tradeDate == null) {
            return List.of();
        }
        return query(listAuctionPointsSql.formatted(market, symbol, tradeDate), AuctionPointRow.class).stream()
                .map(row -> AuctionPoint.builder()
                        .timestamp(row.timestamp())
                        .tradeDate(row.tradeDate() == null ? tradeDate : LocalDate.parse(row.tradeDate()))
                        .timeLabel(row.timeLabel())
                        .phase(row.phase())
                        .price(row.price())
                        .matchedVolume(row.matchedVolume())
                        .matchedAmount(row.matchedAmount())
                        .unmatchedBuyVolume(row.unmatchedBuyVolume())
                        .unmatchedSellVolume(row.unmatchedSellVolume())
                        .build())
                .toList();
    }

    public List<DailyQuote> listLatestDailyQuotes(List<MarketSymbol> symbols) {
        return listLatestDailyQuotesOnOrBefore(symbols, null);
    }

    public List<DailyQuote> listLatestDailyQuotesOnOrBefore(List<MarketSymbol> symbols, LocalDate maxTradeDate) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }
        String condition = symbols.stream()
                .filter(item -> item.market() != null && item.symbol() != null)
                .map(item -> "(market = '%s' AND symbol = '%s')".formatted(
                        escapeSql(item.market().toUpperCase(Locale.ROOT)),
                        escapeSql(item.symbol())
                ))
                .distinct()
                .reduce((left, right) -> left + " OR " + right)
                .orElse("");
        if (condition.isBlank()) {
            return List.of();
        }
        String dateCondition = maxTradeDate == null ? "" : "\n                      AND trade_date <= toDate('" + maxTradeDate + "')";
        String sql = """
                WITH daily AS
                (
                    SELECT
                        market,
                        symbol,
                        trade_date,
                        max(open) AS open,
                        max(high) AS high,
                        min(low) AS low,
                        max(close) AS close,
                        max(volume) AS volume,
                        max(amount) AS amount
                    FROM daily_raw
                    WHERE (%s)%s
                    GROUP BY market, symbol, trade_date
                ),
                ranked AS
                (
                    SELECT
                        *,
                        row_number() OVER (PARTITION BY market, symbol ORDER BY trade_date DESC) AS rn
                    FROM daily
                )
                SELECT
                    cur.market AS market,
                    cur.symbol AS symbol,
                    toString(cur.trade_date) AS tradeDate,
                    cur.open AS open,
                    cur.high AS high,
                    cur.low AS low,
                    cur.close AS close,
                    prev.close AS preClose,
                    cur.close - prev.close AS change,
                    if(prev.close = 0 OR isNull(prev.close), NULL, (cur.close - prev.close) * 100 / prev.close) AS changePct,
                    cur.volume AS volume,
                    cur.amount AS amount,
                    if(prev.close = 0 OR isNull(prev.close), NULL, (cur.high - cur.low) * 100 / prev.close) AS amplitude,
                    if(ret.close5d = 0 OR isNull(ret.close5d), NULL, (cur.close - ret.close5d) * 100 / ret.close5d) AS returnPct5d,
                    if(ret.close10d = 0 OR isNull(ret.close10d), NULL, (cur.close - ret.close10d) * 100 / ret.close10d) AS returnPct10d,
                    if(ret.close30d = 0 OR isNull(ret.close30d), NULL, (cur.close - ret.close30d) * 100 / ret.close30d) AS returnPct30d,
                    if(ret.close60d = 0 OR isNull(ret.close60d), NULL, (cur.close - ret.close60d) * 100 / ret.close60d) AS returnPct60d,
                    if(ret.close252d = 0 OR isNull(ret.close252d), NULL, (cur.close - ret.close252d) * 100 / ret.close252d) AS returnPct252d
                FROM ranked cur
                LEFT JOIN ranked prev
                  ON cur.market = prev.market
                 AND cur.symbol = prev.symbol
                 AND prev.rn = 2
                LEFT JOIN
                (
                    SELECT
                        market,
                        symbol,
                        maxIf(close, rn = 6) AS close5d,
                        maxIf(close, rn = 11) AS close10d,
                        maxIf(close, rn = 31) AS close30d,
                        maxIf(close, rn = 61) AS close60d,
                        maxIf(close, rn = 253) AS close252d
                    FROM ranked
                    GROUP BY market, symbol
                ) ret
                  ON cur.market = ret.market
                 AND cur.symbol = ret.symbol
                WHERE cur.rn = 1
                FORMAT JSONEachRow
                """.formatted(condition, dateCondition);
        return query(sql, DailyQuoteRow.class).stream()
                .map(row -> DailyQuote.builder()
                        .market(row.market())
                        .symbol(row.symbol())
                        .tradeDate(row.tradeDate() == null ? null : LocalDate.parse(row.tradeDate()))
                        .open(row.open())
                        .high(row.high())
                        .low(row.low())
                        .close(row.close())
                        .preClose(row.preClose())
                        .change(row.change())
                        .changePct(row.changePct())
                        .volume(row.volume())
                        .amount(row.amount())
                        .amplitude(row.amplitude())
                        .returnPct5d(row.returnPct5d())
                        .returnPct10d(row.returnPct10d())
                        .returnPct30d(row.returnPct30d())
                        .returnPct60d(row.returnPct60d())
                        .returnPct252d(row.returnPct252d())
                        .build())
                .toList();
    }

    public List<MarketSymbol> listAvailableIndustryApplicationSymbols() {
        String sql = """
                SELECT
                    market,
                    symbol
                FROM
                (
                    SELECT
                        market,
                        symbol,
                        max(trade_date) AS latestTradeDate
                    FROM daily_raw
                    WHERE
                        (
                            market = 'SH'
                            AND
                            (
                                symbol LIKE '8803%%'
                                OR symbol LIKE '8804%%'
                                OR symbol LIKE '8805%%'
                                OR symbol IN ('000001', '000016', '000300', '000688', '000852', '000905', '000932')
                            )
                        )
                        OR (market = 'SZ' AND symbol IN ('399001', '399006'))
                    GROUP BY market, symbol
                )
                ORDER BY market, symbol
                FORMAT JSONEachRow
                """;
        return query(sql, MarketSymbolRow.class).stream()
                .map(row -> new MarketSymbol(row.market(), row.symbol()))
                .toList();
    }

    public List<DailyBar> listDailyBars(String market, String symbol, LocalDate startDate, LocalDate endDate) {
        if (market == null || symbol == null) {
            return List.of();
        }
        List<String> where = new ArrayList<>();
        where.add("market = '" + escapeSql(market.toUpperCase(Locale.ROOT)) + "'");
        where.add("symbol = '" + escapeSql(symbol) + "'");
        if (startDate != null) {
            where.add("trade_date >= toDate('" + startDate + "')");
        }
        if (endDate != null) {
            where.add("trade_date <= toDate('" + endDate + "')");
        }
        String sql = """
                WITH daily AS
                (
                    SELECT
                        trade_date,
                        max(open) AS open,
                        max(high) AS high,
                        min(low) AS low,
                        max(close) AS close,
                        max(volume) AS volume,
                        max(amount) AS amount
                    FROM daily_raw
                    WHERE %s
                    GROUP BY trade_date
                )
                SELECT
                    toString(trade_date) AS tradeDate,
                    open,
                    high,
                    low,
                    close,
                    volume,
                    amount
                FROM daily
                ORDER BY trade_date ASC
                FORMAT JSONEachRow
                """.formatted(String.join(" AND ", where));
        return query(sql, DailyBarRow.class).stream()
                .map(row -> DailyBar.builder()
                        .tradeDate(row.tradeDate() == null ? null : LocalDate.parse(row.tradeDate()))
                        .open(row.open())
                        .high(row.high())
                        .low(row.low())
                        .close(row.close())
                        .volume(row.volume())
                        .amount(row.amount())
                .build())
                .toList();
    }

    public List<DailyBar> listQfqDailyBars(String symbol, int limit) {
        if (symbol == null || symbol.isBlank()) {
            return List.of();
        }
        int resolvedLimit = Math.max(limit, 1);
        String sql = """
                SELECT
                    toString(trade_date) AS tradeDate,
                    open,
                    high,
                    low,
                    close,
                    toDecimal64(volume, 0) AS volume,
                    amount
                FROM daily_qfq
                WHERE code = '%s'
                ORDER BY trade_date DESC
                LIMIT %d
                FORMAT JSONEachRow
                """.formatted(escapeSql(symbol), resolvedLimit);
        List<DailyBar> rows = query(sql, DailyBarRow.class).stream()
                .map(row -> DailyBar.builder()
                        .tradeDate(row.tradeDate() == null ? null : LocalDate.parse(row.tradeDate()))
                        .open(row.open())
                        .high(row.high())
                        .low(row.low())
                        .close(row.close())
                        .volume(row.volume())
                        .amount(row.amount())
                        .build())
                .toList();
        List<DailyBar> ordered = new ArrayList<>(rows);
        ordered.sort((left, right) -> {
            if (left.getTradeDate() == null && right.getTradeDate() == null) {
                return 0;
            }
            if (left.getTradeDate() == null) {
                return -1;
            }
            if (right.getTradeDate() == null) {
                return 1;
            }
            return left.getTradeDate().compareTo(right.getTradeDate());
        });
        return ordered;
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

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "''");
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

    @Data
    @Builder
    public static class AuctionPoint {
        private Long timestamp;
        private LocalDate tradeDate;
        private String timeLabel;
        private String phase;
        private BigDecimal price;
        private BigDecimal matchedVolume;
        private BigDecimal matchedAmount;
        private BigDecimal unmatchedBuyVolume;
        private BigDecimal unmatchedSellVolume;
    }

    public record MarketSymbol(String market, String symbol) {
    }

    @Data
    @Builder
    public static class DailyQuote {
        private String market;
        private String symbol;
        private LocalDate tradeDate;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal preClose;
        private BigDecimal change;
        private BigDecimal changePct;
        private BigDecimal volume;
        private BigDecimal amount;
        private BigDecimal amplitude;
        private BigDecimal returnPct5d;
        private BigDecimal returnPct10d;
        private BigDecimal returnPct30d;
        private BigDecimal returnPct60d;
        private BigDecimal returnPct252d;
    }

    @Data
    @Builder
    public static class DailyBar {
        private LocalDate tradeDate;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
        private BigDecimal amount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LatestTradeDateRow(String latestTradeDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TradeDateRow(String tradeDate) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuctionPointRow(
            Long timestamp,
            String tradeDate,
            String timeLabel,
            String phase,
            BigDecimal price,
            BigDecimal matchedVolume,
            BigDecimal matchedAmount,
            BigDecimal unmatchedBuyVolume,
            BigDecimal unmatchedSellVolume
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DailyQuoteRow(
            String market,
            String symbol,
            String tradeDate,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal preClose,
            BigDecimal change,
            BigDecimal changePct,
            BigDecimal volume,
            BigDecimal amount,
            BigDecimal amplitude,
            BigDecimal returnPct5d,
            BigDecimal returnPct10d,
            BigDecimal returnPct30d,
            BigDecimal returnPct60d,
            BigDecimal returnPct252d
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DailyBarRow(
            String tradeDate,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            BigDecimal amount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MarketSymbolRow(
            String market,
            String symbol
    ) {
    }
}
