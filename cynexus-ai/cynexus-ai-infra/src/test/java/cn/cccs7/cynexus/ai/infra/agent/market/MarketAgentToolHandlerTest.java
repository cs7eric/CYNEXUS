package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class MarketAgentToolHandlerTest {

    @Test
    void exposesCompleteStockDailyDefinitionAndValidatesSymbol() {
        MarketStockDailyFeatureAgentToolHandler handler = new MarketStockDailyFeatureAgentToolHandler(
                mock(MarketDataGateway.class), new ObjectMapper());

        assertThat(handler.definition().getCode()).isEqualTo("market.stock_daily_feature");
        assertThat(handler.definition().getInputSchemaJson()).contains("symbol");
        assertThatIllegalArgumentException().isThrownBy(() -> handler.execute(
                AgentToolCall.builder().toolCode(handler.definition().getCode()).build(), null))
                .withMessageContaining("symbol");
    }
}
