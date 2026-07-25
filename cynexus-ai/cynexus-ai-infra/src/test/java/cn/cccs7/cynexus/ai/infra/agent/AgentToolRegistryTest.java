package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.infra.agent.registry.AgentToolRegistry;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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
    void enforcesStringDateBooleanArrayAndExplicitAdditionalPropertyRules() {
        AgentToolHandler handler = handler("market.structured", true, false);
        org.mockito.Mockito.when(handler.definition()).thenReturn(AgentToolDefinition.builder()
                .code("market.structured").name("structured").module("market").description("structured")
                .inputSchemaJson("{\"type\":\"object\",\"required\":[\"query\",\"tradeDate\"],\"additionalProperties\":false,\"properties\":{"
                        + "\"query\":{\"type\":\"string\",\"minLength\":2,\"maxLength\":8},"
                        + "\"tradeDate\":{\"type\":\"string\",\"format\":\"date\"},"
                        + "\"includeHistory\":{\"type\":\"boolean\"},"
                        + "\"symbols\":{\"type\":\"array\",\"maxItems\":2,\"items\":{\"type\":\"string\",\"minLength\":1}}}}")
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).build());
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler));

        assertThat(registry.execute(AgentToolCall.builder().toolCode("market.structured").arguments(java.util.Map.of(
                "query", "x", "tradeDate", "2026-07-22")).build(), null).getWarnings())
                .contains("ARGUMENT_MINIMUM:query");
        assertThat(registry.execute(AgentToolCall.builder().toolCode("market.structured").arguments(java.util.Map.of(
                "query", "valid", "tradeDate", "not-a-date")).build(), null).getWarnings())
                .contains("ARGUMENT_FORMAT:tradeDate");
        assertThat(registry.execute(AgentToolCall.builder().toolCode("market.structured").arguments(java.util.Map.of(
                "query", "valid", "tradeDate", "2026-07-22", "includeHistory", "true")).build(), null).getWarnings())
                .contains("ARGUMENT_TYPE:includeHistory");
        assertThat(registry.execute(AgentToolCall.builder().toolCode("market.structured").arguments(java.util.Map.of(
                "query", "valid", "tradeDate", "2026-07-22", "unknown", "value")).build(), null).getWarnings())
                .contains("ARGUMENT_UNKNOWN:unknown");
        verify(handler, never()).execute(any(), any());
    }

    @Test
    void rejectsOversizedToolArgumentsBeforeHandlerExecution() {
        AgentToolHandler handler = handler("market.large", true, false);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler));

        AgentToolResult result = registry.execute(AgentToolCall.builder().toolCode("market.large")
                .arguments(java.util.Map.of("payload", "x".repeat(16_001))).build(), null);

        assertThat(result.getWarnings()).contains("ARGUMENT_TOO_LARGE");
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

    @Test
    void hotAddsAnAliasOnlyThroughTheRuntimeHandlerAllowList() {
        AgentToolHandler handler = handler("market.canonical", true, false);
        var repository = mock(cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository.class);
        org.mockito.Mockito.when(repository.listRuntimeCapabilities()).thenReturn(List.of());
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler), repository);
        AgentCapabilityDescriptor descriptor = AgentCapabilityDescriptor.builder()
                .capabilityCode("market.dynamic_alias").version("1.0.0").name("Dynamic market alias")
                .module("market").description("Dynamic alias").inputSchemaJson(handler.definition().getInputSchemaJson())
                .outputSchemaJson(handler.definition().getOutputSchemaJson()).readOnly(true).requiresConfirmation(false)
                .tenantScope("TENANT").source("test").availability("AVAILABLE").governanceAvailability("AVAILABLE")
                .enabled(true).priority(10).promptKey("autonomous_agent").runtimeEntry(handler.getClass().getName())
                .routeGroup("Agent / market").registrationMode("ADMIN").build();

        registry.register(descriptor);

        assertThat(registry.enabledTools()).extracting(AgentToolDefinition::getCode)
                .containsExactly("market.dynamic_alias");
        org.mockito.Mockito.when(handler.execute(any(), any()))
                .thenReturn(AgentToolResult.success("market.canonical", "{}", "ok", "2026-07-23", "test", List.of()));
        assertThat(registry.execute(AgentToolCall.builder().callId("call-1").toolCode("market.dynamic_alias")
                .arguments(java.util.Map.of()).build(), null).getStatus()).isEqualTo("SUCCEEDED");
        org.mockito.Mockito.verify(handler).execute(org.mockito.ArgumentMatchers.argThat(call ->
                "market.canonical".equals(call.getToolCode())), any());
    }

    @Test
    void keepsRuntimeEntryControlledByTheHandlerDefinition() {
        AgentToolHandler handler = handler("market.canonical", true, false);
        var repository = mock(cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository.class);
        org.mockito.Mockito.when(repository.listRuntimeCapabilities()).thenReturn(List.of());
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler), repository);
        AgentCapabilityDescriptor descriptor = AgentCapabilityDescriptor.builder()
                .capabilityCode("market.dynamic_alias").version("1.0.0").name("Dynamic market alias")
                .module("market").description("Dynamic alias").inputSchemaJson(handler.definition().getInputSchemaJson())
                .outputSchemaJson(handler.definition().getOutputSchemaJson()).readOnly(true).requiresConfirmation(false)
                .tenantScope("TENANT").source("test").availability("AVAILABLE").governanceAvailability("AVAILABLE")
                .enabled(true).priority(10).promptKey("autonomous_agent").runtimeEntry(handler.definition().getCode())
                .routeGroup("Agent / market").registrationMode("ADMIN").build();

        assertThatIllegalArgumentException().isThrownBy(() -> registry.register(descriptor));
    }

    @Test
    void rejectsDynamicCapabilityOutsideTheRuntimeHandlerAllowList() {
        AgentToolRegistry registry = new AgentToolRegistry(List.of(handler("market.canonical", true, false)));
        AgentCapabilityDescriptor descriptor = AgentCapabilityDescriptor.builder()
                .capabilityCode("market.remote").version("1.0.0").name("Remote").module("market")
                .description("Remote").inputSchemaJson("{\"type\":\"object\"}")
                .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).requiresConfirmation(false)
                .tenantScope("TENANT").availability("AVAILABLE").governanceAvailability("AVAILABLE")
                .enabled(true).priority(10).promptKey("autonomous_agent").runtimeEntry("https://example.invalid")
                .routeGroup("Agent / market").registrationMode("ADMIN").build();

        assertThatIllegalArgumentException().isThrownBy(() -> registry.register(descriptor));
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
