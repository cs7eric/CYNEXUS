package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketStockF10AgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_f10").name("Stock F10 research").module("market")
            .description("Read one selected stock F10 fundamental or disclosure section.")
            .guidanceJson(MarketResearchAgentToolGuidance.STOCK_F10)
            .inputSchemaJson(MarketResearchAgentToolSchemas.STOCK_F10)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("stock disclosure snapshot").source(SOURCE).sourceUrl("/api/market/stock-chart/f10").build();

    public MarketStockF10AgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String symbol = requiredString(call, "symbol");
        return switch (operation(call)) {
            case "valuation" -> query(DEFINITION,
                    () -> client().getValuation(symbol, optionalString(call, "requestId", "200191")), "Read the stock valuation section.");
            case "finance_report" -> query(DEFINITION,
                    () -> client().getFinanceReport(symbol, optionalString(call, "reportType", "zcfzb")), "Read the stock financial report section.");
            case "business_composition" -> query(DEFINITION,
                    () -> client().getBusinessComposition(symbol, date(call, "reportDate")), "Read the stock business composition section.");
            case "profit_forecast" -> query(DEFINITION,
                    () -> client().getProfitForecast(symbol), "Read the stock profit forecast section.");
            case "announcements" -> query(DEFINITION,
                    () -> client().getAnnouncements(symbol), "Read the stock announcements section.");
            case "news" -> query(DEFINITION,
                    () -> client().getNews(symbol), "Read the stock news section.");
            case "roadshows" -> query(DEFINITION,
                    () -> client().getRoadshows(symbol), "Read the stock roadshows section.");
            case "company_news" -> query(DEFINITION,
                    () -> client().getCompanyNews(symbol, optionalString(call, "section", "gsyj")), "Read the stock company-news section.");
            default -> throw new IllegalArgumentException("Unsupported stock F10 operation: " + operation(call));
        };
    }
}
