package cn.cccs7.cynexus.market.domain.factorresearch.outcome;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Computes post-event outcomes only after discovery/control matching has completed. */
public final class MarketFactorOutcomeEvaluator {
    public List<Outcome> evaluate(BigDecimal entryClose, BigDecimal benchmarkEntryClose, List<DailyClose> postEventCloses,
                                  List<Integer> horizons) {
        if (entryClose == null || entryClose.signum() <= 0 || benchmarkEntryClose == null || benchmarkEntryClose.signum() <= 0) {
            throw new IllegalArgumentException("positive entry closes are required");
        }
        if (horizons == null || horizons.isEmpty()) throw new IllegalArgumentException("outcome horizons are required");
        List<DailyClose> ordered = (postEventCloses == null ? List.<DailyClose>of() : postEventCloses).stream()
                .sorted(Comparator.comparing(DailyClose::tradeDate)).toList();
        List<Outcome> results = new ArrayList<>();
        for (Integer horizon : horizons.stream().distinct().sorted().toList()) {
            if (horizon == null || horizon < 1 || horizon > 252) throw new IllegalArgumentException("outcome horizon must be 1..252");
            if (ordered.size() < horizon) { results.add(new Outcome(horizon, null, null, null, null, null, "UNAVAILABLE")); continue; }
            List<DailyClose> window = ordered.subList(0, horizon);
            DailyClose terminal = window.getLast();
            BigDecimal stockReturn = pct(terminal.close(), entryClose);
            BigDecimal benchmarkReturn = pct(terminal.benchmarkClose(), benchmarkEntryClose);
            BigDecimal maximumFavorable = window.stream().map(point -> pct(point.close(), entryClose)).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            BigDecimal maximumAdverse = window.stream().map(point -> pct(point.close(), entryClose)).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            results.add(new Outcome(horizon, stockReturn, benchmarkReturn, stockReturn.subtract(benchmarkReturn), maximumFavorable, maximumAdverse, "AVAILABLE"));
        }
        return List.copyOf(results);
    }

    private BigDecimal pct(BigDecimal value, BigDecimal base) { return value.subtract(base).multiply(BigDecimal.valueOf(100)).divide(base, 6, RoundingMode.HALF_UP); }
    public record DailyClose(LocalDate tradeDate, BigDecimal close, BigDecimal benchmarkClose) {
        public DailyClose { if (tradeDate == null || close == null || benchmarkClose == null || close.signum() <= 0 || benchmarkClose.signum() <= 0) throw new IllegalArgumentException("daily outcome close is invalid"); }
    }
    public record Outcome(int horizon, BigDecimal stockReturnPct, BigDecimal benchmarkReturnPct, BigDecimal excessReturnPct,
                          BigDecimal maximumFavorablePct, BigDecimal maximumAdversePct, String status) { }
}
