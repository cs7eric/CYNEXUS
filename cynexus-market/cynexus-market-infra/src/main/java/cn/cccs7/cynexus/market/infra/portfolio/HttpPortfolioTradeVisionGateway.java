package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportAssetBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeVisionGateway;
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
        try {
            String dataUrl = "data:" + asset.contentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(asset.contentBytes());
            String body = objectMapper.writeValueAsString(Map.of(
                    "scenarioCode", "portfolio_trade_screenshot_parse",
                    "userInput", "只解析截图中的交易成交事实，缺失字段返回 null。",
                    "images", List.of(Map.of("fileName", asset.fileName() == null ? "trade.png" : asset.fileName(),
                            "contentType", asset.contentType(), "dataUrl", dataUrl))));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/ai/executions/vision"))
                    .timeout(timeout)
                    .header(CommonConstants.HTTP_HEADER_ACCEPT, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI vision request failed: HTTP " + response.statusCode());
            }
            JsonNode data = objectMapper.readTree(response.body()).path("data");
            if (!"SUCCEEDED".equalsIgnoreCase(data.path("status").asText())) {
                throw new IllegalStateException("AI vision execution did not succeed");
            }
            return data.path("content").asText(null);
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
}
