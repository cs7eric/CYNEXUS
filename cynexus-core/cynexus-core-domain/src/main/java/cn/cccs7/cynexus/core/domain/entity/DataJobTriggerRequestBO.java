package cn.cccs7.cynexus.core.domain.entity;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataJobTriggerRequestBO {

    private String triggerRequestId;
    private String triggeredBy;
    private Boolean dryRun;
    private Map<String, Object> params;
}
