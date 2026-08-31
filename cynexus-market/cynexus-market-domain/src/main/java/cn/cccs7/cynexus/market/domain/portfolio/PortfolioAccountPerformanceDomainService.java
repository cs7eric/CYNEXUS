package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PortfolioAccountPerformanceDomainService {
    private static final int SCALE = 8;

    public PortfolioAccountPerformanceBO calculate(List<PortfolioAccountSnapshotBO> source,
                                                   List<PortfolioCashFlowBO> flows) {
        List<PortfolioAccountSnapshotBO> snapshots = source == null ? List.of() : source.stream()
                .filter(java.util.Objects::nonNull).sorted(Comparator.comparing(PortfolioAccountSnapshotBO::snapshotDate)).toList();
        List<PortfolioCashFlowBO> cashFlows = flows == null ? List.of() : flows.stream().filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(PortfolioCashFlowBO::occurredAt)).toList();
        List<String> warnings = new ArrayList<>();
        if (snapshots.size() < 2) warnings.add("INSUFFICIENT_SNAPSHOTS");
        BigDecimal twr = BigDecimal.ZERO;
        if (snapshots.size() >= 2) {
            BigDecimal growth = BigDecimal.ONE;
            for (int i = 1; i < snapshots.size(); i++) {
                PortfolioAccountSnapshotBO previous = snapshots.get(i - 1), current = snapshots.get(i);
                BigDecimal flow = cashFlows.stream().filter(f -> !f.occurredAt().toLocalDate().isBefore(previous.snapshotDate())
                                && !f.occurredAt().toLocalDate().isAfter(current.snapshotDate()))
                        .map(f -> "DEPOSIT".equals(f.flowType()) ? f.amount() : f.amount().negate())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (previous.totalAssets().signum() == 0) { warnings.add("ZERO_BEGINNING_ASSETS"); continue; }
                growth = growth.multiply(current.totalAssets().subtract(flow).divide(previous.totalAssets(), SCALE, RoundingMode.HALF_UP));
            }
            twr = growth.subtract(BigDecimal.ONE);
        }
        BigDecimal mwr = calculateMwr(snapshots, cashFlows, warnings);
        BigDecimal peak = BigDecimal.ZERO, maxDd = BigDecimal.ZERO, maxRate = BigDecimal.ZERO;
        for (PortfolioAccountSnapshotBO snapshot : snapshots) {
            peak = peak.max(snapshot.totalAssets());
            BigDecimal dd = peak.subtract(snapshot.totalAssets());
            maxDd = maxDd.max(dd);
            if (peak.signum() > 0) maxRate = maxRate.max(dd.divide(peak, SCALE, RoundingMode.HALF_UP));
        }
        return new PortfolioAccountPerformanceBO(twr, mwr, maxDd, maxRate, List.copyOf(warnings));
    }

    private BigDecimal calculateMwr(List<PortfolioAccountSnapshotBO> snapshots, List<PortfolioCashFlowBO> flows,
                                    List<String> warnings) {
        if (snapshots.size() < 2 || flows.isEmpty()) { warnings.add("INSUFFICIENT_CASH_FLOWS"); return BigDecimal.ZERO; }
        BigDecimal low = BigDecimal.valueOf(-.9999), high = BigDecimal.valueOf(10);
        LocalDate start = snapshots.get(0).snapshotDate(), end = snapshots.get(snapshots.size() - 1).snapshotDate();
        for (int i = 0; i < 80; i++) {
            BigDecimal mid = low.add(high).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
            BigDecimal npv = snapshots.get(0).totalAssets().negate();
            for (PortfolioCashFlowBO flow : flows) {
                double years = java.time.temporal.ChronoUnit.DAYS.between(start, flow.occurredAt().toLocalDate()) / 365.0;
                BigDecimal factor = BigDecimal.valueOf(Math.pow(1 + mid.doubleValue(), years));
                npv = npv.add(("DEPOSIT".equals(flow.flowType()) ? flow.amount().negate() : flow.amount()).divide(factor, SCALE, RoundingMode.HALF_UP));
            }
            double years = java.time.temporal.ChronoUnit.DAYS.between(start, end) / 365.0;
            npv = npv.add(snapshots.get(snapshots.size() - 1).totalAssets().divide(BigDecimal.valueOf(Math.pow(1 + mid.doubleValue(), years)), SCALE, RoundingMode.HALF_UP));
            if (npv.signum() > 0) low = mid; else high = mid;
        }
        return low.add(high).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
    }
}
