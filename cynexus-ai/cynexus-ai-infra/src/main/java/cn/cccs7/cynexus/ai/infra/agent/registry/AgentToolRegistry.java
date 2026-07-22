package cn.cccs7.cynexus.ai.infra.agent.registry;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolExecutor;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** Spring assembles the tool pack; the model only sees the resulting metadata snapshot. */
@Component
public class AgentToolRegistry implements AgentToolCatalog, AgentToolExecutor {

    private static final int MAX_ARGUMENT_CHARS = 16_000;
    private static final int MAX_SCHEMA_DEPTH = 8;

    private final Map<String, AgentToolHandler> handlers;
    private final AgentCapabilityRepository capabilityRepository;
    private final boolean governed;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentToolRegistry(List<AgentToolHandler> handlers) {
        this(handlers, new AgentCapabilityRepository() {
            @Override public void synchronize(List<AgentCapabilityDescriptor> ignored) { }
            @Override public List<AgentCapabilityDescriptor> listAll() { return handlers.stream().map(AgentToolHandler::capabilityDescriptor).toList(); }
            @Override public List<AgentCapabilityDescriptor> listRuntimeCapabilities() { return listAll(); }
            @Override public boolean updateGovernance(String code, boolean enabled, String availability, int priority) { return false; }
        }, false);
    }

    @Autowired
    public AgentToolRegistry(List<AgentToolHandler> handlers, AgentCapabilityRepository capabilityRepository) {
        this(handlers, capabilityRepository, true);
    }

    private AgentToolRegistry(List<AgentToolHandler> handlers,
                              AgentCapabilityRepository capabilityRepository,
                              boolean governed) {
        this.capabilityRepository = capabilityRepository;
        this.governed = governed;
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
        capabilityRepository.synchronize(handlers.stream().map(AgentToolHandler::capabilityDescriptor).toList());
    }

