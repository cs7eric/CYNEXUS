package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseGenerateDTO;
import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseGenerateRequest;
import cn.cccs7.cynexus.ai.application.service.KnowledgeBaseDocumentGenerationAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge-base")
public class AiKnowledgeBaseController {

    private final KnowledgeBaseDocumentGenerationAppService generationAppService;

    public AiKnowledgeBaseController(KnowledgeBaseDocumentGenerationAppService generationAppService) {
        this.generationAppService = generationAppService;
    }

    @PostMapping("/document-draft")
    public Result<KnowledgeBaseGenerateDTO> generateDocumentDraft(@RequestBody KnowledgeBaseGenerateRequest request) {
        return Result.success(generationAppService.generate(request));
    }
}
