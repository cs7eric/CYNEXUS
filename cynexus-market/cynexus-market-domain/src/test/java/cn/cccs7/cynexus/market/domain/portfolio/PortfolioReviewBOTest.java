package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PortfolioReviewBOTest {
    @Test
    void defaultsEmptyJsonbContent() {
        PortfolioReviewBO review = review(null, null);

        assertEquals("[]", review.tags());
        assertEquals("{}", review.metricsSnapshot());
    }

    @Test
    void convertsHumanReadableTagsToJsonArray() {
        PortfolioReviewBO review = review("突破、跟随,止损", null);

        assertEquals("[\"突破\",\"跟随\",\"止损\"]", review.tags());
    }

    private PortfolioReviewBO review(String tags, String metricsSnapshot) {
        return new PortfolioReviewBO(1L, "principal", "tenant", "600000",
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 5),
                null, null, null, null, null, tags, null, metricsSnapshot, "DRAFT");
    }
}
