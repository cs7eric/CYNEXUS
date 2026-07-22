package cn.cccs7.cynexus.ai.infra.basic.entity.agent;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_task_notification")
public class AgentTaskNotificationPO extends BasePO {
    @TableField("event_key") private String eventKey;
    @TableField("owner_key") private String ownerKey;
    @TableField("tenant_key") private String tenantKey;
    @TableField("principal_key") private String principalKey;
    @TableField("task_no") private String taskNo;
    @TableField("event_type") private String eventType;
    @TableField(value = "payload_json", typeHandler = JsonbTypeHandler.class) private String payloadJson;
    private String status;
    @TableField("attempt_count") private Integer attemptCount;
    @TableField("next_attempt_at") private OffsetDateTime nextAttemptAt;
    @TableField("delivered_at") private OffsetDateTime deliveredAt;
    @TableField("last_error") private String lastError;
}
