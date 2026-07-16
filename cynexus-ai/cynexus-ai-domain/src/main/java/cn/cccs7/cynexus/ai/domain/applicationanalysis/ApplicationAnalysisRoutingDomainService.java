package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import java.util.List;

public interface ApplicationAnalysisRoutingDomainService {

    AiUsageBindingBO findBinding(String capability);

    List<AiModelBO> routeModels(String capability, String preferredModelCode);

    String promptVersion(AiUsageBindingBO binding);
}
