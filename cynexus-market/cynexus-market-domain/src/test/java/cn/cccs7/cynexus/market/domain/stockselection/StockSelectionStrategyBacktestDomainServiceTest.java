package cn.cccs7.cynexus.market.domain.stockselection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import cn.cccs7.cynexus.market.domain.service.screener.StockScreenerQueryDomainService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class StockSelectionStrategyBacktestDomainServiceTest {

    @Test
    void usesNextSessionForEntryAndChargesBothSidesOfCost() {
        StockSelectionStrategyCatalogBO.Strategy strategy = strategy();
        CapturingRepository repository = new CapturingRepository();
        StockSelectionStrategyBacktestDomainService service = service(strategy, repository);

        StockSelectionBacktestBO.Run run = service.run(new StockSelectionStrategyRunBO.Scope("tenant", "owner"), "TREND_MOMENTUM",
                new StockSelectionBacktestBO.Request("2026-01-08", 5, 1, 2, 1, 10, null, null, List.of()));

        assertEquals("SUCCEEDED", run.status());
        assertEquals(5, run.summary().signalCount());
        assertEquals(5, run.summary().evaluatedTradeCount());
        assertEquals(LocalDate.of(2026, 1, 2), run.trades().getFirst().entryDate());
        assertEquals(LocalDate.of(2026, 1, 4), run.trades().getFirst().exitDate());
        assertEquals(new BigDecimal("9.8000"), run.trades().getFirst().netReturnPct());
        assertEquals(run.id(), repository.saved.id());
    }

    @Test
    void rejectsUnsafeParameterBounds() {
        StockSelectionStrategyBacktestDomainService service = service(strategy(), new CapturingRepository());
        assertThrows(IllegalArgumentException.class, () -> service.run(new StockSelectionStrategyRunBO.Scope("tenant", "owner"), "TREND_MOMENTUM",
                new StockSelectionBacktestBO.Request("2026-01-08", 5, 1, 2, 21, 10, null, null, List.of())));
    }

    @Test
    void queuesBacktestBeforePerformingHistoricalReplay() {
        CapturingRepository repository = new CapturingRepository();
        StockSelectionStrategyBacktestDomainService service = service(strategy(), repository);

        StockSelectionBacktestBO.Run run = service.queue(new StockSelectionStrategyRunBO.Scope("tenant", "owner"), "TREND_MOMENTUM",
                new StockSelectionBacktestBO.Request("2026-01-08", 5, 1, 2, 1, 10, null, null, List.of()));

        assertEquals("QUEUED", run.status());
        assertEquals(null, run.completedAt());
        assertEquals(run.id(), repository.saved.id());
    }

    @Test
    void replaysOnlyTheRequestedSymbolWhenSingleStockModeIsEnabled() {
        StockSelectionStrategyBacktestDomainService service = service(strategy(), new CapturingRepository());

        StockSelectionBacktestBO.Run run = service.run(new StockSelectionStrategyRunBO.Scope("tenant", "owner"), "TREND_MOMENTUM",
                new StockSelectionBacktestBO.Request("2026-01-08", 5, 1, 2, 20, 10, "000001", "SZ", List.of()));

        assertEquals("000001", run.config().stockCode());
        assertEquals("SZ", run.config().stockMarket());
        assertEquals(5, run.trades().size());
        assertEquals(true, run.trades().stream().allMatch(item -> "SZ".equals(item.market()) && "000001".equals(item.stockCode())));
        assertEquals(true, run.warnings().stream().anyMatch(item -> item.contains("Single-symbol mode")));
    }

    @Test
    void rejectsPartialSingleStockTarget() {
        StockSelectionStrategyBacktestDomainService service = service(strategy(), new CapturingRepository());

        assertThrows(IllegalArgumentException.class, () -> service.run(new StockSelectionStrategyRunBO.Scope("tenant", "owner"), "TREND_MOMENTUM",
                new StockSelectionBacktestBO.Request("2026-01-08", 5, 1, 2, 1, 10, "000001", null, List.of())));
    }

    @Test
    void replaysEveryExactTargetInListModeAndDeduplicatesSymbols() {
        StockSelectionStrategyBacktestDomainService service = service(strategy(), new CapturingRepository());

        StockSelectionBacktestBO.Run run = service.run(new StockSelectionStrategyRunBO.Scope("tenant", "owner"), "TREND_MOMENTUM",
                new StockSelectionBacktestBO.Request("2026-01-08", 5, 1, 2, 20, 10, null, null,
                        List.of(new StockSelectionBacktestBO.MarketSymbol("SZ", "000001"),
                                new StockSelectionBacktestBO.MarketSymbol("SH", "600000"),
                                new StockSelectionBacktestBO.MarketSymbol("SZ", "000001"))));

        assertEquals(2, run.config().targetSymbols().size());
        assertEquals(10, run.trades().size());
        assertEquals(true, run.trades().stream().anyMatch(item -> "SZ".equals(item.market()) && "000001".equals(item.stockCode())));
        assertEquals(true, run.trades().stream().anyMatch(item -> "SH".equals(item.market()) && "600000".equals(item.stockCode())));
        assertEquals(true, run.warnings().stream().anyMatch(item -> item.contains("Target-list mode")));
    }

    private StockSelectionStrategyBacktestDomainService service(StockSelectionStrategyCatalogBO.Strategy strategy,
                                                                 CapturingRepository repository) {
        StockSelectionStrategyRunDomainService runService = new StockSelectionStrategyRunDomainService(
                new StockSelectionStrategyCatalogDomainService(() -> List.of(strategy)), screener(), new StockSelectionStrategyRunRepository() {
                    @Override public void save(StockSelectionStrategyRunBO.Run value) { }
                    @Override public java.util.Optional<StockSelectionStrategyRunBO.HealthInput> findLatestHealthInput(StockSelectionStrategyRunBO.Scope scope, String strategyCode) { return java.util.Optional.empty(); }
                }, new SequenceIdGenerator());
        return new StockSelectionStrategyBacktestDomainService(new StockSelectionStrategyCatalogDomainService(() -> List.of(strategy)),
                runService, new MarketData(), repository, new SequenceIdGenerator());
    }

    private StockScreenerQueryDomainService screener() {
        return new StockScreenerQueryDomainService() {
            @Override
            public StockScreenerQueryBO.Result screen(StockScreenerQueryBO.QueryInput input) {
                LocalDate date = LocalDate.parse(input.tradeDate());
                boolean isDefault = input.stockCode() == null;
                boolean isPingAn = "000001".equals(input.stockCode()) && "SZ".equals(input.stockMarket());
                boolean isPufa = "600000".equals(input.stockCode()) && "SH".equals(input.stockMarket());
                List<StockScreenerQueryBO.Row> rows = isDefault || isPingAn ? List.of(row("000001", "Test", "SZ"))
                        : isPufa ? List.of(row("600000", "Pufa", "SH")) : List.of();
                return new StockScreenerQueryBO.Result(true, date, date, "ALL", true, true, true, null, null, null, null, null, null,
                        "return_30d", "desc", new StockScreenerQueryBO.Page(1, 20, 0), 1, rows.size(), "test", rows, List.of(), null, null);
            }

            @Override
            public StockScreenerQueryBO.ClassificationOptions listClassificationOptions(String ignored) {
                return new StockScreenerQueryBO.ClassificationOptions(List.of(), List.of(), List.of(), List.of());
            }
        };
    }

    private StockSelectionStrategyCatalogBO.Strategy strategy() {
        return new StockSelectionStrategyCatalogBO.Strategy("TREND_MOMENTUM", "Trend", "test", "SCREENER_PRESET",
                new StockSelectionStrategyCatalogBO.Version(8L, 1,
                        "{\"universe\":\"ALL\",\"excludeSt\":true,\"minReturnPct10d\":5,\"minReturnPct30d\":10,\"minTurnoverPct\":1,\"minVolumeRatio\":1}",
                        "[]", "test", LocalDate.of(2026, 1, 1)));
    }

    private StockScreenerQueryBO.Row row(String code, String name, String market) {
        return new StockScreenerQueryBO.Row(code, name, market, null, "main_sz", false, new BigDecimal("10"), new BigDecimal("1"),
                new BigDecimal("2"), new BigDecimal("100000000"), new BigDecimal("1000000000"), null, new BigDecimal("3"),
                new BigDecimal("6"), new BigDecimal("15"), null, null, null, null, "TDX001", "Technology", "THS001", "Technology",
                List.of(), List.of(), new BigDecimal("2"), new BigDecimal("1.2"), null, null);
    }

    private static final class MarketData implements StockSelectionBacktestMarketDataRepository {
        @Override
        public List<LocalDate> listTradingDatesOnOrBefore(LocalDate ignored, int limit) {
            List<LocalDate> values = new ArrayList<>();
            for (int i = 1; i <= limit; i++) values.add(LocalDate.of(2026, 1, i));
            return values;
        }

        @Override
        public List<StockSelectionBacktestBO.DailyBar> listDailyBars(List<StockSelectionBacktestBO.MarketSymbol> symbols,
                                                                       LocalDate startDate, LocalDate endDate) {
            List<StockSelectionBacktestBO.DailyBar> values = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                LocalDate date = LocalDate.of(2026, 1, i);
                values.add(new StockSelectionBacktestBO.DailyBar("SZ", "000001", date, new BigDecimal("10")));
                values.add(new StockSelectionBacktestBO.DailyBar("SH", "600000", date, new BigDecimal("10")));
                values.add(new StockSelectionBacktestBO.DailyBar("SH", "000300", date, new BigDecimal("100")));
                if (i == 4) {
                    values.set(values.size() - 3, new StockSelectionBacktestBO.DailyBar("SZ", "000001", date, new BigDecimal("11")));
                    values.set(values.size() - 2, new StockSelectionBacktestBO.DailyBar("SH", "600000", date, new BigDecimal("11")));
                }
            }
            return values;
        }

        @Override
        public List<StockSelectionBacktestBO.DailyBar> listLatestDailyBars(List<StockSelectionBacktestBO.MarketSymbol> symbols) {
            return listDailyBars(symbols, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 8));
        }
    }

    private static final class CapturingRepository implements StockSelectionBacktestRepository {
        private StockSelectionBacktestBO.Run saved;
        @Override public void save(StockSelectionBacktestBO.Run value) { saved = value; }
        @Override public java.util.Optional<StockSelectionBacktestBO.ClaimedRun> claimNextQueued() { return java.util.Optional.empty(); }
        @Override public void complete(StockSelectionBacktestBO.Run value) { saved = value; }
        @Override public void fail(StockSelectionBacktestBO.ClaimedRun value, String warning) { }
        @Override public java.util.Optional<StockSelectionBacktestBO.StoredRun> findById(StockSelectionStrategyRunBO.Scope scope, long id) { return java.util.Optional.empty(); }
    }

    private static final class SequenceIdGenerator implements StockSelectionIdGenerator {
        private final AtomicLong value = new AtomicLong(100);
        @Override public long nextId() { return value.incrementAndGet(); }
    }
}
