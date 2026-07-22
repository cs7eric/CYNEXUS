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
@TableName("ai_agent_write_command")
public class AgentWriteCommandPO extends BasePO {
    @TableField("command_no") private String commandNo;
    @TableField("owner_key") private String ownerKey;
    @TableField("tenant_key") private String tenantKey;
    @TableField("principal_key") private String principalKey;
    @TableField("conversation_no") private String conversationNo;
    @TableField("run_no") private String runNo;
    @TableField("tool_code") private String toolCode;
    @TableField(value = "arguments_json", typeHandler = JsonbTypeHandler.class) private String argumentsJson;
    @TableField("argument_digest") private String argumentDigest;
    @TableField("confirmation_token_hash") private String confirmationTokenHash;
    private String status;
    @TableField("expires_at") private OffsetDateTime expiresAt;
    @TableField("confirmed_at") private OffsetDateTime confirmedAt;
    @TableField("executed_at") private OffsetDateTime executedAt;
    @TableField(value = "result_json", typeHandler = JsonbTypeHandler.class) private String resultJson;
    @TableField("error_message") private String errorMessage;
}
