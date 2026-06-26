package cn.cccs7.cynexus.market.domain.valueobject;

import java.math.BigDecimal;

public record MarketMoneyFlow(BigDecimal mainNetInflow, BigDecimal northboundNetInflow) {
}
