package cn.cccs7.cynexus.ai.infra.basic.entity.agent;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_feedback")
public class AgentFeedbackPO extends BasePO {
    @TableField("owner_key") private String ownerKey;
    @TableField("tenant_key") private String tenantKey;
    @TableField("principal_key") private String principalKey;
    @TableField("run_no") private String runNo;
    @TableField("conversation_no") private String conversationNo;
    private Integer rating;
    @TableField("feedback_reason") private String feedbackReason;
    @TableField("correction_text") private String correctionText;
    @TableField("evaluation_label") private String evaluationLabel;
    @TableField("reviewer_key") private String reviewerKey;
    @TableField("reviewer_note") private String reviewerNote;
    @TableField("reviewed_at") private OffsetDateTime reviewedAt;
}
