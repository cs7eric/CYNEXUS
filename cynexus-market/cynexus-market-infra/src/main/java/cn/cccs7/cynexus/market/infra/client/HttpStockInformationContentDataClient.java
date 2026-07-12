package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpStockInformationContentDataClient {

    private static final String TOKEN_HEADER = "X-Cynexus-Internal-Token";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final String internalToken;
    private final Duration requestTimeout;

    public HttpStockInformationContentDataClient(
            @Value("${cynexus.data.query-api.base-url:http://127.0.0.1:18083}") String baseUrl,
            @Value("${cynexus.data.query-api.internal-token:${CYNEXUS_DATA_INTERNAL_TOKEN:}}") String internalToken,
            @Value("${cynexus.data.query-api.timeout-ms:20000}") long timeoutMs) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.internalToken = internalToken == null ? "" : internalToken;
        this.requestTimeout = Duration.ofMillis(Math.max(timeoutMs, 1000));
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.requestTimeout).build();
    }

    public StockInformationBO.Content extract(StockInformationBO.SourceRecord record) {
        try {
            Map<String, String> body = new LinkedHashMap<>();
            putIfPresent(body, "url", record.getUrl());
            putIfPresent(body, "title", record.getTitle());
            putIfPresent(body, "inline_text", record.getInlineText());
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/internal/stock-information/content"))
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            if (!internalToken.isBlank()) {
                builder.header(TOKEN_HEADER, internalToken);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "Information content request failed: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return StockInformationBO.Content.builder().sourceRecord(record).title(text(root, "title", record.getTitle()))
                    .sourceUrl(record.getUrl()).resolvedUrl(text(root, "source_url", record.getUrl()))
                    .contentType(text(root, "content_type", null)).status(text(root, "status", "failed"))
                    .text(text(root, "text", "")).contentHash(text(root, "content_hash", null))
                    .message(text(root, "message", null)).extractedAt(dateTime(root, "extracted_at")).build();
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Information content request failed: " + exception.getMessage());
        }
    }

    private static String trimTrailingSlash(String value) {
        String result = value == null || value.isBlank() ? "http://127.0.0.1:18083" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? fallback : value.asText();
    }

    private static OffsetDateTime dateTime(JsonNode node, String field) {
        String value = text(node, field, null);
        try {
            return value == null ? null : OffsetDateTime.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) target.put(key, value);
    }
}
