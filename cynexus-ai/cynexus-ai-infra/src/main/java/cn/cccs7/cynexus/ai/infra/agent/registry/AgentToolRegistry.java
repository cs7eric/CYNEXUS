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
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** Spring assembles the tool pack; the model only sees the resulting metadata snapshot. */
@Component
public class AgentToolRegistry implements AgentToolCatalog, AgentToolExecutor {

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
