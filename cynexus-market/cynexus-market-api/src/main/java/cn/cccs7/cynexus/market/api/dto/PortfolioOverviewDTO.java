package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioOverviewDTO {
    List<PortfolioPositionDTO> positions;
    List<PortfolioLedgerWarningDTO> dataQualityWarnings;
    BigDecimal costBasis;
    BigDecimal marketValue;
    BigDecimal availableCash;
    BigDecimal totalAssets;
    OffsetDateTime accountUpdatedAt;
    BigDecimal realizedPnl;
    BigDecimal unrealizedPnl;
    BigDecimal totalPnl;
    BigDecimal todayPnl;
    int tradeCount;
    String valuationStatus;
}
