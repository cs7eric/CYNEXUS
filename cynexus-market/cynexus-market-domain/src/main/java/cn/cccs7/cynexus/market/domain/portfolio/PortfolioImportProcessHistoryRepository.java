package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;

public interface PortfolioImportProcessHistoryRepository {
    void append(PortfolioImportProcessHistoryBO history);

    List<PortfolioImportProcessHistoryBO> listByTaskId(String principalKey, String tenantKey, long taskId);
}
