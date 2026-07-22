package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.agent.AgentFeedbackBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentFeedbackSummaryBO;
import java.util.Optional;
import java.time.OffsetDateTime;

public interface AgentFeedbackRepository {
    AgentFeedbackBO upsert(AgentFeedbackBO feedback);
    Optional<AgentFeedbackBO> find(String ownerKey, String runNo);
    AgentFeedbackSummaryBO summarize(String ownerKey);
    default int archiveBefore(OffsetDateTime cutoff) { return 0; }
}
