package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

class AgentToolRegistryTest {

    @Test
    void sortsDefinitionsAndRejectsUnknownTool() {
        AgentToolHandler first = handler("market.z", true, false);
        AgentToolHandler second = handler("market.a", true, false);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(first, second));

        assertThat(registry.enabledTools()).extracting(AgentToolDefinition::getCode)
                .containsExactly("market.a", "market.z");
        AgentToolResult result = registry.execute(AgentToolCall.builder().toolCode("market.missing").build(), null);
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getWarnings()).contains("TOOL_NOT_REGISTERED");
    }

    @Test
    void rejectsDuplicateDefinitions() {
        assertThatIllegalStateException().isThrownBy(() -> new AgentToolRegistry(List.of(
                handler("market.same", true, false), handler("market.same", true, false))));
    }

    @Test
    void rejectsNonReadOnlyOrConfirmationTools() {
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler("market.write", false, false)));
        AgentToolResult result = registry.execute(AgentToolCall.builder().toolCode("market.write").build(), null);
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getWarnings()).contains("TOOL_REQUIRES_CONFIRMATION");
    }

    private AgentToolHandler handler(String code, boolean readOnly, boolean requiresConfirmation) {
        AgentToolHandler handler = mock(AgentToolHandler.class);
        AgentToolDefinition definition = AgentToolDefinition.builder()
                .code(code).name(code).module("market").description(code)
                .inputSchemaJson("{\"type\":\"object\"}")
                .outputSchemaJson("{\"type\":\"object\"}")
                .readOnly(readOnly).requiresConfirmation(requiresConfirmation)
                .source("test").build();
        org.mockito.Mockito.when(handler.definition()).thenReturn(definition);
        return handler;
    }
}
