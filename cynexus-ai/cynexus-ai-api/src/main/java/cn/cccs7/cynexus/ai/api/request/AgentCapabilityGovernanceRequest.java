package cn.cccs7.cynexus.ai.api.request;

import lombok.Data;

@Data
public class AgentCapabilityGovernanceRequest {
    private Boolean enabled;
    private String availability;
    private Integer priority;
}
