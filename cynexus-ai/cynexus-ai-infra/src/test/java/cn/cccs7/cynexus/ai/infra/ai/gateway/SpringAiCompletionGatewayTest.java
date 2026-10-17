package cn.cccs7.cynexus.ai.infra.ai.gateway;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.infra.ai.adapter.OpenAiCompatibleChatAdapter.ProviderHttpException;
import org.junit.jupiter.api.Test;

class SpringAiCompletionGatewayTest {
    private final AiModelBO model = AiModelBO.builder().providerCode("xhhai-grok").modelCode("grok-4.6").build();

    @Test
    void validationFailureDoesNotSilentlyInvokeLegacyAdapter() {
        var executor = mock(cn.cccs7.cynexus.ai.domain.port.AiModelExecutor.class);
        var legacy = mock(AiCompletionGateway.class);
        when(executor.complete(any(), any(), any(AiExecutionContext.class)))
                .thenThrow(new IllegalArgumentException("invalid request"));
        var gateway = new SpringAiCompletionGateway(executor, legacy);

        assertThrows(IllegalArgumentException.class, () -> gateway.complete(model, "prompt",
                new AiExecutionContext("exec", "capability", "capability", "v1", "xhhai-grok", "grok-4.6",
                        "tenant", "principal", "request", "trace")));
        verifyNoInteractions(legacy);
    }

    @Test
    void transientFailureUsesLegacyRollback() {
        var executor = mock(cn.cccs7.cynexus.ai.domain.port.AiModelExecutor.class);
        var legacy = mock(AiCompletionGateway.class);
        when(executor.complete(any(), any(), any(AiExecutionContext.class)))
                .thenThrow(new IllegalStateException("AI provider request timed out after 90 seconds"));
        when(legacy.complete(any(), any(), any(AiExecutionContext.class)))
                .thenReturn(AiCompletionResultBO.builder().content("fallback").build());
        var gateway = new SpringAiCompletionGateway(executor, legacy);
        var context = new AiExecutionContext("exec", "capability", "capability", "v1", "xhhai-grok", "grok-4.6",
                "tenant", "principal", "request", "trace");

        org.junit.jupiter.api.Assertions.assertEquals("fallback", gateway.complete(model, "prompt", context).getContent());
        verify(legacy).complete(model, "prompt", context);
    }

    @Test
    void providerRateLimitUsesLegacyRollbackWithoutMessageParsing() {
        var executor = mock(cn.cccs7.cynexus.ai.domain.port.AiModelExecutor.class);
        var legacy = mock(AiCompletionGateway.class);
        when(executor.complete(any(), any(), any(AiExecutionContext.class)))
                .thenThrow(new ProviderHttpException(429, "rate limited"));
        when(legacy.complete(any(), any(), any(AiExecutionContext.class)))
                .thenReturn(AiCompletionResultBO.builder().content("fallback").build());
        var gateway = new SpringAiCompletionGateway(executor, legacy);
        var context = new AiExecutionContext("exec-429", "capability", "capability", "v1", "xhhai-grok", "grok-4.6",
                "tenant", "principal", "request", "trace");

        org.junit.jupiter.api.Assertions.assertEquals("fallback", gateway.complete(model, "prompt", context).getContent());
        verify(legacy).complete(model, "prompt", context);
    }

    @Test
    void providerBadRequestDoesNotUseLegacyRollback() {
        var executor = mock(cn.cccs7.cynexus.ai.domain.port.AiModelExecutor.class);
        var legacy = mock(AiCompletionGateway.class);
        var failure = new ProviderHttpException(400, "invalid request");
        when(executor.complete(any(), any(), any(AiExecutionContext.class))).thenThrow(failure);
        var gateway = new SpringAiCompletionGateway(executor, legacy);
        var context = new AiExecutionContext("exec-400", "capability", "capability", "v1", "xhhai-grok", "grok-4.6",
                "tenant", "principal", "request", "trace");

        assertThrows(ProviderHttpException.class, () -> gateway.complete(model, "prompt", context));
        verifyNoInteractions(legacy);
    }
}
