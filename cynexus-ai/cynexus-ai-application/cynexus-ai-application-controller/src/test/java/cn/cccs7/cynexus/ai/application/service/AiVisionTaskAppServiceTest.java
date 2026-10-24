package cn.cccs7.cynexus.ai.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.entity.AiVisionTaskBO;
import cn.cccs7.cynexus.ai.domain.service.vision.AiVisionTaskDomainService;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class AiVisionTaskAppServiceTest {
    @Test
    void summaryPreservesOwnerRequestCorrelationForParentWorkflows() {
        AiVisionTaskDomainService domainService = mock(AiVisionTaskDomainService.class);
        AiVisionTaskBO task = AiVisionTaskBO.builder().id(42L).idempotencyKey("food-recognition:7:0").build();
        when(domainService.list(100, "tenant", "owner")).thenReturn(List.of(task));
        AiVisionTaskAppService service = new AiVisionTaskAppService(domainService,
                mock(ScenarioExecutionAppService.class), Runnable::run);

        assertEquals("food-recognition:7:0", service.list(100, "tenant", "owner").getFirst().getIdempotencyKey());
    }

    @Test
    void workerPoolRejectionReleasesTheClaimWithoutFailingTheTask() {
        AiVisionTaskDomainService domainService = mock(AiVisionTaskDomainService.class);
        ScenarioExecutionAppService executionService = mock(ScenarioExecutionAppService.class);
        AiVisionTaskBO task = AiVisionTaskBO.builder().id(42L).claimToken("claim-42").build();
        when(domainService.claimNext("ai-vision-worker")).thenReturn(task);
        Executor executor = command -> { throw new RejectedExecutionException("full"); };

        new AiVisionTaskAppService(domainService, executionService, executor).dispatchNext();

        verify(domainService).releaseClaim(task);
        verify(domainService, never()).fail(any(), any());
        verify(executionService, never()).executeVision(any(), any(), any());
    }
}
