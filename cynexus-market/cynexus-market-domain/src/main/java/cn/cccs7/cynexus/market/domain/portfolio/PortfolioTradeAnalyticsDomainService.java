package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure FIFO matching and net performance statistics for confirmed active trades. */
public final class PortfolioTradeAnalyticsDomainService {
    private static final int SCALE = 8;

    public PortfolioTradeAnalyticsBO calculate(List<PortfolioTradeBO> source, LocalDate from, LocalDate to,
                                                String symbol, PortfolioInstrumentType instrumentType,
                                                String result, String holdingPeriod, String strategy, String tag) {
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("analytics date range is invalid");
        List<PortfolioTradeBO> trades = (source == null ? List.<PortfolioTradeBO>of() : source).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.status() == TradeStatus.ACTIVE)
                .filter(t -> symbol == null || symbol.isBlank() || symbol.equals(t.symbol()))
                .filter(t -> instrumentType == null || instrumentType == t.instrumentType())
                .filter(t -> strategy == null || strategy.isBlank() || strategy.equalsIgnoreCase(t.strategy()))
                .filter(t -> tag == null || tag.isBlank() || (t.tags() != null && t.tags().toLowerCase().contains(tag.toLowerCase())))
                .sorted(Comparator.comparing(PortfolioTradeBO::symbol).thenComparing(PortfolioTradeBO::instrumentType)
                        .thenComparing(PortfolioTradeBO::tradedAt).thenComparing(PortfolioTradeBO::id))
                .toList();
        List<PortfolioTradeRoundBO> rounds = new ArrayList<>();
        List<PortfolioTradeAnalyticsWarningBO> warnings = new ArrayList<>();
        // A symbol should normally have one instrument type. Keep the matching
        // key strict, but surface imported data that would otherwise look like
        // a missing FIFO match in the UI.
        trades.stream().collect(java.util.stream.Collectors.groupingBy(PortfolioTradeBO::symbol))
                .forEach((tradeSymbol, symbolTrades) -> {
                    long types = symbolTrades.stream().map(PortfolioTradeBO::instrumentType).distinct().count();
                    if (types > 1) {
                        warnings.add(new PortfolioTradeAnalyticsWarningBO(
                                "MIXED_INSTRUMENT_TYPE", tradeSymbol, BigDecimal.ZERO,
                                "同一标的存在多个 instrumentType，已按 instrumentType 分开匹配"));
                    }
                });
        Map<String, Deque<Lot>> queues = new LinkedHashMap<>();
        long nextRoundId = 1;
        for (PortfolioTradeBO trade : trades) {
            String key = trade.instrumentType().name() + ":" + trade.symbol();
            Deque<Lot> queue = queues.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            if (trade.side() == TradeSide.BUY) {
                queue.addLast(new Lot(trade, trade.quantity(), trade.totalFee(), nextRoundId++));
                continue;
            }
            BigDecimal remaining = trade.quantity();
            while (remaining.signum() > 0 && !queue.isEmpty()) {
                Lot lot = queue.removeFirst();
                BigDecimal matched = lot.quantity.min(remaining);
                // Allocate the remaining lot fee against the remaining lot
                // quantity. Using originalQuantity here loses fees when one
                // BUY is consumed by multiple SELL fills.
                BigDecimal entryFee = allocate(lot.fee, matched, lot.quantity);
                BigDecimal exitFee = allocate(trade.totalFee(), matched, trade.quantity());
                BigDecimal gross = trade.price().subtract(lot.trade.price()).multiply(matched);
                BigDecimal remainingLot = lot.quantity.subtract(matched);
                rounds.add(round(rounds.size() + 1L, lot, trade, matched, gross, entryFee.add(exitFee), lot.quantity.compareTo(matched) > 0 ? "PARTIAL" : "CLOSED", remainingLot));
                remaining = remaining.subtract(matched);
                if (lot.quantity.compareTo(matched) > 0) {
                    queue.addFirst(lot.withRemaining(lot.quantity.subtract(matched), lot.fee.subtract(entryFee)));
                }
            }
            if (remaining.signum() > 0) warnings.add(new PortfolioTradeAnalyticsWarningBO(
                    "UNMATCHED_SELL", trade.symbol(), remaining, "卖出数量超过可匹配的买入数量"));
        }
        for (Deque<Lot> queue : queues.values()) {
            for (Lot lot : queue) rounds.add(round(rounds.size() + 1L, lot, null, lot.quantity, BigDecimal.ZERO, lot.fee, "OPEN", lot.quantity));
        }
        rounds = aggregatePositionRounds(rounds, trades);
        rounds = rounds.stream()
                .filter(r -> !r.openedAt().toLocalDate().isBefore(from) && !r.openedAt().toLocalDate().isAfter(to))
                .filter(r -> result == null || result.isBlank() || result.equalsIgnoreCase(resultOf(r)))
                .filter(r -> matchesHoldingPeriod(r, holdingPeriod))
                .toList();
        return summarize(rounds, warnings, from, to);
    }

    private List<PortfolioTradeRoundBO> aggregatePositionRounds(List<PortfolioTradeRoundBO> fragments,
                                                                  List<PortfolioTradeBO> trades) {
        Map<String, List<PortfolioTradeBO>> byKey = trades.stream().collect(java.util.stream.Collectors.groupingBy(
                t -> t.instrumentType().name() + ":" + t.symbol(), LinkedHashMap::new, java.util.stream.Collectors.toList()));
        List<PortfolioTradeRoundBO> result = new ArrayList<>();
        long id = 1;
        for (List<PortfolioTradeBO> keyTrades : byKey.values()) {
            List<PositionEpisode> episodes = new ArrayList<>();
            BigDecimal position = BigDecimal.ZERO;
            OffsetDateTime start = null;
            for (PortfolioTradeBO trade : keyTrades) {
                if (trade.side() == TradeSide.BUY) {
                    if (position.signum() == 0) start = trade.tradedAt();
                    position = position.add(trade.quantity());
                } else {
                    position = position.subtract(trade.quantity());
                    if (position.signum() <= 0 && start != null) {
                        episodes.add(new PositionEpisode(start, trade.tradedAt()));
                        start = null;
                        position = BigDecimal.ZERO;
                    }
                }
            }
            if (start != null) episodes.add(new PositionEpisode(start, null));
            for (PositionEpisode episode : episodes) {
                List<PortfolioTradeRoundBO> parts = fragments.stream()
                        .filter(r -> r.symbol().equals(keyTrades.getFirst().symbol())
                                && r.instrumentType() == keyTrades.getFirst().instrumentType()
                                && !r.openedAt().isBefore(episode.openedAt)
                                && (episode.closedAt == null || (r.closedAt() != null && !r.closedAt().isAfter(episode.closedAt))))
                        .toList();
                if (!parts.isEmpty()) {
                    String securityName = keyTrades.stream().map(PortfolioTradeBO::securityName)
                            .filter(name -> name != null && !name.isBlank()).findFirst().orElse(null);
                    result.add(merge(id++, parts, episode, securityName));
                }
            }
        }
        return result;
    }

    private PortfolioTradeRoundBO merge(long id, List<PortfolioTradeRoundBO> parts, PositionEpisode episode, String securityName) {
        BigDecimal entry = parts.stream().map(PortfolioTradeRoundBO::entryQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal exit = parts.stream().map(PortfolioTradeRoundBO::exitQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PortfolioTradeRoundBO> realizedParts = parts.stream().filter(p -> p.exitQuantity().signum() > 0).toList();
        BigDecimal gross = realizedParts.stream().map(PortfolioTradeRoundBO::grossPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fee = realizedParts.stream().map(PortfolioTradeRoundBO::totalFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = realizedParts.stream().map(PortfolioTradeRoundBO::netPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal entryValue = parts.stream().map(p -> p.averageEntryPrice().multiply(p.entryQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal exitValue = parts.stream().filter(p -> p.averageExitPrice() != null).map(p -> p.averageExitPrice().multiply(p.exitQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unmatched = entry.subtract(exit).max(BigDecimal.ZERO);
        String status = unmatched.signum() > 0 ? (exit.signum() > 0 ? "PARTIAL" : "OPEN") : "CLOSED";
        OffsetDateTime effectiveClosedAt = realizedParts.stream().map(PortfolioTradeRoundBO::closedAt)
                .filter(Objects::nonNull).max(OffsetDateTime::compareTo).orElse(null);
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        parts.forEach(p -> ids.addAll(p.sourceTradeIds()));
        BigDecimal cost = entryValue.add(fee);
        return new PortfolioTradeRoundBO(id, parts.getFirst().symbol(), securityName, parts.getFirst().instrumentType(), episode.openedAt,
                effectiveClosedAt, entry, exit, divide(entryValue, entry), exit.signum() == 0 ? null : divide(exitValue, exit),
                gross, fee, net, divide(net, cost), effectiveClosedAt == null ? 0 : ChronoUnit.DAYS.between(episode.openedAt.toLocalDate(), effectiveClosedAt.toLocalDate()),
                status, unmatched, List.copyOf(ids));
    }

    private record PositionEpisode(OffsetDateTime openedAt, OffsetDateTime closedAt) {}

    private boolean matchesHoldingPeriod(PortfolioTradeRoundBO round, String value) {
        if (value == null || value.isBlank()) return true;
        return switch (value.trim().toLowerCase()) {
            case "short" -> round.holdingDays() <= 5;
            case "medium" -> round.holdingDays() > 5 && round.holdingDays() <= 30;
            case "long" -> round.holdingDays() > 30;
            default -> throw new IllegalArgumentException("holdingPeriod must be short, medium, or long");
        };
    }

    private PortfolioTradeAnalyticsBO summarize(List<PortfolioTradeRoundBO> rounds,
                                                  List<PortfolioTradeAnalyticsWarningBO> warnings,
                                                  LocalDate from, LocalDate to) {
        List<PortfolioTradeRoundBO> closed = rounds.stream().filter(r -> "CLOSED".equals(r.status()) || "PARTIAL".equals(r.status())).toList();
        List<PortfolioTradeRoundBO> wins = closed.stream().filter(r -> r.netPnl().signum() > 0).toList();
        List<PortfolioTradeRoundBO> losses = closed.stream().filter(r -> r.netPnl().signum() < 0).toList();
        List<PortfolioTradeRoundBO> flat = closed.stream().filter(r -> r.netPnl().signum() == 0).toList();
        BigDecimal grossWins = sum(wins, PortfolioTradeRoundBO::netPnl);
        BigDecimal grossLosses = sum(losses, r -> r.netPnl().abs());
        BigDecimal avgWin = divide(grossWins, BigDecimal.valueOf(wins.size()));
        BigDecimal avgLoss = divide(grossLosses, BigDecimal.valueOf(losses.size()));
        BigDecimal net = sum(closed, PortfolioTradeRoundBO::netPnl);
        BigDecimal fees = sum(rounds, PortfolioTradeRoundBO::totalFee);
        BigDecimal absGross = rounds.stream().map(r -> r.grossPnl().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PortfolioTradeEquityPointBO> equityCurve = equityCurve(closed);
        BigDecimal maxDrawdown = equityCurve.stream().map(PortfolioTradeEquityPointBO::drawdown).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO).abs();
        return new PortfolioTradeAnalyticsBO(from, to, closed.size(), (int) rounds.stream().filter(r -> "OPEN".equals(r.status())).count(),
                wins.size(), losses.size(), flat.size(), divide(BigDecimal.valueOf(wins.size()), BigDecimal.valueOf(closed.size())),
                avgWin, avgLoss, divide(avgWin, avgLoss), divide(grossWins, grossLosses),
                divide(net, BigDecimal.valueOf(closed.size())), net,
                closed.stream().map(PortfolioTradeRoundBO::netPnl).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
                losses.stream().map(PortfolioTradeRoundBO::netPnl).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
                streak(closed, true), streak(closed, false), averageDays(closed), medianDays(closed), divide(fees, absGross), maxDrawdown, rounds, equityCurve, warnings);
    }

    private List<PortfolioTradeEquityPointBO> equityCurve(List<PortfolioTradeRoundBO> closed) {
        List<PortfolioTradeRoundBO> ordered = closed.stream().filter(r -> r.closedAt() != null)
                .sorted(Comparator.comparing(PortfolioTradeRoundBO::closedAt)).toList();
        List<PortfolioTradeEquityPointBO> points = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        for (PortfolioTradeRoundBO round : ordered) {
            cumulative = cumulative.add(round.netPnl());
            peak = peak.max(cumulative);
            points.add(new PortfolioTradeEquityPointBO(round.closedAt().toLocalDate(), cumulative, cumulative.subtract(peak)));
        }
        return points;
    }

    private PortfolioTradeRoundBO round(long roundId, Lot lot, PortfolioTradeBO exit, BigDecimal quantity, BigDecimal gross,
                                        BigDecimal fee, String status, BigDecimal unmatchedQuantity) {
        boolean open = "OPEN".equals(status);
        BigDecimal net = open ? BigDecimal.ZERO : gross.subtract(fee);
        BigDecimal cost = lot.trade.price().multiply(quantity).add(allocate(lot.fee, quantity, lot.quantity));
        return new PortfolioTradeRoundBO(roundId, lot.trade.symbol(), lot.trade.securityName(), lot.trade.instrumentType(), lot.trade.tradedAt(),
                exit == null ? null : exit.tradedAt(), quantity, open ? BigDecimal.ZERO : quantity, lot.trade.price(),
                exit == null ? null : exit.price(), gross, fee, net, divide(net, cost),
                exit == null ? 0 : ChronoUnit.DAYS.between(lot.trade.tradedAt().toLocalDate(), exit.tradedAt().toLocalDate()),
                status, unmatchedQuantity,
                exit == null ? List.of(lot.trade.id()) : List.of(lot.trade.id(), exit.id()));
    }

    private String resultOf(PortfolioTradeRoundBO round) { return round.netPnl().signum() > 0 ? "WIN" : round.netPnl().signum() < 0 ? "LOSS" : "BREAKEVEN"; }
    private int streak(List<PortfolioTradeRoundBO> rounds, boolean positive) { int best = 0, current = 0; for (var r : rounds) { if ((r.netPnl().signum() > 0) == positive) current++; else current = 0; best = Math.max(best, current); } return best; }
    private BigDecimal averageDays(List<PortfolioTradeRoundBO> rounds) { return divide(rounds.stream().map(r -> BigDecimal.valueOf(r.holdingDays())).reduce(BigDecimal.ZERO, BigDecimal::add), BigDecimal.valueOf(rounds.size())); }
    private BigDecimal medianDays(List<PortfolioTradeRoundBO> rounds) { var values = rounds.stream().map(r -> r.holdingDays()).sorted().toList(); if (values.isEmpty()) return BigDecimal.ZERO; return BigDecimal.valueOf(values.get((values.size() - 1) / 2)); }
    private BigDecimal sum(List<PortfolioTradeRoundBO> values, java.util.function.Function<PortfolioTradeRoundBO, BigDecimal> mapper) { return values.stream().map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private BigDecimal allocate(BigDecimal value, BigDecimal matched, BigDecimal total) { return total.signum() == 0 ? BigDecimal.ZERO : value.multiply(matched).divide(total, SCALE, RoundingMode.HALF_UP); }
    private BigDecimal divide(BigDecimal value, BigDecimal denominator) { return denominator == null || denominator.signum() == 0 ? BigDecimal.ZERO : value.divide(denominator, SCALE, RoundingMode.HALF_UP); }
    private record Lot(PortfolioTradeBO trade, BigDecimal quantity, BigDecimal fee, long roundId, BigDecimal originalQuantity) {
        Lot(PortfolioTradeBO trade, BigDecimal quantity, BigDecimal fee, long roundId) { this(trade, quantity, fee, roundId, quantity); }
        Lot withRemaining(BigDecimal quantity, BigDecimal fee) { return new Lot(trade, quantity, fee, roundId, originalQuantity); }
    }
}
