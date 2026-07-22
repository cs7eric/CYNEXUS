package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void normalizesJacksonLocalDateArrayIntoToolDataDate() throws Exception {
        MarketDataGateway gateway = mock(MarketDataGateway.class);
        when(gateway.getMarketOverview(null)).thenReturn(MarketOverviewDTO.builder()
                .tradeDate(java.time.LocalDate.of(2026, 7, 22)).build());
        MarketOverviewAgentToolHandler handler = new MarketOverviewAgentToolHandler(gateway,
                new ObjectMapper().registerModule(new JavaTimeModule()));

        var result = handler.execute(AgentToolCall.builder().toolCode(handler.definition().getCode()).build(), null);

        assertThat(result.getDataDate()).isEqualTo("2026-07-22");
        assertThat(result.getOutputJson()).contains("2026");
    }

    @Test
    void derivesFirstDateFromHistoryItemsWhenEnvelopeHasNoDate() {
        MarketDataGateway gateway = mock(MarketDataGateway.class);
        when(gateway.getMarketOverviewHistory("2026-07-21", "2026-07-22"))
                .thenReturn(cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO.builder()
                        .items(java.util.List.of(cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO.ItemDTO.builder()
                                .tradeDate(java.time.LocalDate.of(2026, 7, 21)).build()))
                        .build());
        MarketOverviewHistoryAgentToolHandler handler = new MarketOverviewHistoryAgentToolHandler(gateway,
                new ObjectMapper().registerModule(new JavaTimeModule()));

        var result = handler.execute(AgentToolCall.builder().toolCode(handler.definition().getCode())
                .arguments(java.util.Map.of("start", "2026-07-21", "end", "2026-07-22")).build(), null);

        assertThat(result.getDataDate()).isEqualTo("2026-07-21");
    }
}
