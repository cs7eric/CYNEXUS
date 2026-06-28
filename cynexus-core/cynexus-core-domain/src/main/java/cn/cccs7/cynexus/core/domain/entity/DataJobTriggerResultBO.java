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
public class DataJobTriggerResultBO {

    private Boolean accepted;
    private String runId;
    private String jobKey;
    private String status;
    private Map<String, Object> result;
}
