package cn.cccs7.cynexus.market.domain.portfolio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioPerformanceDomainServiceTest {
    private static final OffsetDateTime DAY_ONE = OffsetDateTime.parse("2026-07-01T09:30:00Z");
    private final PortfolioPerformanceDomainService service = new PortfolioPerformanceDomainService(new PortfolioLedgerDomainService());

    @Test
    void calculatesDailyPnlAndReturnFromHistoricalClosingPrices() {
        PortfolioTradeBO trade = new PortfolioTradeBO(1L, "600000", "Demo", PortfolioInstrumentType.STOCK,
                TradeSide.BUY, DAY_ONE, decimal("10"), decimal("10"), BigDecimal.ZERO, TradeStatus.ACTIVE);
        PortfolioPerformanceResultBO result = service.calculate(
                List.of(trade),
                Map.of("600000", List.of(price("2026-07-01", "10"), price("2026-07-02", "12"))),
                Map.of(), decimal("100"), LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-02"),
                PortfolioPerformanceGranularity.DAILY);

        assertEquals(2, result.points().size());
        assertEquals(0, decimal("20").compareTo(result.periodPnl()));
        assertEquals(0, decimal("0.1").compareTo(result.periodReturnRate()));
        assertEquals(0, decimal("220").compareTo(result.endAssets()));
        assertEquals(0, decimal("20").compareTo(result.points().get(1).periodPnl()));
    }

    @Test
    void aggregatesMonthlyPointsByTheLastTradingDayOfEachMonth() {
        PortfolioTradeBO trade = new PortfolioTradeBO(1L, "600000", "Demo", PortfolioInstrumentType.STOCK,
                TradeSide.BUY, DAY_ONE, decimal("10"), decimal("10"), BigDecimal.ZERO, TradeStatus.ACTIVE);
        PortfolioPerformanceResultBO result = service.calculate(
                List.of(trade),
                Map.of("600000", List.of(price("2026-07-01", "10"), price("2026-07-31", "11"), price("2026-08-03", "13"))),
                Map.of(), BigDecimal.ZERO, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-31"),
                PortfolioPerformanceGranularity.MONTHLY);

        assertEquals(2, result.points().size());
        assertEquals(LocalDate.parse("2026-07-31"), result.points().getFirst().tradeDate());
        assertEquals(LocalDate.parse("2026-08-03"), result.points().getLast().tradeDate());
        assertEquals(0, decimal("30").compareTo(result.periodPnl()));
    }

    @Test
    void calculatesTodayPnlFromPreviousCloseAndTodayTrades() {
        PortfolioTradeBO openingTrade = new PortfolioTradeBO(1L, "600000", "Demo", PortfolioInstrumentType.STOCK,
                TradeSide.BUY, DAY_ONE, decimal("10"), decimal("10"), BigDecimal.ZERO, TradeStatus.ACTIVE);
        PortfolioTradeBO todayTrade = new PortfolioTradeBO(2L, "600000", "Demo", PortfolioInstrumentType.STOCK,
                TradeSide.BUY, OffsetDateTime.parse("2026-07-02T09:30:00Z"), decimal("5"), decimal("12.5"), BigDecimal.ZERO,
                TradeStatus.ACTIVE);

        BigDecimal todayPnl = service.calculateTodayPnl(
                List.of(openingTrade, todayTrade),
                Map.of("600000", List.of(price("2026-07-01", "12"))),
                Map.of("600000", decimal("13")),
                LocalDate.parse("2026-07-02"));

        assertEquals(0, decimal("12.505").compareTo(todayPnl));
    }

    private static PortfolioPricePointBO price(String date, String close) {
        return new PortfolioPricePointBO(LocalDate.parse(date), decimal(close));
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
