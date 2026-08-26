package cn.cccs7.cynexus.market.api.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StockSelectionStrategyDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CatalogResponse {
        private List<Strategy> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Strategy {
        private String code;
        private String name;
        private String description;
        private String type;
        private Version activeVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Version {
        private long id;
        private int versionNo;
        private String parameterSchema;
        private String evidenceSchema;
        private String changeSummary;
        private String effectiveFrom;
        private long definitionVersion;
        private Long sourceFactorStudyId;
        private Long sourceFactorSetVersionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveRequest {
        private String name;
        private String description;
        private Map<String, Object> criteria;
        private String changeSummary;
        private Long expectedDefinitionVersion;
        private Long sourceFactorStudyId;
        private Long sourceFactorSetVersionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunResponse {
        private long id;
        private String strategyCode;
        private long strategyVersionId;
        private int strategyVersionNo;
        private String requestedTradeDate;
        private String actualTradeDate;
        private String status;
        private int universeCount;
        private int matchedCount;
        private String source;
        private List<String> warnings;
        private List<Candidate> candidates;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        private long id;
        private String stockCode;
        private String stockName;
        private int rank;
        private java.math.BigDecimal score;
        private String reasonSummary;
        private Map<String, Object> snapshot;
        private List<Evidence> evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Evidence {
        private String code;
        private String label;
        private String operator;
        private Object expectedValue;
        private Object actualValue;
        private boolean matched;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestRequest {
        private String asOfDate;
        private Integer lookbackSessions;
        private Integer signalIntervalSessions;
        private Integer holdingSessions;
        private Integer maxCandidatesPerSignal;
        private Integer transactionCostBpsPerSide;
        private String stockCode;
        private String stockMarket;
        private List<TargetSymbol> targetSymbols;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetSymbol {
        private String market;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestResponse {
        private long id;
        private String strategyCode;
        private long strategyVersionId;
        private int strategyVersionNo;
        private String requestedAsOfDate;
        private String actualAsOfDate;
        private String status;
        private BacktestConfig config;
        private String benchmark;
        private String dataSource;
        private List<String> warnings;
        private BacktestSummary summary;
        private String startedAt;
        private String completedAt;
        private List<BacktestTrade> trades;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestConfig {
        private String requestedAsOfDate;
        private int lookbackSessions;
        private int signalIntervalSessions;
        private int holdingSessions;
        private int maxCandidatesPerSignal;
        private int transactionCostBpsPerSide;
        private String stockCode;
        private String stockMarket;
        private List<TargetSymbol> targetSymbols;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestSummary {
        private int signalCount;
        private int evaluatedTradeCount;
        private int skippedTradeCount;
        private java.math.BigDecimal winRatePct;
        private java.math.BigDecimal averageNetReturnPct;
        private java.math.BigDecimal medianNetReturnPct;
        private java.math.BigDecimal benchmarkAverageReturnPct;
        private java.math.BigDecimal averageExcessReturnPct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestTrade {
        private long id;
        private String signalDate;
        private String entryDate;
        private String exitDate;
        private String market;
        private String stockCode;
        private String stockName;
        private int signalRank;
        private java.math.BigDecimal score;
        private java.math.BigDecimal entryPrice;
        private java.math.BigDecimal exitPrice;
        private java.math.BigDecimal grossReturnPct;
        private java.math.BigDecimal netReturnPct;
        private java.math.BigDecimal benchmarkReturnPct;
        private java.math.BigDecimal excessReturnPct;
        private String outcome;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthResponse {
        private String strategyCode;
        private int strategyVersionNo;
        private String requestedTradeDate;
        private String actualTradeDate;
        private String status;
        private String reason;
        private String recommendedAction;
        private int universeCount;
        private int candidateCount;
        private java.math.BigDecimal signalDensityPct;
        private long freshnessLagDays;
        private RecentPerformance recentPerformance;
        private String source;
        private List<HealthCheck> checks;
        private List<String> warnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthCheck {
        private String code;
        private String label;
        private String status;
        private String actualValue;
        private String threshold;
        private String detail;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecentPerformance {
        private int evaluatedTradeCount;
        private java.math.BigDecimal winRatePct;
        private java.math.BigDecimal averageExcessReturnPct;
        private String asOfDate;
    }
}
