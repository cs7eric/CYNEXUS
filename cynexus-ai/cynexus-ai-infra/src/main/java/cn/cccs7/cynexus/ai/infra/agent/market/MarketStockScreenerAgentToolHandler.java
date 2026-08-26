package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MarketStockScreenerAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_screener").name("Daily stock screener").module("market")
            .description("Run a bounded end-of-day A-share screen with explicit daily technical and liquidity conditions.")
            .guidanceJson(MarketResearchAgentToolGuidance.SCREENER)
            .inputSchemaJson(MarketResearchAgentToolSchemas.SCREENER)
            .outputSchemaJson(MarketResearchAgentToolSchemas.SCREENER_OUTPUT)
            .readOnly(true).requiresConfirmation(false).freshness("daily close snapshot")
            .source(SOURCE).sourceUrl("/api/market/screener").version("1.6.0").priority(70)
            .routeGroup("Agent / market").build();

    public MarketStockScreenerAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        return query(DEFINITION, () -> client().getStockScreener(
                date(call, "tradeDate"), optionalString(call, "universe", "ALL").toUpperCase(Locale.ROOT),
                bool(call, "excludeSt", true), bool(call, "excludeChiNext", true), bool(call, "excludeStar", true), decimal(call, "minPrice"), decimal(call, "maxPrice"),
                decimal(call, "minChangePct"), decimal(call, "maxChangePct"), decimal(call, "minAmount"),
                decimal(call, "minMarketCap"), decimal(call, "maxMarketCap"), optionalString(call, "industry", null),
                optionalString(call, "tdxIndustryCode", null), optionalString(call, "thsIndustryCode", null),
                optionalString(call, "conceptSectorId", null), optionalString(call, "styleSectorId", null), optionalString(call, "board", null), decimal(call, "minAmplitudePct"), decimal(call, "maxAmplitudePct"),
                decimal(call, "minReturnPct5d"), decimal(call, "maxReturnPct5d"), decimal(call, "minReturnPct10d"),
                decimal(call, "maxReturnPct10d"), decimal(call, "minReturnPct30d"), decimal(call, "maxReturnPct30d"),
                decimal(call, "minReturnPct60d"), decimal(call, "maxReturnPct60d"), decimal(call, "minReturnPct252d"),
                decimal(call, "maxReturnPct252d"), decimal(call, "minMainNet"), decimal(call, "minMainNet5d"),
                decimal(call, "minTurnoverPct"), decimal(call, "maxTurnoverPct"), decimal(call, "minVolumeRatio"),
                decimal(call, "maxVolumeRatio"), decimal(call, "minPe"), decimal(call, "maxPe"),
                optionalString(call, "sort", "amount"), optionalString(call, "sortDir", "desc"),
                integer(call, "page", 1, 1, 20), integer(call, "pageSize", 20, 1, 50),
                optionalInteger(call, "volumeContractionDays", 2, 10), decimal(call, "minBreakoutVolumeMultiple"), decimal(call, "maxBreakoutVolumeMultiple"),
                decimal(call, "minBullishBodyPct"), decimal(call, "minCloseAboveMa20Pct"), decimal(call, "minMa5AboveMa20Pct"),
                optionalInteger(call, "breakoutHighDays", 5, 20), decimal(call, "maxAtrPct20"),
                optionalInteger(call, "minConsecutiveBullishDays", 1, 10), decimal(call, "minVolumeVsMa5Multiple"),
                decimal(call, "maxVolumeVsMa5Multiple"), optionalString(call, "candlestickPattern", null)
            ), "Run a bounded daily stock screen.");
    }

    private Boolean bool(AgentToolCall call, String key, boolean fallback) {
        Object value = arguments(call).get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IllegalArgumentException(key + " must be a boolean");
    }

    private BigDecimal decimal(AgentToolCall call, String key) {
        Object value = arguments(call).get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be a number", exception);
        }
    }

    private Integer optionalInteger(AgentToolCall call, String key, int minimum, int maximum) {
        Object value = arguments(call).get(key);
        if (value == null) return null;
        if (!(value instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        int parsed = number.intValue();
        if (parsed < minimum || parsed > maximum) throw new IllegalArgumentException(key + " is out of range");
        return parsed;
    }
}
