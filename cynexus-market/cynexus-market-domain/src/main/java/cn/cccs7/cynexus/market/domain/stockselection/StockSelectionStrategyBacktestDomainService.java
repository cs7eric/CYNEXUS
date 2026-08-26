package cn.cccs7.cynexus.market.domain.stockselection;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Replays a governed strategy using only snapshots available on each signal date. */
@Service
public class StockSelectionStrategyBacktestDomainService {

    private static final int DEFAULT_LOOKBACK_SESSIONS = 60;
    private static final int DEFAULT_SIGNAL_INTERVAL_SESSIONS = 5;
    private static final int DEFAULT_HOLDING_SESSIONS = 10;
    private static final int DEFAULT_MAX_CANDIDATES_PER_SIGNAL = 20;
    private static final int DEFAULT_TRANSACTION_COST_BPS_PER_SIDE = 10;
    private static final int MAX_CALENDAR_SESSIONS = 260;
    private static final int MAX_SYMBOLS = 500;
    private static final String BENCHMARK_MARKET = "SH";
    private static final String BENCHMARK_CODE = "000300";

    private final StockSelectionStrategyCatalogDomainService catalogService;
    private final StockSelectionStrategyRunDomainService strategyRunService;
    private final StockSelectionBacktestMarketDataRepository marketDataRepository;
    private final StockSelectionBacktestRepository backtestRepository;
    private final StockSelectionIdGenerator idGenerator;

    public StockSelectionStrategyBacktestDomainService(
            StockSelectionStrategyCatalogDomainService catalogService,
            StockSelectionStrategyRunDomainService strategyRunService,
            StockSelectionBacktestMarketDataRepository marketDataRepository,
            StockSelectionBacktestRepository backtestRepository,
            StockSelectionIdGenerator idGenerator) {
        this.catalogService = catalogService;
        this.strategyRunService = strategyRunService;
        this.marketDataRepository = marketDataRepository;
        this.backtestRepository = backtestRepository;
        this.idGenerator = idGenerator;
    }

    public StockSelectionBacktestBO.Run run(StockSelectionStrategyRunBO.Scope scope,
                                            String strategyCode,
                                            StockSelectionBacktestBO.Request request) {
        StockSelectionBacktestBO.Config config = config(request);
        StockSelectionBacktestBO.Run run = replay(scope, strategyCode, config, idGenerator.nextId(), OffsetDateTime.now(ZoneOffset.UTC));
        backtestRepository.save(run);
        return run;
    }

    /** Creates a durable job before any historical market query is performed. */
    public StockSelectionBacktestBO.Run queue(StockSelectionStrategyRunBO.Scope scope,
                                              String strategyCode,
                                              StockSelectionBacktestBO.Request request) {
        StockSelectionBacktestBO.Config config = config(request);
        StockSelectionStrategyCatalogBO.Strategy strategy = findStrategy(scope, strategyCode);
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        StockSelectionBacktestBO.Run run = new StockSelectionBacktestBO.Run(idGenerator.nextId(), scope, strategy.code(),
                strategy.activeVersion().id(), strategy.activeVersion().versionNo(), config.requestedAsOfDate(), null,
                "QUEUED", config, "SH:000300", "clickhouse.daily_raw + screener.snapshot", List.of(), emptySummary(),
                startedAt, null, List.of());
        backtestRepository.save(run);
        return run;
    }

    public java.util.Optional<StockSelectionBacktestBO.ClaimedRun> claimNextQueued() {
        return backtestRepository.claimNextQueued();
    }

    public StockSelectionBacktestBO.Run complete(StockSelectionBacktestBO.ClaimedRun claimedRun) {
        StockSelectionBacktestBO.StoredRun stored = find(claimedRun.scope(), claimedRun.id());
        StockSelectionBacktestBO.Run run = replay(claimedRun.scope(), stored.strategyCode(), stored.config(), stored.id(), stored.startedAt());
        backtestRepository.complete(run);
        return run;
    }

    public void fail(StockSelectionBacktestBO.ClaimedRun claimedRun) {
        backtestRepository.fail(claimedRun, "Backtest processing failed. Review market-data availability and retry.");
    }

