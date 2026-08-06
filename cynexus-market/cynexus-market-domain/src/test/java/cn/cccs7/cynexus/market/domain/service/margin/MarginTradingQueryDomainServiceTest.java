package cn.cccs7.cynexus.market.domain.service.margin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustryHistoryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import cn.cccs7.cynexus.market.domain.repository.MarginTradingRepository;
import cn.cccs7.cynexus.market.domain.service.margin.impl.MarginTradingQueryDomainServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarginTradingQueryDomainServiceTest {

    @Test
    void returnsTruthfulEmptyLatestSnapshot() {
        MarginTradingQueryDomainService service = new MarginTradingQueryDomainServiceImpl(new FakeRepository());

        var value = service.latest("2026-07-23", 20);

        assertFalse(value.ok());
        assertEquals("margin trading data is empty", value.error());
    }

    @Test
    void normalizesStockQueryAndPageBounds() {
        int[] offset = new int[1];
        int[] limit = new int[1];
        MarginTradingRepository repository = new FakeRepository() {
            @Override public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) { return Optional.of(endDate); }
            @Override public List<MarginStockDetailBO> listStocks(LocalDate date, String exchange, String industry,
                                                                  String sort, String sortDir, int requestedOffset, int requestedLimit) {
                offset[0] = requestedOffset;
                limit[0] = requestedLimit;
                return List.of();
            }
        };

        var value = new MarginTradingQueryDomainServiceImpl(repository)
                .stocks("2026-07-23", " sh ", " industry ", "bad", "asc", 2, 9999, 10);

        assertEquals(500, offset[0]);
        assertEquals(500, limit[0]);
        assertEquals("SH", value.exchange());
        assertEquals("industry", value.industryCode());
        assertEquals("margin_balance", value.sort());
    }

    @Test
    void defaultsStockPageSizeToFiftyWhenLimitIsNotProvided() {
        int[] limit = new int[1];
        MarginTradingRepository repository = new FakeRepository() {
            @Override public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) { return Optional.of(endDate); }
            @Override public List<MarginStockDetailBO> listStocks(LocalDate date, String exchange, String industry,
                                                                  String sort, String sortDir, int offset, int requestedLimit) {
                limit[0] = requestedLimit;
                return List.of();
            }
        };

        new MarginTradingQueryDomainServiceImpl(repository)
                .stocks("2026-07-23", null, null, null, null, 1, null, 0);

        assertEquals(50, limit[0]);
    }

    @Test
    void clampsIndustryHistoryToConfiguredMaximum() {
        int[] limit = new int[1];
        MarginTradingRepository repository = new FakeRepository() {
            @Override public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) { return Optional.of(endDate); }
            @Override public List<MarginIndustryHistoryBO> listIndustryHistory(LocalDate date, String industry, int requestedLimit) {
                limit[0] = requestedLimit;
                return List.of();
            }
        };

        new MarginTradingQueryDomainServiceImpl(repository)
                .industryHistory("881126", "2026-07-23", 9999);

        assertEquals(500, limit[0]);
    }

    private static class FakeRepository implements MarginTradingRepository {
        @Override public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) { return Optional.empty(); }
        @Override public List<MarginMarketSummaryBO> listMarketSummaryByTradeDate(LocalDate tradeDate) { return List.of(); }
        @Override public Optional<MarginAccountStatsBO> findAccountStatsByTradeDate(LocalDate tradeDate) { return Optional.empty(); }
        @Override public List<MarginMarketSummaryBO> listMarketSummaryHistory(LocalDate start, LocalDate end) { return List.of(); }
        @Override public List<MarginStockDetailBO> listStocks(LocalDate date, String exchange, String industry, String sort, String sortDir, int offset, int limit) { return List.of(); }
        @Override public int countStocks(LocalDate date, String exchange, String industry) { return 0; }
        @Override public List<MarginStockDetailBO> listStockHistory(String stockCode, int limit) { return List.of(); }
        @Override public List<MarginIndustrySummaryBO> listIndustrySummaries(LocalDate date, String sort, String sortDir, int offset, int limit) { return List.of(); }
        @Override public int countIndustrySummaries(LocalDate date) { return 0; }
        @Override public List<MarginIndustryHistoryBO> listIndustryHistory(LocalDate date, String industry, int limit) { return List.of(); }
    }
}
