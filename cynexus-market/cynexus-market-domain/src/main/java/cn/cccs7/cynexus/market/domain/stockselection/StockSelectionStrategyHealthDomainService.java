package cn.cccs7.cynexus.market.domain.stockselection;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Assesses current strategy fitness from the latest owner-scoped immutable strategy run.
 * It never replays the whole screener on a request path; immutable runs and backtests remain the audit facts.
 */
@Service
public class StockSelectionStrategyHealthDomainService {

    private static final BigDecimal MIN_SIGNAL_DENSITY = new BigDecimal("0.15");
    private static final BigDecimal MAX_SIGNAL_DENSITY = new BigDecimal("15.00");
    private static final long WARNING_LAG_DAYS = 3;
    private static final long INVALID_LAG_DAYS = 10;
    private static final int MIN_RECENT_TRADES = 3;

    private final StockSelectionStrategyCatalogDomainService catalogService;
    private final StockSelectionStrategyRunRepository runRepository;
    private final StockSelectionBacktestRepository backtestRepository;
    private final MarketFactorStudyRepository studyRepository;

    public StockSelectionStrategyHealthDomainService(StockSelectionStrategyCatalogDomainService catalogService,
                                                     StockSelectionStrategyRunRepository runRepository,
                                                     StockSelectionBacktestRepository backtestRepository) {
        this(catalogService, runRepository, backtestRepository, null);
    }

    @Autowired
    public StockSelectionStrategyHealthDomainService(StockSelectionStrategyCatalogDomainService catalogService,
                                                     StockSelectionStrategyRunRepository runRepository,
                                                     StockSelectionBacktestRepository backtestRepository,
                                                     MarketFactorStudyRepository studyRepository) {
        this.catalogService = catalogService;
        this.runRepository = runRepository;
        this.backtestRepository = backtestRepository;
        this.studyRepository = studyRepository;
    }

