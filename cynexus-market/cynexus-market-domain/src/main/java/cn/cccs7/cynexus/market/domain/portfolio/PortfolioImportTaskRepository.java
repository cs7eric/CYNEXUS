package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.Optional;
import java.util.List;

public interface PortfolioImportTaskRepository {
    void save(PortfolioImportTaskBO task);

    void update(PortfolioImportTaskBO task);

    Optional<PortfolioImportTaskBO> findByOwnerAndId(String principalKey, String tenantKey, long taskId);

    Optional<PortfolioImportTaskBO> findByIdempotencyKey(String principalKey, String tenantKey, String idempotencyKey);

    List<PortfolioImportTaskBO> findRecentByOwner(String principalKey, String tenantKey, int limit);
}
