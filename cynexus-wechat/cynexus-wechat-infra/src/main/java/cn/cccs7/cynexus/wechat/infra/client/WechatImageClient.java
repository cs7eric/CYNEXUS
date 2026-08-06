package cn.cccs7.cynexus.wechat.infra.client;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatAccessTokenProvider;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatImageGateway;
import cn.cccs7.cynexus.wechat.domain.model.WechatStoredImage;
import cn.cccs7.cynexus.wechat.infra.config.WechatProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class WechatImageClient implements WechatImageGateway {
    private final WechatProperties properties;
    private final WechatAccessTokenProvider accessTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final Duration timeout;

    public WechatImageClient(WechatProperties properties, WechatAccessTokenProvider accessTokenProvider) {
        this.properties = properties;
        this.accessTokenProvider = accessTokenProvider;
        this.timeout = Duration.ofMillis(Math.max(properties.getTimeoutMs(), 1000));
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public WechatStoredImage downloadAndStore(String mediaId, String picUrl, String fromUserName) {
        if (isBlank(mediaId) && isBlank(picUrl)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "WeChat image callback has no media id or picture URL");
        }
        try {
            DownloadedImage image = isBlank(mediaId)
                    ? downloadFromUrl(picUrl)
                    : downloadFromMedia(mediaId);
            if (image.bytes().length == 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "WeChat image response is empty");
            }
            if (image.bytes().length > properties.getImageMaxBytes()) {
                throw new BizException(ErrorCode.BAD_REQUEST, "WeChat image exceeds the configured size limit");
            }

            Path directory = Paths.get(properties.getImageStorageDir()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String extension = extension(image.contentType());
            Path target = directory.resolve(LocalDate.now() + "-" + UUID.randomUUID() + extension).normalize();
            if (!target.startsWith(directory)) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "Invalid WeChat image storage path");
            }
            Files.write(target, image.bytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new WechatStoredImage(mediaId, picUrl, target.toString(), image.contentType(), image.bytes().length);
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Unable to store WeChat image: " + exception.getMessage());
        }
    }

    private DownloadedImage downloadFromMedia(String mediaId) throws Exception {
        URI uri = URI.create(apiBaseUrl() + "/cgi-bin/media/get?access_token="
                + encode(accessTokenProvider.accessToken()) + "&media_id=" + encode(mediaId));
        return download(uri);
    }

    private DownloadedImage downloadFromUrl(String picUrl) throws Exception {
        return download(URI.create(picUrl));
    }

    private DownloadedImage download(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        byte[] bytes = response.body();
        String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "WeChat image download failed: HTTP " + response.statusCode());
        }
        if (contentType.toLowerCase().contains("json")) {
            Map<?, ?> body = objectMapper.readValue(bytes, Map.class);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "WeChat image download failed: " + body.get("errmsg"));
        }
        return new DownloadedImage(bytes, contentType);
    }

    private String apiBaseUrl() {
        return properties.getApiBaseUrl().replaceAll("/+$", "");
    }

    private static String extension(String contentType) {
        String normalized = contentType.toLowerCase();
        if (normalized.contains("jpeg") || normalized.contains("jpg")) return ".jpg";
        if (normalized.contains("png")) return ".png";
        if (normalized.contains("gif")) return ".gif";
        if (normalized.contains("webp")) return ".webp";
        return ".bin";
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record DownloadedImage(byte[] bytes, String contentType) {
    }
}
