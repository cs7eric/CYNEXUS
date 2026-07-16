package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAnalysisModelRouter {

    private final ApplicationAnalysisRoutingDomainService domainService;

    public ApplicationAnalysisModelRouter(ApplicationAnalysisRoutingDomainService domainService) {
        this.domainService = domainService;
    }

    public AiUsageBindingBO findBinding(String capability) {
        return domainService.findBinding(capability);
    }

    public List<AiModelBO> routeModels(String capability, String preferredModelCode) {
        return domainService.routeModels(capability, preferredModelCode);
    }

    public String promptVersion(AiUsageBindingBO binding) {
        return domainService.promptVersion(binding);
    }
}
