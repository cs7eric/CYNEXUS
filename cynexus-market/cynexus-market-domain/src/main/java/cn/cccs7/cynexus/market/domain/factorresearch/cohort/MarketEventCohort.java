package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Frozen source facts used to create study samples. */
public record MarketEventCohort(MarketFactorResearchBO.EventType eventType, int definitionVersion,
                                LocalDate startDate, LocalDate endDate, OffsetDateTime dataCutoffAt,
                                List<Sample> samples, Map<String, Object> coverage, List<String> warnings) {
    public MarketEventCohort {
        samples = samples == null ? List.of() : List.copyOf(samples);
        coverage = coverage == null ? Map.of() : Map.copyOf(coverage);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public record Sample(String code, String market, LocalDate tradeDate, OffsetDateTime anchorAt,
                         String source, Map<String, Object> sourceFacts) {
        public Sample {
            if (code == null || code.isBlank() || tradeDate == null || anchorAt == null) {
                throw new IllegalArgumentException("cohort sample requires code, tradeDate and anchorAt");
            }
            sourceFacts = sourceFacts == null ? Map.of() : Map.copyOf(sourceFacts);
        }
    }
}
