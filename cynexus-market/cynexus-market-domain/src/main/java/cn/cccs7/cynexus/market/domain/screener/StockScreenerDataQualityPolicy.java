package cn.cccs7.cynexus.market.domain.screener;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Predicate;

/** Calculates explainable availability and freshness for a daily screener snapshot. */
public final class StockScreenerDataQualityPolicy {

    private static final BigDecimal READY_COVERAGE = new BigDecimal("95");
    private static final long STALE_AFTER_DAYS = 3;

    public StockScreenerQueryBO.DataQuality assess(StockScreenerQueryBO.Snapshot snapshot) {
        int eligible = Math.max(snapshot.eligibleStockCount(), 0);
        int rows = snapshot.rows().size();
        List<StockScreenerQueryBO.FieldCoverage> coverage = List.of(
                coverage("daily_close", snapshot.rows(), eligible, row -> row.price() != null),
                coverage("amount", snapshot.rows(), eligible, row -> row.amount() != null),
                coverage("momentum_30d", snapshot.rows(), eligible, row -> row.returnPct30d() != null),
                coverage("fund_flow", snapshot.rows(), eligible, row -> row.mainNet() != null),
                coverage("industry", snapshot.rows(), eligible, row -> row.thsIndustryCode() != null),
                coverage("turnover", snapshot.rows(), eligible, row -> row.turnoverPct() != null),
                coverage("volume_ratio", snapshot.rows(), eligible, row -> row.volumeRatio() != null),
                coverage("pe", snapshot.rows(), eligible, row -> row.pe() != null));
        long freshnessLagDays = Math.max(0, ChronoUnit.DAYS.between(snapshot.tradeDate(), LocalDate.now()));
        BigDecimal dailyCoverage = pct(rows, eligible);
        String status = freshnessLagDays > STALE_AFTER_DAYS ? "STALE"
                : dailyCoverage.compareTo(READY_COVERAGE) < 0 ? "PARTIAL" : "READY";
        List<String> warnings = coverage.stream().filter(item -> item.coveragePct().compareTo(READY_COVERAGE) < 0)
                .map(item -> item.field() + " coverage is " + item.coveragePct().stripTrailingZeros().toPlainString() + "% of the eligible universe.").toList();
        return new StockScreenerQueryBO.DataQuality(status, snapshot.tradeDate(), freshnessLagDays, eligible, rows, coverage, warnings);
    }

    private StockScreenerQueryBO.FieldCoverage coverage(String field, List<StockScreenerQueryBO.Row> rows, int eligible,
                                                         Predicate<StockScreenerQueryBO.Row> present) {
        int available = (int) rows.stream().filter(present).count();
        return new StockScreenerQueryBO.FieldCoverage(field, available, eligible, pct(available, eligible));
    }

    private BigDecimal pct(int available, int eligible) {
        if (eligible <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(available).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(eligible), 2, RoundingMode.HALF_UP);
    }
}
