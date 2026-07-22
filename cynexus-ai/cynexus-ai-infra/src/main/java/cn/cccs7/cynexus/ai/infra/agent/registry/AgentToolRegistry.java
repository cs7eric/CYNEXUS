package cn.cccs7.cynexus.ai.infra.agent.registry;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentCapabilityRuntimeCatalog;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolExecutor;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.policy.capability.AgentCapabilityDescriptorPolicy;
import cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** Spring assembles the tool pack; the model only sees the resulting metadata snapshot. */
@Component
public class AgentToolRegistry implements AgentToolCatalog, AgentToolExecutor, AgentCapabilityRuntimeCatalog {

    private static final int MAX_ARGUMENT_CHARS = 16_000;
    private static final int MAX_SCHEMA_DEPTH = 8;

    private final Map<String, AgentToolHandler> handlers;
    private final Map<String, AgentToolHandler> runtimeEntryAllowList;
    private final AgentCapabilityRepository capabilityRepository;
    private final boolean governed;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<Map<String, RegisteredBinding>> runtimeBindings = new AtomicReference<>(Map.of());

    private record RegisteredBinding(AgentCapabilityDescriptor descriptor, AgentToolHandler handler) {
    }

    public AgentToolRegistry(List<AgentToolHandler> handlers) {
        this(handlers, new AgentCapabilityRepository() {
            @Override public void synchronize(List<AgentCapabilityDescriptor> ignored) { }
            @Override public List<AgentCapabilityDescriptor> listAll() { return handlers.stream().map(AgentToolHandler::capabilityDescriptor).toList(); }
            @Override public List<AgentCapabilityDescriptor> listRuntimeCapabilities() { return listAll(); }
            @Override public boolean updateGovernance(String code, boolean enabled, String availability, int priority) { return false; }
            @Override public AgentCapabilityDescriptor register(AgentCapabilityDescriptor descriptor) { return descriptor; }
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
        Map<String, AgentToolHandler> allowList = new HashMap<>();
        for (AgentToolHandler handler : registered.values()) {
            allowList.put(handler.getClass().getName(), handler);
            allowList.put(handler.definition().getCode(), handler);
        }
        this.runtimeEntryAllowList = Map.copyOf(allowList);
        capabilityRepository.synchronize(handlers.stream().map(AgentToolHandler::capabilityDescriptor).toList());
        refreshRuntimeBindings();
    }

    @Override
    public List<AgentToolDefinition> enabledTools() {
        return runtimeBindings.get().values().stream()
                .map(binding -> toDefinition(binding.descriptor(), binding.handler()))
                .sorted(Comparator.comparing(AgentToolDefinition::getCode))
                .toList();
    }

    @Override
    public Optional<AgentToolDefinition> find(String toolCode) {
        return Optional.ofNullable(runtimeBindings.get().get(toolCode))
                .map(binding -> toDefinition(binding.descriptor(), binding.handler()));
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        if (call == null || call.getToolCode() == null || call.getToolCode().isBlank()) {
            return AgentToolResult.failure("unknown", "Agent tool code is missing.", "cynexus-ai", "TOOL_CODE_MISSING");
        }
        RegisteredBinding binding = runtimeBindings.get().get(call.getToolCode());
        if (binding == null) {
            AgentToolHandler knownHandler = handlers.get(call.getToolCode());
            if (governed && knownHandler != null) {
                return AgentToolResult.failure(call.getToolCode(), "The requested capability is disabled or unavailable.",
                        knownHandler.definition().getSource(), "CAPABILITY_NOT_AVAILABLE");
            }
            return AgentToolResult.failure(call.getToolCode(), "The requested Agent tool is not registered.", "cynexus-ai", "TOOL_NOT_REGISTERED");
        }
        AgentToolHandler handler = binding.handler();
        AgentToolDefinition definition = toDefinition(binding.descriptor(), handler);
        if (!definition.isReadOnly() || definition.isRequiresConfirmation()) {
            return AgentToolResult.failure(call.getToolCode(), "The requested tool is not enabled for autonomous read-only execution.", "cynexus-ai", "TOOL_REQUIRES_CONFIRMATION");
        }
        try {
            String validationError = validateArguments(definition, call.getArguments());
            if (validationError != null) {
                return AgentToolResult.failure(call.getToolCode(), "The tool arguments are invalid.", definition.getSource(), validationError);
            }
            String canonicalCode = handler.definition().getCode();
            AgentToolCall handlerCall = canonicalCode.equals(call.getToolCode()) ? call
                    : AgentToolCall.builder().callId(call.getCallId()).toolCode(canonicalCode).arguments(call.getArguments()).build();
            AgentToolResult result = handler.execute(handlerCall, context);
            return result == null || canonicalCode.equals(call.getToolCode()) ? result
                    : AgentToolResult.builder().toolCode(call.getToolCode()).status(result.getStatus())
                    .outputJson(result.getOutputJson()).summary(result.getSummary()).dataDate(result.getDataDate())
                    .source(result.getSource()).warnings(result.getWarnings()).build();
        } catch (RuntimeException exception) {
            return AgentToolResult.failure(call.getToolCode(), "The tool failed before returning data.", definition.getSource(), exception.getMessage());
        }
    }

    @Override
    public void register(AgentCapabilityDescriptor descriptor) {
        AgentCapabilityDescriptorPolicy.validateRegistration(descriptor);
        AgentToolHandler handler = runtimeEntryAllowList.get(descriptor.getRuntimeEntry());
        if (handler == null) {
            throw new IllegalArgumentException("Capability runtimeEntry is not registered in the runtime allow-list");
        }
        validateRuntimeBinding(descriptor, handler);
        runtimeBindings.updateAndGet(current -> {
            Map<String, RegisteredBinding> next = new LinkedHashMap<>(current);
            next.put(descriptor.getCapabilityCode(), new RegisteredBinding(descriptor, handler));
            return Map.copyOf(next);
        });
    }

    private void refreshRuntimeBindings() {
        Map<String, RegisteredBinding> next = new LinkedHashMap<>();
        for (AgentCapabilityDescriptor descriptor : capabilityRepository.listRuntimeCapabilities()) {
            if (descriptor == null) continue;
            AgentToolHandler handler = runtimeEntryAllowList.get(descriptor.getRuntimeEntry());
            if (handler == null) continue;
            try {
                validateRuntimeBinding(descriptor, handler);
                next.put(descriptor.getCapabilityCode(), new RegisteredBinding(descriptor, handler));
            } catch (IllegalArgumentException ignored) {
                // A persisted descriptor must never make the runtime unavailable.
            }
        }
        if (!governed && next.isEmpty()) {
            for (AgentToolHandler handler : handlers.values()) {
                AgentCapabilityDescriptor descriptor = descriptorForHandler(handler);
                next.put(descriptor.getCapabilityCode(), new RegisteredBinding(descriptor, handler));
            }
        }
        runtimeBindings.set(Map.copyOf(next));
    }

    private AgentCapabilityDescriptor descriptorForHandler(AgentToolHandler handler) {
        AgentCapabilityDescriptor descriptor = handler.capabilityDescriptor();
        if (descriptor != null) return descriptor;
        AgentToolDefinition definition = handler.definition();
        return AgentCapabilityDescriptor.builder().capabilityCode(definition.getCode()).version(valueOr(definition.getVersion(), "1.0.0"))
                .name(definition.getName()).module(definition.getModule()).description(definition.getDescription())
                .inputSchemaJson(definition.getInputSchemaJson()).outputSchemaJson(definition.getOutputSchemaJson())
                .readOnly(definition.isReadOnly()).requiresConfirmation(definition.isRequiresConfirmation())
                .tenantScope(valueOr(definition.getTenantScope(), "TENANT")).source(definition.getSource())
                .sourceUrl(definition.getSourceUrl()).freshness(definition.getFreshness())
                .availability(valueOr(definition.getAvailability(), "AVAILABLE")).governanceAvailability("AVAILABLE")
                .enabled(true).priority(definition.getPriority()).promptKey(valueOr(definition.getPromptKey(), "autonomous_agent"))
                .runtimeEntry(valueOr(definition.getRuntimeEntry(), handler.getClass().getName()))
                .routeGroup(valueOr(definition.getRouteGroup(), "Agent / " + definition.getModule()))
                .registrationMode(AgentCapabilityDescriptorPolicy.HANDLER_REGISTRATION).build();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void validateRuntimeBinding(AgentCapabilityDescriptor descriptor, AgentToolHandler handler) {
        AgentToolDefinition definition = handler.definition();
        if (!definition.isReadOnly() || definition.isRequiresConfirmation()) {
            throw new IllegalArgumentException("The runtime handler is not eligible for autonomous execution");
        }
        if (!sameJsonSchema(descriptor.getInputSchemaJson(), definition.getInputSchemaJson())
                || !sameJsonSchema(descriptor.getOutputSchemaJson(), definition.getOutputSchemaJson())) {
            throw new IllegalArgumentException("Capability schemas must match the allow-listed runtime handler");
        }
    }

    private boolean sameJsonSchema(String left, String right) {
        try {
            JsonNode leftNode = objectMapper.readTree(left);
            JsonNode rightNode = objectMapper.readTree(right);
            return leftNode != null && rightNode != null && leftNode.isObject() && rightNode.isObject()
                    && leftNode.equals(rightNode);
        } catch (Exception exception) {
            return false;
        }
    }

    private AgentToolDefinition toDefinition(AgentCapabilityDescriptor descriptor, AgentToolHandler handler) {
        return AgentToolDefinition.builder().code(descriptor.getCapabilityCode()).name(descriptor.getName())
                .module(descriptor.getModule()).description(descriptor.getDescription())
                .inputSchemaJson(descriptor.getInputSchemaJson()).outputSchemaJson(descriptor.getOutputSchemaJson())
                .readOnly(descriptor.isReadOnly()).requiresConfirmation(descriptor.isRequiresConfirmation())
                .freshness(descriptor.getFreshness()).source(descriptor.getSource()).sourceUrl(descriptor.getSourceUrl())
                .version(descriptor.getVersion()).tenantScope(descriptor.getTenantScope()).availability(descriptor.getAvailability())
                .priority(descriptor.getPriority()).promptKey(descriptor.getPromptKey())
                .runtimeEntry(descriptor.getRuntimeEntry()).routeGroup(descriptor.getRouteGroup()).build();
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
