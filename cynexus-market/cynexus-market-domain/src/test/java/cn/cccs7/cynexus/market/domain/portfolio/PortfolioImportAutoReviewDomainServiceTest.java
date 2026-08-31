package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class PortfolioImportAutoReviewDomainServiceTest {

    @Test
    void recordsWhyReviewWasNotApprovedWithoutConfirming() {
        PortfolioImportAutoReviewDomainService service = new PortfolioImportAutoReviewDomainService(
                new ObjectMapper(), null);

        var decision = service.decideAndConfirm(null,
                "{\"consistent\":false,\"confidence\":0.72,\"issues\":[\"price differs from screenshot\"]}",
                "ai-vision-review", OffsetDateTime.now(), LocalDate.now());

        assertFalse(decision.approved());
        assertTrue(decision.details().contains("\"consistent\":false"));
        assertTrue(decision.details().contains("\"confidence\":0.72"));
        assertTrue(decision.details().contains("price differs from screenshot"));
    }
}
