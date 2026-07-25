package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketSkillDataAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.skill_data").name("A-share skill data").module("market")
            .description("Discover and read registered, bounded A-share skill datasets.")
            .guidanceJson(MarketResearchAgentToolGuidance.SKILL_DATA)
            .inputSchemaJson(MarketResearchAgentToolSchemas.SKILL_DATA)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("skill dataset snapshot").source(SOURCE).sourceUrl("/api/market/a-stock-skill").build();

    public MarketSkillDataAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        return switch (operation(call)) {
            case "datasets" -> query(DEFINITION, client()::getSkillDatasets, "Read available A-share skill datasets.");
            case "rows" -> query(DEFINITION, () -> client().getSkillDatasetRows(
                    requiredString(call, "datasetCode"), integer(call, "limit", 50, 1, 200),
                    optionalString(call, "code", null), optionalString(call, "source", null)),
                    "Read rows from the selected A-share skill dataset.");
            default -> throw new IllegalArgumentException("Unsupported skill-data operation: " + operation(call));
        };
    }
}
