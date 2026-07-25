package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

abstract class AbstractMarketResearchAgentToolHandler implements AgentToolHandler {

    protected static final String SOURCE = "Market Query API";

    private final MarketResearchFeignClient client;
    private final ObjectMapper objectMapper;

    protected AbstractMarketResearchAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    protected MarketResearchFeignClient client() {
        return client;
    }

    protected ObjectMapper objectMapper() {
        return objectMapper;
    }

    protected Map<String, Object> arguments(AgentToolCall call) {
        return call == null || call.getArguments() == null ? Map.of() : call.getArguments();
    }

    protected String requiredString(AgentToolCall call, String key) {
        String value = optionalString(call, key, "");
        if (value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    protected String optionalString(AgentToolCall call, String key, String fallback) {
        Object raw = arguments(call).get(key);
        String value = raw == null ? "" : String.valueOf(raw).trim();
        return value.isBlank() ? fallback : value;
    }

    protected int integer(AgentToolCall call, String key, int fallback, int min, int max) {
        String raw = optionalString(call, key, String.valueOf(fallback));
        try {
            int value = Integer.parseInt(raw);
            if (value < min || value > max) throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
    }

    protected String date(AgentToolCall call, String key) {
        String value = optionalString(call, key, "");
        if (value.isBlank()) return null;
        try {
            return LocalDate.parse(value).toString();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(key + " must be YYYY-MM-DD");
        }
    }

    protected String operation(AgentToolCall call) {
        return requiredString(call, "operation");
    }

    protected AgentToolResult query(AgentToolDefinition definition, Supplier<? extends Result<?>> request, String summary) {
        try {
            Result<?> result = request.get();
            if (result == null || result.isFailed() || result.getData() == null) {
                String message = result == null || result.getMessage() == null || result.getMessage().isBlank()
                        ? "No market data was available for this request." : result.getMessage();
                return AgentToolResult.failure(definition.getCode(), message, definition.getSource(), "NO_DATA");
            }
            String output = objectMapper.writeValueAsString(result.getData());
            return AgentToolResult.success(definition.getCode(), output, summary, dataDate(output), definition.getSource(), List.of());
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            return AgentToolResult.failure(definition.getCode(), "The market query failed before returning data.",
                    definition.getSource(), exception.getMessage() == null ? "MARKET_QUERY_FAILED" : exception.getMessage());
        }
    }

    private String dataDate(String output) {
        try {
            JsonNode root = objectMapper.readTree(output);
            return firstText(root, "tradeDate", "tradingDate", "date", "startDate", "endDate", "updatedAt", "capturedAt");
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstText(JsonNode root, String... names) {
        if (root == null) return null;
        for (String name : names) {
            JsonNode value = root.get(name);
            if (value != null && !value.isNull() && value.isValueNode()) {
                String text = value.asText("").trim();
                if (!text.isBlank()) return text;
            }
        }
        if (root.isObject()) {
            JsonNode items = root.get("items");
            if (items != null && items.isArray() && !items.isEmpty()) return firstText(items.get(0), names);
        }
        return null;
    }
}
