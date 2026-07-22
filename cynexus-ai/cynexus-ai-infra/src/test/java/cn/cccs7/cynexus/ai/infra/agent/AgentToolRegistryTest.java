package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.infra.agent.registry.AgentToolRegistry;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
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
    void rejectsMalformedSchemaBeforeCallingTheHandler() {
        AgentToolHandler handler = handler("market.malformed", true, false);
        org.mockito.Mockito.when(handler.definition()).thenReturn(AgentToolDefinition.builder()
                .code("market.malformed").name("malformed").module("market").description("malformed")
                .inputSchemaJson("{not-json").outputSchemaJson("{\"type\":\"object\"}").readOnly(true).build());
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler));

        AgentToolResult result = registry.execute(AgentToolCall.builder().toolCode("market.malformed")
                .arguments(java.util.Map.of("value", "anything")).build(), null);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getWarnings()).contains("ARGUMENT_SCHEMA_INVALID");
        verify(handler, never()).execute(any(), any());
    }

    @Test
    void enforcesIntegerBoundsEnumAndTypeForFuzzedArguments() {
        AgentToolHandler handler = handler("market.fuzz", true, false);
        org.mockito.Mockito.when(handler.definition()).thenReturn(AgentToolDefinition.builder()
                .code("market.fuzz").name("fuzz").module("market").description("fuzz")
                .inputSchemaJson("{\"type\":\"object\",\"properties\":{"
                        + "\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":10},"
                        + "\"mode\":{\"type\":\"string\",\"enum\":[\"safe\",\"full\"]}}}")
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).build());
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler));

        assertThat(registry.execute(AgentToolCall.builder().toolCode("market.fuzz")
                .arguments(java.util.Map.of("limit", "not-an-integer", "mode", "safe")).build(), null)
                .getWarnings()).contains("ARGUMENT_TYPE:limit");
        assertThat(registry.execute(AgentToolCall.builder().toolCode("market.fuzz")
                .arguments(java.util.Map.of("limit", 0, "mode", "safe")).build(), null)
                .getWarnings()).contains("ARGUMENT_MINIMUM:limit");
        assertThat(registry.execute(AgentToolCall.builder().toolCode("market.fuzz")
                .arguments(java.util.Map.of("limit", 11, "mode", "safe")).build(), null)
                .getWarnings()).contains("ARGUMENT_MAXIMUM:limit");
        assertThat(registry.execute(AgentToolCall.builder().toolCode("market.fuzz")
                .arguments(java.util.Map.of("limit", 1, "mode", "unsafe")).build(), null)
                .getWarnings()).contains("ARGUMENT_ENUM:mode");
        verify(handler, never()).execute(any(), any());
    }

    @Test
    void disablesPersistedCapabilityWhenGovernanceRemovesIt() {
        AgentToolHandler handler = handler("finance.synthetic", true, false);
        cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository repository = mock(
                cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository.class);
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
