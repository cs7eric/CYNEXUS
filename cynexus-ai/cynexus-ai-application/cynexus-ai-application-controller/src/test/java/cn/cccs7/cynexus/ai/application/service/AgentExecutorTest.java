package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.service.agent.execution.AgentExecutor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiCompletionGateway;
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
}
