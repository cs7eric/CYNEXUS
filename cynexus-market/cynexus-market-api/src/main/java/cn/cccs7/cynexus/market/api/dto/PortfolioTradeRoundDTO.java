package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioTradeRoundDTO {
    long roundId;
    String symbol;
    String securityName;
    String instrumentType;
    OffsetDateTime openedAt;
    OffsetDateTime closedAt;
    BigDecimal entryQuantity;
    BigDecimal exitQuantity;
    BigDecimal averageEntryPrice;
    BigDecimal averageExitPrice;
    BigDecimal grossPnl;
    BigDecimal totalFee;
    BigDecimal netPnl;
    BigDecimal returnRate;
    long holdingDays;
    String status;
    BigDecimal unmatchedQuantity;
    List<Long> sourceTradeIds;
}
