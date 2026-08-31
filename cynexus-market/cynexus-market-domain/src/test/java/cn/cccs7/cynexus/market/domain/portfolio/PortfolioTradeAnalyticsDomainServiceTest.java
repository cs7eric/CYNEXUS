package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortfolioTradeAnalyticsDomainServiceTest {
    private final PortfolioTradeAnalyticsDomainService service = new PortfolioTradeAnalyticsDomainService();

    @Test
    void matchesPartialSellUsingFifoAndExcludesOpenFromWinRate() {
        var trades = List.of(
                trade(1, TradeSide.BUY, "10", "10", "2026-01-01T10:00:00Z"),
                trade(2, TradeSide.BUY, "10", "12", "2026-01-02T10:00:00Z"),
                trade(3, TradeSide.SELL, "15", "14", "2026-01-03T10:00:00Z"));
        var result = service.calculate(trades, java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 4), null, null, null, null, null, null);
        assertEquals(1, result.completedCount());
        assertEquals(1, result.openCount());
        assertEquals(1, result.winCount());
        assertEquals(2, result.rounds().size());
        assertEquals(0, result.rounds().getFirst().entryQuantity().compareTo(new BigDecimal("10")));
        assertEquals(0, result.rounds().getFirst().netPnl().compareTo(new BigDecimal("40")));
    }

    @Test
    void warnsWhenSellCannotBeMatched() {
        var result = service.calculate(List.of(trade(1, TradeSide.SELL, "5", "10", "2026-01-01T10:00:00Z")),
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 2), null, null, null, null, null, null);
        assertTrue(result.dataQualityWarnings().stream().anyMatch(warning -> "UNMATCHED_SELL".equals(warning.code())));
        assertEquals(0, result.completedCount());
    }

    @Test
    void exposesPartialRoundAndStableFifoOrdering() {
        var result = service.calculate(List.of(
                trade(2, TradeSide.BUY, "10", "12", "2026-01-01T10:00:00Z"),
                trade(1, TradeSide.BUY, "10", "10", "2026-01-01T10:00:00Z"),
                trade(3, TradeSide.SELL, "5", "14", "2026-01-02T10:00:00Z")),
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 3), null, null, null, null, null, null);
        assertEquals("PARTIAL", result.rounds().getFirst().status());
        assertEquals(5, result.rounds().getFirst().exitQuantity().intValue());
        assertEquals(1, result.openCount());
    }

    @Test
    void allocatesBuyFeeAcrossMultipleSellsWithoutDroppingIt() {
        var result = service.calculate(List.of(
                tradeWithFee(1, TradeSide.BUY, "10", "10", "1.00", "2026-01-01T10:00:00Z"),
                tradeWithFee(2, TradeSide.SELL, "4", "11", "0.40", "2026-01-02T10:00:00Z"),
                tradeWithFee(3, TradeSide.SELL, "6", "11", "0.60", "2026-01-03T10:00:00Z")),
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 4), null, null, null, null, null, null);
        assertEquals(1, result.completedCount());
        assertEquals(new BigDecimal("8.00"), result.netPnl().setScale(2));
    }

    @Test
    void doesNotReportOpenLotFeeAsRealizedLoss() {
        var result = service.calculate(List.of(tradeWithFee(1, TradeSide.BUY, "10", "10", "1.00", "2026-01-01T10:00:00Z")),
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 2), null, null, null, null, null, null);
        assertEquals(0, result.rounds().getFirst().netPnl().signum());
        assertEquals(new BigDecimal("1.00"), result.rounds().getFirst().totalFee().setScale(2));
    }

    private PortfolioTradeBO trade(long id, TradeSide side, String quantity, String price, String at) {
        return new PortfolioTradeBO(id, "600000", "Test", PortfolioInstrumentType.STOCK, side,
                OffsetDateTime.parse(at).withOffsetSameInstant(ZoneOffset.ofHours(8)),
                new BigDecimal(quantity), new BigDecimal(price), BigDecimal.ZERO, TradeStatus.ACTIVE);
    }

    private PortfolioTradeBO tradeWithFee(long id, TradeSide side, String quantity, String price, String fee, String at) {
        return new PortfolioTradeBO(id, "600000", "Test", PortfolioInstrumentType.STOCK, side,
                OffsetDateTime.parse(at).withOffsetSameInstant(ZoneOffset.ofHours(8)),
                new BigDecimal(quantity), new BigDecimal(price), new BigDecimal(fee), TradeStatus.ACTIVE);
    }
}
