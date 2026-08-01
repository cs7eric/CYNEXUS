package cn.cccs7.cynexus.market.domain.portfolio;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioImportTaskBOTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-01T10:00:00Z");

    @Test
    void allowsAsyncProcessingAndExplicitConfirmationOnly() {
        PortfolioImportTaskBO task = task();

        assertTrue(task.claimForProcessing(NOW));
        task.markReadyForReview(NOW.plusMinutes(1));
        task.confirm(NOW.plusMinutes(2));

        assertEquals(PortfolioImportStatus.CONFIRMED, task.getStatus());
        assertEquals(3, task.getVersion());
        assertThrows(IllegalStateException.class, () -> task.confirm(NOW.plusMinutes(3)));
    }

    @Test
    void duplicateProcessingClaimIsIdempotent() {
        PortfolioImportTaskBO task = task();

        assertTrue(task.claimForProcessing(NOW));
        assertFalse(task.claimForProcessing(NOW.plusMinutes(1)));
        assertEquals(1, task.getAttemptCount());
    }

    @Test
    void failedTaskNeedsExplicitRetryBeforeDispatch() {
        PortfolioImportTaskBO task = task();
        task.claimForProcessing(NOW);
        task.markFailed("VISION_TIMEOUT", "provider timed out", NOW.plusMinutes(1));
        task.retry();

        assertEquals(PortfolioImportStatus.PENDING_DISPATCH, task.getStatus());
        assertThrows(IllegalStateException.class, () -> task.confirm(NOW.plusMinutes(2)));
    }

    private static PortfolioImportTaskBO task() {
        return new PortfolioImportTaskBO(1L, "workspace-user", "default", 2L,
                "idempotency-1", PortfolioImportStatus.PENDING_DISPATCH, 0, 0);
    }
}
