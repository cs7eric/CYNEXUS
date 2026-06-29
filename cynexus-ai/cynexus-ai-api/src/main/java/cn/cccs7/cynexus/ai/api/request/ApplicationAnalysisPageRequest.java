package cn.cccs7.cynexus.ai.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ApplicationAnalysisPageRequest {

    private ApplicationAnalysisPageRequest() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveTargetsRequest {
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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerRequest {
        @JsonProperty("target_id")
        private String targetId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunRequest {
        @JsonProperty("target_type")
        private String targetType;
        private String symbol;
        private String name;
        private String adjust;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recent30RefreshRequest {
        @JsonProperty("target_id")
        private String targetId;
        private String date;
    }
}
