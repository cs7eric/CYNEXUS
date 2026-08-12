package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.api.dto.PortfolioPerformanceDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioAccountBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioAccountDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioLedgerDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioLedgerResultBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformanceDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformanceGranularity;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformancePointBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformanceResultBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPricePointBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioRealtimePriceGateway;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioRealizedPnlBaselineRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeQueryRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioInstrumentType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioValuationStatus;
import cn.cccs7.cynexus.market.domain.portfolio.TradeSide;
import cn.cccs7.cynexus.market.domain.portfolio.TradeStatus;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortfolioOverviewAppServiceTest {

    @Test
    void includesClosedTradesInCurrentDayPerformancePriceSeries() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate previousDay = today.minusDays(1);
        List<PortfolioTradeBO> trades = List.of(
                trade(1L, TradeSide.BUY, previousDay, "10"),
                trade(2L, TradeSide.SELL, today, "12"));

        PortfolioTradeQueryRepository tradeRepository = mock(PortfolioTradeQueryRepository.class);
        PortfolioRealizedPnlBaselineRepository baselineRepository = mock(PortfolioRealizedPnlBaselineRepository.class);
        PortfolioAccountDomainService accountDomainService = mock(PortfolioAccountDomainService.class);
        PortfolioLedgerDomainService ledgerService = mock(PortfolioLedgerDomainService.class);
        StockChartQueryDomainService stockChartQueryDomainService = mock(StockChartQueryDomainService.class);
        PortfolioRealtimePriceGateway realtimePriceGateway = mock(PortfolioRealtimePriceGateway.class);
        PortfolioPerformanceDomainService performanceDomainService = mock(PortfolioPerformanceDomainService.class);

        when(tradeRepository.list(anyString(), anyString(), any())).thenReturn(trades);
        when(baselineRepository.latestResetAt(anyString(), anyString())).thenReturn(null);
        when(baselineRepository.list(anyString(), anyString())).thenReturn(Map.of());
        when(ledgerService.calculate(anyList(), anyMap())).thenReturn(emptyLedger());
        when(realtimePriceGateway.latestPrices(anyList())).thenReturn(Map.of());
        when(realtimePriceGateway.previousClosePrices(anyList())).thenReturn(Map.of());
        when(realtimePriceGateway.latestPriceDates(anyList())).thenReturn(Map.of());
        when(accountDomainService.get(anyString(), anyString()))
                .thenReturn(new PortfolioAccountBO(BigDecimal.ZERO, BigDecimal.ZERO, null));
        when(accountDomainService.availableCash(any(), any())).thenReturn(BigDecimal.ZERO);
        when(stockChartQueryDomainService.klines(anyString(), anyString(), eq("1d"), eq("qfq")))
                .thenReturn(new StockChartQueryDomainService.KlineResult(
                        "600000", "stock", "1d", "qfq", "test", List.of(
                        bar(previousDay, "10"), bar(today, "12"))));
        when(performanceDomainService.calculate(anyList(), anyMap(), anyMap(), any(), any(), any(), any()))
                .thenReturn(new PortfolioPerformanceResultBO(
                        PortfolioPerformanceGranularity.DAILY, today.minusDays(29), today,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, 1,
                        List.of(new PortfolioPerformancePointBO(today, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))));
        when(performanceDomainService.calculateTodayPnlBySymbol(anyList(), anyMap(), anyMap(), anyMap(), anyMap(), anyMap(), eq(today)))
                .thenReturn(Map.of("600000", new BigDecimal("20")));

        PortfolioOverviewAppService service = new PortfolioOverviewAppService(
                tradeRepository, baselineRepository, accountDomainService, ledgerService,
                stockChartQueryDomainService, realtimePriceGateway, performanceDomainService);

        service.performance("daily", null, null);

        ArgumentCaptor<Map<String, List<PortfolioPricePointBO>>> pricesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(performanceDomainService).calculateTodayPnlBySymbol(
                eq(trades), pricesCaptor.capture(), anyMap(), anyMap(), anyMap(), eq(Map.of()), eq(today));
        assertTrue(pricesCaptor.getValue().containsKey("600000"));
    }

    @Test
    void usesLiveQuotesDuringTheTradingSessionWithoutQueryingMinuteBars() {
        LocalDate today = LocalDate.parse("2026-08-04");
        Fixtures fixtures = fixturesAt(Instant.parse("2026-08-04T02:00:00Z"), today);

        fixtures.service().performance("daily", null, null);

        verify(fixtures.chart(), never()).intraday(anyString(), anyString(), anyString(), anyString(),
                eq(today.toString()), anyList());
    }

    @Test
    void queriesTodayMinuteBarsAtAndAfterTheMarketClose() {
        LocalDate today = LocalDate.parse("2026-08-04");
        Fixtures fixtures = fixturesAt(Instant.parse("2026-08-04T07:00:00Z"), today);

        fixtures.service().performance("daily", null, null);

        verify(fixtures.chart()).intraday(anyString(), anyString(), anyString(), anyString(),
                eq(today.toString()), eq(List.of("1m")));
    }

    @Test
    void preservesWeeklyAggregateWhenLiveTodayPnlIsAvailable() {
        LocalDate today = LocalDate.parse("2026-08-04");
        Fixtures fixtures = fixturesAt(Instant.parse("2026-08-04T02:00:00Z"), today);
        when(fixtures.performance().calculate(anyList(), anyMap(), anyMap(), any(), any(), any(), any()))
                .thenReturn(new PortfolioPerformanceResultBO(
                        PortfolioPerformanceGranularity.WEEKLY, today, today,
                        new BigDecimal("100"), new BigDecimal("0.1"), new BigDecimal("900"),
                        new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("100"), 1,
                        List.of(new PortfolioPerformancePointBO(today, new BigDecimal("1000"),
                                new BigDecimal("1000"), new BigDecimal("900"), new BigDecimal("100"),
                                new BigDecimal("100"), new BigDecimal("0.1")))));
        when(fixtures.performance().calculateTodayPnlBySymbol(anyList(), anyMap(), anyMap(), anyMap(), anyMap(), anyMap(), eq(today)))
                .thenReturn(Map.of("600000", new BigDecimal("5")));

        PortfolioPerformanceDTO result = fixtures.service().performance("weekly", null, null);

        assertEquals(new BigDecimal("100"), result.getPoints().getFirst().getPeriodPnl());
        assertEquals(new BigDecimal("5"), result.getTodayPnl());
    }

    @Test
    void usesOnlyTheLatestDayContributionsForTheDailyView() {
        LocalDate today = LocalDate.parse("2026-08-04");
        Fixtures fixtures = fixturesAt(Instant.parse("2026-08-04T02:00:00Z"), today);
        when(fixtures.performance().calculate(anyList(), anyMap(), anyMap(), any(), any(), any(), any()))
                .thenReturn(new PortfolioPerformanceResultBO(
                        PortfolioPerformanceGranularity.DAILY, today.minusDays(29), today,
                        new BigDecimal("150"), new BigDecimal("0.15"), new BigDecimal("850"),
                        new BigDecimal("1000"), new BigDecimal("150"), new BigDecimal("150"), 2,
                        List.of(new PortfolioPerformancePointBO(today, new BigDecimal("1000"),
                                new BigDecimal("1000"), new BigDecimal("850"), new BigDecimal("150"),
                                new BigDecimal("4"), new BigDecimal("0.01"))),
                        Map.of("600000", new BigDecimal("100"), "000001", new BigDecimal("50")),
                        Map.of("600000", new BigDecimal("5"), "000001", new BigDecimal("-1"))));
        when(fixtures.performance().calculateTodayPnlBySymbol(anyList(), anyMap(), anyMap(), anyMap(), anyMap(), anyMap(), eq(today)))
                .thenReturn(Map.of("600000", new BigDecimal("6"), "000001", new BigDecimal("-2")));

        PortfolioPerformanceDTO result = fixtures.service().performance("daily", null, null);

        Map<String, BigDecimal> contributions = result.getContributions().stream()
                .collect(Collectors.toMap(item -> item.getSymbol(), item -> item.getPeriodPnl()));
        assertEquals(Map.of("600000", new BigDecimal("6"), "000001", new BigDecimal("-2")), contributions);
        assertEquals(new BigDecimal("4"), result.getPeriodPnl());
    }

    @Test
    void keepsPostClosePricesForPerPositionTodayPnl() {
        LocalDate today = LocalDate.parse("2026-08-04");
        Fixtures fixtures = fixturesAt(Instant.parse("2026-08-04T07:00:00Z"), today);

        fixtures.service().overview();

        ArgumentCaptor<Map<String, BigDecimal>> pricesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fixtures.performance()).calculateTodayPnlBySymbol(
                anyList(), anyMap(), pricesCaptor.capture(), anyMap(), anyMap(), anyMap(), eq(today));
        assertTrue(pricesCaptor.getValue().containsKey("600000"));
    }

    @Test
    void usesFreshQuoteForAggregatedPeriodAfterMarketClose() {
        LocalDate today = LocalDate.parse("2026-08-04");
        Fixtures fixtures = fixturesAt(Instant.parse("2026-08-04T08:00:00Z"), today);
        when(fixtures.realtimePriceGateway().latestPrices(anyList()))
                .thenReturn(Map.of("600000", new BigDecimal("15")));
        when(fixtures.realtimePriceGateway().latestPriceDates(anyList()))
                .thenReturn(Map.of("600000", today));

        fixtures.service().performance("weekly", null, null);

        ArgumentCaptor<Map<String, List<PortfolioPricePointBO>>> pricesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fixtures.performance()).calculate(anyList(), pricesCaptor.capture(), anyMap(), any(), any(), any(), any());
        assertEquals(new BigDecimal("15"), pricesCaptor.getValue().get("600000").getLast().close());
    }

    @Test
    void usesTheFullRecentWindowForWeeklyPerformance() {
        LocalDate today = LocalDate.parse("2026-08-04");
        Fixtures fixtures = fixturesAt(Instant.parse("2026-08-04T02:00:00Z"), today);

        fixtures.service().performance("weekly", null, null);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(fixtures.performance()).calculate(anyList(), anyMap(), anyMap(), any(), fromCaptor.capture(), any(), any());
        assertEquals(today.minusWeeks(11).with(java.time.DayOfWeek.MONDAY), fromCaptor.getValue());
    }

    @Test
    void doesNotUseRealizedPnlResetDateAsPerformanceWindowStart() {
        LocalDate today = LocalDate.parse("2026-08-04");
        Fixtures fixtures = fixturesAt(Instant.parse("2026-08-04T02:00:00Z"), today);
        when(fixtures.baselineRepository().latestResetAt(anyString(), anyString()))
                .thenReturn(today.atStartOfDay(ZoneId.of("Asia/Shanghai")).toOffsetDateTime());

        fixtures.service().performance("monthly", "2026-01-01", "2026-12-31");

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(fixtures.performance()).calculate(anyList(), anyMap(), anyMap(), any(), fromCaptor.capture(), any(), any());
        assertEquals(LocalDate.parse("2026-01-01"), fromCaptor.getValue());
    }

    private static Fixtures fixturesAt(Instant instant, LocalDate today) {
        List<PortfolioTradeBO> trades = List.of(trade(1L, TradeSide.BUY, today.minusDays(1), "10"));
        PortfolioTradeQueryRepository tradeRepository = mock(PortfolioTradeQueryRepository.class);
        PortfolioRealizedPnlBaselineRepository baselineRepository = mock(PortfolioRealizedPnlBaselineRepository.class);
        PortfolioAccountDomainService accountDomainService = mock(PortfolioAccountDomainService.class);
        PortfolioLedgerDomainService ledgerService = mock(PortfolioLedgerDomainService.class);
        StockChartQueryDomainService chart = mock(StockChartQueryDomainService.class);
        PortfolioRealtimePriceGateway realtimePriceGateway = mock(PortfolioRealtimePriceGateway.class);
        PortfolioPerformanceDomainService performanceDomainService = mock(PortfolioPerformanceDomainService.class);

        when(tradeRepository.list(anyString(), anyString(), any())).thenReturn(trades);
        when(baselineRepository.latestResetAt(anyString(), anyString())).thenReturn(null);
        when(baselineRepository.list(anyString(), anyString())).thenReturn(Map.of());
        when(ledgerService.calculate(anyList(), anyMap())).thenReturn(emptyLedger());
        when(ledgerService.applyRealizedPnlBaselines(any(), anyMap())).thenReturn(emptyLedger());
        when(ledgerService.valuationStatus(anyList(), any(), anyMap())).thenReturn(PortfolioValuationStatus.CURRENT_MARKET);
        when(realtimePriceGateway.latestPrices(anyList())).thenReturn(Map.of());
        when(realtimePriceGateway.previousClosePrices(anyList())).thenReturn(Map.of());
        when(realtimePriceGateway.latestPriceDates(anyList())).thenReturn(Map.of());
        when(accountDomainService.get(anyString(), anyString()))
                .thenReturn(new PortfolioAccountBO(BigDecimal.ZERO, BigDecimal.ZERO, null));
        when(accountDomainService.availableCash(any(), any())).thenReturn(BigDecimal.ZERO);
        when(chart.klines(anyString(), anyString(), eq("1d"), eq("qfq")))
                .thenReturn(new StockChartQueryDomainService.KlineResult(
                        "600000", "stock", "1d", "qfq", "test", List.of(bar(today, "12"))));
        when(chart.intraday(anyString(), anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(new StockChartQueryDomainService.IntradayResult(
                        "600000", "stock", "Demo", "qfq", "qfq", "qfq", "test", today, today,
                        null, List.of(), Map.of("1m", List.of()), null));
        when(performanceDomainService.calculate(anyList(), anyMap(), anyMap(), any(), any(), any(), any()))
                .thenReturn(new PortfolioPerformanceResultBO(
                        PortfolioPerformanceGranularity.DAILY, today.minusDays(29), today,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, 1,
                        List.of(new PortfolioPerformancePointBO(today, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))));
        when(performanceDomainService.calculateTodayPnlBySymbol(anyList(), anyMap(), anyMap(), anyMap(), anyMap(), anyMap(), eq(today)))
                .thenReturn(Map.of());

        PortfolioOverviewAppService service = new PortfolioOverviewAppService(
                tradeRepository, baselineRepository, accountDomainService, ledgerService,
                chart, realtimePriceGateway, performanceDomainService,
                Clock.fixed(instant, ZoneId.of("Asia/Shanghai")));
        return new Fixtures(service, chart, performanceDomainService, realtimePriceGateway, baselineRepository);
    }

    private record Fixtures(PortfolioOverviewAppService service, StockChartQueryDomainService chart,
                            PortfolioPerformanceDomainService performance,
                            PortfolioRealtimePriceGateway realtimePriceGateway,
                            PortfolioRealizedPnlBaselineRepository baselineRepository) {
    }

    private static PortfolioTradeBO trade(long id, TradeSide side, LocalDate date, String price) {
        return new PortfolioTradeBO(id, "600000", "Demo", PortfolioInstrumentType.STOCK, side,
                date.atTime(LocalTime.of(9, 30)).atOffset(ZoneOffset.ofHours(8)),
                new BigDecimal("10"), new BigDecimal(price), BigDecimal.ZERO, TradeStatus.ACTIVE);
    }

    private static StockChartBO.Bar bar(LocalDate date, String close) {
        return StockChartBO.Bar.builder().tradeDate(date).close(new BigDecimal(close)).build();
    }

    private static PortfolioLedgerResultBO emptyLedger() {
        return new PortfolioLedgerResultBO(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }
}
