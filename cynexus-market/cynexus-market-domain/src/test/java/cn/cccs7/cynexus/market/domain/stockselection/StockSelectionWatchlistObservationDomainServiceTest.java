package cn.cccs7.cynexus.market.domain.stockselection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StockSelectionWatchlistObservationDomainServiceTest {
    private static final StockSelectionStrategyRunBO.Scope SCOPE = new StockSelectionStrategyRunBO.Scope("tenant-a", "owner-a");

    @Test
    void calculatesReturnAndBenchmarkExcessFromFrozenEntryContext() {
        ItemRepository watchlists = new ItemRepository(List.of(item(10L, "SZ", "000001", BigDecimal.TEN, LocalDate.of(2026, 8, 10))));
        MarketData marketData = new MarketData(
                List.of(new StockSelectionBacktestBO.DailyBar("SZ", "000001", LocalDate.of(2026, 8, 14), BigDecimal.valueOf(11))),
                List.of(new StockSelectionBacktestBO.DailyBar("SH", "000300", LocalDate.of(2026, 8, 10), BigDecimal.TEN),
                        new StockSelectionBacktestBO.DailyBar("SH", "000300", LocalDate.of(2026, 8, 14), BigDecimal.valueOf(10.5))));

        StockSelectionWatchlistObservationBO.Item observation = service(watchlists, marketData).observe(SCOPE).items().getFirst();

        assertEquals(BigDecimal.valueOf(11), observation.latestClose());
        assertEquals(LocalDate.of(2026, 8, 14), observation.latestTradeDate());
        assertEquals(new BigDecimal("10.0000"), observation.returnSinceEntryPct());
        assertEquals(new BigDecimal("5.0000"), observation.benchmarkReturnPct());
        assertEquals(new BigDecimal("5.0000"), observation.excessReturnPct());
        assertEquals(List.of(), observation.warnings());
    }

    @Test
    void keepsMissingEntryContextVisibleWithoutProducingAFalseReturn() {
        ItemRepository watchlists = new ItemRepository(List.of(item(10L, null, "000001", null, null)));

        StockSelectionWatchlistObservationBO.Item observation = service(watchlists, new MarketData(List.of(), List.of())).observe(SCOPE).items().getFirst();

        assertNull(observation.latestClose());
        assertNull(observation.returnSinceEntryPct());
        assertEquals(List.of("ENTRY_CONTEXT_UNAVAILABLE"), observation.warnings());
    }

    @Test
    void returnsBoundedDailyReturnAndExcessHistoryFromTheFrozenEntry() {
        ItemRepository watchlists = new ItemRepository(List.of(item(10L, "SZ", "000001", BigDecimal.TEN, LocalDate.of(2026, 8, 10))));
        List<StockSelectionBacktestBO.DailyBar> stockHistory = List.of(
                new StockSelectionBacktestBO.DailyBar("SZ", "000001", LocalDate.of(2026, 8, 10), BigDecimal.TEN),
                new StockSelectionBacktestBO.DailyBar("SZ", "000001", LocalDate.of(2026, 8, 11), BigDecimal.valueOf(11)),
                new StockSelectionBacktestBO.DailyBar("SZ", "000001", LocalDate.of(2026, 8, 12), BigDecimal.valueOf(10.5)));
        List<StockSelectionBacktestBO.DailyBar> benchmarkHistory = List.of(
                new StockSelectionBacktestBO.DailyBar("SH", "000300", LocalDate.of(2026, 8, 10), BigDecimal.valueOf(100)),
                new StockSelectionBacktestBO.DailyBar("SH", "000300", LocalDate.of(2026, 8, 11), BigDecimal.valueOf(102)),
                new StockSelectionBacktestBO.DailyBar("SH", "000300", LocalDate.of(2026, 8, 12), BigDecimal.valueOf(101)));

        StockSelectionWatchlistObservationBO.History history = service(watchlists,
                new MarketData(stockHistory, stockHistory, benchmarkHistory)).history(SCOPE, 10L, 30);

        assertEquals(3, history.points().size());
        assertEquals(new BigDecimal("10.0000"), history.points().get(1).returnSinceEntryPct());
        assertEquals(new BigDecimal("2.0000"), history.points().get(1).benchmarkReturnPct());
        assertEquals(new BigDecimal("8.0000"), history.points().get(1).excessReturnPct());
        assertEquals(new BigDecimal("5.0000"), history.points().get(2).returnSinceEntryPct());
        assertEquals(new BigDecimal("1.0000"), history.points().get(2).benchmarkReturnPct());
        assertEquals(new BigDecimal("4.0000"), history.points().get(2).excessReturnPct());
    }

    private StockSelectionWatchlistObservationDomainService service(ItemRepository watchlists, MarketData marketData) {
        return new StockSelectionWatchlistObservationDomainService(watchlists, marketData);
    }

    private static StockSelectionWatchlistBO.Item item(long id, String market, String code, BigDecimal entryPrice, LocalDate entryDate) {
        return new StockSelectionWatchlistBO.Item(id, SCOPE, code, "Example", 99L, "WATCHING", "Reason", null, null, 0,
                OffsetDateTime.now(ZoneOffset.UTC), 100L, "My watchlist", market, entryDate, entryPrice);
    }

    private static final class ItemRepository implements StockSelectionWatchlistRepository {
        private final List<StockSelectionWatchlistBO.Item> items;

        private ItemRepository(List<StockSelectionWatchlistBO.Item> items) { this.items = items; }

        @Override public Optional<StockSelectionWatchlistBO.Candidate> findCandidate(StockSelectionStrategyRunBO.Scope scope, long candidateId) { return Optional.empty(); }
        @Override public Optional<StockSelectionWatchlistBO.Item> findActive(StockSelectionStrategyRunBO.Scope scope, long watchlistId, String stockCode) { return Optional.empty(); }
        @Override public Optional<StockSelectionWatchlistBO.Watchlist> findActiveWatchlist(StockSelectionStrategyRunBO.Scope scope, String name) { return Optional.empty(); }
        @Override public void saveWatchlist(StockSelectionWatchlistBO.Watchlist watchlist) { }
        @Override public void save(StockSelectionWatchlistBO.Item item) { }
        @Override public boolean update(StockSelectionWatchlistBO.Item item, long expectedVersion) { return false; }
        @Override public void saveReview(StockSelectionWatchlistBO.Review review, StockSelectionStrategyRunBO.Scope scope) { }
        @Override public List<StockSelectionWatchlistBO.Detail> list(StockSelectionStrategyRunBO.Scope scope) {
            return items.stream().map(item -> new StockSelectionWatchlistBO.Detail(item, List.of())).toList();
        }
    }

    private static final class MarketData implements StockSelectionBacktestMarketDataRepository {
        private final List<StockSelectionBacktestBO.DailyBar> latest;
        private final List<StockSelectionBacktestBO.DailyBar> stockHistory;
        private final List<StockSelectionBacktestBO.DailyBar> benchmark;

        private MarketData(List<StockSelectionBacktestBO.DailyBar> latest, List<StockSelectionBacktestBO.DailyBar> benchmark) {
            this(latest, latest, benchmark);
        }

        private MarketData(List<StockSelectionBacktestBO.DailyBar> latest,
                           List<StockSelectionBacktestBO.DailyBar> stockHistory,
                           List<StockSelectionBacktestBO.DailyBar> benchmark) {
            this.latest = latest;
            this.stockHistory = stockHistory;
            this.benchmark = benchmark;
        }

        @Override public List<LocalDate> listTradingDatesOnOrBefore(LocalDate requestedAsOfDate, int limit) { return List.of(); }
        @Override public List<StockSelectionBacktestBO.DailyBar> listDailyBars(List<StockSelectionBacktestBO.MarketSymbol> symbols, LocalDate startDate, LocalDate endDate) {
            return symbols.getFirst().code().equals("000300") ? benchmark : stockHistory;
        }
        @Override public List<StockSelectionBacktestBO.DailyBar> listLatestDailyBars(List<StockSelectionBacktestBO.MarketSymbol> symbols) { return latest; }
    }
}
