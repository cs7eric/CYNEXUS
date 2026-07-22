package cn.cccs7.cynexus.ai.infra.basic.entity.agent;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_task")
public class AgentTaskPO extends BasePO {
    @TableField("task_no") private String taskNo;
    @TableField("owner_key") private String ownerKey;
    @TableField("tenant_key") private String tenantKey;
    @TableField("principal_key") private String principalKey;
    @TableField("conversation_no") private String conversationNo;
    @TableField("user_input") private String userInput;
    @TableField("preferred_model_code") private String preferredModelCode;
    @TableField("schedule_at") private OffsetDateTime scheduleAt;
    private String status;
    @TableField("attempt_count") private Integer attemptCount;
    @TableField("max_attempts") private Integer maxAttempts;
    @TableField("lease_owner") private String leaseOwner;
    @TableField("lease_until") private OffsetDateTime leaseUntil;
    @TableField("next_attempt_at") private OffsetDateTime nextAttemptAt;
    @TableField("run_no") private String runNo;
    @TableField("last_error") private String lastError;
    @TableField("paused_at") private OffsetDateTime pausedAt;
    @TableField("completed_at") private OffsetDateTime completedAt;
}
