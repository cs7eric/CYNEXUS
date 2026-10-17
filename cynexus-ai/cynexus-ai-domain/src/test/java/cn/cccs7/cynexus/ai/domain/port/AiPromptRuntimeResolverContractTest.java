package cn.cccs7.cynexus.ai.domain.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AiPromptRuntimeResolverContractTest {
    @Test
    void rendersDeclaredVariablesAndRejectsMissingVariables() {
        var runtime = new AiPromptRuntimeResolver.AiPromptRuntime("capability", "v1", "system", "input={{input}}");
        assertEquals("system" + System.lineSeparator() + "input=value", runtime.render(Map.of("input", "value")));
        assertThrows(IllegalArgumentException.class, () -> runtime.render(Map.of()));
    }

    @Test
    void rejectsUnexpectedPlaceholderAfterRendering() {
        var runtime = new AiPromptRuntimeResolver.AiPromptRuntime("capability", "v1", "system", "{{known}} {{unknown}}");
        assertThrows(IllegalArgumentException.class, () -> runtime.render(Map.of("known", "ok")));
    }

    @Test
    void rejectsProvidedVariableThatIsNotDeclaredByThePrompt() {
        var runtime = new AiPromptRuntimeResolver.AiPromptRuntime("capability", "v1", "system", "{{known}}");
        assertThrows(IllegalArgumentException.class, () -> runtime.render(Map.of("known", "ok", "ignored", "no")));
    }
}
