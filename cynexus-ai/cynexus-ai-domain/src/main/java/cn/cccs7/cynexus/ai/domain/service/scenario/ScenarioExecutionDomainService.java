package cn.cccs7.cynexus.ai.domain.service.scenario;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import java.util.List;

public interface ScenarioExecutionDomainService {

    AiModelBO routeInformationModel(String preferredModelCode,
                                    List<String> bindingCapabilities,
                                    boolean workspaceChat);

    AiModelBO routeVisionModel(String capability, String preferredModelCode);

    String resolveVisionPrompt(String capability, String candidateContext);
}
