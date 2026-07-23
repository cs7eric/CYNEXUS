package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

import java.util.List;
import java.util.Set;

public record KnowledgeBaseGenerationRoutingParameters(Set<String> tenantCodes, Set<String> qualityLevels,
                                                       Set<String> costTiers, String promptVersion,
                                                       List<String> fallbackModels) {
    public static KnowledgeBaseGenerationRoutingParameters empty() {
        return new KnowledgeBaseGenerationRoutingParameters(Set.of(), Set.of(), Set.of(), "", List.of());
    }

    public boolean matches(KnowledgeBaseGenerationCommand command) {
        return matches(tenantCodes, command == null ? null : command.tenantCode())
                && matches(qualityLevels, command == null ? null : command.routingQuality())
                && matches(costTiers, command == null ? null : command.maxCostTier());
    }

    private boolean matches(Set<String> allowed, String requested) {
        return allowed == null || allowed.isEmpty() || requested == null || requested.isBlank()
                || allowed.stream().anyMatch(value -> value.equalsIgnoreCase(requested.trim()));
    }
}
