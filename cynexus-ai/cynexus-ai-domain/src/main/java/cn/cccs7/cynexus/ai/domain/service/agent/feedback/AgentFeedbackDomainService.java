package cn.cccs7.cynexus.ai.domain.service.agent.feedback;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentFeedbackBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentFeedbackSummaryBO;
import cn.cccs7.cynexus.ai.domain.policy.feedback.AgentFeedbackPolicy;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentFeedbackRepository;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import org.springframework.stereotype.Service;

/** Owns terminal-run eligibility, owner scoping, and feedback normalization. */
@Service
public class AgentFeedbackDomainService {
    private final AgentFeedbackRepository feedbackRepository;
    private final AgentRunRepository runRepository;

    public AgentFeedbackDomainService(AgentFeedbackRepository feedbackRepository, AgentRunRepository runRepository) {
        this.feedbackRepository = feedbackRepository;
        this.runRepository = runRepository;
    }

    public AgentFeedbackBO submit(AgentIdentity identity, String runNo, Integer rating, String feedbackReason,
                                  String correctionText, String evaluationLabel) {
        AgentIdentity safe = identity == null ? AgentIdentity.singleUser() : identity;
        String ownerKey = safe.storageOwnerKey();
        var run = runRepository.findRun(ownerKey, runNo)
                .orElseThrow(() -> new IllegalArgumentException("Agent run was not found"));
        if (!isTerminal(run.getStatus())) {
            throw new IllegalStateException("Feedback is available after the Agent run is terminal");
        }
        return feedbackRepository.upsert(AgentFeedbackPolicy.normalize(AgentFeedbackBO.builder()
                .ownerKey(ownerKey).tenantKey(safe.getTenantKey()).principalKey(safe.getPrincipalKey())
                .runNo(runNo).conversationNo(run.getConversationNo()).rating(rating)
                .feedbackReason(feedbackReason).correctionText(correctionText)
                .evaluationLabel(evaluationLabel).build()));
    }

    public AgentFeedbackBO find(AgentIdentity identity, String runNo) {
        return feedbackRepository.find(owner(identity), runNo).orElse(null);
    }

    public AgentFeedbackSummaryBO summarize(AgentIdentity identity) {
        return feedbackRepository.summarize(owner(identity));
    }

    private String owner(AgentIdentity identity) {
        return identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey();
    }

    private boolean isTerminal(String status) {
        return "SUCCEEDED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)
                || "NEEDS_CLARIFICATION".equals(status);
    }
}
