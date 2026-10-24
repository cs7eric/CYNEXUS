package cn.cccs7.cynexus.common.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.common.core.exception.BizException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpImageMattingClientTest {

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00
    };

    @Test
    void postsMultipartFileWithMattingOptionsAndReturnsPng() throws Exception {
        AtomicReference<String> requestTarget = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            requestTarget.set(exchange.getRequestURI().toString());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, PNG.length);
            exchange.getResponseBody().write(PNG);
            exchange.close();
        });
        try {
            HttpImageMattingClient client = new HttpImageMattingClient(baseUrl(server), 2_000, 5_000);
            ImageMattingResult result = client.matte("source-image".getBytes(StandardCharsets.UTF_8), "food.jpg", "image/jpeg");

            assertThat(requestTarget).hasValue("/matting?stroke=8&padding=16");
            assertThat(requestBody).hasValueSatisfying(body -> {
                assertThat(body).contains("name=\"file\"");
                assertThat(body).contains("filename=\"food.jpg\"");
                assertThat(body).contains("Content-Type: image/jpeg");
                assertThat(body).contains("source-image");
            });
            assertThat(result.content()).containsExactly(PNG);
            assertThat(result.contentType()).isEqualTo("image/png");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void surfacesRemoteErrorsAsBusinessErrors() throws Exception {
        HttpServer server = server(exchange -> {
            byte[] body = "matting unavailable".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        try {
            HttpImageMattingClient client = new HttpImageMattingClient(baseUrl(server), 2_000, 5_000);
            assertThatThrownBy(() -> client.matte(new byte[]{1}, "food.jpg", "image/jpeg"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("HTTP 503");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsSuccessfulNonPngResponses() throws Exception {
        HttpServer server = server(exchange -> {
            byte[] body = "not a png".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        try {
            HttpImageMattingClient client = new HttpImageMattingClient(baseUrl(server), 2_000, 5_000);
            assertThatThrownBy(() -> client.matte(new byte[]{1}, "food.jpg", "image/jpeg"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("not a PNG");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsEmptyInputBeforeCallingRemoteService() {
        HttpImageMattingClient client = new HttpImageMattingClient("http://127.0.0.1:1", 2_000, 5_000);
        assertThatThrownBy(() -> client.matte(new byte[0], "food.jpg", "image/jpeg"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("empty");
    }

    private static HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/matting", handler);
        server.start();
        return server;
    }

    private static String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
