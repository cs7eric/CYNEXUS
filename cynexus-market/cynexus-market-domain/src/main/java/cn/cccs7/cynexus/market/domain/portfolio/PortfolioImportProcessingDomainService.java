package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PortfolioImportProcessingDomainService {
    private final ObjectMapper objectMapper;
    private final PortfolioImportTaskRepository taskRepository;
    private final PortfolioImportCandidateRepository candidateRepository;

    public PortfolioImportProcessingDomainService(ObjectMapper objectMapper,
                                                  PortfolioImportTaskRepository taskRepository,
                                                  PortfolioImportCandidateRepository candidateRepository) {
        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.candidateRepository = candidateRepository;
    }

    public void complete(PortfolioImportTaskBO task, String aiJson, OffsetDateTime completedAt) {
        try {
            JsonNode root = objectMapper.readTree(aiJson);
            if (root == null) throw new IllegalArgumentException("AI response is not valid JSON");
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray()) {
                throw new IllegalArgumentException("AI response candidates must be an array");
            }
            boolean declaredTradeScreenshot = root.path("isTradeScreenshot").asBoolean(false);
            if (!declaredTradeScreenshot && candidates.isEmpty()) {
                throw new IllegalArgumentException("AI response is not a trade screenshot");
            }
            List<PortfolioImportCandidateBO> values = new ArrayList<>();
            int fallbackRow = 1;
            for (JsonNode candidate : candidates) {
                int rowNo = candidate.path("rowNo").asInt(fallbackRow++);
                Double confidence = candidate.hasNonNull("confidence")
                        ? candidate.get("confidence").asDouble() : null;
                List<String> warnings = new ArrayList<>();
                candidate.path("warnings").forEach(item -> warnings.add(item.asText()));
                if (!declaredTradeScreenshot) {
                    warnings.add("AI did not positively classify the image as a trade screenshot; verify this candidate before confirming.");
                }
                values.add(new PortfolioImportCandidateBO(task.getId(), rowNo, candidate,
                        normalize(candidate), confidence, warnings));
            }
            candidateRepository.replace(task.getId(), values);
            task.markReadyForReview(completedAt);
            taskRepository.update(task);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI response is not valid JSON", exception);
        }
    }

    private JsonNode normalize(JsonNode candidate) {
        var normalized = objectMapper.createObjectNode();
        normalized.put("symbol", textOrNull(candidate, "symbol", "securityCode"));
        normalized.put("instrumentType", instrumentTypeOrDefault(candidate));
        normalized.put("side", textOrNull(candidate, "side"));
        normalized.put("tradedAt", tradedAtOrNull(candidate));
        normalized.put("quantity", textOrNull(candidate, "quantity", "executionQuantity"));
        normalized.put("price", textOrNull(candidate, "price", "executionPrice"));
        normalized.put("fee", textOrNull(candidate, "fee"));
        normalized.put("securityName", textOrNull(candidate, "securityName"));
        normalized.put("externalTradeNo", textOrNull(candidate, "externalTradeNo", "tradeId"));
        return normalized;
    }

    private String tradedAtOrNull(JsonNode candidate) {
        String direct = textOrNull(candidate, "tradedAt", "executionAt", "tradeDateTime", "executionDateTime",
                "transactionAt", "datetime", "dateTime", "traded_at", "execution_at");
        if (direct != null) return direct;
        String date = textOrNull(candidate, "tradeDate", "executionDate", "transactionDate", "date",
                "trade_date", "execution_date", "transaction_date");
        String time = textOrNull(candidate, "tradeTime", "executionTime", "transactionTime", "time",
                "trade_time", "execution_time", "transaction_time");
        if (date == null) return null;
        return time == null ? date : date + "T" + time;
    }

    private String textOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            String text = scalarText(value);
            if (text != null) return text;
        }
        return null;
    }

    private String scalarText(JsonNode value) {
        if (value != null && value.isObject() && value.has("value")) {
            return scalarText(value.get("value"));
        }
        if (value == null || value.isNull() || value.isContainerNode()) return null;
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private String instrumentTypeOrDefault(JsonNode candidate) {
        String value = textOrNull(candidate, "instrumentType");
        return PortfolioInstrumentType.fromPayload(value).name();
    }
}
