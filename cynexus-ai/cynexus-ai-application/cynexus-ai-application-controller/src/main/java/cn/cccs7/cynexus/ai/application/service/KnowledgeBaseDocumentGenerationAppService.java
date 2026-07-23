package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseGenerateDTO;
import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseGenerateRequest;
import cn.cccs7.cynexus.ai.domain.knowledgebase.KnowledgeBaseDocumentDraftPolicy;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseDocumentGenerationDomainService;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationCommand;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationOutcome;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationStatus;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseDocumentGenerationAppService {

    private final KnowledgeBaseDocumentGenerationDomainService domainService;

    public KnowledgeBaseDocumentGenerationAppService(KnowledgeBaseDocumentGenerationDomainService domainService) {
        this.domainService = domainService;
    }

    public KnowledgeBaseGenerateDTO generate(KnowledgeBaseGenerateRequest request) {
        KnowledgeBaseGenerationOutcome outcome = domainService.generate(new KnowledgeBaseGenerationCommand(
                request == null ? null : request.getSourceText(), request == null ? null : request.getPreferredModelCode(),
                request == null ? null : request.getTenantCode(), request == null ? null : request.getRoutingQuality(),
                request == null ? null : request.getMaxCostTier()));
        domainService.recordExecutionLogs(outcome);
        if (outcome.status() == KnowledgeBaseGenerationStatus.INVALID_INPUT) {
            throw new BizException(ErrorCode.BAD_REQUEST, outcome.failureReason());
        }
        if (!outcome.succeeded()) {
            throw new BizException(ErrorCode.INTERNAL_ERROR,
                    "Knowledge Base document generation failed: " + safeMessage(outcome.failureReason()));
        }
        KnowledgeBaseDocumentDraftPolicy.Draft draft = outcome.draft();
        return KnowledgeBaseGenerateDTO.builder().title(draft.title()).description(draft.description())
                .tags(draft.tags()).categories(draft.categories()).content(draft.content()).build();
    }

    private String safeMessage(String value) {
        return value == null || value.isBlank() ? "no available AI route" : value;
    }
}
