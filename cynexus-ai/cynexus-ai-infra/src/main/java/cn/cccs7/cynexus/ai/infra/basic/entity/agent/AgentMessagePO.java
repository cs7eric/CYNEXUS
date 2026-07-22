package cn.cccs7.cynexus.ai.infra.basic.entity.agent;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_agent_message", autoResultMap = true)
public class AgentMessagePO extends BasePO {
    @TableField("conversation_no") private String conversationNo;
    @TableField("run_no") private String runNo;
    @TableField("owner_key") private String ownerKey;
    @TableField("sequence_no") private Integer sequenceNo;
    private String role;
    private String content;
    @TableField("tool_call_id") private String toolCallId;
    @TableField("tool_name") private String toolName;
    @TableField(value = "tool_calls", typeHandler = JsonbTypeHandler.class) private String toolCalls;
}
