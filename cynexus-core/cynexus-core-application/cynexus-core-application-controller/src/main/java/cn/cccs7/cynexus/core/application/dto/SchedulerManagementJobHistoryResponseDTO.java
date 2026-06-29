package cn.cccs7.cynexus.core.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerManagementJobHistoryResponseDTO {

    private Boolean ok;
    @JsonProperty("job_id")
    private String jobId;
    private List<Item> items;
    private Integer count;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String id;
        @JsonProperty("start_at")
        private OffsetDateTime startAt;
        @JsonProperty("end_at")
        private OffsetDateTime endAt;
        @JsonProperty("trigger_type")
        private String triggerType;
        private String status;
        private String error;
        private String message;
        @JsonProperty("duration_seconds")
        private BigDecimal durationSeconds;
        @JsonProperty("target_count")
        private Integer targetCount;
        private Integer succeeded;
    }
}
