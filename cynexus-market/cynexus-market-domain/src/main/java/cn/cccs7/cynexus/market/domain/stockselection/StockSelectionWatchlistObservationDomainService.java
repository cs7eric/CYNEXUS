package cn.cccs7.cynexus.market.domain.stockselection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Calculates a bounded, owner-scoped post-entry observation from frozen watchlist entry context. */
@Service
public class StockSelectionWatchlistObservationDomainService {
    private static final String DATA_SOURCE = "clickhouse.daily_raw";
    private static final int MAX_ITEMS = 100;
    private static final StockSelectionBacktestBO.MarketSymbol BENCHMARK = new StockSelectionBacktestBO.MarketSymbol("SH", "000300");

    private final StockSelectionWatchlistRepository watchlistRepository;
    private final StockSelectionBacktestMarketDataRepository marketDataRepository;

    public StockSelectionWatchlistObservationDomainService(StockSelectionWatchlistRepository watchlistRepository,
                                                            StockSelectionBacktestMarketDataRepository marketDataRepository) {
        this.watchlistRepository = watchlistRepository;
        this.marketDataRepository = marketDataRepository;
    }

    public StockSelectionWatchlistObservationBO.Result observe(StockSelectionStrategyRunBO.Scope scope) {
        List<StockSelectionWatchlistBO.Item> items = watchlistRepository.list(scope).stream().map(StockSelectionWatchlistBO.Detail::item).toList();
        List<String> resultWarnings = new ArrayList<>();
        if (items.size() > MAX_ITEMS) {
            resultWarnings.add("OBSERVATION_LIMIT_REACHED: only the first 100 watchlist items were refreshed");
        }
        List<StockSelectionWatchlistBO.Item> bounded = items.stream().limit(MAX_ITEMS).toList();
        List<StockSelectionWatchlistBO.Item> eligible = bounded.stream().filter(this::hasEntryContext).toList();
        Map<String, StockSelectionBacktestBO.DailyBar> latestBySymbol = latestBySymbol(eligible);
        List<StockSelectionBacktestBO.DailyBar> benchmarkBars = benchmarkBars(eligible);
        List<StockSelectionWatchlistObservationBO.Item> observations = bounded.stream()
                .map(item -> observe(item, latestBySymbol.get(key(item.market(), item.stockCode())), benchmarkBars))
                .toList();
        return new StockSelectionWatchlistObservationBO.Result(observations, DATA_SOURCE, List.copyOf(resultWarnings));
    }

    public StockSelectionWatchlistObservationBO.History history(StockSelectionStrategyRunBO.Scope scope, long itemId, int requestedLimit) {
        if (itemId <= 0) throw new IllegalArgumentException("watchlist item id must be positive");
        StockSelectionWatchlistBO.Item item = watchlistRepository.list(scope).stream().map(StockSelectionWatchlistBO.Detail::item)
                .filter(value -> value.id() == itemId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("watchlist item is unavailable"));
        if (!hasEntryContext(item)) {
            return new StockSelectionWatchlistObservationBO.History(item.id(), item.entryTradeDate(), item.entryPrice(), List.of(), DATA_SOURCE,
                    List.of("ENTRY_CONTEXT_UNAVAILABLE"));
        }
        int limit = Math.max(30, Math.min(requestedLimit, 252));
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        List<StockSelectionBacktestBO.DailyBar> stockBars = marketDataRepository.listDailyBars(
                        List.of(new StockSelectionBacktestBO.MarketSymbol(item.market(), item.stockCode())), item.entryTradeDate(), endDate)
                .stream().filter(this::validClose).sorted(Comparator.comparing(StockSelectionBacktestBO.DailyBar::tradeDate)).toList();
        List<StockSelectionBacktestBO.DailyBar> benchmarkBars = marketDataRepository.listDailyBars(List.of(BENCHMARK), item.entryTradeDate(), endDate)
                .stream().filter(this::validClose).sorted(Comparator.comparing(StockSelectionBacktestBO.DailyBar::tradeDate)).toList();
        if (stockBars.isEmpty()) {
            return new StockSelectionWatchlistObservationBO.History(item.id(), item.entryTradeDate(), item.entryPrice(), List.of(), DATA_SOURCE,
                    List.of("HISTORICAL_CLOSE_UNAVAILABLE"));
        }
        StockSelectionBacktestBO.DailyBar benchmarkEntry = firstOnOrAfter(benchmarkBars, item.entryTradeDate());
        List<String> historyWarnings = benchmarkEntry == null ? List.of("BENCHMARK_CLOSE_UNAVAILABLE") : List.of();
        List<StockSelectionBacktestBO.DailyBar> bounded = stockBars.size() <= limit ? stockBars : stockBars.subList(stockBars.size() - limit, stockBars.size());
        List<StockSelectionWatchlistObservationBO.Point> points = bounded.stream()
                .map(bar -> point(item, bar, benchmarkEntry, benchmarkBars)).toList();
        return new StockSelectionWatchlistObservationBO.History(item.id(), item.entryTradeDate(), item.entryPrice(), points, DATA_SOURCE, historyWarnings);
    }

    private Map<String, StockSelectionBacktestBO.DailyBar> latestBySymbol(List<StockSelectionWatchlistBO.Item> eligible) {
        List<StockSelectionBacktestBO.MarketSymbol> symbols = eligible.stream()
                .map(item -> new StockSelectionBacktestBO.MarketSymbol(item.market(), item.stockCode())).distinct().toList();
        Map<String, StockSelectionBacktestBO.DailyBar> values = new HashMap<>();
        for (StockSelectionBacktestBO.DailyBar bar : marketDataRepository.listLatestDailyBars(symbols)) {
            if (validClose(bar)) values.put(key(bar.market(), bar.code()), bar);
        }
        return values;
    }

