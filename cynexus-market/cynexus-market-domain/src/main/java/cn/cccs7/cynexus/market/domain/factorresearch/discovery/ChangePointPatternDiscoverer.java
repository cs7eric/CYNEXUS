package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Learns the strongest event/control separation in adjacent raw sequence movement. */
public final class ChangePointPatternDiscoverer implements PatternDiscoverer {
    @Override public String method() { return "CHANGE_POINT"; }
    @Override public List<PatternDiscoveryCandidate> discover(PatternDiscoveryInput input) {
        List<PatternDiscoveryCandidate> result = new ArrayList<>();
        for (String primitive : primitives(input.events())) {
            double[] event = averageDeltas(input.events(), primitive);
            double[] control = averageDeltas(input.controls(), primitive);
            int offset = bestOffset(event, control);
            if (offset < 0 || event[offset] <= control[offset]) continue;
            Map<String, Object> recipe = new LinkedHashMap<>();
            recipe.put("primitive", primitive); recipe.put("changeOffset", offset + 1); recipe.put("operator", "ABS_DELTA_GTE");
            result.add(new PatternDiscoveryCandidate("CP_" + primitive.replace('.', '_').toUpperCase() + "_" + (offset + 1), method(),
                    MarketFactorResearchBO.DiscoveryScope.COMMON, recipe, Map.of("eventMeanAbsDelta", event[offset], "controlMeanAbsDelta", control[offset]),
                    supporting(input.events(), primitive, offset), supporting(input.controls(), primitive, offset),
                    supportingIds(input.events(), primitive, offset), supportingIds(input.controls(), primitive, offset)));
        }
        return result;
    }
    private List<String> primitives(List<PatternDiscoveryInput.Series> series) { return series.stream().flatMap(value -> value.primitives().keySet().stream()).distinct().sorted().toList(); }
    private double[] averageDeltas(List<PatternDiscoveryInput.Series> rows, String primitive) {
        int length = rows.stream().map(row -> row.primitives().get(primitive)).filter(java.util.Objects::nonNull).mapToInt(List::size).min().orElse(0);
        if (length < 2) return new double[0]; double[] values = new double[length - 1]; int count = 0;
        for (PatternDiscoveryInput.Series row : rows) { List<Double> points = row.primitives().get(primitive); if (points == null || points.size() < length) continue;
            count++; for (int index = 1; index < length; index++) values[index - 1] += Math.abs(points.get(index) - points.get(index - 1)); }
        if (count > 0) for (int index = 0; index < values.length; index++) values[index] /= count; return values;
    }
    private int bestOffset(double[] event, double[] control) { int best = -1; double score = 0; for (int index = 0; index < Math.min(event.length, control.length); index++) if (event[index] - control[index] > score) { score = event[index] - control[index]; best = index; } return best; }
    private int supporting(List<PatternDiscoveryInput.Series> rows, String primitive, int offset) { return (int) rows.stream().filter(row -> { List<Double> points = row.primitives().get(primitive); return points != null && points.size() > offset + 1; }).count(); }
    private List<String> supportingIds(List<PatternDiscoveryInput.Series> rows, String primitive, int offset) { return rows.stream().filter(row -> { List<Double> points = row.primitives().get(primitive); return points != null && points.size() > offset + 1; }).map(PatternDiscoveryInput.Series::sampleId).toList(); }
}
