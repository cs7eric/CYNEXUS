package cn.cccs7.cynexus.ai.infra.basic.entity.agent;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_quota_policy")
public class AgentQuotaPolicyPO extends BasePO {
    @TableField("tenant_key") private String tenantKey;
    @TableField("principal_key") private String principalKey;
    @TableField("daily_request_limit") private Integer dailyRequestLimit;
    @TableField("daily_token_limit") private Long dailyTokenLimit;
    private Boolean enabled;
    @TableField("effective_at") private OffsetDateTime effectiveAt;
}
