package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/** Applies the bounded AI review decision; uncertain reviews remain manual. */
@Service
public class PortfolioImportAutoReviewDomainService {
    private static final double MIN_CONFIDENCE = 0.85d;
    private final ObjectMapper objectMapper;
    private final PortfolioImportConfirmationDomainService confirmationService;

    public PortfolioImportAutoReviewDomainService(ObjectMapper objectMapper,
                                                  PortfolioImportConfirmationDomainService confirmationService) {
        this.objectMapper = objectMapper;
        this.confirmationService = confirmationService;
    }

    public AutoReviewDecision decideAndConfirm(PortfolioImportTaskBO task, String reviewJson,
                                               String confirmedBy, OffsetDateTime confirmedAt, LocalDate today) {
        try {
            JsonNode root = objectMapper.readTree(reviewJson);
            if (root == null || !root.isObject()) {
                return rejected(false, 0d, List.of(), "invalid_review_response");
            }
            boolean consistent = root.path("consistent").asBoolean(false);
            double confidence = root.path("confidence").asDouble(0d);
            List<String> issues = root.path("issues").isArray()
                    ? java.util.stream.StreamSupport.stream(root.path("issues").spliterator(), false)
                    .limit(10).map(JsonNode::asText).map(this::truncate).toList()
                    : List.of("invalid_issues");
            if (!consistent || confidence < MIN_CONFIDENCE || !issues.isEmpty()) {
                return rejected(consistent, confidence, issues, "review_not_approved");
            }
            confirmationService.confirm(task, List.of(), confirmedBy, confirmedAt, today);
            return new AutoReviewDecision(true, details(true, consistent, confidence, issues, "approved"));
        } catch (Exception ignored) {
            return rejected(false, 0d, List.of(), "invalid_review_response");
        }
    }

    private AutoReviewDecision rejected(boolean consistent, double confidence, List<String> issues, String reason) {
        return new AutoReviewDecision(false, details(false, consistent, confidence, issues, reason));
    }

    private String truncate(String value) {
        return value.length() <= 240 ? value : value.substring(0, 240);
    }

    private String details(boolean approved, boolean consistent, double confidence, List<String> issues, String reason) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("approved", approved);
        details.put("consistent", consistent);
        details.put("confidence", confidence);
        details.put("reason", reason);
        details.putPOJO("issues", issues);
        return details.toString();
    }

    public record AutoReviewDecision(boolean approved, String details) {
    }
}
