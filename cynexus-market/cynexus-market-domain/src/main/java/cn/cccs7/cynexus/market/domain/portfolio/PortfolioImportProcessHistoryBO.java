package cn.cccs7.cynexus.market.domain.portfolio;

import java.time.OffsetDateTime;

public record PortfolioImportProcessHistoryBO(
        long id,
        long taskId,
        String principalKey,
        String tenantKey,
        int attemptNo,
        PortfolioImportProcessEventType eventType,
        String fromStatus,
        String toStatus,
        String messageId,
        String correlationId,
        String providerCode,
        String modelCode,
        String errorCode,
        String errorMessage,
        String details,
        OffsetDateTime occurredAt,
        Long durationMs
) {
    public PortfolioImportProcessHistoryBO {
        // A zero id represents a new history event; the persistence adapter allocates it.
        if (id < 0 || taskId <= 0 || principalKey == null || principalKey.isBlank()
                || tenantKey == null || tenantKey.isBlank() || attemptNo < 0 || eventType == null
                || details == null || occurredAt == null || durationMs != null && durationMs < 0) {
            throw new IllegalArgumentException("invalid portfolio process history");
        }
    }
}
