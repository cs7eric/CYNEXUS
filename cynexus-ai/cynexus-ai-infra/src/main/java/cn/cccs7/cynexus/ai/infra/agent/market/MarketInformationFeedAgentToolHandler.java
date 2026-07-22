package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MarketInformationFeedAgentToolHandler implements cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.information_feed").name("Market information feed").module("market-information")
            .description("Read the persisted market information feed, event impact mappings, and analysis status.")
            .inputSchemaJson("{\"type\":\"object\",\"properties\":{\"page\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":20},\"pageSize\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":50},\"impactIndustryCode\":{\"type\":\"string\"},\"analysisStatus\":{\"type\":\"string\"}},\"additionalProperties\":false}")
            .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).requiresConfirmation(false)
            .freshness("persisted feed").source("Market Information API").sourceUrl("/market/pulse").build();

    private final MarketDataGateway marketDataGateway;
    private final ObjectMapper objectMapper;

    public MarketInformationFeedAgentToolHandler(MarketDataGateway marketDataGateway, ObjectMapper objectMapper) {
        this.marketDataGateway = marketDataGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        Map<String, Object> args = call.getArguments() == null ? Map.of() : call.getArguments();
        Object value = marketDataGateway.getMarketInformationFeed(number(args.get("page"), 1), number(args.get("pageSize"), 20),
                string(args.get("impactIndustryCode")), string(args.get("analysisStatus")));
        if (value == null) return AgentToolResult.failure(DEFINITION.getCode(), "No market information feed was available.", DEFINITION.getSource(), "DATA_UNAVAILABLE");
        try {
            return AgentToolResult.success(DEFINITION.getCode(), objectMapper.writeValueAsString(value),
                    "Read the persisted market information feed.", null, DEFINITION.getSource(), java.util.List.of());
        } catch (Exception exception) {
            return AgentToolResult.failure(DEFINITION.getCode(), "Market information could not be serialized.", DEFINITION.getSource(), "OUTPUT_SERIALIZATION_FAILED");
        }
    }

    private Integer number(Object value, int fallback) {
        try { return value == null ? fallback : Integer.valueOf(String.valueOf(value)); }
        catch (NumberFormatException exception) { return fallback; }
    }

    private String string(Object value) { return value == null ? null : String.valueOf(value); }
}
