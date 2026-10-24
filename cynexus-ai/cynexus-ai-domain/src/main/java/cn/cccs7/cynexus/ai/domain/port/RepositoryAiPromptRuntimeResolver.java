package cn.cccs7.cynexus.ai.domain.port;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import java.util.List;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compatibility resolver for direct domain construction in older integrations.
 * Production wiring uses the infrastructure resolver; both obey the same contract.
 */
public final class RepositoryAiPromptRuntimeResolver implements AiPromptRuntimeResolver {
    private static final Pattern VERSION_NUMBER = Pattern.compile("(?:^|\\D)(\\d+)(?:\\D|$)");
    private final ApplicationAnalysisRoutingDomainService routingService;
    private final AiPromptRepository promptRepository;

    public RepositoryAiPromptRuntimeResolver(ApplicationAnalysisRoutingDomainService routingService,
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
            if (active(bound)) return AiPromptRuntime.from(bound);
        }
        String requestedVersion = binding == null ? "" : routingService.promptVersion(binding);
        List<AiPromptBO> prompts = promptRepository.list(promptKey, null, AiConstants.STATUS_ACTIVE).stream()
                .filter(this::active)
                .filter(prompt -> promptKey.equals(prompt.getPromptKey()))
                .filter(prompt -> requestedVersion.isBlank() || requestedVersion.equals(prompt.getPromptVersion()))
                .sorted(Comparator.comparingInt((AiPromptBO prompt) -> versionNumber(prompt.getPromptVersion()))
                        .reversed()
                        .thenComparing(prompt -> prompt.getPromptVersion() == null ? "" : prompt.getPromptVersion(), Comparator.reverseOrder()))
                .toList();
        return prompts.isEmpty() ? new AiPromptRuntime(promptKey, "fallback-service-default",
                fallbackSystemPrompt, fallbackUserPrompt) : AiPromptRuntime.from(prompts.getFirst());
    }

    private boolean active(AiPromptBO prompt) {
        return prompt != null && AiConstants.STATUS_ACTIVE.equalsIgnoreCase(prompt.getStatus());
    }

    private int versionNumber(String version) {
        if (version == null || version.isBlank()) return -1;
        Matcher matcher = VERSION_NUMBER.matcher(version);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
