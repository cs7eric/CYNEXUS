package cn.cccs7.cynexus.market.domain.portfolio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioLedgerDomainServiceTest {
    private static final OffsetDateTime DAY_ONE = OffsetDateTime.parse("2026-08-01T09:30:00Z");
    private final PortfolioLedgerDomainService service = new PortfolioLedgerDomainService();

    @Test
    void calculatesMovingAverageCostAndRealizedUnrealizedPnl() {
        List<PortfolioTradeBO> trades = List.of(
                trade(1L, TradeSide.BUY, "10", "10", "1"),
                trade(2L, TradeSide.BUY, "10", "14", "0"),
                trade(3L, TradeSide.SELL, "8", "20", "2")
        );

        PortfolioLedgerResultBO result = service.calculate(trades, Map.of("600000", decimal("18")));
        PortfolioPositionBO position = result.positions().getFirst();

        assertEquals(decimal("12"), position.quantity());
        assertEquals(decimal("12.05"), position.averageCost());
        assertEquals(decimal("61.6"), result.realizedPnl());
        assertEquals(decimal("71.4"), position.unrealizedPnl());
        assertEquals(decimal("133"), result.totalPnl());
    }

    @Test
    void keepsOverviewAvailableWhenImportedHistoryStartsWithSell() {
        PortfolioLedgerResultBO result = service.calculate(
                List.of(trade(1L, TradeSide.SELL, "1", "10", "0")), Map.of());

        assertEquals(0, result.positions().size());
        assertEquals(0, result.realizedPnl().signum());
        assertEquals(1, result.warnings().size());
        assertEquals("UNMATCHED_SELL", result.warnings().getFirst().code());
        assertEquals(decimal("1"), result.warnings().getFirst().unmatchedQuantity());
    }

    @Test
    void matchesAvailableQuantityAndOnlyWarnsForTheUnmatchedRemainder() {
        PortfolioLedgerResultBO result = service.calculate(List.of(
                trade(1L, TradeSide.BUY, "2", "10", "0"),
                trade(2L, TradeSide.SELL, "3", "12", "0")
        ), Map.of());

        assertEquals(1, result.positions().size());
        assertEquals(BigDecimal.ZERO, result.positions().getFirst().quantity());
        assertEquals(decimal("4"), result.realizedPnl());
        assertEquals(decimal("1"), result.warnings().getFirst().unmatchedQuantity());
    }

    @Test
    void ignoresNonActiveTrades() {
        PortfolioTradeBO replaced = new PortfolioTradeBO(1L, "600000", "Demo", PortfolioInstrumentType.STOCK, TradeSide.BUY, DAY_ONE,
                decimal("10"), decimal("10"), BigDecimal.ZERO, TradeStatus.REPLACED);
        assertEquals(0, service.calculate(List.of(replaced), Map.of()).positions().size());
    }

    @Test
    void reportsPartialMarketDataWhenOnlySomeOpenPositionsHavePrices() {
        List<PortfolioTradeBO> trades = List.of(
                trade(1L, TradeSide.BUY, "10", "10", "0"),
                new PortfolioTradeBO(2L, "000001", "Demo", PortfolioInstrumentType.STOCK, TradeSide.BUY, DAY_ONE.plusMinutes(2),
                        decimal("10"), decimal("20"), BigDecimal.ZERO, TradeStatus.ACTIVE)
        );
        PortfolioLedgerResultBO result = service.calculate(trades, Map.of("600000", decimal("12")));

        assertEquals(PortfolioValuationStatus.PARTIAL_MARKET_DATA,
                service.valuationStatus(trades, result, Map.of("600000", decimal("12"))));
        assertEquals(PortfolioValuationStatus.LATEST_PRICE_UNAVAILABLE,
                service.valuationStatus(trades, result, Map.of()));
    }

    @Test
    void retainsEtfTypeForPositionValuationAndChartRouting() {
        PortfolioTradeBO etf = new PortfolioTradeBO(1L, "159629", "1000ETF", PortfolioInstrumentType.ETF,
                TradeSide.BUY, DAY_ONE, decimal("100"), decimal("2.99"), BigDecimal.ZERO, TradeStatus.ACTIVE);

        PortfolioPositionBO position = service.calculate(List.of(etf), Map.of("159629", decimal("3.01"))).positions().getFirst();

        assertEquals(PortfolioInstrumentType.ETF, position.instrumentType());
        assertEquals("etf", position.instrumentType().chartTargetType());
    }

    @Test
    void reportsNoConfirmedTradesForEmptyOrInactiveLedger() {
        assertEquals(PortfolioValuationStatus.NO_CONFIRMED_TRADES,
                service.valuationStatus(List.of(), service.calculate(List.of(), Map.of()), Map.of()));
    }

    private static PortfolioTradeBO trade(long id, TradeSide side, String quantity, String price, String fee) {
        return new PortfolioTradeBO(id, "600000", "Demo", PortfolioInstrumentType.STOCK, side, DAY_ONE.plusMinutes(id),
                decimal(quantity), decimal(price), decimal(fee), TradeStatus.ACTIVE);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
