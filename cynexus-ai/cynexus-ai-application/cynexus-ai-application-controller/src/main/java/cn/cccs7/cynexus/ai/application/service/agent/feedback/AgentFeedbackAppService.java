package cn.cccs7.cynexus.ai.application.service.agent.feedback;

import cn.cccs7.cynexus.ai.api.dto.AgentFeedbackDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentFeedbackSummaryDTO;
import cn.cccs7.cynexus.ai.api.request.AgentFeedbackRequest;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentFeedbackBO;
import cn.cccs7.cynexus.ai.domain.policy.feedback.AgentFeedbackPolicy;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentFeedbackSummaryBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentFeedbackRepository;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import org.springframework.stereotype.Service;

@Service
public class AgentFeedbackAppService {
    private final AgentFeedbackRepository feedbackRepository;
    private final AgentRunRepository runRepository;

    public AgentFeedbackAppService(AgentFeedbackRepository feedbackRepository, AgentRunRepository runRepository) {
        this.feedbackRepository = feedbackRepository;
        this.runRepository = runRepository;
    }

    public AgentFeedbackDTO submit(AgentIdentity identity, String runNo, AgentFeedbackRequest request) {
        AgentIdentity safeIdentity = identity == null ? AgentIdentity.singleUser() : identity;
        String ownerKey = safeIdentity.storageOwnerKey();
        var run = runRepository.findRun(ownerKey, runNo).orElseThrow(() -> new IllegalArgumentException("Agent run was not found"));
        if (!isTerminal(run.getStatus())) throw new IllegalStateException("Feedback is available after the Agent run is terminal");
        AgentFeedbackBO feedback = AgentFeedbackPolicy.normalize(AgentFeedbackBO.builder()
                .ownerKey(ownerKey).tenantKey(safeIdentity.getTenantKey()).principalKey(safeIdentity.getPrincipalKey())
                .runNo(runNo).conversationNo(run.getConversationNo()).rating(request == null ? null : request.getRating())
                .feedbackReason(request == null ? null : request.getFeedbackReason())
                .correctionText(request == null ? null : request.getCorrectionText())
                .evaluationLabel(request == null ? null : request.getEvaluationLabel()).build());
        return toDTO(feedbackRepository.upsert(feedback));
    }

    public AgentFeedbackDTO get(AgentIdentity identity, String runNo) {
        String ownerKey = identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey();
        return feedbackRepository.find(ownerKey, runNo).map(this::toDTO).orElse(null);
    }

    public AgentFeedbackSummaryDTO summary(AgentIdentity identity) {
        String ownerKey = identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey();
        AgentFeedbackSummaryBO summary = feedbackRepository.summarize(ownerKey);
        return AgentFeedbackSummaryDTO.builder().total(summary.getTotal()).positive(summary.getPositive())
                .negative(summary.getNegative()).reviewed(summary.getReviewed()).averageRating(summary.getAverageRating()).build();
    }

    private boolean isTerminal(String status) {
        return "SUCCEEDED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)
                || "NEEDS_CLARIFICATION".equals(status);
    }

    private AgentFeedbackDTO toDTO(AgentFeedbackBO feedback) {
        return AgentFeedbackDTO.builder().id(feedback.getId()).runNo(feedback.getRunNo()).conversationNo(feedback.getConversationNo())
                .rating(feedback.getRating()).feedbackReason(feedback.getFeedbackReason()).correctionText(feedback.getCorrectionText())
                .evaluationLabel(feedback.getEvaluationLabel()).createdAt(feedback.getCreatedAt()).updatedAt(feedback.getUpdatedAt()).build();
    }
}
