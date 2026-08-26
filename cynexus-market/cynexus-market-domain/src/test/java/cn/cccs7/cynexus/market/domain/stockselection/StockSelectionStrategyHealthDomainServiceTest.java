package cn.cccs7.cynexus.market.domain.stockselection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StockSelectionStrategyHealthDomainServiceTest {

    @Test
    void marksFreshNormalRunAsValid() {
        StockSelectionStrategyHealthBO.Assessment assessment = service(new StockSelectionStrategyRunBO.HealthInput(
                "TREND_MOMENTUM", 1, LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 14), "SUCCEEDED", 5000, 100, "test", List.of()))
                .assess(new StockSelectionStrategyRunBO.Scope("tenant", "owner"), "TREND_MOMENTUM", "2026-08-14");

        assertEquals("VALID", assessment.status());
        assertEquals(new BigDecimal("2.00"), assessment.signalDensityPct());
    }

    @Test
    void requiresCurrentRunBeforeAssessment() {
        StockSelectionStrategyHealthBO.Assessment assessment = service(null)
                .assess(new StockSelectionStrategyRunBO.Scope("tenant", "owner"), "TREND_MOMENTUM", "2026-08-14");

        assertEquals("NOT_ASSESSED", assessment.status());
    }

    private StockSelectionStrategyHealthDomainService service(StockSelectionStrategyRunBO.HealthInput run) {
        StockSelectionStrategyCatalogBO.Strategy strategy = new StockSelectionStrategyCatalogBO.Strategy("TREND_MOMENTUM", "Trend", "test", "SCREENER_PRESET",
                new StockSelectionStrategyCatalogBO.Version(8L, 1, "{}", "[]", "test", LocalDate.of(2026, 1, 1)));
        return new StockSelectionStrategyHealthDomainService(new StockSelectionStrategyCatalogDomainService(() -> List.of(strategy)), new StockSelectionStrategyRunRepository() {
            @Override public void save(StockSelectionStrategyRunBO.Run value) { }
            @Override public Optional<StockSelectionStrategyRunBO.HealthInput> findLatestHealthInput(StockSelectionStrategyRunBO.Scope scope, String strategyCode) { return Optional.ofNullable(run); }
        }, new StockSelectionBacktestRepository() {
            @Override public void save(StockSelectionBacktestBO.Run value) { }
            @Override public Optional<StockSelectionBacktestBO.ClaimedRun> claimNextQueued() { return Optional.empty(); }
            @Override public void complete(StockSelectionBacktestBO.Run value) { }
            @Override public void fail(StockSelectionBacktestBO.ClaimedRun value, String warning) { }
            @Override public Optional<StockSelectionBacktestBO.StoredRun> findById(StockSelectionStrategyRunBO.Scope scope, long id) { return Optional.empty(); }
        });
    }
}
