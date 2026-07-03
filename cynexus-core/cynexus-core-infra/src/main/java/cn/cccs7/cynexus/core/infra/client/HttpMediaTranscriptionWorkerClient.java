package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionWorkerClient;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAskResultBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionBinaryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProgressBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionRemoteParseRequestBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionStreamBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskRefBO;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class HttpMediaTranscriptionWorkerClient implements MediaTranscriptionWorkerClient {

    private static final String TOKEN_HEADER = "X-Cynexus-Internal-Token";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String internalToken;
    private final Duration requestTimeout;

    public HttpMediaTranscriptionWorkerClient(
            @Value("${cynexus.media-transcription.worker.base-url:http://127.0.0.1:18083/internal/media-transcription}") String baseUrl,
            @Value("${cynexus.media-transcription.worker.internal-token:${CYNEXUS_MEDIA_TRANSCRIPTION_WORKER_INTERNAL_TOKEN:${CYNEXUS_DATA_INTERNAL_TOKEN:}}}") String internalToken,
            @Value("${cynexus.media-transcription.worker.timeout-ms:600000}") long timeoutMs) {
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
    public MediaTranscriptionTaskRefBO upload(Path filePath, String fileName, String contentType) {
        String boundary = "----CynexusBoundary" + System.nanoTime();
        HttpRequest request = baseRequest("/transcribe")
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> openMultipartStream(filePath, fileName, contentType, boundary)))
                .build();
        Map<String, Object> response = sendForMap(request);
        return MediaTranscriptionTaskRefBO.builder()
                .taskId(asString(response.get("task_id")))
                .fileName(fileName)
                .build();
    }

    @Override
    public MediaTranscriptionTaskRefBO remoteParse(MediaTranscriptionRemoteParseRequestBO request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("download_url", request == null ? null : request.getDownloadUrl());
        payload.put("title", request == null ? null : request.getTitle());
        payload.put("source_url", request == null ? null : request.getSourceUrl());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", request == null ? null : request.getTitle());
        metadata.put("platform", request == null ? null : request.getPlatform());
        metadata.put("duration", request == null ? null : request.getDuration());
        metadata.put("noteType", request == null ? null : request.getNoteType());
        metadata.put("download_audio_url", request == null ? null : request.getAudioUrl());
        metadata.put("original_url", request == null ? null : request.getOriginalUrl());
        payload.put("metadata", metadata);
        Map<String, Object> response = postForMap("/parse-video", payload);
        return MediaTranscriptionTaskRefBO.builder()
                .taskId(asString(response.get("task_id")))
                .fileName(asString(response.get("file_name")))
                .build();
    }

    @Override
    public MediaTranscriptionTaskBO getTask(String taskId) {
        Map<String, Object> response = getForMap("/task/" + encodePath(taskId));
        return MediaTranscriptionTaskBO.builder()
                .taskId(asString(response.get("task_id")))
                .status(asString(response.get("status")))
                .transcript(asString(response.get("transcript")))
                .polished(asString(response.get("polished")))
                .summary(asString(response.get("summary")))
                .metadata(toMetadata(asMap(response.get("metadata"))))
                .fileName(asString(response.get("file_name")))
                .error(asString(response.get("error")))
                .downloadProgress(toProgress(asMap(response.get("download_progress"))))
                .intakeProgress(toProgress(asMap(response.get("intake_progress"))))
                .build();
    }

    @Override
    public MediaTranscriptionAskResultBO ask(String taskId, String question) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task_id", taskId);
        payload.put("question", question);
        Map<String, Object> response = postForMap("/ask", payload);
        return MediaTranscriptionAskResultBO.builder()
                .answer(asString(response.get("answer")))
                .build();
    }

    @Override
    public MediaTranscriptionBinaryBO exportMarkdown(String taskId) {
        HttpRequest request = baseRequest("/export-markdown/" + encodePath(taskId))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "Media transcription worker request failed: HTTP " + response.statusCode() + " "
                                + new String(response.body(), StandardCharsets.UTF_8));
            }
            return MediaTranscriptionBinaryBO.builder()
                    .content(response.body())
                    .contentType(response.headers().firstValue("Content-Type").orElse("text/markdown; charset=utf-8"))
                    .fileName(resolveDownloadFileName(response.headers().firstValue("Content-Disposition").orElse(null)))
                    .build();
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, buildRequestFailureMessage(request.uri(), exception));
        }
    }

    @Override
    public MediaTranscriptionStreamBO openStream(String taskId) {
        HttpRequest request = baseRequest("/stream/" + encodePath(taskId))
                .header("Accept", "text/event-stream")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "Media transcription worker request failed: HTTP " + response.statusCode() + " " + body);
            }
            return MediaTranscriptionStreamBO.builder()
                    .inputStream(response.body())
                    .contentType(response.headers().firstValue("Content-Type").orElse("text/event-stream"))
                    .build();
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, buildRequestFailureMessage(request.uri(), exception));
        }
    }

    private HttpRequest.Builder baseRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(requestTimeout);
        if (!internalToken.isBlank()) {
            builder.header(TOKEN_HEADER, internalToken);
        }
        return builder;
    }

    private Map<String, Object> getForMap(String path) {
        return sendForMap(baseRequest(path)
                .header("Accept", "application/json")
                .GET()
                .build());
    }

    private Map<String, Object> postForMap(String path, Map<String, Object> payload) {
        HttpRequest request = baseRequest(path)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload), StandardCharsets.UTF_8))
                .build();
        return sendForMap(request);
    }

    private Map<String, Object> sendForMap(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "Media transcription worker request failed: HTTP " + response.statusCode() + " " + response.body());
            }
            return objectMapper.readValue(response.body(), new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, buildRequestFailureMessage(request.uri(), exception));
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Media transcription worker request serialization failed");
        }
    }

    private InputStream openMultipartStream(Path filePath, String fileName, String contentType, String boundary) {
        try {
            String safeFileName = fileName == null || fileName.isBlank() ? "upload.bin" : fileName;
            String safeContentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
            byte[] header = (
                    "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"file\"; filename=\"" + escapeQuotes(safeFileName) + "\"\r\n"
                            + "Content-Type: " + safeContentType + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8);
            byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            List<InputStream> streams = new ArrayList<>();
            streams.add(new ByteArrayInputStream(header));
            streams.add(Files.newInputStream(filePath));
            streams.add(new ByteArrayInputStream(footer));
            Enumeration<InputStream> enumeration = Collections.enumeration(streams);
            return new SequenceInputStream(enumeration);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Media transcription worker upload request creation failed");
        }
    }

    private static MediaTranscriptionMetadataBO toMetadata(Map<String, Object> metadata) {
        if (metadata.isEmpty()) {
            return null;
        }
        return MediaTranscriptionMetadataBO.builder()
                .title(asString(metadata.get("title")))
                .categories(asStringList(metadata.get("categories")))
                .tags(asStringList(metadata.get("tags")))
                .sourceUrl(asString(firstNonNull(metadata.get("sourceUrl"), metadata.get("source_url"))))
                .downloadUrl(asString(firstNonNull(metadata.get("downloadUrl"), metadata.get("download_url"))))
                .platform(asString(metadata.get("platform")))
                .duration(asInteger(metadata.get("duration")))
                .noteType(asString(firstNonNull(metadata.get("noteType"), metadata.get("note_type"))))
                .audioUrl(asString(firstNonNull(metadata.get("audioUrl"), metadata.get("download_audio_url"))))
                .originalUrl(asString(firstNonNull(metadata.get("originalUrl"), metadata.get("original_url"))))
                .build();
    }

    private static MediaTranscriptionProgressBO toProgress(Map<String, Object> progress) {
        if (progress.isEmpty()) {
            return MediaTranscriptionProgressBO.builder().build();
        }
        return MediaTranscriptionProgressBO.builder()
                .phase(asString(progress.get("phase")))
                .progress(asInteger(progress.get("progress")))
                .downloadedBytes(asLong(progress.get("downloaded_bytes")))
                .totalBytes(asLong(progress.get("total_bytes")))
                .processedBytes(asLong(progress.get("processed_bytes")))
                .etaSeconds(asInteger(progress.get("eta_seconds")))
                .speedBytesPerSec(asLong(progress.get("speed_bytes_per_sec")))
                .build();
    }

    private static Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
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

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception exception) {
            return null;
        }
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception exception) {
            return null;
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:18083/internal/media-transcription";
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

    private static String escapeQuotes(String value) {
        return value.replace("\"", "\\\"");
    }

    private static String buildRequestFailureMessage(URI uri, Exception exception) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = exception.getClass().getSimpleName();
        }
        return "Media transcription worker request failed: " + uri + " (" + detail + ")";
    }

    private static String resolveDownloadFileName(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isBlank()) {
            return "transcription.md";
        }
        String[] segments = contentDisposition.split(";");
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.startsWith("filename*=")) {
                int index = trimmed.indexOf("''");
                if (index >= 0 && index + 2 < trimmed.length()) {
                    return URLDecoderCompat.decode(trimmed.substring(index + 2));
                }
            }
            if (trimmed.startsWith("filename=")) {
                return trimmed.substring("filename=".length()).replace("\"", "");
            }
        }
        return "transcription.md";
    }

    private static final class URLDecoderCompat {
        private URLDecoderCompat() {
        }

        private static String decode(String value) {
            try {
                return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
            } catch (Exception exception) {
                return value;
            }
        }
    }
}
