package cn.cccs7.cynexus.ai.infra.basic.entity.agent;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_run_event")
public class AgentRunEventPO extends BasePO {
    @TableField("run_no") private String runNo;
    @TableField("owner_key") private String ownerKey;
    @TableField("sequence_no") private Integer sequenceNo;
    @TableField("trace_id") private String traceId;
    @TableField("event_type") private String eventType;
    private String status;
    @TableField("step_no") private Integer stepNo;
    @TableField("tool_code") private String toolCode;
    private String summary;
    private String answer;
    private String delta;
    @TableField("error_message") private String errorMessage;
    @TableField("occurred_at") private OffsetDateTime occurredAt;
}
