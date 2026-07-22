package cn.cccs7.cynexus.ai.infra.basic.service.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentFeedbackPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentFeedbackSummaryPO;
import java.time.OffsetDateTime;

public interface AgentFeedbackBasicService {
    AgentFeedbackPO upsert(AgentFeedbackPO feedback);
    AgentFeedbackPO find(String ownerKey, String runNo);
    AgentFeedbackSummaryPO summarize(String ownerKey);
    int archiveBefore(OffsetDateTime cutoff);
}
