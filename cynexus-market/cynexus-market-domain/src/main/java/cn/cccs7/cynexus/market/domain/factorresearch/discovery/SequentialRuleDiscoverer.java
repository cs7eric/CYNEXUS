package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Extracts a learned low-to-recovery sequence without naming a fixed indicator or threshold. */
public final class SequentialRuleDiscoverer implements PatternDiscoverer {
    @Override public String method() { return "SEQUENTIAL_RULE"; }
    @Override public List<PatternDiscoveryCandidate> discover(PatternDiscoveryInput input) {
        List<PatternDiscoveryCandidate> output = new ArrayList<>();
        for (String primitive : input.events().stream().flatMap(value -> value.primitives().keySet().stream()).distinct().sorted().toList()) {
            int eventSupport = support(input.events(), primitive); int controlSupport = support(input.controls(), primitive);
            if (eventSupport <= controlSupport || eventSupport == 0) continue;
            output.add(new PatternDiscoveryCandidate("SR_" + primitive.replace('.', '_').toUpperCase(), method(), MarketFactorResearchBO.DiscoveryScope.COMMON,
                    Map.of("primitive", primitive, "sequence", "LOCAL_LOW_THEN_RECOVERY"), Map.of("eventSupportRate", rate(eventSupport, input.events().size()), "controlSupportRate", rate(controlSupport, input.controls().size())), eventSupport, controlSupport,
                    supportingIds(input.events(), primitive), supportingIds(input.controls(), primitive)));
        }
        return output;
    }
    private int support(List<PatternDiscoveryInput.Series> rows, String primitive) { return (int) rows.stream().filter(row -> qualifies(row.primitives().get(primitive))).count(); }
    private List<String> supportingIds(List<PatternDiscoveryInput.Series> rows, String primitive) { return rows.stream().filter(row -> qualifies(row.primitives().get(primitive))).map(PatternDiscoveryInput.Series::sampleId).toList(); }
    private boolean qualifies(List<Double> values) { if (values == null || values.size() < 3) return false; int lowest = 0; for (int index=1; index<values.size()-1; index++) if (values.get(index) < values.get(lowest)) lowest=index; return lowest > 0 && lowest < values.size()-1 && values.get(values.size()-1) > values.get(lowest); }
    private double rate(int numerator, int denominator) { return denominator == 0 ? 0d : (double) numerator / denominator; }
}
