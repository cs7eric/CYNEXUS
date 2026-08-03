package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioPerformancePointDTO {
    LocalDate tradeDate;
    BigDecimal marketValue;
    BigDecimal totalAssets;
    BigDecimal costBasis;
    BigDecimal totalPnl;
    BigDecimal periodPnl;
    BigDecimal returnRate;
}
