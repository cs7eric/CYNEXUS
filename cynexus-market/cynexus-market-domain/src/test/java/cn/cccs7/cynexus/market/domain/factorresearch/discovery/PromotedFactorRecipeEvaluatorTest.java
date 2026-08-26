package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromotedFactorRecipeEvaluatorTest {
    private final PromotedFactorRecipeEvaluator evaluator = new PromotedFactorRecipeEvaluator();

    @Test
    void evaluatesEachFirstVersionRecipeAgainstMaterializedInputs() {
        Map<String, List<Double>> sequences = Map.of("stock.volume", List.of(1d, 0.5d, 3d), "stock.close", List.of(1d, 2d, 1d));
        assertTrue(evaluator.matches(Map.of("primitive", "stock.volume", "operator", "ABS_DELTA_GTE", "changeOffset", 2),
                Map.of("eventMeanAbsDelta", 2d), sequences, Map.of()));
        assertTrue(evaluator.matches(Map.of("primitive", "stock.close", "shapeSignature", "UD"), Map.of(), sequences, Map.of()));
        assertTrue(evaluator.matches(Map.of("primitive", "stock.volume", "sequence", "LOCAL_LOW_THEN_RECOVERY"), Map.of(), sequences, Map.of()));
        assertTrue(evaluator.matches(Map.of("label", "marketRegime", "value", "RISK_ON"), Map.of(), sequences, Map.of("marketRegime", "RISK_ON")));
    }

    @Test
    void rejectsIncompleteOrMismatchedEvidence() {
        assertFalse(evaluator.matches(Map.of("primitive", "stock.volume", "operator", "ABS_DELTA_GTE", "changeOffset", 2),
                Map.of(), Map.of("stock.volume", List.of(1d, 4d)), Map.of()));
        assertFalse(evaluator.matches(Map.of("label", "marketRegime", "value", "RISK_ON"), Map.of(), Map.of(), Map.of("marketRegime", "RISK_OFF")));
    }
}
