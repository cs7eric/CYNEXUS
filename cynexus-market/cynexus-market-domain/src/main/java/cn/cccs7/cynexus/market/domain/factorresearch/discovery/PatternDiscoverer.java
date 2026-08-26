package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import java.util.List;

/** Pluggable deterministic raw-sequence discoverer. It returns recipes, not fixed screening rules. */
public interface PatternDiscoverer {
    String method();
    List<PatternDiscoveryCandidate> discover(PatternDiscoveryInput input);
}
