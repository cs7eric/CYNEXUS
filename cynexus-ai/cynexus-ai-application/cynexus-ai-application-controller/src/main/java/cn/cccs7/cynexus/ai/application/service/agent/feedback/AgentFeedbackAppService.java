package cn.cccs7.cynexus.ai.application.service.agent.feedback;

import cn.cccs7.cynexus.ai.api.dto.AgentFeedbackDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentFeedbackSummaryDTO;
import cn.cccs7.cynexus.ai.api.request.AgentFeedbackRequest;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentFeedbackBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.service.agent.feedback.AgentFeedbackDomainService;
import org.springframework.stereotype.Service;

@Service
public class AgentFeedbackAppService {
    private final AgentFeedbackDomainService domainService;

    public AgentFeedbackAppService(AgentFeedbackDomainService domainService) {
        this.domainService = domainService;
    }

    public AgentFeedbackDTO submit(AgentIdentity identity, String runNo, AgentFeedbackRequest request) {
        AgentIdentity safeIdentity = identity == null ? AgentIdentity.singleUser() : identity;
        AgentFeedbackBO feedback = domainService.submit(safeIdentity, runNo,
                request == null ? null : request.getRating(), request == null ? null : request.getFeedbackReason(),
                request == null ? null : request.getCorrectionText(), request == null ? null : request.getEvaluationLabel());
        return toDTO(feedback);
    }

    public AgentFeedbackDTO get(AgentIdentity identity, String runNo) {
        AgentFeedbackBO feedback = domainService.find(identity, runNo);
        return feedback == null ? null : toDTO(feedback);
    }

    public AgentFeedbackSummaryDTO summary(AgentIdentity identity) {
        var summary = domainService.summarize(identity);
        return AgentFeedbackSummaryDTO.builder().total(summary.getTotal()).positive(summary.getPositive())
                .negative(summary.getNegative()).reviewed(summary.getReviewed()).averageRating(summary.getAverageRating()).build();
    }

    private AgentFeedbackDTO toDTO(AgentFeedbackBO feedback) {
        return AgentFeedbackDTO.builder().id(feedback.getId()).runNo(feedback.getRunNo()).conversationNo(feedback.getConversationNo())
                .rating(feedback.getRating()).feedbackReason(feedback.getFeedbackReason()).correctionText(feedback.getCorrectionText())
                .evaluationLabel(feedback.getEvaluationLabel()).createdAt(feedback.getCreatedAt()).updatedAt(feedback.getUpdatedAt()).build();
    }
}
