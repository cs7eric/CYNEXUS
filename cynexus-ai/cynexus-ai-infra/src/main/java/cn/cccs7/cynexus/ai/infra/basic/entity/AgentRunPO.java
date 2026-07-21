package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_agent_run", autoResultMap = true)
public class AgentRunPO extends BasePO {
    @TableField("run_no") private String runNo;
    @TableField("conversation_no") private String conversationNo;
    @TableField("owner_key") private String ownerKey;
    private String status;
    @TableField("user_input") private String userInput;
    private String answer;
    @TableField("model_code") private String modelCode;
    @TableField(typeHandler = JsonbTypeHandler.class) private String plan;
    @TableField("error_message") private String errorMessage;
    @TableField("turn_count") private Integer turnCount;
    @TableField("tool_call_count") private Integer toolCallCount;
    @TableField("started_at") private OffsetDateTime startedAt;
    @TableField("finished_at") private OffsetDateTime finishedAt;
}
