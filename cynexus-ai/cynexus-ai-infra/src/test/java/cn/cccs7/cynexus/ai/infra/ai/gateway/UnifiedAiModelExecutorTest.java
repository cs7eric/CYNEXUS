package cn.cccs7.cynexus.ai.infra.ai.gateway;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import cn.cccs7.cynexus.ai.infra.ai.adapter.OpenAiCompatibleChatAdapter.ProviderHttpException;
import org.junit.jupiter.api.Test;

class UnifiedAiModelExecutorTest {
    private final AiModelBO model = AiModelBO.builder().providerCode("xhhai-grok").modelCode("grok-4.6").build();

    @Test
    void validationFailureIsNotRetriedThroughLegacyGateway() {
        AiModelExecutor spring = mock(AiModelExecutor.class);
        AiCompletionGatewayImpl legacy = mock(AiCompletionGatewayImpl.class);
        when(spring.complete(any(), any(), any(AiExecutionContext.class)))
                .thenThrow(new IllegalArgumentException("invalid prompt"));
        var executor = new UnifiedAiModelExecutor(spring, legacy);
        assertThrows(IllegalArgumentException.class, () -> executor.complete(model, "prompt",
                new AiExecutionContext("exec", "cap", "cap", "v1", "provider", "model", null, null, null, null)));
        verifyNoInteractions(legacy);
    }

    @Test
    void transientFailureUsesLegacyGateway() {
        AiModelExecutor spring = mock(AiModelExecutor.class);
        AiCompletionGatewayImpl legacy = mock(AiCompletionGatewayImpl.class);
        when(spring.complete(any(), any(), any(AiExecutionContext.class)))
                .thenThrow(new ProviderHttpException(503, "unavailable"));
        when(legacy.complete(any(), any(), any(AiExecutionContext.class)))
                .thenReturn(cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO.builder().content("fallback").build());
        var executor = new UnifiedAiModelExecutor(spring, legacy);
        var context = new AiExecutionContext("exec", "cap", "cap", "v1", "provider", "model", null, null, null, null);
        org.junit.jupiter.api.Assertions.assertEquals("fallback", executor.complete(model, "prompt", context).getContent());
        verify(legacy).complete(model, "prompt", context);
    }
}
