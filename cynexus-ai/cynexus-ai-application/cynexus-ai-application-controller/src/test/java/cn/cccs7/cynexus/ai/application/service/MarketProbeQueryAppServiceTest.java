package cn.cccs7.cynexus.ai.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import org.junit.jupiter.api.Test;

class MarketProbeQueryAppServiceTest {

    @Test
    void rejectsMissingTradeDateBeforeCallingGateway() {
        MarketProbeQueryAppService service = new MarketProbeQueryAppService(mock(MarketDataGateway.class));

        assertThatThrownBy(() -> service.marketOverview(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tradeDate is required");
    }

    @Test
    void rejectsMissingSymbolBeforeCallingGateway() {
        MarketProbeQueryAppService service = new MarketProbeQueryAppService(mock(MarketDataGateway.class));

        assertThatThrownBy(() -> service.stockDailyFeature("", "2026-07-23"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("symbol is required");
    }
}
