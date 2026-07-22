package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_capability_audit")
public class AgentCapabilityAuditPO extends BasePO {
    @TableField("capability_code") private String capabilityCode;
    private String version;
    private String action;
    private Boolean enabled;
    private String availability;
    @TableField("governance_availability") private String governanceAvailability;
    private Integer priority;
    private String reason;
    @TableField("occurred_at") private OffsetDateTime occurredAt;
}
