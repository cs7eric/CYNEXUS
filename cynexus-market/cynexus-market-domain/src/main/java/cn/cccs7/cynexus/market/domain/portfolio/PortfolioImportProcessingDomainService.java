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
            if (root == null || !root.path("isTradeScreenshot").asBoolean(false)) {
                throw new IllegalArgumentException("AI response is not a trade screenshot");
            }
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray()) {
                throw new IllegalArgumentException("AI response candidates must be an array");
            }
            List<PortfolioImportCandidateBO> values = new ArrayList<>();
            int fallbackRow = 1;
            for (JsonNode candidate : candidates) {
                int rowNo = candidate.path("rowNo").asInt(fallbackRow++);
                Double confidence = candidate.hasNonNull("confidence")
                        ? candidate.get("confidence").asDouble() : null;
                List<String> warnings = new ArrayList<>();
                candidate.path("warnings").forEach(item -> warnings.add(item.asText()));
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
        normalized.put("symbol", textOrNull(candidate, "symbol"));
        normalized.put("side", textOrNull(candidate, "side"));
        normalized.put("tradedAt", textOrNull(candidate, "tradedAt"));
        normalized.put("quantity", textOrNull(candidate, "quantity"));
        normalized.put("price", textOrNull(candidate, "price"));
        normalized.put("fee", textOrNull(candidate, "fee"));
        normalized.put("securityName", textOrNull(candidate, "securityName"));
        normalized.put("externalTradeNo", textOrNull(candidate, "externalTradeNo"));
        return normalized;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
