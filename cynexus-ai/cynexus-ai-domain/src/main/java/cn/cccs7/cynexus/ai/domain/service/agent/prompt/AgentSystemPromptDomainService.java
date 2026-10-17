package cn.cccs7.cynexus.ai.domain.service.agent.prompt;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentSystemPromptPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Domain ownership for autonomous Agent system-prompt resolution.
 * Application layer must call this service instead of repositories or BO assembly.
 */
@Service
public class AgentSystemPromptDomainService {

    private final AiPromptRepository promptRepository;
    private final AiUsageBindingRepository usageBindingRepository;
    private final AiPromptRuntimeResolver promptRuntimeResolver;

    public AgentSystemPromptDomainService(AiPromptRepository promptRepository,
                                          AiUsageBindingRepository usageBindingRepository) {
        this.promptRepository = promptRepository;
        this.usageBindingRepository = usageBindingRepository;
        this.promptRuntimeResolver = null;
    }

    /** Production constructor: shared resolver owns binding, active prompt and fallback selection. */
    @Autowired
    public AgentSystemPromptDomainService(AiPromptRepository promptRepository,
                                          AiUsageBindingRepository usageBindingRepository,
                                          AiPromptRuntimeResolver promptRuntimeResolver) {
        this.promptRepository = promptRepository;
        this.usageBindingRepository = usageBindingRepository;
        this.promptRuntimeResolver = promptRuntimeResolver;
    }

    public String resolve(String currentDate,
                          int toolBudget,
                          int usedTools,
                          boolean forceFinalAnswer,
                          AgentExecutionLimits limits) {
        String template = loadTemplate(boundPromptId());
        return AgentSystemPromptPolicy.render(
                template,
                currentDate,
                toolBudget,
                usedTools,
                forceFinalAnswer,
                limits);
    }

    public String resolveStable(AgentExecutionLimits limits) {
        return AgentSystemPromptPolicy.renderStable(loadTemplate(boundPromptId()), limits);
    }

    public String resolveRuntimeContext(String currentDate,
                                        int toolBudget,
                                        int usedTools,
                                        boolean forceFinalAnswer,
                                        AgentExecutionLimits limits) {
        return AgentSystemPromptPolicy.runtimeContext(
                currentDate, toolBudget, usedTools, forceFinalAnswer, limits);
    }

    private Long boundPromptId() {
        if (usageBindingRepository == null) {
            return null;
        }
        List<AiUsageBindingBO> bindings = usageBindingRepository.listAll();
        if (bindings == null || bindings.isEmpty()) {
            return null;
        }
        return bindings.stream()
                .filter(item -> item != null && AgentSystemPromptPolicy.PROMPT_KEY.equals(item.getCapability()))
                .filter(item -> item.getEnabled() == null || Boolean.TRUE.equals(item.getEnabled()))
                .map(AiUsageBindingBO::getPromptId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
    }

    private String loadTemplate(Long preferredPromptId) {
        if (promptRuntimeResolver != null) {
            return promptRuntimeResolver.resolve(AgentSystemPromptPolicy.PROMPT_KEY,
                    AgentSystemPromptPolicy.PROMPT_KEY, AgentSystemPromptPolicy.fallbackTemplate(), "").systemPrompt();
        }
        if (promptRepository != null && preferredPromptId != null) {
            AiPromptBO bound = promptRepository.findById(preferredPromptId);
            if (AgentSystemPromptPolicy.isUsable(bound)) {
                return bound.getSystemPrompt().trim();
            }
        }
        if (promptRepository != null) {
            List<AiPromptBO> prompts = promptRepository.list(AgentSystemPromptPolicy.PROMPT_KEY, null, AiConstants.STATUS_ACTIVE);
            if (prompts != null) {
                for (AiPromptBO prompt : prompts) {
                    if (!AgentSystemPromptPolicy.isUsable(prompt)) {
                        continue;
                    }
                    if (!AgentSystemPromptPolicy.PROMPT_KEY.equals(prompt.getPromptKey())) {
                        continue;
                    }
                    return prompt.getSystemPrompt().trim();
                }
            }
        }
        return AgentSystemPromptPolicy.fallbackTemplate();
    }
}
