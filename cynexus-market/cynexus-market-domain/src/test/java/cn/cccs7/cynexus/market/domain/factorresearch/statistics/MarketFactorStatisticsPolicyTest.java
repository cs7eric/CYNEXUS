package cn.cccs7.cynexus.market.domain.factorresearch.statistics;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketFactorStatisticsPolicyTest {
    private final MarketFactorStatisticsPolicy policy = new MarketFactorStatisticsPolicy();

    @Test
    void reportsSupportCountersLiftAndConservativeDifferenceInterval() {
        MarketFactorStatisticsPolicy.Comparison result = policy.compare(
                new MarketFactorStatisticsPolicy.BinaryEvidence(80, 100),
                new MarketFactorStatisticsPolicy.BinaryEvidence(20, 100));
        assertEquals(0.8d, result.eventSupportRate());
        assertEquals(0.2d, result.controlSupportRate());
        assertEquals(4d, result.lift());
        assertEquals(20, result.eventCounterCount());
        assertTrue(result.effectConfidenceInterval().lower() > 0);
    }

    @Test
    void controlsFalseDiscoveryWithMonotonicBenjaminiHochbergQValues() {
        List<Double> adjusted = policy.benjaminiHochberg(List.of(0.01d, 0.04d, 0.03d));
        assertEquals(List.of(0.03d, 0.04d, 0.04d), adjusted);
    }
}
