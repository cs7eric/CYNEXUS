package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentFeedbackPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentFeedbackSummaryPO;
import java.time.OffsetDateTime;

public interface AgentFeedbackBasicService {
    AgentFeedbackPO upsert(AgentFeedbackPO feedback);
    AgentFeedbackPO find(String ownerKey, String runNo);
    AgentFeedbackSummaryPO summarize(String ownerKey);
    int archiveBefore(OffsetDateTime cutoff);
}
