package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

abstract class AbstractMarketAgentToolHandler implements AgentToolHandler {

    protected static final String SOURCE = "Market API";

    private final MarketDataGateway marketDataGateway;
    private final ObjectMapper objectMapper;

    protected AbstractMarketAgentToolHandler(MarketDataGateway marketDataGateway, ObjectMapper objectMapper) {
        this.marketDataGateway = marketDataGateway;
        this.objectMapper = objectMapper;
    }

    protected MarketDataGateway marketDataGateway() {
        return marketDataGateway;
    }

    protected ObjectMapper objectMapper() {
        return objectMapper;
    }

    protected String stringArgument(AgentToolCall call, String name) {
        if (call == null || call.getArguments() == null) return "";
        Object value = call.getArguments().get(name);
        return value == null ? "" : String.valueOf(value).trim();
    }

    protected String requiredString(AgentToolCall call, String name) {
        String value = stringArgument(call, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Required argument is missing: " + name);
        }
        return value;
    }

    protected String optionalString(AgentToolCall call, String name, String fallback) {
        String value = stringArgument(call, name);
        return value.isBlank() ? fallback : value;
    }

    protected String dateArgument(AgentToolCall call, AgentExecutionContext context) {
        String value = stringArgument(call, "tradeDate");
        return value.isBlank() && context != null && context.getTradeDate() != null
                ? context.getTradeDate().toString() : value;
    }

    protected Integer integerArgument(AgentToolCall call, String name, int fallback) {
        Object value = call == null || call.getArguments() == null ? null : call.getArguments().get(name);
        int result = fallback;
        if (value instanceof Number number) {
            result = number.intValue();
        }
        if (value != null) {
            try {
                result = Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("Argument " + name + " must be an integer");
            }
        }
        if (result < 1 || result > 50) {
            throw new IllegalArgumentException("Argument " + name + " must be between 1 and 50");
        }
        return result;
    }

    protected AgentToolResult success(AgentToolDefinition definition, Object value, String summary) {
        try {
            String outputJson = objectMapper.writeValueAsString(value);
            JsonNode root = objectMapper.readTree(outputJson);
            String dataDate = firstText(root, "tradeDate", "tradingDate", "date", "snapshotDate");
            return AgentToolResult.success(definition.getCode(), outputJson, summary, dataDate, definition.getSource(), List.of());
        } catch (JsonProcessingException exception) {
            return AgentToolResult.failure(definition.getCode(), "The tool returned a value that could not be serialized.", definition.getSource(), "TOOL_OUTPUT_SERIALIZATION_FAILED");
        }
    }

    protected AgentToolResult empty(AgentToolDefinition definition, String summary) {
        return AgentToolResult.failure(definition.getCode(), summary, definition.getSource(), "NO_DATA");
    }

    private String firstText(JsonNode root, String... names) {
        if (root == null) return null;
        for (String name : names) {
            String value = textValue(root.get(name));
            if (value != null) return value;
        }
        if (root.isObject()) {
            JsonNode items = root.get("items");
            if (items != null && items.isArray() && !items.isEmpty()) {
                return firstText(items.get(0), names);
            }
        }
        return null;
    }

    private String textValue(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (value.isTextual() || value.isNumber()) {
            String text = value.asText("").trim();
            return text.isBlank() ? null : text;
        }
        if (value.isArray() && value.size() == 3 && value.get(0).canConvertToInt()
                && value.get(1).canConvertToInt() && value.get(2).canConvertToInt()) {
            try {
                return LocalDate.of(value.get(0).intValue(), value.get(1).intValue(), value.get(2).intValue())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }
}
