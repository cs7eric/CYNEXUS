package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;

public interface PortfolioImportCandidateRepository {
    void replace(long taskId, List<PortfolioImportCandidateBO> candidates);
}
