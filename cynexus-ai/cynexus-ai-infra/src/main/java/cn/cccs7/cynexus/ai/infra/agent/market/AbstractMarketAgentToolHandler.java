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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

abstract class AbstractMarketAgentToolHandler implements AgentToolHandler {

    protected static final String SOURCE = "Market API";
    protected static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    protected static final int TRADE_DATE_LOOKBACK_DAYS = 12;

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

    protected String resolvedTradeDate(AgentToolCall call, AgentExecutionContext context) {
        String value = dateArgument(call, context);
        if (!value.isBlank()) {
            return normalizeDate(value, "tradeDate");
        }
        return LocalDate.now(MARKET_ZONE).toString();
    }

    protected LocalDate[] resolveHistoryRange(AgentToolCall call, AgentExecutionContext context) {
        String endRaw = stringArgument(call, "end");
        String startRaw = stringArgument(call, "start");
        LocalDate end = endRaw.isBlank()
                ? LocalDate.parse(resolvedTradeDate(call, context))
                : LocalDate.parse(normalizeDate(endRaw, "end"));
        LocalDate start = startRaw.isBlank() ? end.minusDays(7) : LocalDate.parse(normalizeDate(startRaw, "start"));
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        return new LocalDate[] {start, end};
    }

    protected Integer integerArgument(AgentToolCall call, String name, int fallback) {
        Object value = call == null || call.getArguments() == null ? null : call.getArguments().get(name);
        int result = fallback;
        if (value instanceof Number number) {
            result = number.intValue();
        } else if (value != null) {
            try {
                result = Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("Argument " + name + " must be an integer");
            }
        }
        if (result < 1 || result > 50) {
            throw new IllegalArgumentException("Argument " + name + " must be between 1 and 50");
        }
        return result;
    }

    protected <T> AgentToolResult loadByTradeDate(AgentToolDefinition definition,
                                                  String preferredTradeDate,
                                                  Function<String, T> loader,
                                                  Function<String, String> successSummary,
                                                  String emptyMessage) {
        LocalDate start;
        try {
            start = LocalDate.parse(preferredTradeDate);
        } catch (DateTimeParseException exception) {
            return AgentToolResult.failure(definition.getCode(),
                    "tradeDate must be YYYY-MM-DD, got: " + preferredTradeDate,
                    definition.getSource(), "ARGUMENT_FORMAT:tradeDate");
        }
        String lastError = null;
        for (int offset = 0; offset <= TRADE_DATE_LOOKBACK_DAYS; offset++) {
            String tradeDate = start.minusDays(offset).toString();
            try {
                T value = loader.apply(tradeDate);
                if (value != null) {
                    String summary = successSummary.apply(tradeDate);
                    if (offset > 0) {
                        summary = summary + " (fallback from " + preferredTradeDate + ")";
                    }
                    return success(definition, value, summary);
                }
                lastError = "no data";
            } catch (RuntimeException exception) {
                lastError = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                if (isHardArgumentError(lastError)) {
                    return AgentToolResult.failure(definition.getCode(), lastError, definition.getSource(), "ARGUMENT_INVALID");
                }
            }
        }
        String detail = lastError == null || lastError.isBlank() ? emptyMessage : emptyMessage + " Last error: " + lastError;
        return empty(definition, detail);
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

    private boolean isHardArgumentError(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("required argument")
                || lower.contains("must be")
                || lower.contains("invalid symbol")
                || lower.contains("argument ");
    }

    private String normalizeDate(String value, String field) {
        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(field + " must be YYYY-MM-DD");
        }
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
