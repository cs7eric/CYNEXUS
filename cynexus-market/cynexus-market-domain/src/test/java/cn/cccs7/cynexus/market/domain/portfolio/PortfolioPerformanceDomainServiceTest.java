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
    void aggregatesWeeklyAndYearlyPoints() {
        PortfolioTradeBO trade = new PortfolioTradeBO(1L, "600000", "Demo", PortfolioInstrumentType.STOCK,
                TradeSide.BUY, DAY_ONE, decimal("10"), decimal("10"), BigDecimal.ZERO, TradeStatus.ACTIVE);
        Map<String, List<PortfolioPricePointBO>> prices = Map.of("600000", List.of(
                price("2026-07-01", "10"), price("2026-07-03", "11"),
                price("2026-07-10", "12"), price("2026-12-31", "13")));

        PortfolioPerformanceResultBO weekly = service.calculate(List.of(trade), prices, Map.of(), BigDecimal.ZERO,
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-12"), PortfolioPerformanceGranularity.WEEKLY);
        PortfolioPerformanceResultBO yearly = service.calculate(List.of(trade), prices, Map.of(), BigDecimal.ZERO,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), PortfolioPerformanceGranularity.YEARLY);

        assertEquals(2, weekly.points().size());
        assertEquals(1, yearly.points().size());
        assertEquals(0, decimal("20").compareTo(weekly.periodPnl()));
        assertEquals(0, decimal("30").compareTo(yearly.periodPnl()));
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

        assertEquals(0, decimal("12.5").compareTo(todayPnl));
    }

    @Test
    void keepsTodayPnlAccurateWhenDisplayedAverageCostIsRounded() {
        List<PortfolioTradeBO> trades = List.of(
                trade(1L, "159796", TradeSide.BUY, "4100", "1.40608519", "2026-07-30T00:00:00Z"),
                trade(2L, "159796", TradeSide.SELL, "1000", "0.840", "2026-07-31T09:30:43Z"),
                trade(3L, "159796", TradeSide.SELL, "400", "0.850", "2026-07-31T09:48:44Z"),
                trade(4L, "159796", TradeSide.BUY, "500", "0.840", "2026-07-31T10:38:17Z"),
                trade(5L, "159796", TradeSide.BUY, "200", "0.840", "2026-07-31T11:24:58Z"),
                trade(6L, "159796", TradeSide.BUY, "500", "0.833", "2026-08-03T00:00:00Z")
        );

        BigDecimal todayPnl = service.calculateTodayPnl(
                trades,
                Map.of("159796", List.of(price("2026-07-31", "0.837"))),
                Map.of("159796", decimal("0.836")),
                LocalDate.parse("2026-08-03"));

        assertEquals(0, decimal("-1.9").compareTo(todayPnl));
    }

    @Test
    void calculatesTodayPnlUsingOpeningQuantityAndTodayBuyPrice() {
        PortfolioTradeBO openingTrade = new PortfolioTradeBO(1L, "159796", "Battery ETF", PortfolioInstrumentType.ETF,
                TradeSide.BUY, DAY_ONE, decimal("3400"), decimal("0.84"), BigDecimal.ZERO, TradeStatus.ACTIVE);
        PortfolioTradeBO todayTrade = new PortfolioTradeBO(2L, "159796", "Battery ETF", PortfolioInstrumentType.ETF,
                TradeSide.BUY, OffsetDateTime.parse("2026-07-02T09:30:00Z"), decimal("500"), decimal("0.833"), BigDecimal.ZERO,
                TradeStatus.ACTIVE);

        BigDecimal todayPnl = service.calculateTodayPnl(
                List.of(openingTrade, todayTrade),
                Map.of("159796", List.of(price("2026-07-01", "0.837"))),
                Map.of("159796", decimal("0.836")),
                Map.of("159796", decimal("0.837")),
                LocalDate.parse("2026-07-02"));

        assertEquals(0, decimal("-1.9").compareTo(todayPnl));
    }

    @Test
    void calculatesHistoricalDailyPointFromPreviousCloseAndSameDayTrades() {
        List<PortfolioTradeBO> trades = List.of(
                trade(1L, "600000", TradeSide.BUY, "100", "10", "2026-07-30T09:30:00Z"),
                trade(2L, "600000", TradeSide.BUY, "50", "12.5", "2026-08-03T09:30:00Z")
        );

        PortfolioPerformanceResultBO result = service.calculate(
                trades,
                Map.of("600000", List.of(price("2026-07-31", "12"), price("2026-08-03", "13"))),
                Map.of(), BigDecimal.ZERO, LocalDate.parse("2026-08-03"), LocalDate.parse("2026-08-03"),
                PortfolioPerformanceGranularity.DAILY);

        assertEquals(0, decimal("125").compareTo(result.periodPnl()));
        assertEquals(0, decimal("125").compareTo(result.points().getFirst().periodPnl()));
    }

    private static PortfolioPricePointBO price(String date, String close) {
        return new PortfolioPricePointBO(LocalDate.parse(date), decimal(close));
    }

    private static PortfolioTradeBO trade(long id, String symbol, TradeSide side, String quantity, String price,
                                          String tradedAt) {
        return new PortfolioTradeBO(id, symbol, "电池", PortfolioInstrumentType.ETF, side,
                OffsetDateTime.parse(tradedAt), decimal(quantity), decimal(price), BigDecimal.ZERO, TradeStatus.ACTIVE);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
