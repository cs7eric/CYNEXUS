package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.infra.ai.adapter.AiModelAdapter;
import cn.cccs7.cynexus.ai.infra.ai.factory.DynamicChatClientFactory;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentExecutor {

    private final DynamicChatClientFactory dynamicChatClientFactory;

    public AgentExecutor(DynamicChatClientFactory dynamicChatClientFactory) {
        this.dynamicChatClientFactory = dynamicChatClientFactory;
    }

    public String execute(AiScenarioBO scenario, AiModelBO model, String prompt, List<String> toolCodes) {
        if (toolCodes == null || toolCodes.isEmpty()) {
            return complete(model, prompt);
        }
        return """
                {"scenario":"%s","model":"%s","toolCodes":"%s","content":"mock structured AI response"}
                """.formatted(scenario.getScenarioCode(), model.getModelCode(), toolCodes);
    }

    public String complete(AiModelBO model, String prompt) {
        return completeDetailed(model, prompt).getContent();
    }

    public AiCompletionResultBO completeDetailed(AiModelBO model, String prompt) {
        String providerCode = model == null || model.getProviderCode() == null || model.getProviderCode().isBlank()
                ? "mock"
                : model.getProviderCode();
        AiModelAdapter adapter = dynamicChatClientFactory.resolve(providerCode);
        if (adapter == null) {
            throw new IllegalStateException("No AI model adapter for provider: " + providerCode);
        }
        return adapter.complete(model, prompt);
    }
}
