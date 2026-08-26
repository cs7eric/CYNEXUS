package cn.cccs7.cynexus.market.domain.factorresearch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Point-in-time primitive snapshot port used only by deterministic study execution. */
public interface MarketFactorStudySnapshotRepository {
    /**
     * Returns one latest available pre-event snapshot per security.  The
     * implementation must never return a bar on or after {@code eventDate}.
     */
    List<Snapshot> findPreEventUniverse(LocalDate eventDate, OffsetDateTime dataCutoffAt, String featureVersion);
    default List<DailyPrimitivePoint> findDailyPrimitives(String code, String market, LocalDate eventDate, int sessions,
                                                          OffsetDateTime dataCutoffAt, String featureVersion) {
        return List.of();
    }
    default OutcomeSeries findOutcomeSeries(String code, String market, LocalDate eventDate, int sessions,
                                            OffsetDateTime dataCutoffAt, String featureVersion) { return null; }

    record Snapshot(String code, String market, LocalDate featureTradeDate, OffsetDateTime availableAt,
                    String industry, boolean st, BigDecimal floatMarketCap, BigDecimal amount,
                    String source, String coverage, String marketRegime, String industryRegime) {
        public Snapshot(String code, String market, LocalDate featureTradeDate, OffsetDateTime availableAt,
                        String industry, boolean st, BigDecimal floatMarketCap, BigDecimal amount,
                        String source, String coverage) {
            this(code, market, featureTradeDate, availableAt, industry, st, floatMarketCap, amount, source, coverage, null, null);
        }
        public Snapshot {
            if (code == null || code.isBlank() || market == null || market.isBlank()
                    || featureTradeDate == null || availableAt == null) {
                throw new IllegalArgumentException("materialized snapshot identity is incomplete");
            }
        }
    }

    record DailyPrimitivePoint(LocalDate tradeDate, OffsetDateTime availableAt, Map<String, BigDecimal> values,
                               String source, String coverage) {
        public DailyPrimitivePoint {
            values = values == null ? Map.of() : Map.copyOf(values);
        }
    }
    record OutcomeSeries(BigDecimal entryClose, BigDecimal benchmarkEntryClose,
                         List<cn.cccs7.cynexus.market.domain.factorresearch.outcome.MarketFactorOutcomeEvaluator.DailyClose> closes) {
        public OutcomeSeries { closes = closes == null ? List.of() : List.copyOf(closes); }
    }
}
