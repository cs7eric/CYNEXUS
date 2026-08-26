package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketEventFactorObservatoryDTO {
    private Boolean ok;
    private LocalDate requestedTradeDate;
    private LocalDate tradeDate;
    private String cohort;
    private BigDecimal minLargeMovePct;
    private Integer populationCount;
    private Integer sampledCount;
    private List<FactorFamily> marketFactors;
    private List<Event> events;
    /** Compatibility-only preset hints. New research must use validated factor-set versions. */
    private Boolean screeningHintsDeprecated;
    @Deprecated(since = "1.1.400", forRemoval = false)
    private List<ScreeningHint> screeningHints;
    private List<String> warnings;
    private String error;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Event {
        private String code;
        private String name;
        private String eventType;
        private BigDecimal changePct;
        private Integer streak;
        private String thsIndustryCode;
        private String thsIndustryName;
        private String tdxIndustryCode;
        private String tdxIndustryName;
        private List<String> concepts;
        private List<String> styles;
        private List<FactorFamily> factors;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FactorFamily {
        private String stage;
        private String code;
        private String label;
        private String status;
        private LocalDate asOfDate;
        private String source;
        private List<Metric> metrics;
        private List<String> supportingEvidence;
        private List<String> counterEvidence;
        private List<String> warnings;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Metric {
        private String key;
        private String label;
        private String value;
        private String unit;
    }

    @Deprecated(since = "1.1.400", forRemoval = false)
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScreeningHint {
        private String criterion;
        private String operator;
        private String value;
        private String label;
        private String basis;
    }
}