    private StockSelectionBacktestBO.Run replay(StockSelectionStrategyRunBO.Scope scope,
                                                 String strategyCode,
                                                 StockSelectionBacktestBO.Config config,
                                                 long runId,
                                                 OffsetDateTime startedAt) {
        LocalDate requestedAsOfDate = config.requestedAsOfDate();
        List<LocalDate> dates = marketDataRepository.listTradingDatesOnOrBefore(
                requestedAsOfDate,
                Math.min(MAX_CALENDAR_SESSIONS, config.lookbackSessions() + config.holdingSessions() + 1));
        if (dates.size() < config.holdingSessions() + 2) {
            throw new IllegalStateException("insufficient trading dates for backtest");
        }
        dates = dates.stream().distinct().sorted().toList();
        LocalDate actualAsOfDate = dates.get(dates.size() - 1);
        StockSelectionStrategyCatalogBO.Strategy strategy = findStrategy(scope, strategyCode);
        List<String> warnings = new ArrayList<>();
        List<StockSelectionBacktestBO.MarketSymbol> targetSymbols = targetSymbols(config);
        if (targetSymbols.size() == 1) {
            StockSelectionBacktestBO.MarketSymbol targetSymbol = targetSymbols.getFirst();
            warnings.add("Single-symbol mode evaluates only " + key(targetSymbol.market(), targetSymbol.code())
                    + "; candidates per signal is ignored.");
        } else if (!targetSymbols.isEmpty()) {
            warnings.add("Target-list mode evaluates exactly " + targetSymbols.size()
                    + " symbols on each signal date; candidates per signal is ignored.");
        }
        if (!actualAsOfDate.equals(requestedAsOfDate)) {
            warnings.add("Requested as-of date was adjusted to the latest available trading date: " + actualAsOfDate + ".");
        }
        if (config.lookbackSessions() + config.holdingSessions() + 1 > MAX_CALENDAR_SESSIONS) {
            warnings.add("The trading-date window was capped at " + MAX_CALENDAR_SESSIONS + " sessions.");
        }

        int lastSignalIndex = dates.size() - config.holdingSessions() - 2;
        int firstSignalIndex = Math.max(0, lastSignalIndex - config.lookbackSessions() + 1);
        List<Signal> signals = new ArrayList<>();
        Map<String, StockSelectionBacktestBO.MarketSymbol> symbols = new LinkedHashMap<>();
        symbols.put(key(BENCHMARK_MARKET, BENCHMARK_CODE), new StockSelectionBacktestBO.MarketSymbol(BENCHMARK_MARKET, BENCHMARK_CODE));
        Map<String, Integer> skippedSignals = new LinkedHashMap<>();
        int evaluatedSignalCount = 0;
        for (int index = firstSignalIndex; index <= lastSignalIndex; index += config.signalIntervalSessions()) {
            LocalDate signalDate = dates.get(index);
            try {
                evaluatedSignalCount++;
                List<StockSelectionStrategyRunDomainService.Evaluation> evaluations = targetSymbols.isEmpty()
                        ? List.of(strategyRunService.evaluate(scope, strategy.code(), signalDate.toString(), config.maxCandidatesPerSignal()))
                        : targetSymbols.stream().map(target -> strategyRunService.evaluateForSymbol(scope, strategy.code(), signalDate.toString(), target)).toList();
                for (StockSelectionStrategyRunDomainService.Evaluation evaluation : evaluations) {
                    if (!evaluation.result().ok()) {
                        increment(skippedSignals, "signal snapshot unavailable");
                        continue;
                    }
                    warnings.addAll(evaluation.warnings());
                    for (StockScreenerQueryBO.Row row : evaluation.rows()) {
                        if (row.market() == null || row.code() == null) {
                            increment(skippedSignals, "candidate market or code missing");
                            continue;
                        }
                        StockSelectionBacktestBO.MarketSymbol symbol = new StockSelectionBacktestBO.MarketSymbol(row.market(), row.code());
                        symbols.put(key(symbol.market(), symbol.code()), symbol);
                        signals.add(new Signal(index, evaluation.result().tradeDate(), row, signals.size() + 1));
                    }
                }
            } catch (RuntimeException exception) {
                increment(skippedSignals, "signal evaluation unavailable");
            }
        }
        if (symbols.size() > MAX_SYMBOLS) {
            warnings.add("The symbol universe was capped at " + MAX_SYMBOLS + " symbols.");
            symbols = new LinkedHashMap<>(symbols.entrySet().stream().limit(MAX_SYMBOLS)
                    .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll));
        }
        if (!skippedSignals.isEmpty()) {
            skippedSignals.forEach((reason, count) -> warnings.add("Skipped " + count + " " + reason + "."));
        }

        Map<String, BigDecimal> closes = new HashMap<>();
        List<StockSelectionBacktestBO.DailyBar> bars = marketDataRepository.listDailyBars(
                List.copyOf(symbols.values()), dates.get(0), actualAsOfDate);
        for (StockSelectionBacktestBO.DailyBar bar : bars) {
            if (bar.tradeDate() != null && bar.close() != null && bar.close().signum() > 0) {
                closes.put(closeKey(bar.market(), bar.code(), bar.tradeDate()), bar.close());
            }
        }

