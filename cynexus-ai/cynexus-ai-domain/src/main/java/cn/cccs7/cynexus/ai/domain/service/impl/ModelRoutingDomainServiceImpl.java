package cn.cccs7.cynexus.ai.domain.service.impl;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.service.ModelRoutingDomainService;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import java.util.List;

public class ModelRoutingDomainServiceImpl implements ModelRoutingDomainService {

    @Override
    public AiModelBO route(AiScenarioBO scenario, List<AiModelBO> candidates, String preferredModelCode) {
        return candidates.stream()
                .filter(AiModelBO::isEnabled)
                .filter(model -> preferredModelCode == null || preferredModelCode.isBlank()
                        || preferredModelCode.equals(model.getModelCode()))
                .filter(model -> !scenario.isToolCallRequired() || model.getCapability().isToolCallSupported())
                .filter(model -> !scenario.isJsonOutputRequired() || model.getCapability().isStructuredOutputSupported())
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "No available model for scenario"));
    }
}
