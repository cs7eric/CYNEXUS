package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketIndustryAnalysisAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.industry_analysis").name("Industry analysis").module("market")
            .description("Read bounded industry application rankings and fund-flow analysis.")
            .guidanceJson(MarketResearchAgentToolGuidance.INDUSTRY)
            .inputSchemaJson(MarketResearchAgentToolSchemas.INDUSTRY)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("industry analysis snapshot").source(SOURCE).sourceUrl("/api/market/industry-application").build();

    public MarketIndustryAnalysisAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        return switch (operation(call)) {
            case "targets" -> query(DEFINITION, client()::getIndustryTargets, "Read configured industry application targets.");
            case "target_codes" -> query(DEFINITION,
                    () -> client().getIndustryTargetCodes(optionalString(call, "kind", null)), "Read available industry target codes.");
            case "overview" -> query(DEFINITION, () -> client().getIndustryOverview(
                    optionalString(call, "sortBy", null), Boolean.parseBoolean(optionalString(call, "ascending", "false")),
                    integer(call, "count", 100, 1, 100)), "Read the industry application overview.");
            case "kline" -> query(DEFINITION, () -> client().getIndustryKline(
                    optionalString(call, "targetType", "stock"), requiredString(call, "symbol"),
                    optionalString(call, "period", "day"), integer(call, "klineCount", 120, 1, 240)), "Read industry application kline data.");
            case "result" -> query(DEFINITION,
                    () -> client().getIndustryResult(requiredString(call, "targetId")), "Read one industry application result.");
            case "fund_flow" -> query(DEFINITION, () -> client().getIndustryFundFlow(
                    date(call, "tradeDate"), optionalInteger(call, "top", 50, 1, 100)), "Read the industry fund-flow snapshot.");
            case "fund_flow_history" -> query(DEFINITION, () -> client().getIndustryFundFlowHistory(
                    date(call, "maxTradeDate"), integer(call, "days", 30, 1, 365), optionalInteger(call, "top", 50, 1, 100)),
                    "Read industry fund-flow history.");
            case "tdx_fund_flow" -> query(DEFINITION, () -> client().getIndustryTdxFundFlow(
                    optionalString(call, "targetType", "stock"), requiredString(call, "symbol"), date(call, "tradeDate"),
                    integer(call, "days", 30, 1, 365)), "Read target-level TDX fund-flow history.");
            default -> throw new IllegalArgumentException("Unsupported industry analysis operation: " + operation(call));
        };
    }

    private Integer optionalInteger(AgentToolCall call, String key, int fallback, int min, int max) {
        return integer(call, key, fallback, min, max);
    }
}
