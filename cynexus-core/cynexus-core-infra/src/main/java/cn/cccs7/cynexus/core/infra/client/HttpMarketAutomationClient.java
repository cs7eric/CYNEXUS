package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.client.MarketAutomationClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class HttpMarketAutomationClient implements MarketAutomationClient {
    private static final String REFRESH_PATH = "/api/market/information/pipeline/refresh";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI refreshUri;
    private final Duration timeout;

    public HttpMarketAutomationClient(
            @Value("${cynexus.market.automation.base-url:http://127.0.0.1:18082}") String baseUrl,
            @Value("${cynexus.market.automation.timeout-ms:30000}") long timeoutMs) {
        String normalized = baseUrl == null ? "http://127.0.0.1:18082" : baseUrl.trim().replaceAll("/+$", "");
        this.refreshUri = URI.create(normalized + REFRESH_PATH);
        this.timeout = Duration.ofMillis(Math.max(timeoutMs, 1000));
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public int refreshInformationPipeline() {
        HttpRequest request = HttpRequest.newBuilder(refreshUri).timeout(timeout)
                .header(CommonConstants.HTTP_HEADER_ACCEPT, CommonConstants.MEDIA_TYPE_APPLICATION_JSON).POST(HttpRequest.BodyPublishers.noBody()).build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "Market automation refresh failed: HTTP " + response.statusCode());
            }
            Map<?, ?> root = objectMapper.readValue(response.body(), Map.class);
            Object data = root.get("data");
            if (data instanceof Map<?, ?> map && map.get("clusterCount") instanceof Number count) return count.intValue();
            return 0;
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Market automation refresh failed: " + exception.getMessage());
        }
    }
}
