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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

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

    @Test
    void validatesRequiredAndUnknownArgumentsBeforeExecution() {
        AgentToolHandler handler = handler("market.required", true, false);
        org.mockito.Mockito.when(handler.definition()).thenReturn(AgentToolDefinition.builder()
                .code("market.required").name("required").module("market").description("required")
                .inputSchemaJson("{\"type\":\"object\",\"required\":[\"symbol\"],\"properties\":{\"symbol\":{\"type\":\"string\"}}}")
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).build());
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler));

        AgentToolResult result = registry.execute(AgentToolCall.builder().toolCode("market.required")
                .arguments(java.util.Map.of("other", "value")).build(), null);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getWarnings()).contains("ARGUMENT_REQUIRED:symbol");
        verify(handler, never()).execute(any(), any());
    }

    @Test
    void disablesPersistedCapabilityWhenGovernanceRemovesIt() {
        AgentToolHandler handler = handler("finance.synthetic", true, false);
        cn.cccs7.cynexus.ai.domain.repository.AgentCapabilityRepository repository = mock(
                cn.cccs7.cynexus.ai.domain.repository.AgentCapabilityRepository.class);
        org.mockito.Mockito.when(repository.listRuntimeCapabilities()).thenReturn(List.of());
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler), repository);

        assertThat(registry.enabledTools()).isEmpty();
        assertThat(registry.execute(AgentToolCall.builder().toolCode("finance.synthetic").build(), null)
                .getWarnings()).contains("CAPABILITY_NOT_AVAILABLE");
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
