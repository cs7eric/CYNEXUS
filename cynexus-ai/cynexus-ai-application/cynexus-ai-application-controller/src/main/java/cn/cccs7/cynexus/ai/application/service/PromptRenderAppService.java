package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.service.prompt.PromptRenderDomainService;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Thin compatibility adapter. Application must not own prompt rendering algorithms. */
@Service
public class PromptRenderAppService {

    private final PromptRenderDomainService domainService;

    public PromptRenderAppService(PromptRenderDomainService domainService) {
        this.domainService = domainService;
    }

    public String render(String template, String userInput, Map<String, Object> variables) {
        return domainService.render(template, userInput, variables);
    }
}
