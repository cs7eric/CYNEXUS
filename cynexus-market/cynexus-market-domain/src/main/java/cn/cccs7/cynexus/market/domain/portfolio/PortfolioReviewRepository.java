package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioReviewRepository {
    long create(PortfolioReviewBO review);
    boolean update(PortfolioReviewBO review);
    Optional<PortfolioReviewBO> findById(String principalKey, String tenantKey, long id);
    List<PortfolioReviewBO> list(String principalKey, String tenantKey, String symbol);
}
