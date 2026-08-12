package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
            BigDecimal currentTotalAssets,
            LocalDate from,
            LocalDate to,
            PortfolioPerformanceGranularity granularity) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("performance date range is invalid");
        }
        Map<String, List<PortfolioPricePointBO>> normalizedPrices = normalizePrices(priceSeries);
        LocalDate portfolioStartDate = portfolioStartDate(trades);
        if (portfolioStartDate == null) {
            return empty(from, to, granularity);
        }
        TreeSet<LocalDate> dates = new TreeSet<>();
        normalizedPrices.values().forEach(series -> series.stream()
                .map(PortfolioPricePointBO::tradeDate)
                .filter(date -> !date.isBefore(portfolioStartDate))
                .forEach(dates::add));
        if (dates.isEmpty()) {
            return empty(from, to, granularity);
        }
        List<DailySnapshot> snapshots = new ArrayList<>();
        for (LocalDate date : dates) {
            snapshots.add(snapshot(date, trades, normalizedPrices, realizedPnlBaselines));
        }
        DailyPerformance dailyPerformance = dailyPerformance(
                snapshots, from, to, trades, normalizedPrices, currentTotalAssets);
        List<PortfolioPerformancePointBO> dailyPoints = dailyPerformance.points();
        List<PortfolioPerformancePointBO> points = switch (granularity) {
            case DAILY -> dailyPoints;
            case WEEKLY -> aggregatePoints(dailyPoints, PortfolioPerformanceGranularity.WEEKLY, dailyPerformance.startAssets());
            case MONTHLY -> aggregatePoints(dailyPoints, PortfolioPerformanceGranularity.MONTHLY, dailyPerformance.startAssets());
            case YEARLY -> aggregatePoints(dailyPoints, PortfolioPerformanceGranularity.YEARLY, dailyPerformance.startAssets());
        };
        if (points.isEmpty()) {
            return empty(from, to, granularity);
        }
        PortfolioPerformancePointBO last = points.getLast();
        BigDecimal startAssets = dailyPerformance.startAssets();
        BigDecimal periodPnl = money(points.stream().map(PortfolioPerformancePointBO::periodPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal periodReturnRate = rate(periodPnl, startAssets);
        BigDecimal best = points.stream().map(PortfolioPerformancePointBO::periodPnl).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal worst = points.stream().map(PortfolioPerformancePointBO::periodPnl).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return new PortfolioPerformanceResultBO(granularity, from, to, periodPnl, periodReturnRate,
                money(startAssets), money(last.totalAssets()), money(best), money(worst), points.size(), points,
                dailyPerformance.periodPnlBySymbol(), dailyPerformance.lastDayPnlBySymbol());
    }

    public BigDecimal cumulativePnl(
            List<PortfolioTradeBO> trades,
            Map<String, List<PortfolioPricePointBO>> priceSeries,
            Map<String, BigDecimal> realizedPnlBaselines,
            LocalDate from,
            LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            return BigDecimal.ZERO;
        }
        PortfolioPerformanceResultBO result = calculate(
                trades, priceSeries, realizedPnlBaselines, BigDecimal.ZERO, from, to,
                PortfolioPerformanceGranularity.DAILY);
        return result == null ? BigDecimal.ZERO : money(result.periodPnl());
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
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, Map.of(), Map.of(), Map.of(), today, false).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTodayPnl(List<PortfolioTradeBO> trades,
                                        Map<String, List<PortfolioPricePointBO>> priceSeries,
                                        Map<String, BigDecimal> realtimePrices,
                                        Map<String, BigDecimal> previousClosePrices,
                                        LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, previousClosePrices, Map.of(), Map.of(), today, true).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTodayPnl(List<PortfolioTradeBO> trades,
                                        Map<String, List<PortfolioPricePointBO>> priceSeries,
                                        Map<String, BigDecimal> realtimePrices,
                                        Map<String, BigDecimal> previousClosePrices,
                                        Map<String, LocalDate> realtimePriceDates,
                                        LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, previousClosePrices, realtimePriceDates, Map.of(), today, true).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTodayPnl(List<PortfolioTradeBO> trades,
                                        Map<String, List<PortfolioPricePointBO>> priceSeries,
                                        Map<String, BigDecimal> realtimePrices,
                                        Map<String, BigDecimal> previousClosePrices,
                                        Map<String, LocalDate> realtimePriceDates,
                                        Map<String, List<PortfolioIntradayPricePointBO>> intradayPriceSeries,
                                        LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, previousClosePrices,
                realtimePriceDates, intradayPriceSeries, today, true).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> calculateTodayPnlBySymbol(List<PortfolioTradeBO> trades,
                                                              Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                              Map<String, BigDecimal> realtimePrices,
                                                              LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, Map.of(), Map.of(), Map.of(), today, false);
    }

    public Map<String, BigDecimal> calculateTodayPnlBySymbol(List<PortfolioTradeBO> trades,
                                                              Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                              Map<String, BigDecimal> realtimePrices,
                                                              Map<String, BigDecimal> previousClosePrices,
                                                              LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, previousClosePrices, Map.of(), Map.of(), today, true);
    }

    public Map<String, BigDecimal> calculateTodayPnlBySymbol(List<PortfolioTradeBO> trades,
                                                              Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                              Map<String, BigDecimal> realtimePrices,
                                                              Map<String, BigDecimal> previousClosePrices,
                                                              Map<String, LocalDate> realtimePriceDates,
                                                              LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, previousClosePrices, realtimePriceDates, Map.of(), today, true);
    }

    public Map<String, BigDecimal> calculateTodayPnlBySymbol(List<PortfolioTradeBO> trades,
                                                              Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                              Map<String, BigDecimal> realtimePrices,
                                                              Map<String, BigDecimal> previousClosePrices,
                                                              Map<String, LocalDate> realtimePriceDates,
                                                              Map<String, List<PortfolioIntradayPricePointBO>> intradayPriceSeries,
                                                              LocalDate today) {
        return calculateTodayPnlBySymbol(trades, priceSeries, realtimePrices, previousClosePrices,
                realtimePriceDates, intradayPriceSeries, today, true);
    }

    private Map<String, BigDecimal> calculateTodayPnlBySymbol(List<PortfolioTradeBO> trades,
                                                               Map<String, List<PortfolioPricePointBO>> priceSeries,
                                                               Map<String, BigDecimal> realtimePrices,
                                                               Map<String, BigDecimal> previousClosePrices,
                                                               Map<String, LocalDate> realtimePriceDates,
                                                               Map<String, List<PortfolioIntradayPricePointBO>> intradayPriceSeries,
                                                               LocalDate today,
                                                               boolean usePostTradeShares) {
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

        LocalDate portfolioBaselineDate = portfolioStartDate(trades);

        java.util.Set<String> symbols = new java.util.HashSet<>(previousPrices.keySet());
        symbols.addAll(currentPrices.keySet());
        trades.stream().filter(Objects::nonNull).map(PortfolioTradeBO::symbol).forEach(symbols::add);
        Map<String, BigDecimal> result = new HashMap<>();
        for (String symbol : symbols) {
            BigDecimal currentPrice = currentPrices.get(symbol);
            BigDecimal previousPrice = previousPrices.get(symbol);
            if (currentPrice == null) continue;
            boolean portfolioBaselineDay = today.equals(portfolioBaselineDate);
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
            List<PortfolioIntradayPricePointBO> intradayPoints = intradayPriceSeries == null
                    ? List.of()
                    : intradayPriceSeries.getOrDefault(symbol, List.of());
            boolean hasSameDayTrades = effectiveTrades.stream()
                    .anyMatch(trade -> trade.tradedAt().toLocalDate().equals(today));
            if (usePostTradeShares && previousPrice != null && (hasSameDayTrades || !intradayPoints.isEmpty())) {
                result.put(symbol, money(calculateIntradayPnl(symbolTrades, openingQuantity, previousPrice,
                        currentPrice, intradayPoints, today, portfolioBaselineDay)));
                continue;
            }
            BigDecimal priceMove = previousPrice == null
                    ? BigDecimal.ZERO
                    : currentPrice.subtract(previousPrice).multiply(openingQuantity);
            BigDecimal todayTradeImpact = effectiveTrades.stream()
                    .filter(trade -> trade.tradedAt().toLocalDate().equals(today))
                    .map(trade -> {
                        BigDecimal quantity = trade.quantity();
                        BigDecimal impact;
                        if (trade.side() == TradeSide.BUY) {
                            // The first confirmed portfolio date is the import
                            // baseline, not a gain/loss earned during that
                            // session. A symbol first bought on a later date is
                            // a real same-day trade and must still contribute
                            // its execution-price impact.
                            if (portfolioBaselineDay && openingQuantity.signum() <= 0) {
                                // The import fee is part of the opening cost
                                // basis as well; it must not become a loss on
                                // the baseline day.
                                return BigDecimal.ZERO;
                            }
                            impact = currentPrice.subtract(trade.price()).multiply(quantity);
                        } else if (previousPrice != null) {
                            impact = (usePostTradeShares ? trade.price().subtract(currentPrice)
                                    : trade.price().subtract(previousPrice)).multiply(quantity);
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

    private BigDecimal calculateIntradayPnl(List<PortfolioTradeBO> symbolTrades,
                                            BigDecimal openingQuantity,
                                            BigDecimal previousPrice,
                                            BigDecimal currentPrice,
                                            List<PortfolioIntradayPricePointBO> intradayPoints,
                                            LocalDate today,
                                            boolean portfolioBaselineDay) {
        List<PortfolioIntradayPricePointBO> points = intradayPoints.stream()
                .filter(point -> point.tradedAt().toLocalDate().equals(today))
                .sorted(Comparator.comparing(PortfolioIntradayPricePointBO::tradedAt))
                .toList();
        List<PortfolioTradeBO> todayTrades = symbolTrades.stream()
                .filter(trade -> trade.tradedAt().toLocalDate().equals(today))
                .sorted(Comparator.comparing(PortfolioTradeBO::tradedAt).thenComparing(PortfolioTradeBO::id))
                .toList();
        if (todayTrades.isEmpty()) {
            return currentPrice.subtract(previousPrice).multiply(openingQuantity);
        }

        BigDecimal pnl = BigDecimal.ZERO;
        BigDecimal quantity = openingQuantity;
        BigDecimal lastMarketPrice = previousPrice;
        for (PortfolioTradeBO trade : todayTrades) {
            PortfolioIntradayPricePointBO marketPoint = priceAtTrade(points, trade.tradedAt().toLocalDateTime(), trade.price());
            BigDecimal marketPrice = marketPoint.price();
            pnl = pnl.add(marketPrice.subtract(lastMarketPrice).multiply(quantity));
            if (trade.side() == TradeSide.BUY) {
                if (portfolioBaselineDay && openingQuantity.signum() <= 0) {
                    lastMarketPrice = marketPrice;
                    continue;
                }
                pnl = pnl.add(marketPrice.subtract(trade.price()).multiply(trade.quantity()))
                        .subtract(fee(trade));
                quantity = quantity.add(trade.quantity());
            } else {
                BigDecimal matchedQuantity = trade.quantity().min(quantity);
                if (matchedQuantity.signum() > 0) {
                    pnl = pnl.add(trade.price().subtract(marketPrice).multiply(matchedQuantity))
                            .subtract(allocatedFee(trade, matchedQuantity));
                    quantity = quantity.subtract(matchedQuantity);
                }
            }
            lastMarketPrice = marketPrice;
        }
        return pnl.add(currentPrice.subtract(lastMarketPrice).multiply(quantity));
    }

    private PortfolioIntradayPricePointBO priceAtTrade(List<PortfolioIntradayPricePointBO> points,
                                                       java.time.LocalDateTime tradeTime,
                                                       BigDecimal fallbackPrice) {
        PortfolioIntradayPricePointBO atOrBefore = points.stream()
                .filter(point -> !point.tradedAt().isAfter(tradeTime))
                .reduce((left, right) -> right).orElse(null);
        if (atOrBefore != null) {
            return atOrBefore;
        }
        // A quote after the execution time cannot be used for the price
        // segment before the trade. Fall back to the executed price until a
        // quote at or before the trade is available.
        return new PortfolioIntradayPricePointBO(tradeTime, fallbackPrice);
    }

    private BigDecimal fee(PortfolioTradeBO trade) {
        return trade.totalFee() == null ? BigDecimal.ZERO : trade.totalFee();
    }

    private BigDecimal allocatedFee(PortfolioTradeBO trade, BigDecimal matchedQuantity) {
        if (trade.quantity().signum() == 0) return BigDecimal.ZERO;
        return fee(trade).multiply(matchedQuantity).divide(trade.quantity(), SCALE, RoundingMode.HALF_UP);
    }

    private DailySnapshot snapshot(LocalDate date,
                                   List<PortfolioTradeBO> trades,
                                   Map<String, List<PortfolioPricePointBO>> priceSeries,
                                   Map<String, BigDecimal> baselines) {
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
        return new DailySnapshot(date, money(marketValue), money(costBasis), money(ledger.totalPnl()));
    }

    private DailyPerformance dailyPerformance(List<DailySnapshot> snapshots,
                                              LocalDate from,
                                              LocalDate to,
                                              List<PortfolioTradeBO> trades,
                                              Map<String, List<PortfolioPricePointBO>> priceSeries,
                                              BigDecimal currentTotalAssets) {
        List<DailyCalculation> calculations = new ArrayList<>();
        for (DailySnapshot snapshot : snapshots) {
            Map<String, BigDecimal> bySymbol = calculateTodayPnlBySymbol(
                    trades, priceSeries, Map.of(), Map.of(), snapshot.date());
            BigDecimal dailyPnl = money(bySymbol.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            calculations.add(new DailyCalculation(snapshot, dailyPnl, bySymbol));
        }
        if (calculations.isEmpty()) {
            return new DailyPerformance(List.of(), BigDecimal.ZERO, Map.of(), Map.of());
        }

        BigDecimal runningAssets = money(currentTotalAssets);
        List<DailyCalculatedPoint> reversed = new ArrayList<>();
        for (int index = calculations.size() - 1; index >= 0; index--) {
            DailyCalculation calculation = calculations.get(index);
            BigDecimal previousAssets = money(runningAssets.subtract(calculation.periodPnl()));
            PortfolioPerformancePointBO value = point(calculation.snapshot(), runningAssets,
                    calculation.periodPnl(), rate(calculation.periodPnl(), previousAssets));
            reversed.add(new DailyCalculatedPoint(value, calculation.bySymbol()));
            runningAssets = previousAssets;
        }
        Collections.reverse(reversed);

        List<DailyCalculatedPoint> selected = reversed.stream()
                .filter(item -> !item.point().tradeDate().isBefore(from) && !item.point().tradeDate().isAfter(to))
                .toList();
        if (selected.isEmpty()) {
            return new DailyPerformance(List.of(), BigDecimal.ZERO, Map.of(), Map.of());
        }
        Map<String, BigDecimal> periodPnlBySymbol = new LinkedHashMap<>();
        selected.forEach(item -> item.bySymbol().forEach((symbol, pnl) ->
                periodPnlBySymbol.merge(symbol, pnl, BigDecimal::add)));
        periodPnlBySymbol.replaceAll((symbol, pnl) -> money(pnl));
        PortfolioPerformancePointBO first = selected.getFirst().point();
        BigDecimal startAssets = money(first.totalAssets().subtract(first.periodPnl()));
        return new DailyPerformance(selected.stream().map(DailyCalculatedPoint::point).toList(), startAssets,
                periodPnlBySymbol, selected.getLast().bySymbol());
    }

    private List<PortfolioPerformancePointBO> aggregatePoints(List<PortfolioPerformancePointBO> dailyPoints,
                                                              PortfolioPerformanceGranularity granularity,
                                                              BigDecimal initialAssets) {
        Map<LocalDate, List<PortfolioPerformancePointBO>> grouped = new LinkedHashMap<>();
        for (PortfolioPerformancePointBO point : dailyPoints) {
            LocalDate key = switch (granularity) {
                case WEEKLY -> sundayWeekStart(point.tradeDate());
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

    private LocalDate sundayWeekStart(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() % 7L);
    }

    private PortfolioPerformancePointBO point(DailySnapshot snapshot, BigDecimal totalAssets,
                                               BigDecimal periodPnl, BigDecimal returnRate) {
        return new PortfolioPerformancePointBO(snapshot.date(), snapshot.marketValue(), money(totalAssets),
                snapshot.costBasis(), snapshot.totalPnl(), money(periodPnl), returnRate);
    }

    private LocalDate portfolioStartDate(List<PortfolioTradeBO> trades) {
        if (trades == null) return null;
        return trades.stream().filter(Objects::nonNull)
                .filter(trade -> trade.status() == TradeStatus.ACTIVE)
                .map(trade -> trade.tradedAt().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(null);
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

    private record DailySnapshot(LocalDate date, BigDecimal marketValue, BigDecimal costBasis, BigDecimal totalPnl) {
    }

    private record DailyCalculation(DailySnapshot snapshot, BigDecimal periodPnl,
                                    Map<String, BigDecimal> bySymbol) {
    }

    private record DailyCalculatedPoint(PortfolioPerformancePointBO point, Map<String, BigDecimal> bySymbol) {
    }

    private record DailyPerformance(List<PortfolioPerformancePointBO> points, BigDecimal startAssets,
                                    Map<String, BigDecimal> periodPnlBySymbol,
                                    Map<String, BigDecimal> lastDayPnlBySymbol) {
    }
}
