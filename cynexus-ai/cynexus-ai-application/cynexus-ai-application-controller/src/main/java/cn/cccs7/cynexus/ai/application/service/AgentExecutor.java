package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentExecutor {

    public String execute(AiScenarioBO scenario, AiModelBO model, String prompt, List<String> toolCodes) {
        return """
                {"scenario":"%s","model":"%s","toolCodes":"%s","content":"mock structured AI response"}
                """.formatted(scenario.scenarioCode(), model.modelCode(), toolCodes);
    }
}
