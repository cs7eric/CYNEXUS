package cn.cccs7.cynexus.ai.infra.agent.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketEventFactorObservatoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketEventReplayDTO;
import cn.cccs7.cynexus.market.api.dto.MarketFactorStudyDTO;
import cn.cccs7.cynexus.market.api.dto.StockSelectionStrategyDTO;
import cn.cccs7.cynexus.market.api.dto.StockSelectionWatchlistDTO;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketEventFactorObservatoryAgentToolHandlerTest {

    @Test
    void readsTheBoundedObservatoryWithDefaults() {
        MarketResearchFeignClient client = mock(MarketResearchFeignClient.class);
        when(client.getMarketEventFactorObservatory(null, "LIMIT_UP", null, 40, null))
                .thenReturn(Result.success(MarketEventFactorObservatoryDTO.builder().ok(true).cohort("LIMIT_UP")
                        .populationCount(0).sampledCount(0).marketFactors(List.of()).events(List.of()).warnings(List.of()).build()));
        MarketEventFactorObservatoryAgentToolHandler handler = new MarketEventFactorObservatoryAgentToolHandler(client, new ObjectMapper());

        var result = handler.execute(AgentToolCall.builder().toolCode(handler.definition().getCode()).arguments(Map.of()).build(), null);

        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(handler.definition().isReadOnly()).isTrue();
        assertThat(handler.definition().getVersion()).isEqualTo("1.1.0");
        assertThat(handler.definition().getOutputSchemaJson())
                .contains("\"screeningHintsDeprecated\":{\"type\":\"boolean\"}")
                .contains("\"criterion\":{\"type\":\"string\"}")
                .doesNotContain("\"marketFactors\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}}");
        verify(client).getMarketEventFactorObservatory(null, "LIMIT_UP", null, 40, null);
    }

    @Test
    void freezesResearchContractFieldNames() throws Exception {
        assertFields(MarketEventFactorObservatoryDTO.class,
                "ok", "tradeDate", "cohort", "populationCount", "sampledCount", "marketFactors", "events",
                "screeningHintsDeprecated", "screeningHints", "warnings");
        assertFields(MarketEventReplayDTO.class,
                "code", "tradeDate", "anchorAt", "status", "source", "dataCutoffAt", "coverage",
                "stockMinuteBars", "marketReplay");
        assertFields(MarketFactorStudyDTO.Response.class,
                "studyId", "status", "eventType", "dataCutoffAt", "featureRegistryVersion",
                "leakageCheckPassed", "eventSampleCount", "controlSampleCount");
        assertFields(StockSelectionStrategyDTO.Version.class,
                "versionNo", "definitionVersion", "sourceFactorStudyId", "sourceFactorSetVersionId");
        assertFields(StockSelectionWatchlistDTO.Item.class,
                "sourceStrategyCandidateId", "status", "invalidationCondition", "nextReviewDate", "version");
    }

    private static void assertFields(Class<?> type, String... expected) {
        Set<String> actual = Set.of(type.getDeclaredFields()).stream().map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.toSet());
        assertThat(actual).contains(expected);
    }
}
