package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioPerformanceContributionDTO {
    String symbol;
    String securityName;
    String instrumentType;
    BigDecimal periodPnl;
}
