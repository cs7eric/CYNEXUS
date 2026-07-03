package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpIndustryApplicationDataClient {

    private static final String TOKEN_HEADER = "X-Cynexus-Internal-Token";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final String internalToken;
    private final Duration requestTimeout;

    public HttpIndustryApplicationDataClient(
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

    public IndustryApplicationBO.TdxFundFlowSnapshot tdxFundFlow(String targetType, String symbol, LocalDate tradeDate, int days) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("target_type", targetType);
        params.put("symbol", symbol);
        if (tradeDate != null) {
            params.put("trade_date", tradeDate.toString());
        }
        params.put("days", Integer.toString(Math.max(1, days)));
        JsonNode root = get("/internal/industry-application/tdx-fund-flow", params);
        List<IndustryApplicationBO.TdxFundFlowPoint> history = new ArrayList<>();
        for (JsonNode row : iterable(root.path("history"))) {
            history.add(IndustryApplicationBO.TdxFundFlowPoint.builder()
                    .tradeDate(localDate(text(row, "trade_date")))
                    .flowTradeDate(localDate(text(row, "flow_trade_date")))
                    .snapshotStatus(text(row, "snapshot_status"))
                    .selectionMode(text(row, "selection_mode"))
                    .mainNet(decimal(row, "main_net"))
                    .largeOrderNet(decimal(row, "large_order_net"))
                    .mediumOrderNet(decimal(row, "medium_order_net"))
                    .smallOrderNet(decimal(row, "small_order_net"))
                    .build());
        }
        return IndustryApplicationBO.TdxFundFlowSnapshot.builder()
                .applicable(bool(root, "applicable"))
                .ok(bool(root, "ok"))
                .targetType(text(root, "target_type"))
                .symbol(text(root, "symbol"))
                .requestedTradeDate(localDate(text(root, "requested_trade_date")))
                .tradeDate(localDate(text(root, "trade_date")))
                .code6(text(root, "code6"))
                .fullCode(text(root, "full_code"))
                .tdxName(text(root, "tdx_name"))
                .flowSectorName(text(root, "flow_sector_name"))
                .seedCode(text(root, "seed_code"))
                .flowTradeDate(localDate(text(root, "flow_trade_date")))
                .snapshotStatus(text(root, "snapshot_status"))
                .selectionMode(text(root, "selection_mode"))
                .nameMatchType(text(root, "name_match_type"))
                .mainNet(decimal(root, "main_net"))
                .largeOrderNet(decimal(root, "large_order_net"))
                .mediumOrderNet(decimal(root, "medium_order_net"))
                .smallOrderNet(decimal(root, "small_order_net"))
                .rowCount(integer(root, "row_count"))
                .historyDays(integer(root, "history_days"))
                .history(history)
                .source(text(root, "source"))
                .error(text(root, "error"))
                .build();
    }

    private JsonNode get(String path, Map<String, String> params) {
        HttpRequest request = baseRequest(path, params).GET().build();
        return send(request);
    }

    private HttpRequest.Builder baseRequest(String path, Map<String, String> params) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path + queryString(params)))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(requestTimeout)
                .header("Accept", "application/json");
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
                        "Industry application data request failed: HTTP " + response.statusCode() + " " + response.body());
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
        return "Industry application data request failed: " + uri + " (" + detail + ")";
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
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private static Boolean bool(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asBoolean();
    }

    private static LocalDate localDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }
}
