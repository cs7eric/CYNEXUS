package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketStockInformationAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_information").name("Stock information").module("market")
            .description("Read a stock information feed or one already indexed item content or analysis result.")
            .guidanceJson(MarketResearchAgentToolGuidance.STOCK_INFORMATION)
            .inputSchemaJson(MarketResearchAgentToolSchemas.STOCK_INFORMATION)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("stock information snapshot").source(SOURCE).sourceUrl("/api/market/stock-information").build();

    public MarketStockInformationAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String symbol = requiredString(call, "symbol");
        return switch (operation(call)) {
            case "feed" -> query(DEFINITION,
                    () -> client().getStockInformation(symbol, integer(call, "limit", 50, 1, 100)),
                    "Read the stock information feed.");
            case "content" -> query(DEFINITION,
                    () -> client().getStockInformationContent(symbol, requiredString(call, "kind"),
                            requiredString(call, "source"), requiredString(call, "itemId"), false),
                    "Read already indexed stock information content.");
            case "analysis" -> query(DEFINITION,
                    () -> client().getStockInformationAnalysis(symbol, requiredString(call, "kind"),
                            requiredString(call, "source"), requiredString(call, "itemId")),
                    "Read the existing stock information analysis result.");
            default -> throw new IllegalArgumentException("Unsupported stock information operation: " + operation(call));
        };
    }
}
