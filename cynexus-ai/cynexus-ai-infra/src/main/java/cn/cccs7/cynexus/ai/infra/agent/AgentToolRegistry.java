package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolExecutor;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Spring assembles the tool pack; the model only sees the resulting metadata snapshot. */
@Component
public class AgentToolRegistry implements AgentToolCatalog, AgentToolExecutor {

    private final Map<String, AgentToolHandler> handlers;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentToolRegistry(List<AgentToolHandler> handlers) {
        Map<String, AgentToolHandler> registered = new LinkedHashMap<>();
        for (AgentToolHandler handler : handlers) {
            String code = handler.definition().getCode();
            if (code == null || code.isBlank()) {
                throw new IllegalStateException("Agent tool code cannot be blank");
            }
            if (registered.put(code, handler) != null) {
                throw new IllegalStateException("Duplicate Agent tool code: " + code);
            }
        }
        this.handlers = Map.copyOf(registered);
    }

    @Override
    public List<AgentToolDefinition> enabledTools() {
        return handlers.values().stream()
                .map(AgentToolHandler::definition)
                .sorted(Comparator.comparing(AgentToolDefinition::getCode))
                .toList();
    }

    @Override
    public Optional<AgentToolDefinition> find(String toolCode) {
        return Optional.ofNullable(handlers.get(toolCode)).map(AgentToolHandler::definition);
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        if (call == null || call.getToolCode() == null || call.getToolCode().isBlank()) {
            return AgentToolResult.failure("unknown", "Agent tool code is missing.", "cynexus-ai", "TOOL_CODE_MISSING");
        }
        AgentToolHandler handler = handlers.get(call.getToolCode());
        if (handler == null) {
            return AgentToolResult.failure(call.getToolCode(), "The requested Agent tool is not registered.", "cynexus-ai", "TOOL_NOT_REGISTERED");
        }
        if (!handler.definition().isReadOnly() || handler.definition().isRequiresConfirmation()) {
            return AgentToolResult.failure(call.getToolCode(), "The requested tool is not enabled for autonomous read-only execution.", "cynexus-ai", "TOOL_REQUIRES_CONFIRMATION");
        }
        try {
            String validationError = validateArguments(handler.definition(), call.getArguments());
            if (validationError != null) {
                return AgentToolResult.failure(call.getToolCode(), "The tool arguments are invalid.", handler.definition().getSource(), validationError);
            }
            return handler.execute(call, context);
        } catch (RuntimeException exception) {
            return AgentToolResult.failure(call.getToolCode(), "The tool failed before returning data.", handler.definition().getSource(), exception.getMessage());
        }
    }

    private String validateArguments(AgentToolDefinition definition, Map<String, Object> arguments) {
        try {
            JsonNode schema = objectMapper.readTree(definition.getInputSchemaJson());
            Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
            JsonNode required = schema.get("required");
            if (required != null && required.isArray()) {
                for (JsonNode item : required) {
                    String key = item.asText();
                    if (!safeArguments.containsKey(key) || safeArguments.get(key) == null
                            || String.valueOf(safeArguments.get(key)).isBlank()) return "ARGUMENT_REQUIRED:" + key;
                }
            }
            JsonNode properties = schema.get("properties");
            if (properties == null || !properties.isObject()) return null;
            for (Map.Entry<String, Object> entry : safeArguments.entrySet()) {
                JsonNode property = properties.get(entry.getKey());
                if (property == null) return "ARGUMENT_UNKNOWN:" + entry.getKey();
                String type = property.path("type").asText("");
                if ("integer".equals(type)) {
                    long value;
                    try { value = Long.parseLong(String.valueOf(entry.getValue())); }
                    catch (NumberFormatException exception) { return "ARGUMENT_TYPE:" + entry.getKey(); }
                    if (property.has("minimum") && value < property.get("minimum").asLong()) return "ARGUMENT_MINIMUM:" + entry.getKey();
                    if (property.has("maximum") && value > property.get("maximum").asLong()) return "ARGUMENT_MAXIMUM:" + entry.getKey();
                } else if ("string".equals(type) && !(entry.getValue() instanceof String)) {
                    return "ARGUMENT_TYPE:" + entry.getKey();
                }
                JsonNode enumValues = property.get("enum");
                if (enumValues != null && enumValues.isArray()) {
                    boolean allowed = false;
                    for (JsonNode enumValue : enumValues) allowed |= enumValue.asText().equals(String.valueOf(entry.getValue()));
                    if (!allowed) return "ARGUMENT_ENUM:" + entry.getKey();
                }
            }
            return null;
        } catch (Exception exception) {
            return "ARGUMENT_SCHEMA_INVALID";
        }
    }
}
