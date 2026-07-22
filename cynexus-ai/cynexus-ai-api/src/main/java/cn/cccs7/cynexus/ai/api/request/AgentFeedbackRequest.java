package cn.cccs7.cynexus.ai.api.request;

import lombok.Data;

@Data
public class AgentFeedbackRequest {
    private Integer rating;
    private String feedbackReason;
    private String correctionText;
    private String evaluationLabel;
}
