package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioPositionDTO {
    String symbol;
    BigDecimal quantity;
    BigDecimal averageCost;
    BigDecimal costBasis;
    BigDecimal realizedPnl;
    BigDecimal latestPrice;
    BigDecimal marketValue;
    BigDecimal unrealizedPnl;
    BigDecimal unrealizedReturnRate;
}
