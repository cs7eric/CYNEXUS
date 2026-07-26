package cn.cccs7.cynexus.ai.infra.basic.entity.agent;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_agent_conversation", autoResultMap = true)
public class AgentConversationPO extends BasePO {
    @TableField("conversation_no") private String conversationNo;
    @TableField("owner_key") private String ownerKey;
    private String title;
    private String status;
    @TableField("last_run_no") private String lastRunNo;
    @TableField("last_message") private String lastMessage;
    @TableField(typeHandler = JsonbTypeHandler.class) private String metadata;
    @TableField("provider_response_id") private String providerResponseId;
    @TableField("provider_state_provider_code") private String providerStateProviderCode;
    @TableField("provider_state_model_code") private String providerStateModelCode;
    @TableField("provider_state_signature") private String providerStateSignature;
    @TableField("provider_state_updated_at") private java.time.OffsetDateTime providerStateUpdatedAt;
}
