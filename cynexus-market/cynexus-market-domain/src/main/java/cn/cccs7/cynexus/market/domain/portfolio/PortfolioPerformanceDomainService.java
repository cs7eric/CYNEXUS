package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.DayOfWeek;
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
        DailySnapshot anchor = snapshots.stream().filter(item -> item.date().isBefore(from)).reduce((left, right) -> right)
                .orElse(null);
        BigDecimal initialAssets = anchor == null ? null : anchor.totalAssets();
        List<PortfolioPerformancePointBO> dailyPoints = dailyPoints(snapshots, from, trades, normalizedPrices, initialAssets);
        List<PortfolioPerformancePointBO> points = switch (granularity) {
            case DAILY -> dailyPoints;
            case WEEKLY -> aggregatePoints(dailyPoints, PortfolioPerformanceGranularity.WEEKLY, initialAssets);
            case MONTHLY -> aggregatePoints(dailyPoints, PortfolioPerformanceGranularity.MONTHLY, initialAssets);
            case YEARLY -> aggregatePoints(dailyPoints, PortfolioPerformanceGranularity.YEARLY, initialAssets);
        };
        if (points.isEmpty()) {
            return empty(from, to, granularity);
        }
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
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, Map.of(), Map.of(), today).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTodayPnl(List<PortfolioTradeBO> trades,
                                        Map<String, List<PortfolioPricePointBO>> priceSeries,
                                        Map<String, BigDecimal> realtimePrices,
                                        Map<String, BigDecimal> previousClosePrices,
                                        LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, previousClosePrices, Map.of(), today).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTodayPnl(List<PortfolioTradeBO> trades,
                                        Map<String, List<PortfolioPricePointBO>> priceSeries,
                                        Map<String, BigDecimal> realtimePrices,
                                        Map<String, BigDecimal> previousClosePrices,
                                        Map<String, LocalDate> realtimePriceDates,
                                        LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, previousClosePrices, realtimePriceDates, today).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> calculateTodayPnlBySymbol(List<PortfolioTradeBO> trades,
                                                              Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                              Map<String, BigDecimal> realtimePrices,
                                                              LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, Map.of(), Map.of(), today);
    }

    public Map<String, BigDecimal> calculateTodayPnlBySymbol(List<PortfolioTradeBO> trades,
                                                              Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                              Map<String, BigDecimal> realtimePrices,
                                                              Map<String, BigDecimal> previousClosePrices,
                                                              LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, previousClosePrices, Map.of(), today);
    }

    public Map<String, BigDecimal> calculateTodayPnlBySymbol(List<PortfolioTradeBO> trades,
                                                              Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                              Map<String, BigDecimal> realtimePrices,
                                                              Map<String, BigDecimal> previousClosePrices,
                                                              Map<String, LocalDate> realtimePriceDates,
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
        boolean hasQuoteDates = realtimePriceDates != null && !realtimePriceDates.isEmpty();
        if (previousClosePrices != null) previousClosePrices.forEach((symbol, price) -> {
            if (!hasQuoteDates || today.equals(realtimePriceDates.get(symbol))) previousPrices.put(symbol, price);
        });
        if (realtimePrices != null) realtimePrices.forEach((symbol, price) -> {
            if (!hasQuoteDates || today.equals(realtimePriceDates.get(symbol))) currentPrices.put(symbol, price);
        });
        if (hasQuoteDates) {
            // Before the market opens, the quote provider still returns the
            // previous session's quote. Mark those symbols flat for today
            // instead of leaving the overview without a per-position value.
            realtimePriceDates.forEach((symbol, quoteDate) -> {
                if (!today.equals(quoteDate) && !currentPrices.containsKey(symbol)) {
                    BigDecimal previousPrice = previousPrices.get(symbol);
                    if (previousPrice != null) currentPrices.put(symbol, previousPrice);
                }
            });
        }
        if (currentPrices.isEmpty()) return Map.of();

        java.util.Set<String> symbols = new java.util.HashSet<>(previousPrices.keySet());
        symbols.addAll(currentPrices.keySet());
        trades.stream().filter(Objects::nonNull).map(PortfolioTradeBO::symbol).forEach(symbols::add);
        Map<String, BigDecimal> result = new HashMap<>();
        for (String symbol : symbols) {
            BigDecimal currentPrice = currentPrices.get(symbol);
            BigDecimal previousPrice = previousPrices.get(symbol);
            if (currentPrice == null) continue;
            List<PortfolioTradeBO> symbolTrades = trades.stream()
                    .filter(Objects::nonNull)
                    .filter(trade -> trade.status() == TradeStatus.ACTIVE)
                    .filter(trade -> symbol.equals(trade.symbol()))
                    .toList();
            List<PortfolioTradeBO> openingTrades = symbolTrades.stream()
                    .filter(trade -> trade.tradedAt().toLocalDate().isBefore(today))
                    .toList();
            List<PortfolioTradeBO> effectiveTrades = symbolTrades.stream()
                    .filter(trade -> !trade.tradedAt().toLocalDate().isAfter(today))
                    .toList();
            BigDecimal openingQuantity = openingTrades.stream()
                    .map(trade -> trade.side() == TradeSide.BUY ? trade.quantity() : trade.quantity().negate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .max(BigDecimal.ZERO);
            BigDecimal priceMove = previousPrice == null
                    ? BigDecimal.ZERO
                    : currentPrice.subtract(previousPrice).multiply(openingQuantity);
            BigDecimal todayTradeImpact = effectiveTrades.stream()
                    .filter(trade -> trade.tradedAt().toLocalDate().equals(today))
                    .map(trade -> {
                        BigDecimal quantity = trade.quantity();
                        BigDecimal impact;
                        if (trade.side() == TradeSide.BUY) {
                            impact = currentPrice.subtract(trade.price()).multiply(quantity);
                        } else if (previousPrice != null) {
                            impact = trade.price().subtract(previousPrice).multiply(quantity);
                        } else {
                            impact = BigDecimal.ZERO;
                        }
                        return impact.subtract(trade.totalFee() == null ? BigDecimal.ZERO : trade.totalFee());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private List<PortfolioPerformancePointBO> dailyPoints(List<DailySnapshot> snapshots,
                                                          LocalDate from,
                                                          List<PortfolioTradeBO> trades,
                                                          Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                          BigDecimal initialAssets) {
        List<PortfolioPerformancePointBO> points = new ArrayList<>();
        BigDecimal previousAssets = initialAssets;
        for (DailySnapshot snapshot : snapshots) {
            if (snapshot.date().isBefore(from)) {
                continue;
            }
            BigDecimal dailyPnl = calculateTodayPnl(trades, priceSeries, Map.of(), snapshot.date());
            BigDecimal denominator = previousAssets == null ? snapshot.totalAssets() : previousAssets;
            points.add(point(snapshot, dailyPnl, rate(dailyPnl, denominator)));
            previousAssets = snapshot.totalAssets();
        }
        return points;
    }

    private List<PortfolioPerformancePointBO> aggregatePoints(List<PortfolioPerformancePointBO> dailyPoints,
                                                              PortfolioPerformanceGranularity granularity,
                                                              BigDecimal initialAssets) {
        Map<LocalDate, List<PortfolioPerformancePointBO>> grouped = new LinkedHashMap<>();
        for (PortfolioPerformancePointBO point : dailyPoints) {
            LocalDate key = switch (granularity) {
                case WEEKLY -> point.tradeDate().with(DayOfWeek.MONDAY);
                case MONTHLY -> point.tradeDate().withDayOfMonth(1);
                case YEARLY -> point.tradeDate().withDayOfYear(1);
                case DAILY -> point.tradeDate();
            };
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(point);
        }
        List<PortfolioPerformancePointBO> points = new ArrayList<>();
        BigDecimal previousAssets = initialAssets;
        for (List<PortfolioPerformancePointBO> periodPoints : grouped.values()) {
            PortfolioPerformancePointBO last = periodPoints.getLast();
            BigDecimal periodPnl = periodPoints.stream().map(PortfolioPerformancePointBO::periodPnl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal denominator = previousAssets == null ? last.totalAssets() : previousAssets;
            points.add(new PortfolioPerformancePointBO(last.tradeDate(), last.marketValue(), last.totalAssets(),
                    last.costBasis(), last.totalPnl(), money(periodPnl), rate(periodPnl, denominator)));
            previousAssets = last.totalAssets();
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
