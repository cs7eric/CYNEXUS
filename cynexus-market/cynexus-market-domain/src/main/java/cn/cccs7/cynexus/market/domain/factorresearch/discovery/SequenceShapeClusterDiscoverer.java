package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Clusters only by an observed coarse raw shape signature; it never assigns a semantic label. */
public final class SequenceShapeClusterDiscoverer implements PatternDiscoverer {
    @Override public String method() { return "SEQUENCE_SHAPE_CLUSTER"; }
    @Override public List<PatternDiscoveryCandidate> discover(PatternDiscoveryInput input) {
        List<PatternDiscoveryCandidate> output = new ArrayList<>();
        for (String primitive : input.events().stream().flatMap(value -> value.primitives().keySet().stream()).distinct().sorted().toList()) {
            Map<String, Long> event = signatures(input.events(), primitive); Map<String, Long> control = signatures(input.controls(), primitive);
            event.entrySet().stream().max(Map.Entry.comparingByValue()).ifPresent(entry -> {
                long controls = control.getOrDefault(entry.getKey(), 0L);
                if (entry.getValue() > controls) output.add(new PatternDiscoveryCandidate("SC_" + primitive.replace('.', '_').toUpperCase() + "_" + entry.getKey(), method(),
                        entry.getValue() >= Math.max(3, input.events().size() / 2) ? MarketFactorResearchBO.DiscoveryScope.COMMON : MarketFactorResearchBO.DiscoveryScope.SUBGROUP,
                        Map.of("primitive", primitive, "shapeSignature", entry.getKey()), Map.of("eventMembers", entry.getValue(), "controlMembers", controls),
                        entry.getValue().intValue(), (int) controls, matchingIds(input.events(), primitive, entry.getKey()), matchingIds(input.controls(), primitive, entry.getKey())));
            });
        }
        return output;
    }
    private Map<String, Long> signatures(List<PatternDiscoveryInput.Series> values, String primitive) {
        return values.stream().map(value -> signature(value.primitives().get(primitive))).filter(value -> !value.isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(value -> value, java.util.TreeMap::new, java.util.stream.Collectors.counting()));
    }
    private String signature(List<Double> points) { if (points == null || points.size() < 2) return ""; StringBuilder result = new StringBuilder(); for (int index=1; index<points.size(); index++) result.append(points.get(index) > points.get(index-1) ? 'U' : points.get(index) < points.get(index-1) ? 'D' : 'F'); return result.toString(); }
    private List<String> matchingIds(List<PatternDiscoveryInput.Series> rows, String primitive, String expected) { return rows.stream().filter(row -> expected.equals(signature(row.primitives().get(primitive)))).map(PatternDiscoveryInput.Series::sampleId).toList(); }
}
