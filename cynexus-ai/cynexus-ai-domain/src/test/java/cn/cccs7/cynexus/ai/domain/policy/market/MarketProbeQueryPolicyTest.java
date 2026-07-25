package cn.cccs7.cynexus.ai.domain.policy.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MarketProbeQueryPolicyTest {

    private final MarketProbeQueryPolicy policy = new MarketProbeQueryPolicy();

    @Test
    void normalizesProbeInputs() {
        assertThat(policy.requireTradeDate(" 2026-07-23 ")).isEqualTo("2026-07-23");
        assertThat(policy.requireSymbol(" 600000 ")).isEqualTo("600000");
        assertThat(policy.normalizeTopN(null)).isEqualTo(10);
        assertThat(policy.normalizeTopN(-1)).isEqualTo(1);
        assertThat(policy.normalizeTopN(150)).isEqualTo(100);
    }

    @Test
    void rejectsRequiredProbeInputs() {
        assertThatThrownBy(() -> policy.requireTradeDate(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tradeDate is required");
        assertThatThrownBy(() -> policy.requireSymbol(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("symbol is required");
    }
}
