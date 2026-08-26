package cn.cccs7.cynexus.market.domain.entity.factor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A bounded, factual event-study result. Factor families are intentionally
 * separate so a correlation is never presented as a single causal score.
 */
public final class MarketEventFactorObservatoryBO {

    private MarketEventFactorObservatoryBO() {
    }

    public record Query(String tradeDate, String cohort, BigDecimal minLargeMovePct, Integer limit, String symbol) {
    }

    public record Result(boolean ok, LocalDate requestedTradeDate, LocalDate tradeDate, String cohort,
                         BigDecimal minLargeMovePct, int populationCount, int sampledCount,
                         List<FactorFamily> marketFactors, List<Event> events,
                         List<ScreeningHint> screeningHints, List<String> warnings, String error) {
        public Result {
            marketFactors = marketFactors == null ? List.of() : List.copyOf(marketFactors);
            events = events == null ? List.of() : List.copyOf(events);
            screeningHints = screeningHints == null ? List.of() : List.copyOf(screeningHints);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    public record Event(String code, String name, String eventType, BigDecimal changePct, Integer streak,
                        String thsIndustryCode, String thsIndustryName, String tdxIndustryCode, String tdxIndustryName,
                        List<String> concepts, List<String> styles, List<FactorFamily> factors) {
        public Event {
            concepts = concepts == null ? List.of() : List.copyOf(concepts);
            styles = styles == null ? List.of() : List.copyOf(styles);
            factors = factors == null ? List.of() : List.copyOf(factors);
        }
    }

    public record FactorFamily(String stage, String code, String label, String status, LocalDate asOfDate,
                               String source, List<Metric> metrics, List<String> supportingEvidence,
                               List<String> counterEvidence, List<String> warnings) {
        public FactorFamily {
            metrics = metrics == null ? List.of() : List.copyOf(metrics);
            supportingEvidence = supportingEvidence == null ? List.of() : List.copyOf(supportingEvidence);
            counterEvidence = counterEvidence == null ? List.of() : List.copyOf(counterEvidence);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    public record Metric(String key, String label, String value, String unit) {
    }

    /** Only approved, existing screener fields may be handed back to the UI. */
    public record ScreeningHint(String criterion, String operator, String value, String label, String basis) {
    }
}
