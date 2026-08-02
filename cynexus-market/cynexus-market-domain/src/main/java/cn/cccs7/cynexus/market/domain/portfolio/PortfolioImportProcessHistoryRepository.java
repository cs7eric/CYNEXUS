package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.Optional;

public interface PortfolioImportProcessHistoryRepository {
    void append(PortfolioImportProcessHistoryBO history);

    Optional<PortfolioImportProcessHistoryAggregateBO> findByTaskId(String principalKey, String tenantKey, long taskId);
}