    private List<StockSelectionBacktestBO.DailyBar> benchmarkBars(List<StockSelectionWatchlistBO.Item> eligible) {
        LocalDate start = eligible.stream().map(StockSelectionWatchlistBO.Item::entryTradeDate).min(Comparator.naturalOrder()).orElse(null);
        if (start == null) return List.of();
        return marketDataRepository.listDailyBars(List.of(BENCHMARK), start, LocalDate.now(ZoneOffset.UTC)).stream()
                .filter(this::validClose).sorted(Comparator.comparing(StockSelectionBacktestBO.DailyBar::tradeDate)).toList();
    }

    private StockSelectionWatchlistObservationBO.Item observe(StockSelectionWatchlistBO.Item item,
                                                               StockSelectionBacktestBO.DailyBar latest,
                                                               List<StockSelectionBacktestBO.DailyBar> benchmarkBars) {
        List<String> warnings = new ArrayList<>();
        if (!hasEntryContext(item)) {
            warnings.add("ENTRY_CONTEXT_UNAVAILABLE");
            return new StockSelectionWatchlistObservationBO.Item(item.id(), null, null, null, null, null, DATA_SOURCE, List.copyOf(warnings));
        }
        if (!validClose(latest)) {
            warnings.add("LATEST_CLOSE_UNAVAILABLE");
            return new StockSelectionWatchlistObservationBO.Item(item.id(), null, null, null, null, null, DATA_SOURCE, List.copyOf(warnings));
        }
        BigDecimal stockReturn = pctChange(item.entryPrice(), latest.close());
        StockSelectionBacktestBO.DailyBar benchmarkEntry = firstOnOrAfter(benchmarkBars, item.entryTradeDate());
        StockSelectionBacktestBO.DailyBar benchmarkLatest = lastOnOrBefore(benchmarkBars, latest.tradeDate());
        BigDecimal benchmarkReturn = null;
        if (benchmarkEntry == null || benchmarkLatest == null) {
            warnings.add("BENCHMARK_CLOSE_UNAVAILABLE");
        } else {
            benchmarkReturn = pctChange(benchmarkEntry.close(), benchmarkLatest.close());
        }
        BigDecimal excess = stockReturn == null || benchmarkReturn == null ? null : stockReturn.subtract(benchmarkReturn).setScale(4, RoundingMode.HALF_UP);
        return new StockSelectionWatchlistObservationBO.Item(item.id(), latest.close(), latest.tradeDate(), stockReturn,
                benchmarkReturn, excess, DATA_SOURCE, List.copyOf(warnings));
    }

    private StockSelectionWatchlistObservationBO.Point point(StockSelectionWatchlistBO.Item item,
                                                              StockSelectionBacktestBO.DailyBar stock,
                                                              StockSelectionBacktestBO.DailyBar benchmarkEntry,
                                                              List<StockSelectionBacktestBO.DailyBar> benchmarkBars) {
        List<String> warnings = new ArrayList<>();
        BigDecimal stockReturn = pctChange(item.entryPrice(), stock.close());
        StockSelectionBacktestBO.DailyBar benchmark = lastOnOrBefore(benchmarkBars, stock.tradeDate());
        BigDecimal benchmarkReturn = null;
        if (benchmarkEntry == null || benchmark == null) {
            warnings.add("BENCHMARK_CLOSE_UNAVAILABLE");
        } else {
            benchmarkReturn = pctChange(benchmarkEntry.close(), benchmark.close());
        }
        BigDecimal excess = stockReturn == null || benchmarkReturn == null ? null
                : stockReturn.subtract(benchmarkReturn).setScale(4, RoundingMode.HALF_UP);
        return new StockSelectionWatchlistObservationBO.Point(stock.tradeDate(), stock.close(), stockReturn, benchmarkReturn, excess, List.copyOf(warnings));
    }

    private boolean hasEntryContext(StockSelectionWatchlistBO.Item item) {
        return item.market() != null && !item.market().isBlank() && item.entryTradeDate() != null
                && item.entryPrice() != null && item.entryPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean validClose(StockSelectionBacktestBO.DailyBar bar) {
        return bar != null && bar.close() != null && bar.close().compareTo(BigDecimal.ZERO) > 0 && bar.tradeDate() != null;
    }

    private StockSelectionBacktestBO.DailyBar firstOnOrAfter(List<StockSelectionBacktestBO.DailyBar> bars, LocalDate date) {
        return bars.stream().filter(bar -> !bar.tradeDate().isBefore(date)).findFirst().orElse(null);
    }

    private StockSelectionBacktestBO.DailyBar lastOnOrBefore(List<StockSelectionBacktestBO.DailyBar> bars, LocalDate date) {
        return bars.stream().filter(bar -> !bar.tradeDate().isAfter(date)).reduce((left, right) -> right).orElse(null);
    }

    private BigDecimal pctChange(BigDecimal entry, BigDecimal latest) {
        if (entry == null || latest == null || entry.compareTo(BigDecimal.ZERO) <= 0) return null;
        return latest.subtract(entry).multiply(BigDecimal.valueOf(100)).divide(entry, 4, RoundingMode.HALF_UP);
    }

    private String key(String market, String code) {
        return (market == null ? "" : market.toUpperCase(Locale.ROOT)) + ':' + code;
    }
}
