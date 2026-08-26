package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class PortfolioImportTaskBOTest {
    @Test
    void clearsRetryErrorWhenTheNextAttemptReachesReview() {
        OffsetDateTime now = OffsetDateTime.now();
        PortfolioImportTaskBO task = PortfolioImportTaskBO.rehydrate(1L, "owner", "tenant", 2L, "key",
                PortfolioImportStatus.RETRY_WAITING, 1, 1, now, now, now, null, null,
                "PORTFOLIO_PARSE_RETRY", "AI vision request failed");

        task.claimForProcessing(now.plusSeconds(1));
        task.markReadyForReview(now.plusSeconds(2));

        assertNull(task.getLastErrorCode());
        assertNull(task.getLastErrorMessage());
    }
}
