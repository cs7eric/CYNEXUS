package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public boolean confirmIfApproved(PortfolioImportTaskBO task, String reviewJson,
                                    String confirmedBy, OffsetDateTime confirmedAt, LocalDate today) {
        try {
            JsonNode root = objectMapper.readTree(reviewJson);
            if (root == null || !root.isObject()
                    || !root.path("consistent").asBoolean(false)
                    || root.path("confidence").asDouble(0d) < MIN_CONFIDENCE
                    || !root.path("issues").isArray()
                    || !root.path("issues").isEmpty()) {
                return false;
            }
            confirmationService.confirm(task, List.of(), confirmedBy, confirmedAt, today);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
