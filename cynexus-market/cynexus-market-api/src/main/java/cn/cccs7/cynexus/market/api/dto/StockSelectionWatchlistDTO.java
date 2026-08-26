package cn.cccs7.cynexus.market.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StockSelectionWatchlistDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Item> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceResponse {
        private List<Performance> items;
        private String dataSource;
        private List<String> warnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Performance {
        private long watchlistItemId;
        private java.math.BigDecimal latestClose;
        private String latestTradeDate;
        private java.math.BigDecimal returnSinceEntryPct;
        private java.math.BigDecimal benchmarkReturnPct;
        private java.math.BigDecimal excessReturnPct;
        private String dataSource;
        private List<String> warnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceHistoryResponse {
        private long watchlistItemId;
        private String entryTradeDate;
        private java.math.BigDecimal entryPrice;
        private List<PerformancePoint> points;
        private String dataSource;
        private List<String> warnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformancePoint {
        private String tradeDate;
        private java.math.BigDecimal close;
        private java.math.BigDecimal returnSinceEntryPct;
        private java.math.BigDecimal benchmarkReturnPct;
        private java.math.BigDecimal excessReturnPct;
        private List<String> warnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private long id;
        private long watchlistId;
        private String watchlistName;
        private String stockCode;
        private String stockName;
        private Long sourceStrategyCandidateId;
        private String status;
        private String attentionReason;
        private String invalidationCondition;
        private String nextReviewDate;
        private long version;
        private String updatedAt;
        private String market;
        private String entryTradeDate;
        private java.math.BigDecimal entryPrice;
        private List<Review> reviews;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddRequest {
        private long candidateId;
        private String watchlistName;
        private String attentionReason;
        private String invalidationCondition;
        private String nextReviewDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewRequest {
        private String decision;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        private long id;
        private String decision;
        private String note;
        private String reviewedAt;
    }
}
