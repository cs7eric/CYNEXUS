package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketPulseAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.pulse").name("Market pulse research").module("market")
            .description("Read market pulse snapshots, returns, heatmaps, style sectors, and constituents.")
            .guidanceJson(MarketResearchAgentToolGuidance.PULSE)
            .inputSchemaJson(MarketResearchAgentToolSchemas.PULSE)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("market pulse snapshot").source(SOURCE).sourceUrl("/api/market/pulse").build();

    public MarketPulseAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        return switch (operation(call)) {
            case "overview_akshare" -> query(DEFINITION, client()::getMarketOverviewAkshare, "Read the latest AkShare market overview.");
            case "overview_archive" -> query(DEFINITION,
                    () -> client().getMarketOverviewArchive(requiredDate(call, "tradingDate")), "Read an archived market overview.");
            case "overview_eltdx" -> query(DEFINITION, client()::getMarketOverviewEltdx, "Read the latest ELTDX market overview.");
            case "overview_history" -> query(DEFINITION,
                    () -> client().getMarketOverviewPulseHistory(optionalString(call, "range", "60d")), "Read market overview history.");
            case "manual_fund_flow" -> query(DEFINITION,
                    () -> client().getManualFundFlow(requiredDate(call, "tradingDate")), "Read manual market fund-flow data.");
            case "sector_snapshot" -> query(DEFINITION, client()::getLatestSectorSnapshot, "Read the latest market sector snapshot.");
            case "sector_history" -> query(DEFINITION,
                    () -> client().getSectorSnapshotHistory(integer(call, "days", 30, 1, 120)), "Read market sector snapshot history.");
            case "index_returns" -> query(DEFINITION,
                    () -> client().getIndexReturns(integer(call, "days", 5, 1, 30)), "Read recent market index returns.");
            case "index_returns_history" -> query(DEFINITION,
                    () -> client().getIndexReturnsHistory(integer(call, "window", 5, 1, 30),
                            requiredDate(call, "start"), requiredDate(call, "end")), "Read historical market index returns.");
            case "index_kline" -> query(DEFINITION,
                    () -> client().getIndexKlineBatch(optionalString(call, "codes", null), requiredDate(call, "date"),
                            optionalString(call, "interval", "1m")), "Read index kline data.");
            case "style_sectors" -> query(DEFINITION, client()::getStyleSectors, "Read available style sectors.");
            case "style_constituents" -> query(DEFINITION,
                    () -> client().getStyleSectorConstituents(requiredString(call, "name")), "Read style-sector constituents.");
            case "industry_constituents" -> query(DEFINITION,
                    () -> client().getIndustryConstituents(requiredString(call, "code")), "Read industry constituents.");
            case "heatmap" -> query(DEFINITION,
                    () -> client().getHeatmap(optionalString(call, "kind", "industries"), integer(call, "topN", 100, 1, 100)),
                    "Read the market heatmap.");
            case "limit_emotion" -> query(DEFINITION, client()::getPulseLimitEmotion, "Read the latest market limit emotion snapshot.");
            default -> throw new IllegalArgumentException("Unsupported market pulse operation: " + operation(call));
        };
    }

    private String requiredDate(AgentToolCall call, String key) {
        String value = date(call, key);
        if (value == null) throw new IllegalArgumentException(key + " is required");
        return value;
    }
}
