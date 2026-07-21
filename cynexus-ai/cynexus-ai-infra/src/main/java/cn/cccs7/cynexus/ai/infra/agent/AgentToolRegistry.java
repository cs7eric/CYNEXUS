package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolExecutor;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
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
            return handler.execute(call, context);
        } catch (RuntimeException exception) {
            return AgentToolResult.failure(call.getToolCode(), "The tool failed before returning data.", handler.definition().getSource(), exception.getMessage());
        }
    }
}
