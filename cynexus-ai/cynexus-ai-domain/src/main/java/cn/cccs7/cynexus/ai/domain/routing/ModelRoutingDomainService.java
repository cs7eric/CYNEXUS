package cn.cccs7.cynexus.ai.domain.routing;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import java.util.List;

public interface ModelRoutingDomainService {

    AiModelBO route(AiScenarioBO scenario, List<AiModelBO> candidates, String preferredModelCode);
}
