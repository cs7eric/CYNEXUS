package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseGenerateDTO;
import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseGenerateRequest;
import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseSearchRequest;
import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseSearchResultDTO;
import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseAnswerDTO;
import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseAnswerRequest;
import cn.cccs7.cynexus.ai.application.service.KnowledgeBaseDocumentGenerationAppService;
import cn.cccs7.cynexus.ai.application.service.KnowledgeBaseSearchAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/ai/knowledge-base")
public class AiKnowledgeBaseController {

    private final KnowledgeBaseDocumentGenerationAppService generationAppService;
    private final ObjectProvider<KnowledgeBaseSearchAppService> searchAppService;

    public AiKnowledgeBaseController(KnowledgeBaseDocumentGenerationAppService generationAppService, ObjectProvider<KnowledgeBaseSearchAppService> searchAppService) {
        this.generationAppService = generationAppService;
        this.searchAppService = searchAppService;
    }

    @PostMapping("/document-draft")
    public Result<KnowledgeBaseGenerateDTO> generateDocumentDraft(@RequestBody KnowledgeBaseGenerateRequest request) {
        return Result.success(generationAppService.generate(request));
    }

    @PostMapping("/search")
    public Result<java.util.List<KnowledgeBaseSearchResultDTO>> search(@RequestBody KnowledgeBaseSearchRequest request,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        KnowledgeBaseSearchAppService service = searchAppService.getIfAvailable();
        if (service == null) throw new cn.cccs7.cynexus.common.core.exception.BizException(cn.cccs7.cynexus.common.core.exception.ErrorCode.INTERNAL_ERROR, "Knowledge vector store is not configured");
        return Result.success(service.search(request, principal, tenant, signature));
    }

    @PostMapping("/answer")
    public Result<KnowledgeBaseAnswerDTO> answer(@RequestBody KnowledgeBaseAnswerRequest request,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        KnowledgeBaseSearchAppService service = searchAppService.getIfAvailable();
        if (service == null) throw new cn.cccs7.cynexus.common.core.exception.BizException(cn.cccs7.cynexus.common.core.exception.ErrorCode.INTERNAL_ERROR, "Knowledge vector store is not configured");
        return Result.success(service.answer(request, principal, tenant, signature));
    }
}
