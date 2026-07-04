package cn.cccs7.cynexus.core.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerManagementJobsResponseDTO {

    private Boolean ok;
    private List<Item> items;
    private Integer count;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String id;
        private String name;
        private String description;
        @com.fasterxml.jackson.annotation.JsonProperty("config_file")
        private String configFile;
        @com.fasterxml.jackson.annotation.JsonProperty("service_module")
        private String serviceModule;
        @com.fasterxml.jackson.annotation.JsonProperty("service_class")
        private String serviceClass;
        @com.fasterxml.jackson.annotation.JsonProperty("registered_at")
        private OffsetDateTime registeredAt;
        private List<String> categories;
        private java.util.Map<String, Integer> categorySortOrders;
        @com.fasterxml.jackson.annotation.JsonProperty("supports_enable")
        private Boolean supportsEnable;
        private Boolean enabled;
        @com.fasterxml.jackson.annotation.JsonProperty("config_enabled")
        private Boolean configEnabled;
        private Config config;
        private Live live;
        private Progress progress;
        @com.fasterxml.jackson.annotation.JsonProperty("last_run")
        private LastRun lastRun;
        @com.fasterxml.jackson.annotation.JsonProperty("today_stats")
        private TodayStats todayStats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private Boolean enabled;
        private String schedule;
        private Integer tickSeconds;
        private Integer timezoneOffsetHours;
        private OffsetDateTime lastRunAt;
        private String lastStatus;
        private String lastError;
        private BigDecimal lastDurationSeconds;
        private Integer lastTargetsProcessed;
        private Integer totalRuns;
        private String lastStep;
        private List<String> lastCompletedSteps;
        private List<ChainStepResult> lastStepResults;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Live {
        private Boolean running;
        private OffsetDateTime schedulerStartedAt;
        private OffsetDateTime stoppedAt;
        private String lastMessage;
        @com.fasterxml.jackson.annotation.JsonProperty("last_run")
        private LastRun lastRun;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastRun {
        @com.fasterxml.jackson.annotation.JsonProperty("last_run_at")
        private OffsetDateTime lastRunAt;
        @com.fasterxml.jackson.annotation.JsonProperty("last_status")
        private String lastStatus;
        @com.fasterxml.jackson.annotation.JsonProperty("last_targets_processed")
        private Integer lastTargetsProcessed;
        @com.fasterxml.jackson.annotation.JsonProperty("last_duration_seconds")
        private BigDecimal lastDurationSeconds;
        @com.fasterxml.jackson.annotation.JsonProperty("last_error")
        private String lastError;
        @com.fasterxml.jackson.annotation.JsonProperty("total_runs")
        private Integer totalRuns;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayStats {
        @com.fasterxml.jackson.annotation.JsonProperty("expected_count")
        private Integer expectedCount;
        @com.fasterxml.jackson.annotation.JsonProperty("actual_count")
        private Integer actualCount;
        @com.fasterxml.jackson.annotation.JsonProperty("success_count")
        private Integer successCount;
        @com.fasterxml.jackson.annotation.JsonProperty("failed_count")
        private Integer failedCount;
        @com.fasterxml.jackson.annotation.JsonProperty("skipped_count")
        private Integer skippedCount;
        @com.fasterxml.jackson.annotation.JsonProperty("manual_count")
        private Integer manualCount;
        @com.fasterxml.jackson.annotation.JsonProperty("auto_count")
        private Integer autoCount;
        @com.fasterxml.jackson.annotation.JsonProperty("failed_at")
        private OffsetDateTime failedAt;
        @com.fasterxml.jackson.annotation.JsonProperty("failed_error")
        private String failedError;
        @com.fasterxml.jackson.annotation.JsonProperty("failed_message")
        private String failedMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChainStepResult {
        private String jobId;
        private Boolean ok;
        private String lastStatus;
        private String lastRunError;
        private String lastMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Progress {
        private Integer schemaVersion;
        private String type;
        private String status;
        private BigDecimal percent;
        private String currentStep;
        private Map<String, Object> currentStepProgress;
        private List<String> completedSteps;
        private List<ChainStepResult> stepResults;
        private String message;
        private Map<String, Object> metrics;
        private Map<String, Object> payload;
        private OffsetDateTime updatedAt;
    }
}
