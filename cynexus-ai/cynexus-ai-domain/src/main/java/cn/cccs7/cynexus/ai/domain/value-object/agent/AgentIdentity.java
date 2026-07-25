package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
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
        return AgentIdentity.builder().principalKey("single-user").tenantKey(CommonConstants.DEFAULT_SPACE_CODE).build();
    }
}
