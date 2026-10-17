package cn.cccs7.cynexus.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.application.assembler.AiExecutionAssembler;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionTracePort;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiToolCatalog;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import cn.cccs7.cynexus.ai.domain.service.scenario.ScenarioExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.scenario.PreparedScenarioVisionExecution;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioExecutionAppServiceTest {

    @Test
    void fallsBackToTheNextVisionModelAfterThePrimaryFails() {
        AiModelExecutor executor = mock(AiModelExecutor.class);
        AiToolCatalog toolCatalog = mock(AiToolCatalog.class);
        AiExecutionTracePort tracePort = mock(AiExecutionTracePort.class);
        AiExecutionAssembler assembler = mock(AiExecutionAssembler.class);
        ScenarioExecutionDomainService domainService = mock(ScenarioExecutionDomainService.class);
        AiModelBO primary = AiModelBO.builder().modelCode("primary").build();
        AiModelBO fallback = AiModelBO.builder().modelCode("fallback").build();
        PreparedScenarioVisionExecution prepared = PreparedScenarioVisionExecution.builder()
                .scenario(AiScenarioBO.builder().scenarioCode("portfolio_trade_screenshot_parse").build())
                .model(primary).fallbackModels(List.of(fallback)).prompt("prompt")
                .capability("portfolio_trade_screenshot_parse").build();
        AiExecutionBO execution = new AiExecutionBO("AI-fallback", prepared.getScenario(), fallback.getModelCode(),
                AiExecutionStatus.SUCCEEDED, "{}", List.of());
        AiExecuteDTO response = AiExecuteDTO.builder().executionNo("AI-fallback").modelCode("fallback").status("SUCCEEDED").build();
        when(domainService.prepareVisionExecution(any(), any(), any())).thenReturn(prepared);
        doThrow(new IllegalStateException("quota exceeded")).when(executor)
                .completeVision(eq(primary), eq("prompt"), any(), any(AiExecutionContext.class));
        when(executor.completeVision(eq(fallback), eq("prompt"), any(), any(AiExecutionContext.class)))
                .thenReturn(AiCompletionResultBO.builder().content("{}").build());
        when(domainService.finalizeVisionExecution(any(), eq("{}"))).thenReturn(execution);
        when(assembler.toDTO(execution)).thenReturn(response);

        AiExecuteDTO result = new ScenarioExecutionAppService(executor, toolCatalog, tracePort, assembler, domainService)
                .executeVision(new AiVisionExecuteRequest());

        assertThat(result.getModelCode()).isEqualTo("fallback");
        verify(executor).completeVision(eq(primary), eq("prompt"), any(), any(AiExecutionContext.class));
        verify(executor).completeVision(eq(fallback), eq("prompt"), any(), any(AiExecutionContext.class));
        verify(tracePort).record("AI-fallback", AiExecutionStatus.SUCCEEDED);
    }
}
