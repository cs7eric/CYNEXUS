package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketMarginTradingAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.margin_trading").name("Margin trading research").module("market")
            .description("Read bounded margin-trading summaries, rankings, and stock history.")
            .guidanceJson(MarketResearchAgentToolGuidance.MARGIN)
            .inputSchemaJson(MarketResearchAgentToolSchemas.MARGIN)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("margin-trading snapshot").source(SOURCE).sourceUrl("/api/market/margin").build();

    public MarketMarginTradingAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String sort = optionalString(call, "sort", "margin_balance");
        String sortDir = optionalString(call, "sortDir", "desc");
        return switch (operation(call)) {
            case "latest" -> query(DEFINITION, () -> client().getMarginLatest(date(call, "tradeDate"),
                    integer(call, "top", 20, 1, 50)), "Read the latest margin-trading summary.");
            case "summary_history" -> query(DEFINITION, () -> client().getMarginSummaryHistory(
                    date(call, "start"), date(call, "end")), "Read margin-trading summary history.");
            case "stocks" -> query(DEFINITION, () -> client().getMarginStocks(
                    date(call, "tradeDate"), optionalString(call, "exchange", null), optionalString(call, "industryCode", null),
                    sort, sortDir, optionalInteger(call, "page", 1, 1, 100), optionalInteger(call, "pageSize", 50, 1, 100),
                    integer(call, "limit", 100, 1, 100)), "Read margin-trading stock rankings.");
            case "stock_history" -> query(DEFINITION, () -> client().getMarginStockHistory(
                    requiredString(call, "stockCode"), integer(call, "limit", 60, 1, 120)), "Read margin-trading history for one stock.");
            case "industries" -> query(DEFINITION, () -> client().getMarginIndustries(
                    date(call, "tradeDate"), sort, sortDir, optionalInteger(call, "page", 1, 1, 100),
                    optionalInteger(call, "pageSize", 50, 1, 100)), "Read margin-trading industry rankings.");
            case "industry_history" -> query(DEFINITION, () -> client().getMarginIndustryHistory(
                    requiredString(call, "industryCode"), date(call, "tradeDate"),
                    integer(call, "limit", 60, 1, 120)), "Read margin-trading history for one industry.");
            default -> throw new IllegalArgumentException("Unsupported margin-trading operation: " + operation(call));
        };
    }

    private Integer optionalInteger(AgentToolCall call, String key, int fallback, int min, int max) {
        return integer(call, key, fallback, min, max);
    }
}
