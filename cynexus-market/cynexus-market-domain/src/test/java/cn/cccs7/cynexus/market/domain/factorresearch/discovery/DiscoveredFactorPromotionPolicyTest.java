package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveredFactorPromotionPolicyTest {
    private final DiscoveredFactorPromotionPolicy policy = new DiscoveredFactorPromotionPolicy();

    @Test
    void insufficientPopulationRemainsExploratoryRatherThanScreenable() {
        assertEquals(DiscoveredFactorPromotionPolicy.Verdict.EXPLORATORY, policy.evaluate(
                new DiscoveredFactorPromotionPolicy.Evaluation(59, 180, .9, .9, .01, .2, .2, true, true, true)));
    }

    @Test
    void promotionRequiresChronologicalRegimeAndOutcomeStability() {
        DiscoveredFactorPromotionPolicy.Evaluation result = new DiscoveredFactorPromotionPolicy.Evaluation(
                80, 240, .9, .85, .01, .2, .1, true, true, true);
        assertEquals(DiscoveredFactorPromotionPolicy.Verdict.CANDIDATE, policy.evaluate(result));
        assertTrue(policy.mayPromote(result));
    }
}
