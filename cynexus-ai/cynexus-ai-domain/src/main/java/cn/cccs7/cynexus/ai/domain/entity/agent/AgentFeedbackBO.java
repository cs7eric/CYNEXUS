package cn.cccs7.cynexus.ai.domain.entity.agent;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentFeedbackBO {
    private Long id;
    private String ownerKey;
    private String tenantKey;
    private String principalKey;
    private String runNo;
    private String conversationNo;
    private Integer rating;
    private String feedbackReason;
    private String correctionText;
    private String evaluationLabel;
    private String reviewerKey;
    private String reviewerNote;
    private OffsetDateTime reviewedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
