package cn.cccs7.cynexus.ai.infra.agent.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.FuturesPositionDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingIndustryHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketEtfDTO;
import cn.cccs7.cynexus.market.api.dto.MarketIndicatorDTO;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NewMarketAgentToolHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void indicatorDispatchesLatestAndIndustryDetail() {
        MarketResearchFeignClient client = mock(MarketResearchFeignClient.class);
        when(client.getMarketIndicatorLatest(null, "all", "all", 20))
                .thenReturn(Result.success(MarketIndicatorDTO.Overview.builder().snapshotCount(1).build()));
        when(client.getMarketIndicatorIndustryDetail("BK001", null, 0, 50))
                .thenReturn(Result.success(MarketIndicatorDTO.IndustryDetail.builder().industryCode("BK001").build()));
        MarketIndicatorAgentToolHandler handler = new MarketIndicatorAgentToolHandler(client, objectMapper);

        var latest = handler.execute(call(handler.definition().getCode(), Map.of("operation", "latest", "limit", 20)), null);
        var detail = handler.execute(call(handler.definition().getCode(),
                Map.of("operation", "industry_detail", "industryCode", "BK001")), null);

        assertThat(latest.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(detail.getStatus()).isEqualTo("SUCCEEDED");
        verify(client).getMarketIndicatorLatest(null, "all", "all", 20);
        verify(client).getMarketIndicatorIndustryDetail("BK001", null, 0, 50);
    }

    @Test
    void indicatorRejectsOutOfRangeIndustryPage() {
        MarketIndicatorAgentToolHandler handler = new MarketIndicatorAgentToolHandler(
                mock(MarketResearchFeignClient.class), objectMapper);

        assertThatIllegalArgumentException().isThrownBy(() -> handler.execute(call(handler.definition().getCode(),
                Map.of("operation", "industry_detail", "industryCode", "BK001", "page", 101)), null))
                .withMessageContaining("page");
    }

    @Test
    void etfUsesBoundedDefaultsAndExposesReadOnlyDefinition() {
        MarketResearchFeignClient client = mock(MarketResearchFeignClient.class);
        when(client.getMarketEtf(10, 180)).thenReturn(Result.success(MarketEtfDTO.builder().ok(true).build()));
        MarketEtfAgentToolHandler handler = new MarketEtfAgentToolHandler(client, objectMapper);

        var result = handler.execute(call(handler.definition().getCode(), Map.of()), null);

        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(handler.definition().isReadOnly()).isTrue();
        assertThat(handler.definition().isRequiresConfirmation()).isFalse();
        verify(client).getMarketEtf(10, 180);
    }

    @Test
    void futuresPositionDispatchesCoverageAndRequiresPathArgumentsForHistory() {
        MarketResearchFeignClient client = mock(MarketResearchFeignClient.class);
        when(client.getFuturesPositionCoverage(null))
                .thenReturn(Result.success(FuturesPositionDTO.Coverage.builder().build()));
        MarketFuturesPositionAgentToolHandler handler = new MarketFuturesPositionAgentToolHandler(client, objectMapper);

        var result = handler.execute(call(handler.definition().getCode(), Map.of("operation", "coverage")), null);

        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
        verify(client).getFuturesPositionCoverage(null);
        assertThatIllegalArgumentException().isThrownBy(() -> handler.execute(call(handler.definition().getCode(),
                Map.of("operation", "history")), null)).withMessageContaining("exchangeCode");
    }

    @Test
    void marginIndustryHistoryUsesTheNewReadOperation() {
        MarketResearchFeignClient client = mock(MarketResearchFeignClient.class);
        when(client.getMarginIndustryHistory("BK001", null, 60))
                .thenReturn(Result.success(MarginTradingIndustryHistoryDTO.builder().industryCode("BK001").build()));
        MarketMarginTradingAgentToolHandler handler = new MarketMarginTradingAgentToolHandler(client, objectMapper);

        var result = handler.execute(call(handler.definition().getCode(),
                Map.of("operation", "industry_history", "industryCode", "BK001")), null);

        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
        verify(client).getMarginIndustryHistory("BK001", null, 60);
    }

    @Test
    void futuresAiFailureUsesStableCodeWithoutLeakingProviderMessage() {
        FuturesPositionAiDomainService service = mock(FuturesPositionAiDomainService.class);
        when(service.analyze(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("provider secret"));
        FuturesPositionAiAgentToolHandler handler = new FuturesPositionAiAgentToolHandler(service, objectMapper);

        var result = handler.execute(call(handler.definition().getCode(), Map.of(
                "varietyCode", "IF",
                "current", Map.of("instrumentCode", "IF2608"),
                "history", List.of())), null);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getWarnings()).containsExactly("FUTURES_POSITION_ANALYSIS_FAILED");
        assertThat(result.getWarnings()).doesNotContain("provider secret");
    }

    private AgentToolCall call(String code, Map<String, Object> arguments) {
        return AgentToolCall.builder().toolCode(code).arguments(arguments).build();
    }
}
