package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioOverviewDTO {
    List<PortfolioPositionDTO> positions;
    BigDecimal realizedPnl;
    BigDecimal unrealizedPnl;
    BigDecimal totalPnl;
    int tradeCount;
    String valuationStatus;
}
