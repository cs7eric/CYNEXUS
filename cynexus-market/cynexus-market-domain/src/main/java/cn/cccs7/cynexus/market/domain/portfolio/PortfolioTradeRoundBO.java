package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PortfolioTradeRoundBO(
        long roundId,
        String symbol,
        String securityName,
        PortfolioInstrumentType instrumentType,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        BigDecimal entryQuantity,
        BigDecimal exitQuantity,
        BigDecimal averageEntryPrice,
        BigDecimal averageExitPrice,
        BigDecimal grossPnl,
        BigDecimal totalFee,
        BigDecimal netPnl,
        BigDecimal returnRate,
        long holdingDays,
        String status,
        BigDecimal unmatchedQuantity,
        List<Long> sourceTradeIds) {
}
