package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.core.domain.finance.port.ReceiptAiParserPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class HttpReceiptAiParser implements ReceiptAiParserPort {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Environment environment;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public HttpReceiptAiParser(Environment environment) { this.environment = environment; }

    @Override
    public ParseResult parse(String ownerKey, List<ReceiptImage> images) {
        try {
            List<Map<String, Object>> payloadImages = images.stream().map(image -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("fileName", image.fileName()); value.put("contentType", image.contentType());
                value.put("dataUrl", "data:" + image.contentType() + ";base64," + Base64.getEncoder().encodeToString(image.content()));
                return value;
            }).toList();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("scenarioCode", "accounting_receipt_parse"); payload.put("userInput", "请识别这些记账图片，按图片索引返回明细。"); payload.put("images", payloadImages);
            String baseUrl = environment.getProperty("cynexus.finance.ai.base-url", "http://127.0.0.1:28081").replaceAll("/+$", "");
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/ai/executions/vision")).timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("AI vision request failed: " + response.statusCode());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!"0".equals(root.path("code").asText("0")) || data.isMissingNode()) throw new IllegalStateException("AI vision response is invalid");
            String content = data.path("content").asText("{}");
            JsonNode parsed = objectMapper.readTree(content);
            List<ParsedItem> items = parsed.path("items").isArray() ? java.util.stream.StreamSupport.stream(parsed.path("items").spliterator(), false).map(this::item).toList() : List.of();
            return new ParseResult(data.path("modelCode").asText(null), items, content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("AI vision request interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("AI receipt parsing failed: " + e.getMessage(), e);
        }
    }

    private ParsedItem item(JsonNode node) {
        OffsetDateTime occurredAt = null;
        String date = node.path("occurredAt").asText("");
        if (!date.isBlank()) try { occurredAt = OffsetDateTime.parse(date); } catch (Exception ignored) { }
        return new ParsedItem(node.path("imageIndex").asInt(0), node.path("transactionType").asText("expense"), occurredAt,
                node.path("amount").isNumber() ? node.path("amount").decimalValue() : null, node.path("currency").asText("CNY"),
                text(node, "merchant"), text(node, "categoryHint"), text(node, "note"), node.path("confidence").isNumber() ? node.path("confidence").decimalValue() : null,
                text(node, "rawText"));
    }

    private String text(JsonNode node, String field) { String value = node.path(field).asText(""); return value.isBlank() ? null : value; }
}
