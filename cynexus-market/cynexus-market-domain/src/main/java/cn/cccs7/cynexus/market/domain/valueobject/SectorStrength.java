package cn.cccs7.cynexus.market.domain.valueobject;

import java.math.BigDecimal;

public record SectorStrength(BigDecimal changePercent, BigDecimal turnoverAmount, BigDecimal mainNetInflow) {
}
