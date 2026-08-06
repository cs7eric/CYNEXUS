package cn.cccs7.cynexus.market.domain.portfolio;

import java.time.LocalDate;

public record PortfolioReviewBO(Long id, String principalKey, String tenantKey, String symbol,
                                LocalDate periodStart, LocalDate periodEnd, String thesis, String plan,
                                String execution, String lessons, String nextAction, String tags,
                                Integer rating, String metricsSnapshot, String status) {
    public PortfolioReviewBO {
        if (symbol == null || symbol.isBlank() || periodStart == null || periodEnd == null
                || periodEnd.isBefore(periodStart) || rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("invalid portfolio review");
        }
        tags = PortfolioReviewContentPolicy.normalizeTags(tags);
        metricsSnapshot = PortfolioReviewContentPolicy.normalizeMetricsSnapshot(metricsSnapshot);
    }
}
