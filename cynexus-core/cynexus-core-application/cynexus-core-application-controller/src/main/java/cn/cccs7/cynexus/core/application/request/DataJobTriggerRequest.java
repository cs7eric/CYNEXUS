package cn.cccs7.cynexus.core.application.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataJobTriggerRequest {

    private String triggerRequestId;
    private String triggeredBy;
    private Boolean dryRun;
    private Map<String, Object> params;
}
