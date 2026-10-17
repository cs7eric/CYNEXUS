package cn.cccs7.cynexus.ai.infra.ai.prompt;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Infrastructure lookup for the DB-backed Prompt contract; fallback text stays in the Domain caller. */
@Component
public class AiPromptRuntimeResolverImpl implements AiPromptRuntimeResolver {

    private final ApplicationAnalysisRoutingDomainService routingService;
    private final AiPromptRepository promptRepository;

    public AiPromptRuntimeResolverImpl(ApplicationAnalysisRoutingDomainService routingService,
                                       AiPromptRepository promptRepository) {
        this.routingService = routingService;
        this.promptRepository = promptRepository;
    }

    @Override
    public AiPromptRuntime resolve(String capability, String promptKey, String fallbackSystemPrompt,
                                   String fallbackUserPrompt) {
        AiUsageBindingBO binding = routingService.findBinding(capability);
        if (binding != null && binding.getPromptId() != null) {
            AiPromptBO bound = promptRepository.findById(binding.getPromptId());
            if (active(bound) && keyMatches(bound, promptKey)) return AiPromptRuntime.from(bound);
        }
        String requestedVersion = binding == null ? "" : routingService.promptVersion(binding);
        List<AiPromptBO> prompts = promptRepository.list(promptKey, null, AiConstants.STATUS_ACTIVE).stream()
                .filter(this::active)
                .filter(prompt -> keyMatches(prompt, promptKey))
                .toList();
        if (!prompts.isEmpty()) {
            if (!requestedVersion.isBlank()) {
                for (AiPromptBO prompt : prompts) {
                    if (requestedVersion.equals(prompt.getPromptVersion())) return AiPromptRuntime.from(prompt);
                }
            }
            return AiPromptRuntime.from(prompts.getFirst());
        }
        return new AiPromptRuntime(promptKey, "fallback-service-default", fallbackSystemPrompt, fallbackUserPrompt);
    }

    private boolean active(AiPromptBO prompt) {
        return prompt != null && AiConstants.STATUS_ACTIVE.equalsIgnoreCase(prompt.getStatus());
    }

    private boolean keyMatches(AiPromptBO prompt, String promptKey) {
        return prompt != null && promptKey != null && promptKey.equals(prompt.getPromptKey());
    }
}
