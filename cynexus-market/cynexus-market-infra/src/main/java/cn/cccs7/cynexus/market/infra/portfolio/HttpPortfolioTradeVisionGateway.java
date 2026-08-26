package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportAssetBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeVisionGateway;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeVisionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpPortfolioTradeVisionGateway implements PortfolioTradeVisionGateway {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Duration timeout;

    public HttpPortfolioTradeVisionGateway(
            ObjectMapper objectMapper,
            @Value("${cynexus.ai.api.base-url:http://127.0.0.1:18081}") String baseUrl,
            @Value("${cynexus.ai.api.timeout-ms:120000}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.baseUrl = trim(baseUrl);
        this.timeout = Duration.ofMillis(Math.max(timeoutMs, 1000));
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public String parse(PortfolioImportAssetBO asset) {
        return parse(asset.fileName(), asset.contentType(), asset.contentBytes());
    }

    @Override
    public String review(PortfolioImportAssetBO asset, String extractedResult) {
        try {
            String dataUrl = "data:" + asset.contentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(asset.contentBytes());
            String body = objectMapper.writeValueAsString(Map.of(
                    "scenarioCode", "portfolio_trade_screenshot_review",
                    "userInput", extractedResult == null ? "{}" : extractedResult,
                    "images", List.of(Map.of("fileName", asset.fileName() == null ? "trade.png" : asset.fileName(),
                            "contentType", asset.contentType(), "dataUrl", dataUrl))));
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/ai/executions/vision"))
                    .timeout(timeout)
                    .header(CommonConstants.HTTP_HEADER_ACCEPT, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerFailure(response.statusCode(), response.body());
            }
            JsonNode envelope = objectMapper.readTree(response.body());
            if (envelope.path("success").isBoolean() && !envelope.path("success").asBoolean()) {
                throw executionFailure(envelope);
            }
            JsonNode data = envelope.path("data");
            if (!"SUCCEEDED".equalsIgnoreCase(data.path("status").asText())) {
                throw new PortfolioTradeVisionException("PORTFOLIO_AI_REVIEW_FAILED",
                        "AI trade review did not succeed.", true);
            }
            return data.path("content").asText(null);
        } catch (PortfolioTradeVisionException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI trade review interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("AI trade review failed", exception);
        }
    }

    @Override
    public String parse(String fileName, String contentType, byte[] content) {
        try {
            String dataUrl = "data:" + contentType + ";base64,"
                    + Base64.getEncoder().encodeToString(content);
            String body = objectMapper.writeValueAsString(Map.of(
                    "scenarioCode", "portfolio_trade_screenshot_parse",
                    "userInput", "只解析截图中的交易成交事实，缺失字段返回 null。",
                    "images", List.of(Map.of("fileName", fileName == null ? "trade.png" : fileName,
                            "contentType", contentType, "dataUrl", dataUrl))));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/ai/executions/vision"))
                    .timeout(timeout)
                    .header(CommonConstants.HTTP_HEADER_ACCEPT, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerFailure(response.statusCode(), response.body());
            }
            JsonNode envelope = objectMapper.readTree(response.body());
            if (envelope.path("success").isBoolean() && !envelope.path("success").asBoolean()) {
                throw executionFailure(envelope);
            }
            JsonNode data = envelope.path("data");
            if (!"SUCCEEDED".equalsIgnoreCase(data.path("status").asText())) {
                throw new PortfolioTradeVisionException("PORTFOLIO_AI_EXECUTION_FAILED",
                        "AI vision execution did not succeed.", true);
            }
            return data.path("content").asText(null);
        } catch (PortfolioTradeVisionException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI vision request interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("AI vision request failed", exception);
        }
    }

    private static String trim(String value) {
        String result = value == null || value.isBlank() ? "http://127.0.0.1:18081" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private PortfolioTradeVisionException providerFailure(int statusCode, String body) {
        String providerCode = null;
        String providerMessage = null;
        try {
            JsonNode payload = objectMapper.readTree(body);
            providerCode = text(payload, "code");
            providerMessage = text(payload, "message");
            JsonNode data = payload.path("data");
            if (providerCode == null) providerCode = text(data, "code");
            if (providerMessage == null) providerMessage = text(data, "message");
        } catch (Exception ignored) {
            // Provider body is intentionally not exposed when it is not a recognized API error payload.
        }
        boolean retryable = statusCode == 408 || statusCode == 429 || statusCode >= 500;
        String code = "GROUP_DISABLED".equals(providerCode) ? "PORTFOLIO_AI_GROUP_DISABLED"
                : statusCode == 401 || statusCode == 403 ? "PORTFOLIO_AI_PROVIDER_UNAUTHORIZED"
                : retryable ? "PORTFOLIO_AI_SERVICE_UNAVAILABLE" : "PORTFOLIO_AI_PROVIDER_REJECTED";
        String detail = providerCode == null ? "" : " " + providerCode + ".";
        if (providerMessage != null) detail += " " + truncate(providerMessage, 360);
        return new PortfolioTradeVisionException(code,
                "AI provider rejected the vision request (HTTP " + statusCode + ")." + detail, retryable);
    }

    private PortfolioTradeVisionException executionFailure(JsonNode envelope) {
        String errorCode = text(envelope, "code");
        String message = text(envelope, "message");
        if (message == null) message = "AI vision execution failed before a result was produced.";
        String combined = (errorCode == null ? "" : errorCode + ". ") + truncate(message, 360);
        int providerStatus = providerStatus(message);
        boolean retryable = providerStatus == 408 || providerStatus == 429 || providerStatus >= 500 || providerStatus == 0;
        String code = providerStatus == 401 || providerStatus == 403
                ? "PORTFOLIO_AI_PROVIDER_UNAUTHORIZED"
                : retryable ? "PORTFOLIO_AI_SERVICE_UNAVAILABLE" : "PORTFOLIO_AI_PROVIDER_REJECTED";
        return new PortfolioTradeVisionException(code, "AI vision execution failed. " + combined, retryable);
    }

    private static int providerStatus(String message) {
        if (message == null) return 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\bHTTP\\s+(\\d{3})\\b")
                .matcher(message);
        if (!matcher.find()) return 0;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText().trim();
    }

    private static String truncate(String value, int length) {
        return value.length() <= length ? value : value.substring(0, length);
    }
}
