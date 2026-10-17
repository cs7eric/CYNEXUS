package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.service.agent.execution.AgentExecutor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.eq;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentExecutorTest {

    @Test
    void neverReturnsSyntheticSuccessForToolEnabledGenericExecution() {
        AiCompletionGateway gateway = mock(AiCompletionGateway.class);
        AgentExecutor executor = new AgentExecutor(gateway);

        assertThatThrownBy(() -> executor.execute(null, AiModelBO.builder().modelCode("gpt-5.4-mini").build(),
                "tool request", List.of("market.overview")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("autonomous Agent workflow");
        verifyNoInteractions(gateway);
    }

    @Test
    void fallbackPreservesExecutionContext() {
        AiCompletionGateway gateway = mock(AiCompletionGateway.class);
        when(gateway.complete(org.mockito.ArgumentMatchers.any(), eq("prompt"), org.mockito.ArgumentMatchers.any(AiExecutionContext.class)))
                .thenReturn(AiCompletionResultBO.builder().content("fallback").build());
        AgentExecutor executor = new AgentExecutor(gateway);
        AiExecutionContext context = new AiExecutionContext("exec-1", "capability", "prompt-key", "v1",
                "provider", "model", "tenant", "principal", "request", "trace");

        assertThat(executor.completeDetailed(AiModelBO.builder().modelCode("model").build(), "prompt", context).getContent())
                .isEqualTo("fallback");
        verify(gateway).complete(org.mockito.ArgumentMatchers.any(), eq("prompt"), eq(context));
    }

    @Test
    void langChainFailureFallsBackWithSameExecutionContext() {
        AiCompletionGateway gateway = mock(AiCompletionGateway.class);
        var langChain = mock(cn.cccs7.cynexus.ai.domain.port.LangChain4jCompletionPort.class);
        AiModelBO model = AiModelBO.builder().modelCode("model").build();
        AiExecutionContext context = new AiExecutionContext("exec-2", "capability", "prompt-key", "v1",
                "provider", "model", "tenant", "principal", "request", "trace");
        when(gateway.complete(eq(model), eq("prompt"), eq(context)))
                .thenReturn(AiCompletionResultBO.builder().content("fallback").build());
        doThrow(new IllegalStateException("typed service unavailable"))
                .when(langChain).complete(eq(model), eq("prompt"), eq(context));

        AgentExecutor executor = new AgentExecutor(gateway, langChain);
        assertThat(executor.completeDetailed(model, "prompt", context).getContent()).isEqualTo("fallback");
        verify(langChain).complete(eq(model), eq("prompt"), eq(context));
        verify(gateway).complete(eq(model), eq("prompt"), eq(context));
    }
}
