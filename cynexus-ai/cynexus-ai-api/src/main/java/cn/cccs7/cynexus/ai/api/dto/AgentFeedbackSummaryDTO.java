package cn.cccs7.cynexus.ai.api.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentFeedbackSummaryDTO {
    long total;
    long positive;
    long negative;
    long reviewed;
    double averageRating;
}
