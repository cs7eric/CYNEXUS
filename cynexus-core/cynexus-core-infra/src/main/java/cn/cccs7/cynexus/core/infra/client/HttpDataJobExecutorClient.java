package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.client.DataJobExecutorClient;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class HttpDataJobExecutorClient implements DataJobExecutorClient {

    private static final String TOKEN_HEADER = "X-Cynexus-Internal-Token";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String internalToken;
    private final Duration requestTimeout;

    public HttpDataJobExecutorClient(
            @Value("${cynexus.data.job-executor.base-url:http://127.0.0.1:18083}") String baseUrl,
            @Value("${cynexus.data.job-executor.internal-token:${CYNEXUS_DATA_INTERNAL_TOKEN:}}") String internalToken,
            @Value("${cynexus.data.job-executor.timeout-ms:600000}") long timeoutMs) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.internalToken = internalToken == null ? "" : internalToken;
        this.requestTimeout = Duration.ofMillis(Math.max(timeoutMs, 1000));
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(this.requestTimeout)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> health() {
        return getMap("/internal/health");
    }

    @Override
    public Map<String, Object> listJobs() {
        return getMap("/internal/jobs");
    }

    @Override
    public Map<String, Object> getJob(String jobKey) {
        return getMap("/internal/jobs/" + encodePath(jobKey));
    }

    @Override
    public DataJobTriggerResultBO triggerJob(String jobKey, DataJobTriggerRequestBO request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trigger_request_id", request == null ? null : request.getTriggerRequestId());
        body.put("triggered_by", request == null ? null : request.getTriggeredBy());
        body.put("dry_run", request != null && Boolean.TRUE.equals(request.getDryRun()));
        body.put("params", request == null || request.getParams() == null ? Map.of() : request.getParams());
        Map<String, Object> response = postMap("/internal/jobs/" + encodePath(jobKey) + "/trigger", body);
        return DataJobTriggerResultBO.builder()
                .accepted(asBoolean(response.get("accepted")))
                .runId(asString(response.get("run_id")))
                .jobKey(asString(response.get("job_key")))
                .status(asString(response.get("status")))
                .result(asMap(response.get("result")))
                .build();
    }

    @Override
    public Map<String, Object> getJobRun(String runId) {
        return getMap("/internal/job-runs/" + encodePath(runId));
    }

    private Map<String, Object> getMap(String path) {
        HttpRequest request = baseRequest(path).GET().build();
        return sendForMap(request);
    }

    private Map<String, Object> postMap(String path, Map<String, Object> body) {
        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(body), StandardCharsets.UTF_8))
                .build();
        return sendForMap(request);
    }

    private HttpRequest.Builder baseRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(requestTimeout)
                .header("Accept", "application/json");
        if (!internalToken.isBlank()) {
            builder.header(TOKEN_HEADER, internalToken);
        }
        return builder;
    }

    private Map<String, Object> sendForMap(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "Data job executor request failed: HTTP " + response.statusCode() + " " + response.body());
            }
            return asMap(objectMapper.readValue(response.body(), Map.class));
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, buildRequestFailureMessage(request.uri(), exception));
        }
    }

    private static String buildRequestFailureMessage(URI uri, Exception exception) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = exception.getClass().getSimpleName();
        }
        return "Data job executor request failed: " + uri + " (" + detail + ")";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Data job executor request serialization failed");
        }
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

    private static String encodePath(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }
}
