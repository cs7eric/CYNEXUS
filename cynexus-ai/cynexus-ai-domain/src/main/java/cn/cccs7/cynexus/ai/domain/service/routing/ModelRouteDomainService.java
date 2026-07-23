package cn.cccs7.cynexus.ai.domain.service.routing;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;

public interface ModelRouteDomainService {

    AiModelBO route(AiScenarioBO scenario, String preferredModelCode);
}
