package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.util.List;
import java.util.Map;

public record PatternDiscoveryCandidate(String code, String method, MarketFactorResearchBO.DiscoveryScope scope,
                                        Map<String, Object> recipe, Map<String, Object> learnedBounds,
                                        int eventSupport, int controlSupport, List<String> eventSampleIds,
                                        List<String> controlSampleIds) {
    public PatternDiscoveryCandidate {
        recipe = recipe == null ? Map.of() : Map.copyOf(recipe);
        learnedBounds = learnedBounds == null ? Map.of() : Map.copyOf(learnedBounds);
        eventSampleIds = eventSampleIds == null ? List.of() : List.copyOf(eventSampleIds);
        controlSampleIds = controlSampleIds == null ? List.of() : List.copyOf(controlSampleIds);
    }

    public PatternDiscoveryCandidate(String code, String method, MarketFactorResearchBO.DiscoveryScope scope,
                                     Map<String, Object> recipe, Map<String, Object> learnedBounds,
                                     int eventSupport, int controlSupport) {
        this(code, method, scope, recipe, learnedBounds, eventSupport, controlSupport, List.of(), List.of());
    }
}
