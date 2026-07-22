package cn.cccs7.cynexus.ai.domain.agent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentIdentity {

    String principalKey;
    String tenantKey;

    public String storageOwnerKey() {
        return tenantKey + "::" + principalKey;
    }

    public static AgentIdentity singleUser() {
        return AgentIdentity.builder().principalKey("single-user").tenantKey("default").build();
    }
}
