package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Calculates historical portfolio performance from the confirmed ledger and daily closes. */
public final class PortfolioPerformanceDomainService {
    private static final int SCALE = 8;
    private final PortfolioLedgerDomainService ledgerService;

    public PortfolioPerformanceDomainService(PortfolioLedgerDomainService ledgerService) {
        this.ledgerService = ledgerService;
    }

    public PortfolioPerformanceResultBO calculate(
            List<PortfolioTradeBO> trades,
            Map<String, List<PortfolioPricePointBO>> priceSeries,
            Map<String, BigDecimal> realizedPnlBaselines,
            BigDecimal availableCash,
            LocalDate from,
            LocalDate to,
            PortfolioPerformanceGranularity granularity) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("performance date range is invalid");
        }
        Map<String, List<PortfolioPricePointBO>> normalizedPrices = normalizePrices(priceSeries);
        TreeSet<LocalDate> dates = new TreeSet<>();
        normalizedPrices.values().forEach(series -> series.stream()
                .map(PortfolioPricePointBO::tradeDate)
                .filter(date -> !date.isAfter(to))
                .forEach(dates::add));
        if (dates.isEmpty()) {
            return empty(from, to, granularity);
        }
        LocalDate anchorDate = dates.stream().filter(date -> date.isBefore(from)).reduce((left, right) -> right).orElse(null);
        List<LocalDate> sampledDates = dates.stream().filter(date -> !date.isBefore(from)).toList();
        List<DailySnapshot> snapshots = new ArrayList<>();
        if (anchorDate != null) {
            snapshots.add(snapshot(anchorDate, trades, normalizedPrices, realizedPnlBaselines, availableCash));
        }
        for (LocalDate date : sampledDates) {
            snapshots.add(snapshot(date, trades, normalizedPrices, realizedPnlBaselines, availableCash));
        }
        if (snapshots.size() < 2 && snapshots.stream().noneMatch(item -> !item.date().isBefore(from))) {
            return empty(from, to, granularity);
        }
        List<PortfolioPerformancePointBO> points = switch (granularity) {
            case DAILY -> dailyPoints(snapshots, from);
            case WEEKLY -> weeklyPoints(snapshots, from);
            case MONTHLY -> monthlyPoints(snapshots, from);
            case YEARLY -> yearlyPoints(snapshots, from);
        };
        if (points.isEmpty()) {
            return empty(from, to, granularity);
        }
        DailySnapshot anchor = snapshots.stream().filter(item -> item.date().isBefore(from)).reduce((left, right) -> right)
                .orElse(null);
        PortfolioPerformancePointBO last = points.getLast();
        BigDecimal startAssets = anchor == null ? points.getFirst().totalAssets() : anchor.totalAssets();
        BigDecimal periodPnl = money(points.stream().map(PortfolioPerformancePointBO::periodPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal periodReturnRate = rate(periodPnl, startAssets);
        BigDecimal best = points.stream().map(PortfolioPerformancePointBO::periodPnl).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal worst = points.stream().map(PortfolioPerformancePointBO::periodPnl).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return new PortfolioPerformanceResultBO(granularity, from, to, periodPnl, periodReturnRate,
                money(startAssets), money(last.totalAssets()), money(best), money(worst), points.size(), points);
    }

    /**
     * Calculates the current-day movement without treating positions bought
     * on earlier dates as if their entire unrealized P&amp;L was earned today.
     * The opening portfolio is marked at the previous trading close, then
     * today's trade impact is added at the current price.
     */
    public BigDecimal calculateTodayPnl(List<PortfolioTradeBO> trades,
                                        Map<String, List<PortfolioPricePointBO>> priceSeries,
                                        Map<String, BigDecimal> realtimePrices,
                                        LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, today).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> calculateTodayPnlBySymbol(List<PortfolioTradeBO> trades,
                                                              Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                              Map<String, BigDecimal> realtimePrices,
                                                              LocalDate today) {
        if (today == null || trades == null || trades.isEmpty()) return Map.of();
        Map<String, List<PortfolioPricePointBO>> normalizedPrices = normalizePrices(priceSeries);
        Map<String, BigDecimal> previousPrices = new HashMap<>();
        Map<String, BigDecimal> currentPrices = new HashMap<>();
        normalizedPrices.forEach((symbol, series) -> {
            series.stream().filter(point -> point.tradeDate().isBefore(today)).reduce((left, right) -> right)
                    .ifPresent(point -> previousPrices.put(symbol, point.close()));
            series.stream().filter(point -> point.tradeDate().equals(today)).reduce((left, right) -> right)
                    .ifPresent(point -> currentPrices.put(symbol, point.close()));
        });
        if (realtimePrices != null) currentPrices.putAll(realtimePrices);
        if (currentPrices.isEmpty() || previousPrices.isEmpty()) return Map.of();

        java.util.Set<String> symbols = new java.util.HashSet<>(previousPrices.keySet());
        symbols.addAll(currentPrices.keySet());
        trades.stream().filter(Objects::nonNull).map(PortfolioTradeBO::symbol).forEach(symbols::add);
        Map<String, BigDecimal> result = new HashMap<>();
        for (String symbol : symbols) {
            BigDecimal previousPrice = previousPrices.get(symbol);
            BigDecimal currentPrice = currentPrices.get(symbol);
            if (previousPrice == null || currentPrice == null) continue;
            List<PortfolioTradeBO> symbolTrades = trades.stream()
                    .filter(Objects::nonNull)
                    .filter(trade -> symbol.equals(trade.symbol()))
                    .toList();
            List<PortfolioTradeBO> openingTrades = symbolTrades.stream()
                    .filter(trade -> trade.tradedAt().toLocalDate().isBefore(today))
                    .toList();
            List<PortfolioTradeBO> effectiveTrades = symbolTrades.stream()
                    .filter(trade -> !trade.tradedAt().toLocalDate().isAfter(today))
                    .toList();
            Map<String, BigDecimal> previousPriceMap = Map.of(symbol, previousPrice);
            Map<String, BigDecimal> currentPriceMap = Map.of(symbol, currentPrice);
            PortfolioLedgerResultBO openingAtPreviousClose = ledgerService.calculate(openingTrades, previousPriceMap);
            PortfolioLedgerResultBO openingAtCurrent = ledgerService.calculate(openingTrades, currentPriceMap);
            PortfolioLedgerResultBO effectiveAtCurrent = ledgerService.calculate(effectiveTrades, currentPriceMap);
            BigDecimal priceMove = openingAtCurrent.totalPnl().subtract(openingAtPreviousClose.totalPnl());
            BigDecimal todayTradeImpact = effectiveAtCurrent.totalPnl().subtract(openingAtCurrent.totalPnl());
            result.put(symbol, money(priceMove.add(todayTradeImpact)));
        }
        return result;
    }

    private DailySnapshot snapshot(LocalDate date,
                                   List<PortfolioTradeBO> trades,
                                   Map<String, List<PortfolioPricePointBO>> priceSeries,
                                   Map<String, BigDecimal> baselines,
                                   BigDecimal availableCash) {
        List<PortfolioTradeBO> prefix = (trades == null ? List.<PortfolioTradeBO>of() : trades).stream()
                .filter(Objects::nonNull)
                .filter(trade -> !trade.tradedAt().toLocalDate().isAfter(date))
                .toList();
        Map<String, BigDecimal> prices = new HashMap<>();
        priceSeries.forEach((symbol, series) -> series.stream()
                .filter(point -> !point.tradeDate().isAfter(date))
                .reduce((left, right) -> right)
                .ifPresent(point -> prices.put(symbol, point.close())));
        PortfolioLedgerResultBO ledger = ledgerService.applyRealizedPnlBaselines(
                ledgerService.calculate(prefix, prices), baselines);
        BigDecimal marketValue = ledger.positions().stream().map(PortfolioPositionBO::marketValue)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costBasis = ledger.positions().stream().map(PortfolioPositionBO::costBasis)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cash = availableCash == null ? BigDecimal.ZERO : availableCash;
        return new DailySnapshot(date, money(marketValue), money(marketValue.add(cash)), money(costBasis), money(ledger.totalPnl()));
    }

    private List<PortfolioPerformancePointBO> dailyPoints(List<DailySnapshot> snapshots, LocalDate from) {
        List<PortfolioPerformancePointBO> points = new ArrayList<>();
        DailySnapshot previous = null;
        for (DailySnapshot snapshot : snapshots) {
            if (snapshot.date().isBefore(from)) {
                previous = snapshot;
                continue;
            }
            BigDecimal delta = snapshot.totalPnl().subtract(previous == null ? BigDecimal.ZERO : previous.totalPnl());
            points.add(point(snapshot, delta, rate(delta, previous == null ? snapshot.totalAssets() : previous.totalAssets())));
            previous = snapshot;
        }
        return points;
    }

    private List<PortfolioPerformancePointBO> monthlyPoints(List<DailySnapshot> snapshots, LocalDate from) {
        Map<YearMonth, DailySnapshot> monthEnds = new LinkedHashMap<>();
        for (DailySnapshot snapshot : snapshots) {
            if (!snapshot.date().isBefore(from)) monthEnds.put(YearMonth.from(snapshot.date()), snapshot);
        }
        List<PortfolioPerformancePointBO> points = new ArrayList<>();
        DailySnapshot previous = snapshots.stream().filter(item -> item.date().isBefore(from)).reduce((left, right) -> right).orElse(null);
        for (DailySnapshot snapshot : monthEnds.values()) {
            BigDecimal delta = snapshot.totalPnl().subtract(previous == null ? BigDecimal.ZERO : previous.totalPnl());
            points.add(point(snapshot, delta, rate(delta, previous == null ? snapshot.totalAssets() : previous.totalAssets())));
            previous = snapshot;
        }
        return points;
    }

    private List<PortfolioPerformancePointBO> weeklyPoints(List<DailySnapshot> snapshots, LocalDate from) {
        Map<LocalDate, DailySnapshot> weekEnds = new LinkedHashMap<>();
        for (DailySnapshot snapshot : snapshots) {
            if (!snapshot.date().isBefore(from)) {
                LocalDate weekStart = snapshot.date().with(DayOfWeek.MONDAY);
                weekEnds.put(weekStart, snapshot);
            }
        }
        List<PortfolioPerformancePointBO> points = new ArrayList<>();
        DailySnapshot previous = snapshots.stream().filter(item -> item.date().isBefore(from))
                .reduce((left, right) -> right).orElse(null);
        for (DailySnapshot snapshot : weekEnds.values()) {
            BigDecimal delta = snapshot.totalPnl().subtract(previous == null ? BigDecimal.ZERO : previous.totalPnl());
            points.add(point(snapshot, delta, rate(delta, previous == null ? snapshot.totalAssets() : previous.totalAssets())));
            previous = snapshot;
        }
        return points;
    }

    private List<PortfolioPerformancePointBO> yearlyPoints(List<DailySnapshot> snapshots, LocalDate from) {
        Map<Year, DailySnapshot> yearEnds = new LinkedHashMap<>();
        for (DailySnapshot snapshot : snapshots) {
            if (!snapshot.date().isBefore(from)) yearEnds.put(Year.from(snapshot.date()), snapshot);
        }
        List<PortfolioPerformancePointBO> points = new ArrayList<>();
        DailySnapshot previous = snapshots.stream().filter(item -> item.date().isBefore(from))
                .reduce((left, right) -> right).orElse(null);
        for (DailySnapshot snapshot : yearEnds.values()) {
            BigDecimal delta = snapshot.totalPnl().subtract(previous == null ? BigDecimal.ZERO : previous.totalPnl());
            points.add(point(snapshot, delta, rate(delta, previous == null ? snapshot.totalAssets() : previous.totalAssets())));
            previous = snapshot;
        }
        return points;
    }

    private PortfolioPerformancePointBO point(DailySnapshot snapshot, BigDecimal periodPnl, BigDecimal returnRate) {
        return new PortfolioPerformancePointBO(snapshot.date(), snapshot.marketValue(), snapshot.totalAssets(),
                snapshot.costBasis(), snapshot.totalPnl(), money(periodPnl), returnRate);
    }

    private Map<String, List<PortfolioPricePointBO>> normalizePrices(Map<String, List<PortfolioPricePointBO>> source) {
        if (source == null) return Map.of();
        return source.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue() == null ? List.of() : entry.getValue().stream().filter(Objects::nonNull)
                        .sorted(Comparator.comparing(PortfolioPricePointBO::tradeDate)).toList()));
    }

    private PortfolioPerformanceResultBO empty(LocalDate from, LocalDate to, PortfolioPerformanceGranularity granularity) {
        return new PortfolioPerformanceResultBO(granularity, from, to, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, List.of());
    }

    private BigDecimal rate(BigDecimal pnl, BigDecimal denominator) {
        return denominator == null || denominator.signum() <= 0 ? BigDecimal.ZERO
                : pnl.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private record DailySnapshot(LocalDate date, BigDecimal marketValue, BigDecimal totalAssets,
                                 BigDecimal costBasis, BigDecimal totalPnl) {
    }
}
