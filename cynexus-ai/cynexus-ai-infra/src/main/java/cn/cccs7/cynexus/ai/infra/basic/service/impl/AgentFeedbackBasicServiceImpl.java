package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentFeedbackPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentFeedbackSummaryPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentFeedbackMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentFeedbackBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class AgentFeedbackBasicServiceImpl implements AgentFeedbackBasicService {
    private final AgentFeedbackMapper mapper;
    private final SnowflakeIdGenerator idGenerator;

    public AgentFeedbackBasicServiceImpl(AgentFeedbackMapper mapper, SnowflakeIdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public AgentFeedbackPO upsert(AgentFeedbackPO feedback) {
        if (feedback.getId() == null) feedback.setId(idGenerator.nextId());
        if (feedback.getCreatedAt() == null) feedback.setCreatedAt(OffsetDateTime.now());
        mapper.upsert(feedback);
        AgentFeedbackPO persisted = mapper.find(feedback.getOwnerKey(), feedback.getRunNo());
        return persisted == null ? feedback : persisted;
    }

    @Override public AgentFeedbackPO find(String ownerKey, String runNo) { return mapper.find(ownerKey, runNo); }
    @Override public AgentFeedbackSummaryPO summarize(String ownerKey) { return mapper.summarize(ownerKey); }
    @Override public int archiveBefore(OffsetDateTime cutoff) { return mapper.archiveBefore(cutoff); }
}
