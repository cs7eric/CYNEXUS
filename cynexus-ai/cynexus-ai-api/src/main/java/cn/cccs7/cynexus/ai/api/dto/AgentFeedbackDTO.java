package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentFeedbackDTO {
    Long id;
    String runNo;
    String conversationNo;
    Integer rating;
    String feedbackReason;
    String correctionText;
    String evaluationLabel;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
