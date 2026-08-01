package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.Optional;

public interface PortfolioImportTaskRepository {
    void save(PortfolioImportTaskBO task);

    Optional<PortfolioImportTaskBO> findByOwnerAndId(String principalKey, String tenantKey, long taskId);

    Optional<PortfolioImportTaskBO> findByIdempotencyKey(String principalKey, String tenantKey, String idempotencyKey);
}
