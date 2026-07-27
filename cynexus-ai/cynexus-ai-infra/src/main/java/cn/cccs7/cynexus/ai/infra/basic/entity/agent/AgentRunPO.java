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
@TableName(value = "ai_agent_run", autoResultMap = true)
public class AgentRunPO extends BasePO {
    @TableField("run_no") private String runNo;
    @TableField("conversation_no") private String conversationNo;
    @TableField("owner_key") private String ownerKey;
    @TableField("idempotency_key") private String idempotencyKey;
    private String status;
    @TableField("user_input") private String userInput;
    private String answer;
    @TableField("provider_code") private String providerCode;
    @TableField("model_code") private String modelCode;
    @TableField("reasoning_effort") private String reasoningEffort;
    @TableField(typeHandler = JsonbTypeHandler.class) private String plan;
    @TableField(value = "context_json", typeHandler = JsonbTypeHandler.class) private String contextJson;
    @TableField(value = "assumptions_json", typeHandler = JsonbTypeHandler.class) private String assumptionsJson;
    @TableField(value = "clarification_json", typeHandler = JsonbTypeHandler.class) private String clarificationJson;
    @TableField(value = "evidence_json", typeHandler = JsonbTypeHandler.class) private String evidenceJson;
    @TableField(value = "warnings_json", typeHandler = JsonbTypeHandler.class) private String warningsJson;
    @TableField("error_message") private String errorMessage;
    @TableField("failure_category") private String failureCategory;
    @TableField("turn_count") private Integer turnCount;
    @TableField("tool_call_count") private Integer toolCallCount;
    @TableField("input_tokens") private Integer inputTokens;
    @TableField("cached_input_tokens") private Integer cachedInputTokens;
    @TableField("cache_write_tokens") private Integer cacheWriteTokens;
    @TableField("output_tokens") private Integer outputTokens;
    @TableField("total_tokens") private Integer totalTokens;
    @TableField("estimated_cost") private java.math.BigDecimal estimatedCost;
    @TableField("usage_source") private String usageSource;
    @TableField("started_at") private OffsetDateTime startedAt;
    @TableField("finished_at") private OffsetDateTime finishedAt;
    @TableField("cancellation_requested_at") private OffsetDateTime cancellationRequestedAt;
    @TableField("cancellation_requested_by") private String cancellationRequestedBy;
}
