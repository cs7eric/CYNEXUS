package cn.cccs7.cynexus.ai.domain.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ApplicationAnalysisPageBO {

    private ApplicationAnalysisPageBO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private Long id;
        private Integer horizonDays;
        private Integer horizonSegments;
        private Integer monthlyKeep;
        private Integer weeklyKeep;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Target {
        private Long id;
        private String targetKey;
        private String legacyKey;
        private String targetType;
        private String symbol;
        private String market;
        private String name;
        private String adjust;
        private Boolean enabled;
        private Integer intervalMinutes;
        private Integer sortOrder;
        private String tagsJson;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentResult {
        private String targetKey;
        private String payloadJson;
        private OffsetDateTime analysisRunAt;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryResult {
        private String targetKey;
        private String historyKey;
        private String payloadJson;
        private OffsetDateTime analysisRunAt;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySnapshot {
        private Long targetId;
        private String targetKey;
        private LocalDate tradeDate;
        private String payloadJson;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentResultUpsertCommand {
        private Long targetId;
        private String targetKey;
        private String status;
        private OffsetDateTime analysisRunAt;
        private String payloadJson;
        private String remark;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryResultCreateCommand {
        private Long targetId;
        private String targetKey;
        private String historyKey;
        private String sourceKind;
        private OffsetDateTime analysisRunAt;
        private String payloadJson;
        private String remark;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySnapshotUpsertCommand {
        private Long targetId;
        private String targetKey;
        private LocalDate tradeDate;
        private String snapshotKind;
        private String payloadJson;
        private String remark;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveTargetsCommand {
        private Integer horizonDays;
        private Integer horizonSegments;
        private Integer monthlyKeep;
        private Integer weeklyKeep;
        private List<Target> targets;
    }
}
