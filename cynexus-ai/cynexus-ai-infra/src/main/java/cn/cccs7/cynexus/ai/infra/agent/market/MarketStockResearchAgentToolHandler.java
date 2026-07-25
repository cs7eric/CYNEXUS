package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketStockResearchAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_research").name("Stock research").module("market")
            .description("Read bounded stock lookup, metadata, sector relations, fund flow, intraday activity, or auction data.")
            .guidanceJson(MarketResearchAgentToolGuidance.STOCK_RESEARCH)
            .inputSchemaJson(MarketResearchAgentToolSchemas.STOCK_RESEARCH)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("stock research snapshot").source(SOURCE).sourceUrl("/api/market/stock-chart").build();

    public MarketStockResearchAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        return switch (operation(call)) {
            case "search" -> query(DEFINITION,
                    () -> client().searchStocks(requiredString(call, "keyword")), "Searched stocks and instruments.");
            case "metadata" -> query(DEFINITION,
                    () -> client().getStockMeta(requiredString(call, "symbol")), "Read stock metadata.");
            case "sectors" -> query(DEFINITION,
                    () -> client().getStockSectors(requiredString(call, "symbol")), "Read stock sector relations.");
            case "sector_stocks" -> query(DEFINITION,
                    () -> client().getSectorStocks(optionalString(call, "sectorId", null),
                            optionalInteger(call, "categoryRaw", 0, 0, Integer.MAX_VALUE),
                            optionalString(call, "topicId", null), optionalString(call, "sectorName", null),
                            integer(call, "limit", 100, 1, 200)), "Read stocks related to a sector.");
            case "fund_flow" -> query(DEFINITION,
                    () -> client().getStockFundFlow(requiredString(call, "symbol")), "Read stock fund-flow data.");
            case "intraday" -> query(DEFINITION, () -> client().getStockIntraday(
                    optionalString(call, "targetType", "stock"), requiredString(call, "symbol"),
                    optionalString(call, "name", null), optionalString(call, "adjust", "qfq"),
                    date(call, "tradeDate"), optionalString(call, "periods", "1m,5m,15m,30m")),
                    "Read stock intraday data.");
            case "auction" -> query(DEFINITION,
                    () -> client().getStockAuction(requiredString(call, "symbol"), date(call, "tradeDate")),
                    "Read the stock auction snapshot.");
            case "auction_history" -> query(DEFINITION,
                    () -> client().getStockAuctionHistory(requiredString(call, "symbol"), integer(call, "limit", 30, 1, 120)),
                    "Read stock auction history.");
            default -> throw new IllegalArgumentException("Unsupported stock research operation: " + operation(call));
        };
    }

    private Integer optionalInteger(AgentToolCall call, String key, int fallback, int min, int max) {
        Object raw = arguments(call).get(key);
        if (raw == null || String.valueOf(raw).isBlank()) return null;
        return integer(call, key, fallback, min, max);
    }
}