    public StockSelectionStrategyHealthBO.Assessment assess(StockSelectionStrategyRunBO.Scope scope,
                                                            String strategyCode,
                                                            String tradeDate) {
        if (scope == null) {
            throw new IllegalArgumentException("stock selection scope is required");
        }
        StockSelectionStrategyCatalogBO.Strategy strategy = catalogService.listActive(scope).items().stream()
                .filter(item -> item.code().equalsIgnoreCase(strategyCode == null ? "" : strategyCode.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("strategy is not active"));

        LocalDate requested = parseRequestedDate(tradeDate);
        StockSelectionStrategyRunBO.HealthInput run = runRepository.findLatestHealthInput(scope, strategy.code()).orElse(null);
        if (run == null) {
            return notAssessed(strategy, requested);
        }
        LocalDate actual = run.actualTradeDate();
        long lagDays = actual == null ? Long.MAX_VALUE : Math.max(0, ChronoUnit.DAYS.between(actual, requested));
        int universe = Math.max(0, run.universeCount());
        int candidates = Math.max(0, run.matchedCount());
        BigDecimal density = universe == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(candidates * 100.0 / universe).setScale(2, RoundingMode.HALF_UP);
        StockSelectionStrategyHealthBO.RecentPerformance recentPerformance = recentPerformance(scope, strategy.code());

        List<StockSelectionStrategyHealthBO.Check> checks = new ArrayList<>();
        boolean snapshotAvailable = "SUCCEEDED".equals(run.status()) && actual != null && universe > 0;
        checks.add(check("snapshot", "Daily snapshot", snapshotAvailable ? "PASS" : "FAIL",
                actual == null ? "unavailable" : actual.toString(), "available", snapshotAvailable ? "A governed daily-close snapshot is available." : "No usable daily-close snapshot is available."));
        boolean coverageHealthy = universe >= 1000;
        checks.add(check("coverage", "Universe coverage", coverageHealthy ? "PASS" : "WARN",
                Integer.toString(universe), ">= 1,000 securities", coverageHealthy ? "The snapshot covers the expected broad market universe." : "The snapshot is smaller than the expected broad-market coverage."));
        boolean densityHealthy = density.compareTo(MIN_SIGNAL_DENSITY) >= 0 && density.compareTo(MAX_SIGNAL_DENSITY) <= 0 && candidates > 0;
        checks.add(check("signal_density", "Signal density", densityHealthy ? "PASS" : "WARN",
                density + "%", MIN_SIGNAL_DENSITY + "% – " + MAX_SIGNAL_DENSITY + "%", densityHealthy ? "Candidate volume is within the governed operating band." : "Candidate volume is unusually sparse or broad for this strategy."));
        boolean freshnessHealthy = lagDays <= WARNING_LAG_DAYS;
        checks.add(check("freshness", "Snapshot freshness", freshnessHealthy ? "PASS" : lagDays <= INVALID_LAG_DAYS ? "WARN" : "FAIL",
                actual == null ? "unavailable" : lagDays + " calendar days late", "<= " + WARNING_LAG_DAYS + " days", freshnessHealthy ? "The snapshot is recent enough for today's review." : "The snapshot is older than the preferred daily review window."));
        boolean recentPerformanceAvailable = recentPerformance != null;
        boolean recentPerformanceHealthy = recentPerformanceAvailable
                && recentPerformance.evaluatedTradeCount() >= MIN_RECENT_TRADES
                && recentPerformance.averageExcessReturnPct().signum() >= 0;
        checks.add(check("recent_performance", "Recent hit and excess performance",
                !recentPerformanceAvailable ? "WARN" : recentPerformanceHealthy ? "PASS" : "WARN",
                recentPerformanceAvailable ? recentPerformance.evaluatedTradeCount() + " trades; hit " + recentPerformance.winRatePct() + "%; excess " + recentPerformance.averageExcessReturnPct() + "%" : "not assessed",
                ">= " + MIN_RECENT_TRADES + " trades; excess >= 0%",
                !recentPerformanceAvailable ? "Run a full-universe backtest to populate recent performance monitoring." : recentPerformanceHealthy ? "Recent completed trades are meeting the monitoring threshold." : "Recent trades are sparse or underperforming the CSI 300 benchmark."));

        if (strategy.activeVersion().sourceFactorStudyId() != null) {
            boolean researchAvailable = studyRepository != null && studyRepository.findById(scope.tenantId(), scope.ownerId(), String.valueOf(strategy.activeVersion().sourceFactorStudyId()))
                    .map(study -> study.status() == MarketFactorResearchBO.StudyStatus.SUCCEEDED || study.status() == MarketFactorResearchBO.StudyStatus.PARTIAL)
                    .orElse(false);
            checks.add(check("factor_research", "Factor research coverage", researchAvailable ? "PASS" : "WARN",
                    researchAvailable ? "study available" : "study unavailable or incomplete", "SUCCEEDED/PARTIAL", researchAvailable
                            ? "The strategy's source study remains available for factor evidence." : "The source study is unavailable; factor coverage and research decay cannot be confirmed."));
        }

        String status;
        String reason;
        String action;
        if (!snapshotAvailable || lagDays > INVALID_LAG_DAYS) {
            status = "INVALID";
            reason = !snapshotAvailable ? "No usable daily snapshot is available." : "The latest available snapshot is too old to validate today's logic.";
            action = "Do not promote new candidates until the daily data is refreshed.";
        } else if (!coverageHealthy || !densityHealthy || !freshnessHealthy) {
            status = "WEAKENING";
            reason = !coverageHealthy ? "Market coverage is below the expected operating range." : !densityHealthy ? "Signal density has moved outside the strategy's normal operating range." : "The latest snapshot is stale relative to today's date.";
            action = "Keep tracking existing candidates and confirm the next fresh snapshot before acting.";
        } else {
            status = "VALID";
            reason = "Today's snapshot is fresh, broad enough, and producing a normal signal density.";
            action = "Continue tracking candidates; use the evidence and backtest tabs before making a decision.";
        }

        List<String> warnings = new ArrayList<>();
        if (run.warnings() != null) {
            run.warnings().stream().filter(value -> value != null && !value.isBlank() && !value.startsWith("Daily close snapshot."))
                    .forEach(warnings::add);
        }
        return new StockSelectionStrategyHealthBO.Assessment(strategy.code(), run.strategyVersionNo(), requested, actual,
                status, reason, action, universe, candidates, density, lagDays, recentPerformance, run.source(), List.copyOf(checks), List.copyOf(warnings));
    }

    private StockSelectionStrategyHealthBO.Assessment notAssessed(StockSelectionStrategyCatalogBO.Strategy strategy, LocalDate requested) {
        return new StockSelectionStrategyHealthBO.Assessment(strategy.code(), strategy.activeVersion().versionNo(), requested, null,
                "NOT_ASSESSED", "Run this governed strategy to create a current, owner-scoped health assessment.",
                "Run the strategy, then review its evidence before adding candidates to your watchlist.", 0, 0, BigDecimal.ZERO, 0,
                null, "strategy run snapshot", List.of(check("snapshot", "Daily snapshot", "WARN", "not assessed", "current strategy run", "No current owner-scoped strategy run is available.")), List.of());
    }

    private StockSelectionStrategyHealthBO.RecentPerformance recentPerformance(StockSelectionStrategyRunBO.Scope scope, String strategyCode) {
        StockSelectionBacktestBO.StoredRun run = backtestRepository.findLatestCompletedByStrategy(scope, strategyCode).orElse(null);
        if (run == null || !"SUCCEEDED".equals(run.status()) || run.actualAsOfDate() == null
                || run.config().stockCode() != null || !run.config().targetSymbols().isEmpty()) return null;
        LocalDate cutoff = run.actualAsOfDate().minusDays(90);
        List<StockSelectionBacktestBO.Trade> trades = run.trades().stream()
                .filter(item -> item.signalDate() != null && !item.signalDate().isBefore(cutoff)).toList();
        if (trades.isEmpty()) return new StockSelectionStrategyHealthBO.RecentPerformance(0, BigDecimal.ZERO, BigDecimal.ZERO, run.actualAsOfDate());
        long wins = trades.stream().filter(item -> item.netReturnPct().signum() > 0).count();
        BigDecimal winRate = BigDecimal.valueOf(wins * 100.0 / trades.size()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageExcess = trades.stream().map(StockSelectionBacktestBO.Trade::excessReturnPct).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(trades.size()), 4, RoundingMode.HALF_UP);
        return new StockSelectionStrategyHealthBO.RecentPerformance(trades.size(), winRate, averageExcess, run.actualAsOfDate());
    }

    private LocalDate parseRequestedDate(String tradeDate) {
        if (tradeDate == null || tradeDate.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(tradeDate.trim());
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("tradeDate must be yyyy-MM-dd", exception);
        }
    }

    private StockSelectionStrategyHealthBO.Check check(String code, String label, String status, String actualValue,
                                                        String threshold, String detail) {
        return new StockSelectionStrategyHealthBO.Check(code, label, status, actualValue, threshold, detail);
    }
}
