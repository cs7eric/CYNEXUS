package cn.cccs7.cynexus.ai.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ApplicationAnalysisPageDTO {

    private ApplicationAnalysisPageDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetsResponse {
        private List<Target> items;
        private ConfigPayload config;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigPayload {
        private Integer version;
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
        private Horizon horizon;
        private List<Target> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Horizon {
        private Integer days;
        private Integer segments;
        @JsonProperty("monthly_keep")
        private Integer monthlyKeep;
        @JsonProperty("weekly_keep")
        private Integer weeklyKeep;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Target {
        private String id;
        @JsonProperty("target_type")
        private String targetType;
        private String symbol;
        private String market;
        private String name;
        private String adjust;
        private Boolean enabled;
        @JsonProperty("interval_minutes")
        private Integer intervalMinutes;
        private List<String> tags;
        @JsonProperty("last_updated_at")
        private OffsetDateTime lastUpdatedAt;
        @JsonProperty("last_result_path")
        private String lastResultPath;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResultResponse {
        @JsonRawValue
        @JsonProperty("analysis_input")
        private String analysisInputJson;
        @JsonRawValue
        @JsonProperty("analysis_result")
        private String analysisResultJson;
        @JsonRawValue
        @JsonProperty("raw_result")
        private String rawResultJson;
        @JsonProperty("_meta_result_path")
        private String metaResultPath;
        @JsonProperty("_meta_history")
        private List<ResultFile> metaHistory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultFile {
        private String filename;
        private String path;
        @JsonProperty("size_bytes")
        private Integer sizeBytes;
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionResponse {
        private Boolean ok;
        @JsonProperty("target_id")
        private String targetId;
        private String error;
        private List<ActionItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private String id;
        private Boolean ok;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulerStatus {
        private Boolean running;
        @JsonProperty("started_at")
        private OffsetDateTime startedAt;
        @JsonProperty("tick_count")
        private Integer tickCount;
        @JsonProperty("runs_count")
        private Integer runsCount;
        @JsonProperty("enabled_target_count")
        private Integer enabledTargetCount;
        @JsonProperty("total_target_count")
        private Integer totalTargetCount;
        @JsonRawValue
        private String inflight;
        @JsonRawValue
        @JsonProperty("last_run")
        private String lastRun;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulerControlResponse {
        private Boolean ok;
        private SchedulerStatus status;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySnapshotResponse {
        private Boolean ok;
        private String error;
        @JsonProperty("target_id")
        private String targetId;
        private String date;
        @JsonProperty("snapshot_path")
        private String snapshotPath;
        @JsonRawValue
        @JsonProperty("short_term_trend")
        private String shortTermTrendJson;
        @JsonRawValue
        @JsonProperty("current_situation")
        private String currentSituationJson;
        @JsonProperty("snapshots")
        private List<DailySnapshotFile> snapshots;
        private DailySnapshotDetail snapshot;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recent30FullResponse {
        private Boolean ok;
        @JsonProperty("target_id")
        private String targetId;
        private List<Recent30FullItem> items;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recent30FullItem {
        private String filename;
        private String path;
        private String date;
        @JsonProperty("size_bytes")
        private Integer sizeBytes;
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
        private DailySnapshotDetail snapshot;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySnapshotFile {
        private String filename;
        private String path;
        private String date;
        @JsonProperty("size_bytes")
        private Integer sizeBytes;
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySnapshotDetail {
        private Target target;
        private String date;
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
        @JsonRawValue
        @JsonProperty("short_term_trend")
        private String shortTermTrendJson;
        @JsonRawValue
        @JsonProperty("current_situation")
        private String currentSituationJson;
        @JsonRawValue
        @JsonProperty("summary")
        private String summaryJson;
    }
}
