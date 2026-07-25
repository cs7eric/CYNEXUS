package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionLinkResolver;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionResolvedLinkBO;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class HttpMediaTranscriptionLinkResolver implements MediaTranscriptionLinkResolver {

    private static final Logger log = LoggerFactory.getLogger(HttpMediaTranscriptionLinkResolver.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Duration timeout;

    public HttpMediaTranscriptionLinkResolver(
            @Value("${cynexus.media-transcription.downloader.base-url:https://downloader-api.bhwa233.com}") String baseUrl,
            @Value("${cynexus.media-transcription.downloader.timeout-ms:60000}") long timeoutMs) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.timeout = Duration.ofMillis(Math.max(1000, timeoutMs));
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public MediaTranscriptionResolvedLinkBO resolve(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        URI uri = URI.create(baseUrl + "/api/parse?url=" + URLEncoder.encode(sourceUrl, StandardCharsets.UTF_8));
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header(CommonConstants.HTTP_HEADER_ACCEPT, CommonConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Media link resolver returned HTTP {} for sourceUrl={}", response.statusCode(), sourceUrl);
                return null;
            }
            Map<String, Object> envelope = objectMapper.readValue(response.body(), new TypeReference<LinkedHashMap<String, Object>>() {
            });
            if (!Boolean.TRUE.equals(envelope.get("success"))) {
                return null;
            }
            Map<String, Object> data = asMap(envelope.get("data"));
            String videoUrl = asString(data.get("downloadVideoUrl"));
            String audioUrl = asString(data.get("downloadAudioUrl"));
            if (videoUrl == null && audioUrl == null) {
                return null;
            }
            return MediaTranscriptionResolvedLinkBO.builder()
                    .title(asString(data.get("title")))
                    .platform(asString(data.get("platform")))
                    .duration(asInteger(data.get("duration")))
                    .noteType(asString(data.get("noteType")))
                    .downloadVideoUrl(videoUrl)
                    .downloadAudioUrl(audioUrl)
                    .originalUrl(asString(data.get("url")))
                    .build();
        } catch (Exception exception) {
            log.warn("Media link resolver failed, direct URL fallback will be used: sourceUrl={}", sourceUrl, exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private String asString(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? null : Integer.valueOf(String.valueOf(value)); }
        catch (Exception exception) { return null; }
    }

    private String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
