package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.service.routing.ModelRouteDomainService;
import org.springframework.stereotype.Service;

@Service
public class ModelRouteAppService {

    private final ModelRouteDomainService modelRouteDomainService;

    public ModelRouteAppService(ModelRouteDomainService modelRouteDomainService) {
        this.modelRouteDomainService = modelRouteDomainService;
    }

    public AiModelBO route(AiScenarioBO scenario, String preferredModelCode) {
        return modelRouteDomainService.route(scenario, preferredModelCode);
    }
}
