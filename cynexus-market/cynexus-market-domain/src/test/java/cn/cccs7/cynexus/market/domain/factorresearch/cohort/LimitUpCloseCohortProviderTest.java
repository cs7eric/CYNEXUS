package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimitUpCloseCohortProviderTest {
    @Test
    void resolvesPersistedFinalLimitUpFactsWithShanghaiCloseAnchor() {
        LocalDate date = LocalDate.of(2026, 8, 14);
        LimitUpCloseCohortProvider provider = new LimitUpCloseCohortProvider(new PulseRepository(date));
        MarketEventCohort cohort = provider.resolve(new MarketEventCohortQuery(MarketFactorResearchBO.EventType.LIMIT_UP_CLOSE, 1, date, date, 10));
        assertEquals(1, cohort.samples().size());
        assertEquals("600000", cohort.samples().getFirst().code());
        assertEquals("SH", cohort.samples().getFirst().market());
        assertEquals("15:00", cohort.samples().getFirst().anchorAt().toLocalTime().toString());
        assertEquals("limit_up", cohort.samples().getFirst().sourceFacts().get("category"));
    }

    @Test
    void registryRejectsUnknownDefinitionVersion() {
        LimitUpCloseCohortProvider provider = new LimitUpCloseCohortProvider(new PulseRepository(LocalDate.of(2026, 8, 14)));
        MarketEventCohortProviderRegistry registry = new MarketEventCohortProviderRegistry(List.of(provider));
        assertThrows(IllegalArgumentException.class, () -> registry.require(MarketFactorResearchBO.EventType.LIMIT_UP_CLOSE, 2));
    }

    private static final class PulseRepository implements MarketPulseQueryRepository {
        private final LocalDate date;
        private PulseRepository(LocalDate date) { this.date = date; }
        @Override public List<MarketLimitDailyStockBO> listLimitStocks(LocalDate value, String category, int limit) {
            return date.equals(value) && "limit_up".equals(category) ? List.of(MarketLimitDailyStockBO.builder().code("600000")
                    .category(category).changePct(new BigDecimal("10")).streak(1).build()) : List.of();
        }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO> findLatestOverview() { return Optional.empty(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO> findOverviewByDate(LocalDate v) { return Optional.empty(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO> findPreviousOverview(LocalDate v) { return Optional.empty(); }
        @Override public List<cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO> listOverviewSince(LocalDate v, int n) { return List.of(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO> findManualFundFlow(LocalDate v) { return Optional.empty(); }
        @Override public void upsertManualFundFlow(LocalDate v, cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO b) { }
        @Override public Optional<LocalDate> findLatestPulseTradeDate(LocalDate v) { return Optional.empty(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO> findLatestPulseBatch(LocalDate v) { return Optional.empty(); }
        @Override public List<LocalDate> listLatestPulseTradeDates(int n) { return List.of(); }
        @Override public List<cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO> listPulseRowsByTradeDate(LocalDate v) { return List.of(); }
        @Override public List<cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO> listPulseRowsByTradeDates(List<LocalDate> v) { return List.of(); }
        @Override public List<cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO> listLatestStyleSectors() { return List.of(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO> findLatestStyleSector(String v) { return Optional.empty(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO> findLatestHeatmap(String v) { return Optional.empty(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO> findLatestLimitEmotion() { return Optional.empty(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO> findLimitEmotionByDate(LocalDate v) { return Optional.empty(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO> findLimitEmotionBefore(LocalDate v) { return Optional.empty(); }
        @Override public List<cn.cccs7.cynexus.market.domain.entity.IndexReturnDailyBO> listLatestIndexReturns(int n) { return List.of(); }
        @Override public List<cn.cccs7.cynexus.market.domain.entity.IndexReturnDailyBO> listIndexReturnsHistory(int n, LocalDate a, LocalDate b) { return List.of(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.IndexReturnDailyBO> findLatestIndexReturnByCode(String v) { return Optional.empty(); }
        @Override public Optional<LocalDate> findLatestIndexMinuteDate(String v, LocalDate d) { return Optional.empty(); }
        @Override public Optional<cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO> findDailyBreadth(LocalDate d) { return Optional.empty(); }
        @Override public List<cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO> listDailyBreadth(LocalDate a, LocalDate b) { return List.of(); }
        @Override public List<cn.cccs7.cynexus.market.domain.entity.IndexMinuteBarBO> listIndexMinuteBars(String c, LocalDate d) { return List.of(); }
        @Override public Optional<BigDecimal> findIndexPreviousClose(String c, LocalDate d) { return Optional.empty(); }
    }
}
