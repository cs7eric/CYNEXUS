package cn.cccs7.cynexus.common.image;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HTTP implementation shared by services that need background removal.
 * The remote contract is intentionally kept here so callers only depend on the
 * small {@link ImageMattingClient} port.
 */
public class HttpImageMattingClient implements ImageMattingClient {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8000";
    private static final String MATTE_PATH = "/matting?stroke=8&padding=16";
    private static final String PNG_CONTENT_TYPE = "image/png";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Duration requestTimeout;

    public HttpImageMattingClient(String baseUrl, long connectTimeoutMs, long requestTimeoutMs) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.requestTimeout = Duration.ofMillis(Math.max(requestTimeoutMs, 1_000));
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1_000)))
                .build();
    }

    @Override
    public ImageMattingResult matte(byte[] content, String fileName, String contentType) {
        if (content == null || content.length == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Image file is empty");
        }
        String boundary = "----CynexusImageBoundary" + System.nanoTime();
        byte[] body = multipartBody(content, fileName, contentType, boundary);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + MATTE_PATH))
                .timeout(requestTimeout)
                .header("Accept", PNG_CONTENT_TYPE)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String detail = response.body() == null ? "" : new String(response.body(), StandardCharsets.UTF_8);
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "Image matting request failed: HTTP " + response.statusCode() + (detail.isBlank() ? "" : " " + detail));
            }
            byte[] result = response.body();
            if (result == null || result.length == 0) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "Image matting response was empty");
            }
            String responseType = response.headers().firstValue("Content-Type")
                    .map(value -> value.split(";", 2)[0].trim())
                    .orElse(PNG_CONTENT_TYPE);
            if (!PNG_CONTENT_TYPE.equalsIgnoreCase(responseType)) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "Image matting response was not a PNG image");
            }
            if (!isPng(result)) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "Image matting response did not contain PNG data");
            }
            return new ImageMattingResult(result, PNG_CONTENT_TYPE);
        } catch (BizException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Image matting request interrupted");
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR,
                    "Image matting request failed: " + request.uri() + " (" + message(exception) + ")");
        }
    }

    private static byte[] multipartBody(byte[] content, String fileName, String contentType, String boundary) {
        String safeName = fileName == null || fileName.isBlank()
                ? "image.jpg"
                : fileName.replace("\"", "").replace("\r", "").replace("\n", "");
        String safeType = contentType != null && contentType.matches("[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+")
                ? contentType
                : "application/octet-stream";
        byte[] header = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeName + "\"\r\n"
                + "Content-Type: " + safeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[header.length + content.length + footer.length];
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(content, 0, body, header.length, content.length);
        System.arraycopy(footer, 0, body, header.length + content.length, footer.length);
        return body;
    }

    private static String trimTrailingSlash(String value) {
        String result = value == null || value.isBlank() ? DEFAULT_BASE_URL : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String message(Exception exception) {
        String value = exception.getMessage();
        return value == null || value.isBlank() ? exception.getClass().getSimpleName() : value;
    }

    private static boolean isPng(byte[] content) {
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

}
