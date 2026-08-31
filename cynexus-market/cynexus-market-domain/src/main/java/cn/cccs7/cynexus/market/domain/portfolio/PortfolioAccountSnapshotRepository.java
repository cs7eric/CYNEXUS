package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;

public interface PortfolioAccountSnapshotRepository {
    List<PortfolioAccountSnapshotBO> list(String principalKey, String tenantKey);
    PortfolioAccountSnapshotBO upsert(String principalKey, String tenantKey, PortfolioAccountSnapshotBO snapshot);
}
