package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternDiscoveryEngineTest {
    @Test
    void runsAllFourDeterministicDiscoverersOnRawEventAndControlSequences() {
        PatternDiscoveryEngine engine = new PatternDiscoveryEngine(List.of(new ChangePointPatternDiscoverer(),
                new SequenceShapeClusterDiscoverer(), new SequentialRuleDiscoverer(), new InterpretableSubgroupDiscoverer()));
        PatternDiscoveryInput input = new PatternDiscoveryInput(List.of(
                series("e1", List.of(1d, .2d, 3d), "industry", "BANK"), series("e2", List.of(1d, .1d, 4d), "industry", "BANK"),
                series("e3", List.of(1d, .3d, 3.5d), "industry", "BANK")), List.of(
                series("c1", List.of(1d, .9d, 1.1d), "industry", "TECH"), series("c2", List.of(1d, .8d, 1.2d), "industry", "TECH")));
        List<PatternDiscoveryCandidate> candidates = engine.discover(input);
        assertTrue(candidates.stream().anyMatch(value -> value.method().equals("CHANGE_POINT")));
        assertTrue(candidates.stream().anyMatch(value -> value.method().equals("SEQUENCE_SHAPE_CLUSTER")));
        assertTrue(candidates.stream().anyMatch(value -> value.method().equals("SEQUENTIAL_RULE")));
        assertTrue(candidates.stream().anyMatch(value -> value.method().equals("INTERPRETABLE_SUBGROUP")));
        assertTrue(candidates.stream().allMatch(value -> value.eventSupport() == value.eventSampleIds().size()
                && value.controlSupport() == value.controlSampleIds().size()));
        assertEquals(candidates.stream().map(PatternDiscoveryCandidate::method).sorted().toList(), candidates.stream().map(PatternDiscoveryCandidate::method).toList());
    }

    private PatternDiscoveryInput.Series series(String id, List<Double> values, String label, String labelValue) {
        return new PatternDiscoveryInput.Series(id, Map.of("stock.volume", values), Map.of(label, labelValue));
    }
}
