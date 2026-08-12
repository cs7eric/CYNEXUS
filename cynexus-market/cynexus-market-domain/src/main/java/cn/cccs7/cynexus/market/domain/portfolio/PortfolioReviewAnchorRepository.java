package cn.cccs7.cynexus.market.domain.portfolio;

import java.time.LocalDate;
import java.util.List;

public interface PortfolioReviewAnchorRepository {
    List<PortfolioReviewAnchorBO> list(String principalKey, String tenantKey, String symbol, LocalDate tradeDate);

    long create(PortfolioReviewAnchorBO anchor);

    boolean update(PortfolioReviewAnchorBO anchor);

    boolean delete(String principalKey, String tenantKey, long id);
}
