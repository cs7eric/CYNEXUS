package cn.cccs7.cynexus.market.domain.factorresearch.sequence;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Aligns raw pre-event points and normalizes them without inspecting event or post-event values. */
public final class EventAlignedSequencePolicy {
    private final MarketFactorResearchPolicy leakagePolicy = new MarketFactorResearchPolicy();

    public Sequence daily(String sequenceCode, LocalDate eventTradeDate, List<DailyPoint> rawPoints) {
        return daily(sequenceCode, eventTradeDate, null, rawPoints);
    }

    public Sequence daily(String sequenceCode, LocalDate eventTradeDate, OffsetDateTime dataCutoffAt, List<DailyPoint> rawPoints) {
        if (sequenceCode == null || sequenceCode.isBlank() || eventTradeDate == null || rawPoints == null || rawPoints.isEmpty()) {
            throw new IllegalArgumentException("sequence code, event date and pre-event points are required");
        }
        List<DailyPoint> points = rawPoints.stream().sorted(Comparator.comparing(DailyPoint::tradeDate)).toList();
        for (DailyPoint point : points) {
            leakagePolicy.requireNoDailyLeakage(point.tradeDate(), eventTradeDate);
            if (point.availableAt() == null || point.availableAt().toLocalDate().isBefore(point.tradeDate())) {
                throw new IllegalStateException("daily primitive availability is invalid");
            }
            if (dataCutoffAt != null && point.availableAt().isAfter(dataCutoffAt)) {
                throw new IllegalStateException("daily primitive became available after study data cutoff");
            }
        }
        BigDecimal median = median(points.stream().map(DailyPoint::value).toList());
        BigDecimal mad = median(points.stream().map(point -> point.value().subtract(median).abs()).toList());
        BigDecimal denominator = mad.signum() == 0 ? BigDecimal.ONE : mad;
        List<Point> aligned = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            DailyPoint value = points.get(index);
            aligned.add(new Point(index - points.size(), value.tradeDate(), value.availableAt(), value.value(),
                    value.value().subtract(median).divide(denominator, 8, RoundingMode.HALF_UP)));
        }
        return new Sequence(sequenceCode, "DAILY", median, mad, aligned);
    }

    private BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2));
    }

    public record DailyPoint(LocalDate tradeDate, OffsetDateTime availableAt, BigDecimal value) {
        public DailyPoint { if (tradeDate == null || availableAt == null || value == null) throw new IllegalArgumentException("daily point is incomplete"); }
    }
    public record Point(int offset, LocalDate observedAt, OffsetDateTime availableAt, BigDecimal rawValue, BigDecimal normalizedValue) { }
    public record Sequence(String code, String frequency, BigDecimal median, BigDecimal medianAbsoluteDeviation, List<Point> points) {
        public Sequence { points = List.copyOf(points); }
    }
}
