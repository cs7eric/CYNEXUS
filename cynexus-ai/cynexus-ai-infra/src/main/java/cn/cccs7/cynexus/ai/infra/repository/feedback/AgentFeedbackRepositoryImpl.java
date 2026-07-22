package cn.cccs7.cynexus.ai.infra.repository.feedback;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentFeedbackBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentFeedbackSummaryBO;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentFeedbackRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentFeedbackBasicService;
import cn.cccs7.cynexus.ai.infra.convert.agent.AgentFeedbackPOConverter;
import java.util.Optional;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class AgentFeedbackRepositoryImpl implements AgentFeedbackRepository {
    private final AgentFeedbackBasicService service;

    public AgentFeedbackRepositoryImpl(AgentFeedbackBasicService service) { this.service = service; }

    @Override
    public AgentFeedbackBO upsert(AgentFeedbackBO feedback) {
        return AgentFeedbackPOConverter.INSTANCE.toTarget(service.upsert(AgentFeedbackPOConverter.INSTANCE.toSource(feedback)));
    }

    @Override
    public Optional<AgentFeedbackBO> find(String ownerKey, String runNo) {
        return Optional.ofNullable(service.find(ownerKey, runNo)).map(AgentFeedbackPOConverter.INSTANCE::toTarget);
    }

    @Override
    public AgentFeedbackSummaryBO summarize(String ownerKey) {
        var summary = service.summarize(ownerKey);
        return AgentFeedbackSummaryBO.builder().total(summary == null ? 0 : summary.getTotal())
                .positive(summary == null ? 0 : summary.getPositive()).negative(summary == null ? 0 : summary.getNegative())
                .reviewed(summary == null ? 0 : summary.getReviewed()).averageRating(summary == null ? 0 : summary.getAverageRating()).build();
    }

    @Override public int archiveBefore(OffsetDateTime cutoff) { return service.archiveBefore(cutoff); }
}
