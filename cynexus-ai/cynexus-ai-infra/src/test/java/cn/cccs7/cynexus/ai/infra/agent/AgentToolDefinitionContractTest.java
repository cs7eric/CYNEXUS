package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.infra.agent.registry.AgentToolRegistry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.application.analysis.ApplicationAnalysisRecent30AgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.application.analysis.ApplicationAnalysisResultAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.finance.FinanceOverviewAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.finance.FinanceTransactionsAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.knowledge.KnowledgeDocumentAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.knowledge.KnowledgeSearchAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.ClickHouseDailyBarsAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketInformationFeedAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketDataFreshnessAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketEtfAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketFuturesPositionAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketIndustryAnalysisAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketIndicatorAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketMarginTradingAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketOverviewAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketOverviewHistoryAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketPulseAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketSectorRankAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketStockChartKlinesAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketStockDailyFeatureAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketStockF10AgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketStockIntradayFeatureAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketStockResearchAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketSentimentAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketSkillDataAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.MarketStockInformationAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.StockSelectionStrategyHealthAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.market.FuturesPositionAiAgentToolHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AgentToolDefinitionContractTest {

    private static final List<Class<? extends AgentToolHandler>> REGISTERED_HANDLERS = List.of(
            ApplicationAnalysisRecent30AgentToolHandler.class,
            ApplicationAnalysisResultAgentToolHandler.class,
            FinanceOverviewAgentToolHandler.class,
            FinanceTransactionsAgentToolHandler.class,
            KnowledgeDocumentAgentToolHandler.class,
            KnowledgeSearchAgentToolHandler.class,
            ClickHouseDailyBarsAgentToolHandler.class,
            MarketInformationFeedAgentToolHandler.class,
            MarketOverviewAgentToolHandler.class,
            MarketOverviewHistoryAgentToolHandler.class,
            MarketDataFreshnessAgentToolHandler.class,
            MarketEtfAgentToolHandler.class,
            MarketFuturesPositionAgentToolHandler.class,
            FuturesPositionAiAgentToolHandler.class,
            MarketIndustryAnalysisAgentToolHandler.class,
            MarketIndicatorAgentToolHandler.class,
            MarketMarginTradingAgentToolHandler.class,
            MarketPulseAgentToolHandler.class,
            MarketSectorRankAgentToolHandler.class,
            MarketStockChartKlinesAgentToolHandler.class,
            MarketStockDailyFeatureAgentToolHandler.class,
            MarketStockIntradayFeatureAgentToolHandler.class,
            MarketStockF10AgentToolHandler.class,
            MarketStockResearchAgentToolHandler.class,
            MarketSentimentAgentToolHandler.class,
            MarketSkillDataAgentToolHandler.class,
            MarketStockInformationAgentToolHandler.class,
            StockSelectionStrategyHealthAgentToolHandler.class);

    @Test
    void everyRegisteredCapabilityHasExecutableMetadataContract() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<AgentToolDefinition> definitions = REGISTERED_HANDLERS.stream()
                .map(this::instantiate)
                .map(AgentToolHandler::definition)
                .toList();

        assertThat(definitions).hasSize(28);
        assertThat(definitions).extracting(AgentToolDefinition::getCode).doesNotHaveDuplicates();
        assertThat(definitions).allSatisfy(definition -> {
            assertThat(definition.getCode()).matches("[a-z0-9_]+\\.[a-z0-9_]+" );
            assertThat(definition.getName()).isNotBlank();
            assertThat(definition.getModule()).isNotBlank();
            assertThat(definition.getDescription()).isNotBlank();
            assertThat(definition.getGuidanceJson()).isNotBlank();
            assertThat(definition.getFreshness()).isNotBlank();
            assertThat(definition.getSource()).isNotBlank();
            assertThat(definition.getSourceUrl()).startsWith("/");
            assertThat(definition.isReadOnly()).isTrue();
            assertThat(definition.isRequiresConfirmation()).isFalse();
            assertSchema(objectMapper, definition.getInputSchemaJson(), "object");
            assertInputFieldDescriptions(objectMapper, definition.getInputSchemaJson());
            assertSchema(objectMapper, definition.getOutputSchemaJson(), null);
            assertGuidance(objectMapper, definition.getGuidanceJson());
        });

        AgentToolRegistry registry = new AgentToolRegistry(REGISTERED_HANDLERS.stream().map(this::instantiate).toList());
        Set<String> catalogCodes = registry.enabledTools().stream().map(AgentToolDefinition::getCode).collect(Collectors.toSet());
        assertThat(catalogCodes).containsExactlyInAnyOrderElementsOf(
                definitions.stream().map(AgentToolDefinition::getCode).collect(Collectors.toSet()));
    }

    private void assertSchema(ObjectMapper objectMapper, String rawSchema, String expectedType) {
        try {
            JsonNode schema = objectMapper.readTree(rawSchema);
            assertThat(schema.isObject()).isTrue();
            assertThat(schema.path("type").asText()).isNotBlank();
            if (expectedType != null) assertThat(schema.path("type").asText()).isEqualTo(expectedType);
        } catch (Exception exception) {
            throw new AssertionError("Agent tool schema must be valid JSON: " + rawSchema, exception);
        }
    }

    private void assertInputFieldDescriptions(ObjectMapper objectMapper, String rawSchema) {
        try {
            JsonNode properties = objectMapper.readTree(rawSchema).path("properties");
            properties.fields().forEachRemaining(field ->
                    assertThat(field.getValue().path("description").asText())
                            .as("description for input field %s", field.getKey())
                            .isNotBlank());
        } catch (Exception exception) {
            throw new AssertionError("Agent tool input schema must describe every field: " + rawSchema, exception);
        }
    }

    private void assertGuidance(ObjectMapper objectMapper, String rawGuidance) {
        try {
            JsonNode guidance = objectMapper.readTree(rawGuidance);
            assertThat(guidance.isObject()).isTrue();
            assertThat(guidance.path("purpose").asText()).isNotBlank();
            assertThat(guidance.path("useWhen").isArray()).isTrue();
            assertThat(guidance.path("parameterNotes").isObject()).isTrue();
        } catch (Exception exception) {
            throw new AssertionError("Agent tool guidance must be valid JSON: " + rawGuidance, exception);
        }
    }

    private AgentToolHandler instantiate(Class<? extends AgentToolHandler> handlerType) {
        try {
            Constructor<?> constructor = handlerType.getConstructors()[0];
            Object[] arguments = Arrays.stream(constructor.getParameterTypes()).map(this::dependency).toArray();
            return (AgentToolHandler) constructor.newInstance(arguments);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to instantiate registered Agent handler " + handlerType.getName(), exception);
        }
    }

    private Object dependency(Class<?> type) {
        if (type == ObjectMapper.class) return new ObjectMapper();
        if (type == String.class) return "http://127.0.0.1:28123";
        return mock(type);
    }
}
