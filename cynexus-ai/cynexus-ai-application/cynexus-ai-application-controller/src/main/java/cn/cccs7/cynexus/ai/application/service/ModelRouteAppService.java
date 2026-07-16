package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ModelRouteAppService {

    private final AiModelRepository aiModelRepository;
    private final ModelRoutingDomainService modelRoutingDomainService;

    public ModelRouteAppService(AiModelRepository aiModelRepository,
                                ModelRoutingDomainService modelRoutingDomainService) {
        this.aiModelRepository = aiModelRepository;
        this.modelRoutingDomainService = modelRoutingDomainService;
    }

    public AiModelBO route(AiScenarioBO scenario, String preferredModelCode) {
        List<AiModelBO> candidates = aiModelRepository.findEnabledModels();
        return modelRoutingDomainService.route(scenario, candidates, preferredModelCode);
    }
}
