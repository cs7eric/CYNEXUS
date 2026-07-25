package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketSentimentAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.sentiment").name("Market sentiment").module("market")
            .description("Read one bounded market sentiment metric for an available trading session.")
            .guidanceJson(MarketResearchAgentToolGuidance.SENTIMENT)
            .inputSchemaJson(MarketResearchAgentToolSchemas.SENTIMENT)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("market sentiment snapshot").source(SOURCE).sourceUrl("/api/market/sentiment").build();

    public MarketSentimentAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String date = date(call, "date");
        return switch (operation(call)) {
            case "ma_count" -> query(DEFINITION, () -> client().getMaCount(date), "Read the market advance/decline sentiment metric.");
            case "risk_appetite" -> query(DEFINITION, () -> client().getRiskAppetite(date), "Read the market risk-appetite metric.");
            case "limit_emotion" -> query(DEFINITION, () -> client().getLimitEmotion(date), "Read the market limit-up/limit-down emotion metric.");
            case "volatility" -> query(DEFINITION, () -> client().getVolatility(date), "Read the market volatility sentiment metric.");
            case "turnover" -> query(DEFINITION, () -> client().getTurnover(date), "Read the market turnover activity metric.");
            case "style_risk" -> query(DEFINITION, () -> client().getStyleRisk(date), "Read the market style risk-appetite metric.");
            case "profit_effect" -> query(DEFINITION, () -> client().getProfitEffect(date), "Read the market profit-effect metric.");
            case "sector_breadth" -> query(DEFINITION, () -> client().getSectorBreadth(date), "Read the market sector-breadth metric.");
            case "msi_index" -> query(DEFINITION, () -> client().getMsiIndex(date), "Read the market sentiment index.");
            case "ma_count_history" -> query(DEFINITION, () -> client().getMaCountHistory(requiredDate(call, "start"), requiredDate(call, "end")), "Read advance/decline history.");
            case "risk_appetite_history" -> query(DEFINITION, () -> client().getRiskAppetiteHistory(requiredDate(call, "start"), requiredDate(call, "end")), "Read risk-appetite history.");
            case "limit_emotion_history" -> query(DEFINITION, () -> client().getLimitEmotionHistory(requiredDate(call, "start"), requiredDate(call, "end")), "Read limit-emotion history.");
            case "volatility_history" -> query(DEFINITION, () -> client().getVolatilityHistory(requiredDate(call, "start"), requiredDate(call, "end")), "Read volatility history.");
            case "turnover_history" -> query(DEFINITION, () -> client().getTurnoverHistory(requiredDate(call, "start"), requiredDate(call, "end")), "Read turnover-activity history.");
            case "style_risk_history" -> query(DEFINITION, () -> client().getStyleRiskHistory(requiredDate(call, "start"), requiredDate(call, "end")), "Read style-risk history.");
            case "profit_effect_history" -> query(DEFINITION, () -> client().getProfitEffectHistory(requiredDate(call, "start"), requiredDate(call, "end")), "Read profit-effect history.");
            case "sector_breadth_history" -> query(DEFINITION, () -> client().getSectorBreadthHistory(integer(call, "days", 30, 1, 120), date(call, "end")), "Read sector-breadth history.");
            case "msi_index_history" -> query(DEFINITION, () -> client().getMsiIndexHistory(requiredDate(call, "start"), requiredDate(call, "end")), "Read market sentiment index history.");
            case "index_daily" -> query(DEFINITION, () -> client().getIndexDaily(requiredString(call, "code"), requiredDate(call, "start"), requiredDate(call, "end")), "Read daily index history.");
            default -> throw new IllegalArgumentException("Unsupported market sentiment operation: " + operation(call));
        };
    }

    private String requiredDate(AgentToolCall call, String key) {
        String value = date(call, key);
        if (value == null) throw new IllegalArgumentException(key + " is required");
        return value;
    }
}
