package cn.cccs7.cynexus.core.application.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerManagementTriggerRequest {

    @JsonProperty("target_date")
    private String targetDate;
    private Boolean dryRun;
}
