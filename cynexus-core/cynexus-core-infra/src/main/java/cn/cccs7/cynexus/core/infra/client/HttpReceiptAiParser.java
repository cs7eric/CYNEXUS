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
    public ParseResult parse(String ownerKey, List<ReceiptImage> images, List<AccountCandidate> accounts,
                             List<CategoryCandidate> categories) {
        try {
            List<Map<String, Object>> payloadImages = images.stream().map(image -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("fileName", image.fileName()); value.put("contentType", image.contentType());
                value.put("dataUrl", "data:" + image.contentType() + ";base64," + Base64.getEncoder().encodeToString(image.content()));
                return value;
            }).toList();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("scenarioCode", "accounting_receipt_parse");
            payload.put("userInput", candidatePrompt(accounts, categories));
            payload.put("images", payloadImages);
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
                text(node, "merchant"), longValue(node, "accountId"), text(node, "accountHint"), longValue(node, "categoryId"),
                text(node, "categoryHint"), text(node, "note"), integerValue(node, "installmentCount"), decimalValue(node, "installmentFee"),
                node.path("confidence").isNumber() ? node.path("confidence").decimalValue() : null,
                text(node, "rawText"));
    }

    private String candidatePrompt(List<AccountCandidate> accounts, List<CategoryCandidate> categories) {
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("instruction", "只从下面候选中选择 accountId 和 categoryId，不得创造新的账户或分类；transactionType 只能是 expense 或 income。若无法确定账户，accountId 返回 null。日期缺失返回 null。若图片明确显示分期、期数或手续费，填写 installmentCount 和 installmentFee，否则返回 null。");
            context.put("accounts", accounts.stream().map(item -> Map.of("id", item.id(), "name", item.name(), "accountType", item.accountType(), "nature", item.nature(), "notes", item.notes() == null ? "" : item.notes())).toList());
            context.put("categories", categories.stream().map(item -> Map.of("id", item.id(), "name", item.name(), "direction", item.direction())).toList());
            return objectMapper.writeValueAsString(context);
        } catch (Exception exception) {
            throw new IllegalStateException("AI receipt candidate context failed", exception);
        }
    }

    private String text(JsonNode node, String field) { String value = node.path(field).asText(""); return value.isBlank() ? null : value; }
    private Long longValue(JsonNode node, String field) { return node.path(field).canConvertToLong() ? node.path(field).asLong() : null; }
    private Integer integerValue(JsonNode node, String field) { return node.path(field).canConvertToInt() ? node.path(field).asInt() : null; }
    private java.math.BigDecimal decimalValue(JsonNode node, String field) { return node.path(field).isNumber() ? node.path(field).decimalValue() : null; }
}
