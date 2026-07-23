package cn.cccs7.cynexus.ai.domain.service.routing;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainService;
import java.util.Objects;

public class ModelRouteDomainServiceImpl implements ModelRouteDomainService {

    private final AiModelRepository aiModelRepository;
    private final ModelRoutingDomainService modelRoutingDomainService;

    public ModelRouteDomainServiceImpl(AiModelRepository aiModelRepository,
                                       ModelRoutingDomainService modelRoutingDomainService) {
        this.aiModelRepository = Objects.requireNonNull(aiModelRepository, "aiModelRepository");
        this.modelRoutingDomainService = Objects.requireNonNull(modelRoutingDomainService, "modelRoutingDomainService");
    }

    @Override
    public AiModelBO route(AiScenarioBO scenario, String preferredModelCode) {
        return modelRoutingDomainService.route(
                scenario,
                aiModelRepository.findEnabledModels(),
                preferredModelCode);
    }
}
