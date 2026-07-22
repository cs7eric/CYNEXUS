package cn.cccs7.cynexus.ai.infra.basic.service.agent;

import java.time.OffsetDateTime;

public interface AgentRunRetentionBasicService {
    int archiveBefore(OffsetDateTime cutoff);
}
