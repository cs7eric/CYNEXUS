package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Selects a repeated observed metadata subgroup, retaining its literal condition in the recipe. */
public final class InterpretableSubgroupDiscoverer implements PatternDiscoverer {
    @Override public String method() { return "INTERPRETABLE_SUBGROUP"; }
    @Override public List<PatternDiscoveryCandidate> discover(PatternDiscoveryInput input) {
        List<PatternDiscoveryCandidate> output = new ArrayList<>();
        Map<String, Long> event = labels(input.events()); Map<String, Long> control = labels(input.controls());
        event.entrySet().stream().filter(entry -> entry.getValue() >= 2).filter(entry -> entry.getValue() > control.getOrDefault(entry.getKey(), 0L))
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(20).forEach(entry -> {
                    String[] condition = entry.getKey().split("=", 2);
                    output.add(new PatternDiscoveryCandidate("SG_" + condition[0].toUpperCase() + "_" + Integer.toUnsignedString(entry.getKey().hashCode()), method(),
                            MarketFactorResearchBO.DiscoveryScope.SUBGROUP, Map.of("label", condition[0], "value", condition[1]),
                            Map.of("eventMembers", entry.getValue(), "controlMembers", control.getOrDefault(entry.getKey(), 0L)), entry.getValue().intValue(), control.getOrDefault(entry.getKey(), 0L).intValue(),
                            matchingIds(input.events(), condition[0], condition[1]), matchingIds(input.controls(), condition[0], condition[1])));
                });
        return output;
    }
    private Map<String, Long> labels(List<PatternDiscoveryInput.Series> values) { return values.stream().flatMap(value -> value.labels().entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()))
            .collect(java.util.stream.Collectors.groupingBy(value -> value, java.util.TreeMap::new, java.util.stream.Collectors.counting())); }
    private List<String> matchingIds(List<PatternDiscoveryInput.Series> rows, String label, String value) { return rows.stream().filter(row -> value.equals(row.labels().get(label))).map(PatternDiscoveryInput.Series::sampleId).toList(); }
}
