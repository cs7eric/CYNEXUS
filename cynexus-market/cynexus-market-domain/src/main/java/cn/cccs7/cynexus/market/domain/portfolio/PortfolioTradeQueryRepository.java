package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;

public interface PortfolioTradeQueryRepository {
    List<PortfolioTradeBO> list(String principalKey, String tenantKey, String symbol);
}
