package cn.cccs7.cynexus.market.domain.portfolio;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortfolioImportProcessHistoryBOTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-01T10:00:00Z");

    @Test
    void allowsZeroIdForNewHistoryEvents() {
        assertDoesNotThrow(() -> new PortfolioImportProcessHistoryBO(
                0L, 42L, "workspace-user", "default", 0,
                PortfolioImportProcessEventType.TASK_CREATED, null, "PENDING_DISPATCH",
                null, "correlation-1", null, null, null, null, "{}", NOW, null));
    }

    @Test
    void rejectsNegativeIds() {
        assertThrows(IllegalArgumentException.class, () -> new PortfolioImportProcessHistoryBO(
                -1L, 42L, "workspace-user", "default", 0,
                PortfolioImportProcessEventType.TASK_CREATED, null, "PENDING_DISPATCH",
                null, "correlation-1", null, null, null, null, "{}", NOW, null));
    }

    @Test
    void groupsEventsInOneTaskHistoryRecord() {
        PortfolioImportProcessHistoryBO event = new PortfolioImportProcessHistoryBO(
                1L, 42L, "workspace-user", "default", 0,
                PortfolioImportProcessEventType.TASK_CREATED, null, "PENDING_DISPATCH",
                null, "correlation-1", null, null, null, null, "{}", NOW, null);

        PortfolioImportProcessHistoryAggregateBO aggregate = new PortfolioImportProcessHistoryAggregateBO(
                1L, 42L, "workspace-user", "default", List.of(event), NOW, NOW);

        assertEquals(1, aggregate.events().size());
        assertEquals(PortfolioImportProcessEventType.TASK_CREATED, aggregate.events().getFirst().eventType());
    }
}