    @Override
    public List<AgentToolDefinition> enabledTools() {
        Map<String, AgentCapabilityDescriptor> runtimeCapabilities = capabilityRepository.listRuntimeCapabilities().stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(AgentCapabilityDescriptor::getCapabilityCode, value -> value,
                        (left, right) -> left, LinkedHashMap::new));
        return handlers.values().stream()
                .filter(handler -> {
                    AgentCapabilityDescriptor descriptor = handler.capabilityDescriptor();
                    return (!this.governed && descriptor == null) || (descriptor != null && runtimeCapabilities.containsKey(descriptor.getCapabilityCode()));
                })
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
        if (governed && !capabilityRepository.listRuntimeCapabilities().stream().filter(java.util.Objects::nonNull)
                .anyMatch(item -> item.getCapabilityCode().equals(call.getToolCode()))) {
            return AgentToolResult.failure(call.getToolCode(), "The requested capability is disabled or unavailable.",
                    handler.definition().getSource(), "CAPABILITY_NOT_AVAILABLE");
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
            if (schema == null || !schema.isObject()) return "ARGUMENT_SCHEMA_INVALID";
            Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
            JsonNode argumentNode = objectMapper.valueToTree(safeArguments);
            if (!argumentNode.isObject() || objectMapper.writeValueAsString(argumentNode).length() > MAX_ARGUMENT_CHARS) {
                return "ARGUMENT_TOO_LARGE";
            }
            return validateValue(schema, argumentNode, "", 0);
        } catch (Exception exception) {
            return "ARGUMENT_SCHEMA_INVALID";
        }
    }

    private String validateValue(JsonNode schema, JsonNode value, String path, int depth) {
        if (depth > MAX_SCHEMA_DEPTH) return "ARGUMENT_SCHEMA_DEPTH";
        if (schema == null || !schema.isObject()) return "ARGUMENT_SCHEMA_INVALID";
        JsonNode type = schema.get("type");
        if (type != null && !matchesType(type, value)) return "ARGUMENT_TYPE:" + displayPath(path);
        JsonNode enumValues = schema.get("enum");
        if (enumValues != null) {
            if (!enumValues.isArray()) return "ARGUMENT_SCHEMA_INVALID";
            boolean allowed = false;
            for (JsonNode item : enumValues) allowed |= item.equals(value);
            if (!allowed) return "ARGUMENT_ENUM:" + displayPath(path);
        }
        if (value.isTextual()) {
            int length = value.textValue().codePointCount(0, value.textValue().length());
            if (schema.has("minLength") && length < schema.get("minLength").asInt()) return "ARGUMENT_MINIMUM:" + displayPath(path);
            if (schema.has("maxLength") && length > schema.get("maxLength").asInt()) return "ARGUMENT_MAXIMUM:" + displayPath(path);
            String format = schema.path("format").asText("");
            try {
                if ("date".equals(format)) LocalDate.parse(value.textValue());
                if ("date-time".equals(format)) OffsetDateTime.parse(value.textValue());
            } catch (RuntimeException exception) {
                return "ARGUMENT_FORMAT:" + displayPath(path);
            }
            if (schema.has("pattern")) {
                try {
                    if (!Pattern.compile(schema.get("pattern").asText()).matcher(value.textValue()).find()) {
                        return "ARGUMENT_PATTERN:" + displayPath(path);
                    }
                } catch (RuntimeException exception) {
                    return "ARGUMENT_SCHEMA_INVALID";
                }
            }
        }
        if (value.isNumber()) {
            if (schema.has("minimum") && value.decimalValue().compareTo(schema.get("minimum").decimalValue()) < 0) return "ARGUMENT_MINIMUM:" + displayPath(path);
            if (schema.has("maximum") && value.decimalValue().compareTo(schema.get("maximum").decimalValue()) > 0) return "ARGUMENT_MAXIMUM:" + displayPath(path);
        }
        if (value.isArray()) {
            if (schema.has("minItems") && value.size() < schema.get("minItems").asInt()) return "ARGUMENT_MINIMUM:" + displayPath(path);
            if (schema.has("maxItems") && value.size() > schema.get("maxItems").asInt()) return "ARGUMENT_MAXIMUM:" + displayPath(path);
            JsonNode items = schema.get("items");
            if (items != null) {
                for (int index = 0; index < value.size(); index++) {
                    String error = validateValue(items, value.get(index), path + "[" + index + "]", depth + 1);
                    if (error != null) return error;
                }
            }
        }
        if (value.isObject()) {
            JsonNode properties = schema.get("properties");
            if (properties != null && !properties.isObject()) return "ARGUMENT_SCHEMA_INVALID";
            JsonNode required = schema.get("required");
            if (required != null) {
                if (!required.isArray()) return "ARGUMENT_SCHEMA_INVALID";
                for (JsonNode item : required) {
                    String key = item.asText();
                    JsonNode itemValue = value.get(key);
                    if (itemValue == null || (itemValue.isTextual() && itemValue.textValue().isBlank())) {
                        return "ARGUMENT_REQUIRED:" + joinPath(path, key);
                    }
                }
            }
            // Tool schemas are a security boundary: fields must be explicitly allowed.
            boolean allowAdditional = schema.has("additionalProperties")
                    && schema.get("additionalProperties").isBoolean()
                    && schema.get("additionalProperties").asBoolean();
            for (var field : value.properties()) {
                JsonNode property = properties == null ? null : properties.get(field.getKey());
                if (property == null) {
                    if (!allowAdditional) return "ARGUMENT_UNKNOWN:" + joinPath(path, field.getKey());
                    continue;
                }
                String error = validateValue(property, field.getValue(), joinPath(path, field.getKey()), depth + 1);
                if (error != null) return error;
            }
        }
        return null;
    }

    private boolean matchesType(JsonNode type, JsonNode value) {
        if (type.isArray()) {
            for (JsonNode option : type) if (matchesType(option, value)) return true;
            return false;
        }
        return switch (type.asText()) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            case "boolean" -> value.isBoolean();
            case "null" -> value.isNull();
            default -> false;
        };
    }

    private String joinPath(String path, String key) {
        return path == null || path.isBlank() ? key : path + "." + key;
    }

    private String displayPath(String path) {
        return path == null || path.isBlank() ? "root" : path;
    }
}
