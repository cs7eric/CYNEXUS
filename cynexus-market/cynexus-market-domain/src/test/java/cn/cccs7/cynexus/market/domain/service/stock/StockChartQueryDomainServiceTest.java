package cn.cccs7.cynexus.market.domain.service.stock;

import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.entity.StockIntradayFeatureBO;
import cn.cccs7.cynexus.market.domain.repository.StockChartQueryRepository;
import cn.cccs7.cynexus.market.domain.service.stock.impl.StockChartQueryDomainServiceImpl;
import cn.cccs7.cynexus.market.domain.stock.impl.StockChartDomainServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockChartQueryDomainServiceTest {

    @Test
    void normalizesKlineInputsAndSelectsDailySource() {
        FakeRepository repository = new FakeRepository();
        repository.dailyBars = List.of(StockChartBO.Bar.builder().tradeDate(LocalDate.of(2026, 7, 23)).build());

        StockChartQueryDomainService service = service(repository);
        StockChartQueryDomainService.KlineResult result = service.klines(" ", "600000", "daily", " ");

        assertEquals("stock", result.targetType());
        assertEquals("1d", result.period());
        assertEquals("qfq", result.adjust());
        assertEquals("clickhouse.daily_qfq", result.source());
        assertEquals(1, result.items().size());
        assertEquals("stock", repository.dailyTargetType);
        assertEquals("qfq", repository.dailyAdjust);
    }

    @Test
    void usesLatestAvailableTradingDateAndPreservesEmptySemantics() {
        FakeRepository repository = new FakeRepository();
        repository.latestMinuteDate = Optional.of(LocalDate.of(2026, 7, 22));
        repository.minuteBars = List.of();
        repository.timeshare = List.of();

        StockChartQueryDomainService.IntradayResult result = service(repository)
                .intraday(null, "600000", null, null, "2026-07-23", List.of("1", "5min"));

        assertEquals(LocalDate.of(2026, 7, 22), result.tradeDate());
        assertEquals(LocalDate.of(2026, 7, 23), result.requestedTradeDate());
        assertEquals("clickhouse.ch_dwd_kline_1m-empty", result.source());
        assertEquals("minute kline / timeshare data not found", result.error());
        assertTrue(result.minuteBars().containsKey("1m"));
        assertTrue(result.minuteBars().containsKey("5m"));
        assertEquals("stock", repository.minuteTargetType);
    }

    @Test
    void groupsAuctionRowsInDomainAndExposesAnchorFacts() {
        FakeRepository repository = new FakeRepository();
        repository.latestAuctionDate = Optional.of(LocalDate.of(2026, 7, 23));
        repository.auctionRows = List.of(
                StockChartBO.AuctionPoint.builder()
                        .timeLabel("09:25:00").phase("auction_0925")
                        .price(new BigDecimal("10.20"))
                        .matchedVolume(new BigDecimal("100"))
                        .matchedAmount(new BigDecimal("1020"))
                        .unmatchedBuyVolume(new BigDecimal("30"))
                        .unmatchedSellVolume(new BigDecimal("10"))
                        .build(),
                StockChartBO.AuctionPoint.builder()
                        .timeLabel("15:00:00").phase("closing")
                        .price(new BigDecimal("10.30"))
                        .matchedVolume(new BigDecimal("120"))
                        .matchedAmount(new BigDecimal("1236"))
                        .build());

        StockChartQueryDomainService.AuctionResult result = service(repository)
                .auction("600000", "2026-07-23");

        assertEquals(LocalDate.of(2026, 7, 23), result.tradeDate());
        assertEquals("clickhouse.ch_ods_stock_auction_point", result.source());
        assertEquals(1, result.openingPoints().size());
        assertEquals("09:25:00", result.opening().time());
        assertTrue(result.opening().anchorExact());
        assertEquals(Boolean.TRUE, result.auction0925().get("has_auction_0925"));
        assertFalse(result.allPoints().isEmpty());
    }

    @Test
    void calculatesDailyFeatureFromPersistedBarsInDomain() {
        FakeRepository repository = new FakeRepository();
        repository.dailyBars = List.of(
                bar("2026-06-25", "10", "100", null),
                bar("2026-06-26", "11", "200", "2.50"));
        repository.meta = StockChartBO.StockMeta.builder().symbol("600519").name("贵州茅台").build();

        StockDailyFeatureBO result = service(repository).dailyFeature("600519", "2026-06-26");

        assertEquals("贵州茅台", result.getStock().getStockName());
        assertEquals(new BigDecimal("10.0000"), result.getChangePercent());
        assertEquals(new BigDecimal("2.0000"), result.getVolumeRatio());
    }

    @Test
    void keepsMissingIntradayFeatureAsNoData() {
        StockIntradayFeatureBO result = service(new FakeRepository()).intradayFeature("600519", "2026-06-26");

        org.junit.jupiter.api.Assertions.assertNull(result);
    }

    private StockChartBO.Bar bar(String date, String close, String volume, String turnoverRate) {
        return StockChartBO.Bar.builder().tradeDate(LocalDate.parse(date)).close(new BigDecimal(close))
                .open(new BigDecimal(close)).high(new BigDecimal(close)).low(new BigDecimal(close))
                .volume(new BigDecimal(volume)).turnoverRate(turnoverRate == null ? null : new BigDecimal(turnoverRate)).build();
    }

    private StockChartQueryDomainService service(FakeRepository repository) {
        return new StockChartQueryDomainServiceImpl(repository, new StockChartDomainServiceImpl());
    }

    private static final class FakeRepository implements StockChartQueryRepository {
        private List<StockChartBO.Bar> dailyBars = List.of();
        private String dailyTargetType;
        private String dailyAdjust;
        private Optional<LocalDate> latestMinuteDate = Optional.empty();
        private String minuteTargetType;
        private List<StockChartBO.Bar> minuteBars = List.of();
        private StockChartBO.StockMeta meta = StockChartBO.StockMeta.builder().build();
        private List<StockChartBO.IntradayPoint> timeshare = List.of();
        private Optional<LocalDate> latestAuctionDate = Optional.empty();
        private List<StockChartBO.AuctionPoint> auctionRows = List.of();

        @Override
        public List<StockChartBO.SearchItem> search(String keyword, int limit) { return List.of(); }

        @Override
        public List<StockChartBO.Bar> listDailyBars(String targetType, String symbol, int limit, String adjust) {
            dailyTargetType = targetType;
            dailyAdjust = adjust;
            return dailyBars;
        }

        @Override
        public Optional<LocalDate> findLatestMinuteDate(String targetType, String symbol, LocalDate requestedDate) {
            minuteTargetType = targetType;
            return latestMinuteDate;
        }

        @Override
        public List<StockChartBO.Bar> listMinuteBars(String targetType, String symbol, LocalDate tradeDate, int periodMinutes) {
            return minuteBars;
        }

        @Override
        public List<StockChartBO.IntradayPoint> listTimeshare(String targetType, String symbol, LocalDate tradeDate) {
            return timeshare;
        }

        @Override
        public Optional<LocalDate> findLatestAuctionDate(String targetType, String symbol, LocalDate requestedDate) {
            return latestAuctionDate;
        }

        @Override
        public List<LocalDate> listAuctionDates(String targetType, String symbol, int limit) { return List.of(); }

        @Override
        public List<StockChartBO.AuctionPoint> listAuctionPoints(String targetType, String symbol, LocalDate tradeDate) {
            return auctionRows;
        }

        @Override
        public StockChartBO.StockMeta stockMeta(String symbol) { return meta; }

        @Override
        public StockChartBO.StockSectors stockSectors(String symbol) { return StockChartBO.StockSectors.builder().build(); }

        @Override
        public StockChartBO.SectorStocks sectorStocks(String sectorId, Integer categoryRaw, String topicId, String name, int limit) {
            return StockChartBO.SectorStocks.builder().build();
        }

        @Override
        public StockChartBO.FundFlowSnapshot fundFlow(String symbol) { return StockChartBO.FundFlowSnapshot.builder().build(); }

        @Override
        public StockChartBO.F10Valuation valuation(String symbol, String reqId) { return StockChartBO.F10Valuation.builder().build(); }

        @Override
        public StockChartBO.F10FinanceReport financeReport(String symbol, String reportType) { return StockChartBO.F10FinanceReport.builder().build(); }

        @Override
        public StockChartBO.F10BusinessComposition businessComposition(String symbol, String reportDate) { return StockChartBO.F10BusinessComposition.builder().build(); }

        @Override
        public StockChartBO.F10ProfitForecast profitForecast(String symbol) { return StockChartBO.F10ProfitForecast.builder().build(); }

        @Override
        public StockChartBO.F10Announcements announcements(String symbol) { return StockChartBO.F10Announcements.builder().build(); }

        @Override
        public StockChartBO.F10News news(String symbol) { return StockChartBO.F10News.builder().build(); }

        @Override
        public StockChartBO.F10Roadshows roadshows(String symbol) { return StockChartBO.F10Roadshows.builder().build(); }

        @Override
        public StockChartBO.F10CompanyNews companyNews(String symbol, String section) { return StockChartBO.F10CompanyNews.builder().build(); }
    }
}
