package cn.cccs7.cynexus.market.domain.entity;

import cn.cccs7.cynexus.market.domain.valueobject.MarketBreadth;
import cn.cccs7.cynexus.market.domain.valueobject.MarketMoneyFlow;
import java.math.BigDecimal;

public record MarketOverviewBO(
        String tradeDate,
        BigDecimal indexChangePercent,
        BigDecimal turnoverAmount,
        MarketBreadth breadth,
        MarketMoneyFlow moneyFlow
) {
}
