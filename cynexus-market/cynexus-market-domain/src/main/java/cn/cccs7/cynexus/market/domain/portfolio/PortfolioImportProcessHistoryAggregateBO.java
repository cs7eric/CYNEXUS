package cn.cccs7.cynexus.market.domain.portfolio;

import java.time.OffsetDateTime;
import java.util.List;

public record PortfolioImportProcessHistoryAggregateBO(
        long id,
        long taskId,
        String principalKey,
        String tenantKey,
        List<PortfolioImportProcessHistoryBO> events,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public PortfolioImportProcessHistoryAggregateBO {
        if (id <= 0 || taskId <= 0 || principalKey == null || principalKey.isBlank()
                || tenantKey == null || tenantKey.isBlank() || events == null
                || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("invalid portfolio process history aggregate");
        }
        events = List.copyOf(events);
    }
}
