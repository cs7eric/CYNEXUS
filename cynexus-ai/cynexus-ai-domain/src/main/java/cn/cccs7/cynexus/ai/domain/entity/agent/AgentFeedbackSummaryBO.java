package cn.cccs7.cynexus.ai.domain.agent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentFeedbackSummaryBO {
    long total;
    long positive;
    long negative;
    long reviewed;
    double averageRating;
}
