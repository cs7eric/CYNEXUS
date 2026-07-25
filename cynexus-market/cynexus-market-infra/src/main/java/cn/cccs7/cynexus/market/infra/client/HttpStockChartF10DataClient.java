package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpStockChartF10DataClient {

    private static final String TOKEN_HEADER = CommonConstants.HTTP_HEADER_INTERNAL_TOKEN;
    private static final Duration F10_REQUEST_TIMEOUT = Duration.ofSeconds(35);
    private static final String F10_SOURCE_TIMEOUT_SECONDS = "30";
    private static final String F10_SOURCE_POOL_SIZE = "1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final String internalToken;
    private final Duration requestTimeout;

    public HttpStockChartF10DataClient(
            @Value("${cynexus.data.query-api.base-url:http://127.0.0.1:18083}") String baseUrl,
            @Value("${cynexus.data.query-api.internal-token:${CYNEXUS_DATA_INTERNAL_TOKEN:}}") String internalToken,
            @Value("${cynexus.data.query-api.timeout-ms:10000}") long timeoutMs) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.internalToken = internalToken == null ? "" : internalToken;
        this.requestTimeout = Duration.ofMillis(Math.max(timeoutMs, 1000));
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(this.requestTimeout)
                .build();
    }

    public StockChartBO.F10Valuation valuation(String symbol, String reqId) {
        JsonNode root = getF10("/internal/f10/valuation", Map.of("symbol", symbol, "req_id", reqId));
        List<StockChartBO.F10ValuationRow> rows = new ArrayList<>();
        for (JsonNode row : iterable(root.path("rows"))) {
            rows.add(StockChartBO.F10ValuationRow.builder()
                    .date(text(row, "date"))
                    .peTtm(decimal(row, "pe_ttm"))
                    .peBfw(decimal(row, "pe_bfw"))
                    .pbMrq(decimal(row, "pb_mrq"))
                    .pbBfw(decimal(row, "pb_bfw"))
                    .pcfOcfTtm(decimal(row, "pcf_ocf_ttm"))
                    .pcfBfw(decimal(row, "pcf_bfw"))
                    .psTtm(decimal(row, "ps_ttm"))
                    .psBfw(decimal(row, "ps_bfw"))
                    .peg(decimal(row, "peg"))
                    .avgMarketCap(decimal(row, "avg_market_cap"))
                    .aliqMarketCap(decimal(row, "aliq_market_cap"))
                    .build());
        }
        return StockChartBO.F10Valuation.builder()
                .symbol(text(root, "symbol"))
                .reqId(text(root, "req_id"))
                .rows(rows)
                .fetchedAt(offsetDateTime(root, "fetched_at"))
                .source(text(root, "source"))
                .build();
    }

    public StockChartBO.F10FinanceReport financeReport(String symbol, String reportType) {
        JsonNode root = getF10("/internal/f10/finance-report", Map.of("symbol", symbol, "report_type", reportType));
        List<StockChartBO.F10FinanceReportRow> rows = new ArrayList<>();
        for (JsonNode row : iterable(root.path("rows"))) {
            rows.add(StockChartBO.F10FinanceReportRow.builder()
                    .rq(text(row, "rq"))
                    .fields(objectMap(row.path("fields")))
                    .build());
        }
        return StockChartBO.F10FinanceReport.builder()
                .symbol(text(root, "symbol"))
                .reportType(text(root, "report_type"))
                .rows(rows)
                .fetchedAt(offsetDateTime(root, "fetched_at"))
                .source(text(root, "source"))
                .build();
    }

    public StockChartBO.F10BusinessComposition businessComposition(String symbol, String reportDate) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        if (reportDate != null && !reportDate.isBlank()) {
            params.put("report_date", reportDate);
        }
        JsonNode root = getF10("/internal/f10/business-composition", params);
        List<StockChartBO.F10BusinessCompositionItem> items = new ArrayList<>();
        for (JsonNode item : iterable(root.path("items"))) {
            items.add(StockChartBO.F10BusinessCompositionItem.builder()
                    .category(text(item, "category"))
                    .subType(valueAsString(item.get("sub_type")))
                    .name(text(item, "name"))
                    .revenue(decimal(item, "revenue"))
                    .ratio(decimal(item, "ratio"))
                    .extras(objectList(item.path("extras")))
                    .build());
        }
        return StockChartBO.F10BusinessComposition.builder()
                .symbol(text(root, "symbol"))
                .reportDate(text(root, "report_date"))
                .items(items)
                .fetchedAt(offsetDateTime(root, "fetched_at"))
                .source(text(root, "source"))
                .build();
    }

    public StockChartBO.F10ProfitForecast profitForecast(String symbol) {
        JsonNode root = getF10("/internal/f10/profit-forecast", Map.of("symbol", symbol));
        List<StockChartBO.F10ProfitForecastItem> items = new ArrayList<>();
        for (JsonNode item : iterable(root.path("items"))) {
            items.add(StockChartBO.F10ProfitForecastItem.builder()
                    .year(text(item, "year"))
                    .netProfit(decimal(item, "net_profit"))
                    .eps(decimal(item, "eps"))
                    .revenue(decimal(item, "revenue"))
                    .extras(objectList(item.path("extras")))
                    .build());
        }
        return StockChartBO.F10ProfitForecast.builder()
                .symbol(text(root, "symbol"))
                .items(items)
                .fetchedAt(offsetDateTime(root, "fetched_at"))
                .source(text(root, "source"))
                .build();
    }

    public StockChartBO.F10Announcements announcements(String symbol) {
        JsonNode root = getF10("/internal/f10/announcements", Map.of("symbol", symbol));
        List<StockChartBO.F10AnnouncementItem> items = new ArrayList<>();
        for (JsonNode item : iterable(root.path("items"))) {
            items.add(StockChartBO.F10AnnouncementItem.builder()
                    .issueDate(text(item, "issue_date"))
                    .title(text(item, "title"))
                    .typecode(text(item, "typecode"))
                    .typename(text(item, "typename"))
                    .recId(text(item, "rec_id"))
                    .tableid(text(item, "tableid"))
                    .url(text(item, "url"))
                    .redistime(text(item, "redistime"))
                    .source(text(item, "source"))
                    .build());
        }
        return StockChartBO.F10Announcements.builder()
                .symbol(text(root, "symbol"))
                .items(items)
                .count(integer(root, "count"))
                .fetchedAt(offsetDateTime(root, "fetched_at"))
                .source(text(root, "source"))
                .build();
    }

    public StockChartBO.F10News news(String symbol) {
        JsonNode root = getF10("/internal/f10/news", Map.of("symbol", symbol));
        List<StockChartBO.F10NewsItem> items = new ArrayList<>();
        for (JsonNode item : iterable(root.path("items"))) {
            items.add(StockChartBO.F10NewsItem.builder()
                    .issueDate(text(item, "issue_date"))
                    .title(text(item, "title"))
                    .recId(text(item, "rec_id"))
                    .tableid(text(item, "tableid"))
                    .redistime(text(item, "redistime"))
                    .source(text(item, "source"))
                    .relatecolumn(text(item, "relatecolumn"))
                    .build());
        }
        return StockChartBO.F10News.builder()
                .symbol(text(root, "symbol"))
                .items(items)
                .count(integer(root, "count"))
                .fetchedAt(offsetDateTime(root, "fetched_at"))
                .source(text(root, "source"))
                .build();
    }

    public StockChartBO.F10Roadshows roadshows(String symbol) {
        JsonNode root = getF10("/internal/f10/roadshows", Map.of("symbol", symbol));
        List<StockChartBO.F10RoadshowItem> items = new ArrayList<>();
        for (JsonNode item : iterable(root.path("items"))) {
            items.add(StockChartBO.F10RoadshowItem.builder()
                    .title(text(item, "title"))
                    .roadshowType(text(item, "roadshow_type"))
                    .startDate(text(item, "start_date"))
                    .startTime(text(item, "start_time"))
                    .endTime(text(item, "end_time"))
                    .summary(text(item, "summary"))
                    .url(text(item, "url"))
                    .build());
        }
        return StockChartBO.F10Roadshows.builder()
                .symbol(text(root, "symbol"))
                .items(items)
                .count(integer(root, "count"))
                .fetchedAt(offsetDateTime(root, "fetched_at"))
                .source(text(root, "source"))
                .build();
    }

    public StockChartBO.F10CompanyNews companyNews(String symbol, String section) {
        JsonNode root = getF10("/internal/f10/company-news", Map.of("symbol", symbol, "section", section));
        List<StockChartBO.F10CompanyNewsItem> items = new ArrayList<>();
        for (JsonNode item : iterable(root.path("items"))) {
            items.add(StockChartBO.F10CompanyNewsItem.builder()
                    .rating(text(item, "rating"))
                    .analysts(text(item, "analysts"))
                    .recId(text(item, "rec_id"))
                    .issueDate(text(item, "issue_date"))
                    .title(text(item, "title"))
                    .nflag(text(item, "nflag"))
                    .docHash(text(item, "doc_hash"))
                    .build());
        }
        return StockChartBO.F10CompanyNews.builder()
                .symbol(text(root, "symbol"))
                .section(text(root, "section"))
                .items(items)
                .count(integer(root, "count"))
                .fetchedAt(offsetDateTime(root, "fetched_at"))
                .source(text(root, "source"))
                .build();
    }

    private JsonNode get(String path, Map<String, String> params) {
        HttpRequest request = baseRequest(path, params).GET().build();
        return send(request);
    }

    private JsonNode getF10(String path, Map<String, String> params) {
        Map<String, String> f10Params = new LinkedHashMap<>(params);
        f10Params.put("timeout_seconds", F10_SOURCE_TIMEOUT_SECONDS);
        f10Params.put("pool_size", F10_SOURCE_POOL_SIZE);
        HttpRequest request = baseRequest(path, f10Params, F10_REQUEST_TIMEOUT).GET().build();
        return send(request);
    }

    private HttpRequest.Builder baseRequest(String path, Map<String, String> params) {
        return baseRequest(path, params, requestTimeout);
    }

    private HttpRequest.Builder baseRequest(String path, Map<String, String> params, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path + queryString(params)))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .header(CommonConstants.HTTP_HEADER_ACCEPT, CommonConstants.MEDIA_TYPE_APPLICATION_JSON);
        if (!internalToken.isBlank()) {
            builder.header(TOKEN_HEADER, internalToken);
        }
        return builder;
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "Stock chart F10 data request failed: HTTP " + response.statusCode() + " " + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, buildRequestFailureMessage(request.uri(), exception));
        }
    }

    private static String queryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!first) {
                builder.append("&");
            }
            builder.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
            first = false;
        }
        return first ? "" : builder.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:18083";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String buildRequestFailureMessage(URI uri, Exception exception) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = exception.getClass().getSimpleName();
        }
        return "Stock chart F10 data request failed: " + uri + " (" + detail + ")";
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        return node != null && node.isArray() ? node : List.of();
    }

    private static String text(JsonNode node, String field) {
        return valueAsString(node == null ? null : node.get(field));
    }

    private static String valueAsString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.isTextual() ? node.asText() : node.toString();
        return text == null || text.isBlank() ? null : text;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return decimal(node == null ? null : node.get(field));
    }

    private static BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer integer(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private static OffsetDateTime offsetDateTime(JsonNode node, String field) {
        String text = text(node, field);
        if (text == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (Exception exception) {
            return null;
        }
    }

    private static Map<String, Object> objectMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> result.put(entry.getKey(), objectValue(entry.getValue())));
        return result;
    }

    private static List<Object> objectList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<Object> result = new ArrayList<>();
        for (JsonNode item : node) {
            result.add(objectValue(item));
        }
        return result;
    }

    private static Object objectValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        if (node.isFloatingPointNumber()) {
            return node.decimalValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isArray()) {
            return objectList(node);
        }
        if (node.isObject()) {
            return objectMap(node);
        }
        return node.asText();
    }
}
