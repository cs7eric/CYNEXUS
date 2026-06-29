package cn.cccs7.cynexus.core.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerManagementJobActionResponseDTO {

    private Boolean ok;
    @JsonProperty("job_id")
    private String jobId;
    private Boolean enabled;
    private String status;
    @JsonProperty("run_id")
    private String runId;
    private Boolean accepted;
    private Integer count;
    @JsonProperty("failed_count")
    private Integer failedCount;
    private List<TriggerItem> items;
    private SchedulerManagementJobsResponseDTO.Item job;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerItem {
        private String jobId;
        private Boolean ok;
        private String lastStatus;
        private String lastRunError;
        private String lastMessage;
    }
}
