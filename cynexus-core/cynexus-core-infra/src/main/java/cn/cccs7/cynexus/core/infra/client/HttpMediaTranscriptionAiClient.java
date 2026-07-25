package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionAiClient;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAskResultBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionMetadataBO;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class HttpMediaTranscriptionAiClient implements MediaTranscriptionAiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Duration requestTimeout;

    public HttpMediaTranscriptionAiClient(
            @Value("${cynexus.media-transcription.ai.base-url:http://127.0.0.1:18081/api/ai/media-transcription}") String baseUrl,
            @Value("${cynexus.media-transcription.ai.timeout-ms:600000}") long timeoutMs) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.requestTimeout = Duration.ofMillis(Math.max(timeoutMs, 1000));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String polish(String text) {
        Map<String, Object> data = postForData("/polish", Map.of("text", text == null ? "" : text));
        return asString(data.get("text"));
    }

    @Override
    public String summarize(String text, String title, String platform, Integer duration) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", text == null ? "" : text);
        payload.put("title", title);
        payload.put("platform", platform);
        payload.put("duration", duration);
        Map<String, Object> data = postForData("/summary", payload);
        return asString(data.get("text"));
    }

    @Override
    public MediaTranscriptionMetadataBO metadata(String polishedText, String summaryText) {
        Map<String, Object> data = postForData("/metadata", Map.of(
                "polishedText", polishedText == null ? "" : polishedText,
                "summaryText", summaryText == null ? "" : summaryText
        ));
        return MediaTranscriptionMetadataBO.builder()
                .title(asString(data.get("title")))
                .categories(asStringList(data.get("categories")))
                .tags(asStringList(data.get("tags")))
                .build();
    }

    @Override
    public MediaTranscriptionAskResultBO ask(String polishedText, String summaryText, String question) {
        Map<String, Object> data = postForData("/ask", Map.of(
                "polishedText", polishedText == null ? "" : polishedText,
                "summaryText", summaryText == null ? "" : summaryText,
                "question", question == null ? "" : question
        ));
        return MediaTranscriptionAskResultBO.builder()
                .answer(asString(data.get("answer")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForData(String path, Map<String, Object> payload) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(requestTimeout)
                .header(CommonConstants.HTTP_HEADER_ACCEPT, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                .header(CommonConstants.HTTP_HEADER_CONTENT_TYPE, CommonConstants.MEDIA_TYPE_APPLICATION_JSON_UTF8)
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "Media transcription AI request failed: HTTP " + response.statusCode() + " " + response.body());
            }
            Map<String, Object> envelope = objectMapper.readValue(response.body(), new TypeReference<LinkedHashMap<String, Object>>() {
            });
            Object data = envelope.get("data");
            if (data instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                map.forEach((key, value) -> result.put(String.valueOf(key), value));
                return result;
            }
            return Map.of();
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            String detail = exception.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = exception.getClass().getSimpleName();
            }
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Media transcription AI request failed: " + detail);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Media transcription AI request serialization failed");
        }
    }

    private static String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(item -> item != null && !String.valueOf(item).isBlank())
                .map(String::valueOf)
                .toList();
    }
}
