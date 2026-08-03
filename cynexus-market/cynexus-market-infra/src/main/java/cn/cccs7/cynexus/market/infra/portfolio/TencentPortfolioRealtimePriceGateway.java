package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPositionBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioRealtimePriceGateway;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Tencent quote adapter used for live portfolio mark-to-market values. */
@Component
public class TencentPortfolioRealtimePriceGateway implements PortfolioRealtimePriceGateway {
    private static final Logger log = LoggerFactory.getLogger(TencentPortfolioRealtimePriceGateway.class);
    private static final Pattern QUOTE = Pattern.compile("v_(sh|sz)(\\d{6})=\\\"([^\\\"]*)\\\";");
    private final HttpClient httpClient;
    private final String endpoint;
    private final Duration timeout;

    public TencentPortfolioRealtimePriceGateway(
            @Value("${cynexus.market.realtime-quote.base-url:https://qt.gtimg.cn/q=}") String baseUrl,
            @Value("${cynexus.market.realtime-quote.timeout-ms:2500}") long timeoutMs) {
        this.endpoint = baseUrl.endsWith("=") ? baseUrl : baseUrl + "=";
        this.timeout = Duration.ofMillis(Math.max(timeoutMs, 500));
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    @Override
    public Map<String, BigDecimal> latestPrices(List<PortfolioPositionBO> positions) {
        if (positions == null || positions.isEmpty()) return Map.of();
        List<String> symbols = positions.stream()
                .filter(position -> position != null && position.quantity() != null && position.quantity().signum() > 0)
                .map(PortfolioPositionBO::symbol)
                .filter(symbol -> symbol != null && symbol.matches("\\d{6}"))
                .distinct()
                .toList();
        if (symbols.isEmpty()) return Map.of();
        String query = symbols.stream().map(symbol -> market(symbol) + symbol).reduce((left, right) -> left + "," + right).orElse("");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + query))
                    .timeout(timeout)
                    .header("Accept", "text/plain")
                    .header("User-Agent", "CynexusMarket/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Realtime portfolio quote source returned status={}", response.statusCode());
                return Map.of();
            }
            return parseResponse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Realtime portfolio quote request was interrupted", exception);
            return Map.of();
        } catch (Exception exception) {
            log.warn("Realtime portfolio quote request failed", exception);
            return Map.of();
        }
    }

    @Override
    public Map<String, LocalDate> latestPriceDates(List<PortfolioPositionBO> positions) {
        if (positions == null || positions.isEmpty()) return Map.of();
        List<String> symbols = positions.stream()
                .filter(position -> position != null && position.quantity() != null && position.quantity().signum() > 0)
                .map(PortfolioPositionBO::symbol)
                .filter(symbol -> symbol != null && symbol.matches("\\d{6}"))
                .distinct()
                .toList();
        if (symbols.isEmpty()) return Map.of();
        String query = symbols.stream().map(symbol -> market(symbol) + symbol).reduce((left, right) -> left + "," + right).orElse("");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + query))
                    .timeout(timeout)
                    .header("Accept", "text/plain")
                    .header("User-Agent", "CynexusMarket/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) return Map.of();
            return parseDateResponse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Map.of();
        } catch (Exception exception) {
            log.warn("Realtime portfolio quote date request failed", exception);
            return Map.of();
        }
    }

    @Override
    public Map<String, BigDecimal> previousClosePrices(List<PortfolioPositionBO> positions) {
        if (positions == null || positions.isEmpty()) return Map.of();
        List<String> symbols = positions.stream()
                .filter(position -> position != null && position.quantity() != null && position.quantity().signum() > 0)
                .map(PortfolioPositionBO::symbol)
                .filter(symbol -> symbol != null && symbol.matches("\\d{6}"))
                .distinct()
                .toList();
        if (symbols.isEmpty()) return Map.of();
        String query = symbols.stream().map(symbol -> market(symbol) + symbol).reduce((left, right) -> left + "," + right).orElse("");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + query))
                    .timeout(timeout)
                    .header("Accept", "text/plain")
                    .header("User-Agent", "CynexusMarket/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) return Map.of();
            return parsePreviousCloseResponse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Map.of();
        } catch (Exception exception) {
            log.warn("Previous-close portfolio quote request failed", exception);
            return Map.of();
        }
    }

    static Map<String, BigDecimal> parseResponse(String body) {
        if (body == null || body.isBlank()) return Map.of();
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        Matcher matcher = QUOTE.matcher(body);
        while (matcher.find()) {
            String[] fields = matcher.group(3).split("~", -1);
            if (fields.length <= 3 || fields[3].isBlank()) continue;
            try {
                BigDecimal price = new BigDecimal(fields[3]);
                if (price.signum() >= 0) prices.put(matcher.group(2), price);
            } catch (NumberFormatException ignored) {
                // Ignore a malformed quote and keep the other symbols usable.
            }
        }
        return prices;
    }

    static Map<String, BigDecimal> parsePreviousCloseResponse(String body) {
        if (body == null || body.isBlank()) return Map.of();
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        Matcher matcher = QUOTE.matcher(body);
        while (matcher.find()) {
            String[] fields = matcher.group(3).split("~", -1);
            if (fields.length <= 4 || fields[4].isBlank()) continue;
            try {
                BigDecimal price = new BigDecimal(fields[4]);
                if (price.signum() > 0) prices.put(matcher.group(2), price);
            } catch (NumberFormatException ignored) {
                // Keep other quote rows usable.
            }
        }
        return prices;
    }

    static Map<String, LocalDate> parseDateResponse(String body) {
        if (body == null || body.isBlank()) return Map.of();
        Map<String, LocalDate> dates = new LinkedHashMap<>();
        Matcher matcher = QUOTE.matcher(body);
        while (matcher.find()) {
            String[] fields = matcher.group(3).split("~", -1);
            if (fields.length <= 30 || fields[30].length() < 8) continue;
            try {
                dates.put(matcher.group(2), LocalDate.parse(fields[30].substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE));
            } catch (DateTimeParseException ignored) {
                // Keep other quote rows usable.
            }
        }
        return dates;
    }

    private String market(String symbol) {
        return symbol.startsWith("6") || symbol.startsWith("5") ? "sh" : "sz";
    }
}
