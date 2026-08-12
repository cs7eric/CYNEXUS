package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class MarketIndicatorDTO {
    private MarketIndicatorDTO() { }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Snapshot {
        private String code; private String name; private String industry; private String market;
        private BigDecimal lastPrice; private BigDecimal prevClose; private BigDecimal openPrice;
        private BigDecimal highPrice; private BigDecimal lowPrice; private BigDecimal changePct;
        private BigDecimal amount; private BigDecimal volume; private BigDecimal turnoverRate;
        private BigDecimal limitUpPrice; private BigDecimal limitDownPrice;
        private String limitStatus; private String source; private OffsetDateTime capturedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Event {
        private Long id; private LocalDate tradeDate; private String code; private String name;
        private String market; private String eventType; private OffsetDateTime eventAt;
        private BigDecimal signalValue; private BigDecimal lastPrice; private BigDecimal changePct;
        private String limitStatus; private String source;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Breadth {
        private Integer totalCount; private Integer advancingCount; private Integer decliningCount; private Integer flatCount;
        private Integer limitUpCount; private Integer limitUpTouchCount; private Integer limitDownCount; private Integer limitDownTouchCount;
        private Integer rapidRiseCount; private Integer rapidFallCount; private OffsetDateTime capturedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Industry {
        private Long id; private LocalDate tradeDate; private OffsetDateTime capturedAt;
        private String industryCode; private String industryName; private Integer stockCount;
        private Integer advancingCount; private Integer decliningCount; private Integer flatCount;
        private Integer limitUpCount; private Integer limitUpTouchCount; private Integer limitDownCount; private Integer limitDownTouchCount;
        private Integer rapidRiseCount; private Integer rapidFallCount;
        private OffsetDateTime firstLimitUpAt; private String firstLimitUpCode; private String firstLimitUpName;
        private BigDecimal firstLimitUpChangePct; private OffsetDateTime firstEventAt; private String firstEventCode;
        private String firstEventName; private String firstEventType; private BigDecimal firstEventSignalValue; private String source;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IndustryRelation {
        private String industryCode; private String industryName; private String categoryLabel;
        private Integer overlapCount; private Integer sourceStockCount; private Integer targetStockCount;
        private Integer advancingCount; private Integer decliningCount; private Integer flatCount;
        private BigDecimal averageChangePct; private BigDecimal averageRelationLevel;
        private BigDecimal relationScore; private String relationState;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IndustryDetail {
        private String industryCode; private String industryName; private String categoryLabel;
        private Integer totalCount; private Integer page; private Integer pageSize;
        private Integer advancingCount; private Integer decliningCount; private Integer flatCount;
        private BigDecimal averageChangePct; private OffsetDateTime capturedAt;
        private List<Snapshot> constituents; private List<IndustryRelation> relations;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Overview {
        private Boolean ok; private String error; private LocalDate tradeDate;
        private OffsetDateTime capturedAt; private String source;
        private String scope; private String universeMode; private Boolean fullConfirmationRequired;
        private BigDecimal coverageRatio; private Integer observedCount; private Integer eligibleCount;
        private Long freshnessMs; private String algorithmVersion; private Integer snapshotCount;
        private Breadth breadth; private List<Snapshot> snapshots; private List<Snapshot> limitUp;
        private List<Snapshot> limitDown; private List<Industry> industries; private List<Event> events;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReplayTimeline {
        private Boolean ok;
        private String error;
        private LocalDate tradeDate;
        private LocalDate dataTradeDate;
        private String dataStatus;
        private String source;
        private OffsetDateTime requestedFrom;
        private OffsetDateTime requestedTo;
        private Integer indexPointCount;
        private Integer eventCount;
        private Integer industryPointCount;
        private List<ReplayIndexPoint> indexPoints;
        private List<Event> events;
        private List<ReplayIndustryPoint> industryPoints;
        private String message;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReplayIndexPoint {
        private String time;
        private Long timestamp;
        private BigDecimal close;
        private BigDecimal changePct;
        private BigDecimal turnover;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReplayIndustryPoint {
        private OffsetDateTime capturedAt;
        private String industryCode;
        private String industryName;
        private Integer stockCount;
        private Integer advancingCount;
        private Integer decliningCount;
        private Integer limitUpCount;
        private Integer limitUpTouchCount;
        private Integer limitDownCount;
        private Integer limitDownTouchCount;
        private Integer rapidRiseCount;
        private Integer rapidFallCount;
        private OffsetDateTime firstLimitUpAt;
        private String firstLimitUpCode;
        private String firstLimitUpName;
        private OffsetDateTime firstEventAt;
        private String firstEventCode;
        private String firstEventName;
        private String firstEventType;
    }
}
