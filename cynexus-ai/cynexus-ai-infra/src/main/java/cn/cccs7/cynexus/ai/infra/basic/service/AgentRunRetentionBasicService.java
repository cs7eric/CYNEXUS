package cn.cccs7.cynexus.ai.infra.basic.service;

import java.time.OffsetDateTime;

public interface AgentRunRetentionBasicService {
    int archiveBefore(OffsetDateTime cutoff);
}
