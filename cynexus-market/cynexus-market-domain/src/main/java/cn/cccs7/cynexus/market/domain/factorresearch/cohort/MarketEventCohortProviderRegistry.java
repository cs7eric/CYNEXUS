package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Spring supplies the provider list; duplicate event/version registrations are rejected at startup. */
public final class MarketEventCohortProviderRegistry {
    private final Map<MarketFactorResearchBO.EventType, MarketEventCohortProvider> providers;

    public MarketEventCohortProviderRegistry(List<MarketEventCohortProvider> providers) {
        Map<MarketFactorResearchBO.EventType, MarketEventCohortProvider> values =
                new EnumMap<>(MarketFactorResearchBO.EventType.class);
        for (MarketEventCohortProvider provider : providers == null ? List.<MarketEventCohortProvider>of() : providers) {
            if (provider == null) continue;
            if (values.putIfAbsent(provider.eventType(), provider) != null) {
                throw new IllegalArgumentException("duplicate event cohort provider: " + provider.eventType());
            }
        }
        this.providers = Map.copyOf(values);
    }

    public MarketEventCohortProvider require(MarketFactorResearchBO.EventType eventType, int definitionVersion) {
        MarketEventCohortProvider provider = providers.get(eventType);
        if (provider == null) throw new IllegalArgumentException("event cohort provider is not registered: " + eventType);
        if (provider.definitionVersion() != definitionVersion) {
            throw new IllegalArgumentException("unsupported cohort definition version for " + eventType);
        }
        return provider;
    }
}