        List<StockSelectionBacktestBO.Trade> trades = new ArrayList<>();
        Map<String, Integer> skippedTrades = new LinkedHashMap<>();
        for (Signal signal : signals) {
            int entryIndex = signal.calendarIndex() + 1;
            int exitIndex = entryIndex + config.holdingSessions();
            LocalDate entryDate = dates.get(entryIndex);
            LocalDate exitDate = dates.get(exitIndex);
            BigDecimal entry = close(closes, signal.row().market(), signal.row().code(), entryDate);
            BigDecimal exit = close(closes, signal.row().market(), signal.row().code(), exitDate);
            BigDecimal benchmarkEntry = close(closes, BENCHMARK_MARKET, BENCHMARK_CODE, entryDate);
            BigDecimal benchmarkExit = close(closes, BENCHMARK_MARKET, BENCHMARK_CODE, exitDate);
            if (entry == null || exit == null) {
                increment(skippedTrades, "stock entry or exit close missing");
                continue;
            }
            if (benchmarkEntry == null || benchmarkExit == null) {
                increment(skippedTrades, "benchmark entry or exit close missing");
                continue;
            }
            BigDecimal gross = percentageChange(entry, exit);
            BigDecimal benchmark = percentageChange(benchmarkEntry, benchmarkExit);
            BigDecimal net = gross.subtract(costPct(config.transactionCostBpsPerSide()).multiply(BigDecimal.valueOf(2)))
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal excess = net.subtract(benchmark).setScale(4, RoundingMode.HALF_UP);
            trades.add(new StockSelectionBacktestBO.Trade(idGenerator.nextId(), signal.signalDate(), entryDate, exitDate,
                    signal.row().market(), signal.row().code(), signal.row().name(), signal.rank(),
                    strategyRunService.score(signal.row(), strategy.code()), entry, exit, gross, net, benchmark, excess,
                    net.signum() > 0 ? "WIN" : net.signum() < 0 ? "LOSS" : "FLAT"));
        }
        if (!skippedTrades.isEmpty()) {
            skippedTrades.forEach((reason, count) -> warnings.add("Skipped " + count + " trades: " + reason + "."));
        }
        StockSelectionBacktestBO.Summary summary = summary(evaluatedSignalCount, trades, skippedTrades);
        OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
        return new StockSelectionBacktestBO.Run(runId, scope, strategy.code(),
                strategy.activeVersion().id(), strategy.activeVersion().versionNo(), requestedAsOfDate, actualAsOfDate,
                trades.isEmpty() ? "NO_DATA" : "SUCCEEDED", config, "SH:000300", "clickhouse.daily_raw + screener.snapshot",
                distinctWarnings(warnings), summary, startedAt, completedAt, List.copyOf(trades));
    }

    public StockSelectionBacktestBO.StoredRun find(StockSelectionStrategyRunBO.Scope scope, long backtestId) {
        return backtestRepository.findById(scope, backtestId)
                .orElseThrow(() -> new IllegalArgumentException("backtest not found"));
    }

    private StockSelectionBacktestBO.Config config(StockSelectionBacktestBO.Request request) {
        StockSelectionBacktestBO.Request value = request == null ? new StockSelectionBacktestBO.Request(null, null, null, null, null, null, null, null, List.of()) : request;
        LocalDate asOfDate = parseDate(value.asOfDate());
        int lookback = bounded(value.lookbackSessions(), DEFAULT_LOOKBACK_SESSIONS, 5, MAX_CALENDAR_SESSIONS);
        int interval = bounded(value.signalIntervalSessions(), DEFAULT_SIGNAL_INTERVAL_SESSIONS, 1, 30);
        int holding = bounded(value.holdingSessions(), DEFAULT_HOLDING_SESSIONS, 1, 60);
        int candidates = bounded(value.maxCandidatesPerSignal(), DEFAULT_MAX_CANDIDATES_PER_SIGNAL, 1, 20);
        int cost = bounded(value.transactionCostBpsPerSide(), DEFAULT_TRANSACTION_COST_BPS_PER_SIDE, 0, 100);
        String stockCode = normalizeStockCode(value.stockCode());
        String stockMarket = normalizeStockMarket(value.stockMarket());
        if ((stockCode == null) != (stockMarket == null)) {
            throw new IllegalArgumentException("stockCode and stockMarket must be provided together");
        }
        List<StockSelectionBacktestBO.MarketSymbol> targetSymbols = normalizeTargetSymbols(value.targetSymbols());
        if (stockCode != null && !targetSymbols.isEmpty()) {
            throw new IllegalArgumentException("stockCode/stockMarket and targetSymbols are mutually exclusive");
        }
        return new StockSelectionBacktestBO.Config(asOfDate, lookback, interval, holding, candidates, cost, stockCode, stockMarket, targetSymbols);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("asOfDate must be an ISO date", exception);
        }
    }

    private int bounded(Integer value, int fallback, int min, int max) {
        int resolved = value == null ? fallback : value;
        if (resolved < min || resolved > max) throw new IllegalArgumentException("backtest parameter is outside its allowed range");
        return resolved;
    }

    private String normalizeStockCode(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (!normalized.matches("\\d{6}")) throw new IllegalArgumentException("stockCode must be a six-digit A-share code");
        return normalized;
    }

    private String normalizeStockMarket(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"SH".equals(normalized) && !"SZ".equals(normalized) && !"BJ".equals(normalized)) {
            throw new IllegalArgumentException("stockMarket must be SH, SZ, or BJ");
        }
        return normalized;
    }

    private List<StockSelectionBacktestBO.MarketSymbol> targetSymbols(StockSelectionBacktestBO.Config config) {
        if (config.stockCode() != null) {
            return List.of(new StockSelectionBacktestBO.MarketSymbol(config.stockMarket(), config.stockCode()));
        }
        return config.targetSymbols();
    }

    private List<StockSelectionBacktestBO.MarketSymbol> normalizeTargetSymbols(List<StockSelectionBacktestBO.MarketSymbol> values) {
        if (values == null || values.isEmpty()) return List.of();
        if (values.size() > 50) throw new IllegalArgumentException("targetSymbols supports at most 50 symbols");
        Map<String, StockSelectionBacktestBO.MarketSymbol> normalized = new LinkedHashMap<>();
        for (StockSelectionBacktestBO.MarketSymbol value : values) {
            if (value == null) throw new IllegalArgumentException("targetSymbols cannot contain null values");
            String market = normalizeStockMarket(value.market());
            String code = normalizeStockCode(value.code());
            if (market == null || code == null) throw new IllegalArgumentException("each target symbol requires market and code");
            normalized.put(key(market, code), new StockSelectionBacktestBO.MarketSymbol(market, code));
        }
        return List.copyOf(normalized.values());
    }

    private StockSelectionStrategyCatalogBO.Strategy findStrategy(StockSelectionStrategyRunBO.Scope scope, String code) {
        String normalized = code == null ? "" : code.trim();
        return catalogService.listActive(scope).items().stream()
                .filter(item -> item.code().equalsIgnoreCase(normalized))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("strategy is not active"));
    }

    private StockSelectionBacktestBO.Summary summary(int signalCount,
                                                      List<StockSelectionBacktestBO.Trade> trades,
                                                      Map<String, Integer> skippedTrades) {
        List<BigDecimal> netReturns = trades.stream().map(StockSelectionBacktestBO.Trade::netReturnPct).sorted().toList();
        BigDecimal wins = BigDecimal.valueOf(trades.stream().filter(item -> item.netReturnPct().signum() > 0).count());
        BigDecimal count = BigDecimal.valueOf(trades.size());
        return new StockSelectionBacktestBO.Summary(signalCount, trades.size(), skippedTrades.values().stream().mapToInt(Integer::intValue).sum(),
                count.signum() == 0 ? BigDecimal.ZERO : wins.multiply(BigDecimal.valueOf(100)).divide(count, 4, RoundingMode.HALF_UP),
                average(netReturns), median(netReturns), average(trades.stream().map(StockSelectionBacktestBO.Trade::benchmarkReturnPct).sorted().toList()),
                average(trades.stream().map(StockSelectionBacktestBO.Trade::excessReturnPct).sorted().toList()));
    }

    private StockSelectionBacktestBO.Summary emptySummary() {
        return new StockSelectionBacktestBO.Summary(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) return values.get(middle).setScale(4, RoundingMode.HALF_UP);
        return values.get(middle - 1).add(values.get(middle)).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal percentageChange(BigDecimal entry, BigDecimal exit) {
        return exit.subtract(entry).divide(entry, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal costPct(int bps) {
        return BigDecimal.valueOf(bps).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal close(Map<String, BigDecimal> closes, String market, String code, LocalDate date) {
        return closes.get(closeKey(market, code, date));
    }

    private String closeKey(String market, String code, LocalDate date) { return key(market, code) + "@" + date; }
    private String key(String market, String code) { return market + ":" + code; }

    private void increment(Map<String, Integer> values, String key) { values.merge(key, 1, Integer::sum); }

    private List<String> distinctWarnings(List<String> values) {
        return values.stream().filter(value -> value != null && !value.isBlank()).distinct().limit(30).toList();
    }

    private record Signal(int calendarIndex, LocalDate signalDate, StockScreenerQueryBO.Row row, int rank) { }
}
