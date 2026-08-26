package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import java.util.List;

/** Executes registered discoverers in deterministic method order and bounds candidate output. */
public final class PatternDiscoveryEngine {
    private final List<PatternDiscoverer> discoverers;
    public PatternDiscoveryEngine(List<PatternDiscoverer> discoverers) {
        this.discoverers = (discoverers == null ? List.<PatternDiscoverer>of() : discoverers).stream()
                .sorted(java.util.Comparator.comparing(PatternDiscoverer::method)).toList();
    }
    public List<PatternDiscoveryCandidate> discover(PatternDiscoveryInput input) {
        return discoverers.stream().flatMap(discoverer -> discoverer.discover(input).stream()).limit(100).toList();
    }
}
