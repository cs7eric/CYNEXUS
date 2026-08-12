package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioTradeQueryRepository {
    List<PortfolioTradeBO> list(String principalKey, String tenantKey, String symbol);

    default Optional<PortfolioTradeBO> findById(String principalKey, String tenantKey, long tradeId) {
        return list(principalKey, tenantKey, null).stream()
                .filter(trade -> trade.id() == tradeId)
                .findFirst();
    }
}
