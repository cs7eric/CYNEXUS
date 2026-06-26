package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;

public record MarketOverviewDTO(
        String tradeDate,
        BigDecimal indexChangePercent,
        BigDecimal turnoverAmount,
        BigDecimal northboundNetInflow,
        Integer riseCount,
        Integer fallCount,
        String summary
) {
}
