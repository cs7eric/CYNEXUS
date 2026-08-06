package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioPerformanceDTO {
    String range;
    LocalDate from;
    LocalDate to;
    BigDecimal periodPnl;
    BigDecimal todayPnl;
    BigDecimal periodReturnRate;
    BigDecimal startAssets;
    BigDecimal endAssets;
    BigDecimal bestPeriodPnl;
    BigDecimal worstPeriodPnl;
    int tradingDays;
    List<PortfolioPerformancePointDTO> points;
    List<PortfolioPerformanceContributionDTO> contributions;
}
