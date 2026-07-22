package cn.cccs7.cynexus.ai.infra.basic.entity.agent;

import lombok.Data;

@Data
public class AgentFeedbackSummaryPO {
    private long total;
    private long positive;
    private long negative;
    private long reviewed;
    private double averageRating;
}
