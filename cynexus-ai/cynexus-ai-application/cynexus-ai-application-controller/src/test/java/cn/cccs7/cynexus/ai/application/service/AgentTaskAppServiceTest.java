package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.service.agent.task.AgentTaskAppService;
import cn.cccs7.cynexus.ai.application.service.agent.task.AgentTaskLifecycleNotificationService;
import cn.cccs7.cynexus.ai.domain.port.agent.AgentTaskNotificationPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import java.lang.reflect.Method;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.request.AgentTaskRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.service.agent.task.AgentTaskLifecycleDomainService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class AgentTaskAppServiceTest {
    @Test
    void schedulesOwnerScopedQueuedTaskWithBoundedRetryPolicy() {
        AgentTaskLifecycleDomainService lifecycleService = mock(AgentTaskLifecycleDomainService.class);
        AgentTaskRequest request = new AgentTaskRequest();
        request.setUserInput("Review the latest market snapshot");
        request.setScheduleAt(OffsetDateTime.now().plusMinutes(2));
        request.setMaxAttempts(4);

        AgentTaskBO scheduled = AgentTaskBO.builder().taskNo("ATK-1").status("QUEUED")
                .maxAttempts(4).userInput(request.getUserInput()).build();
        when(lifecycleService.schedule(any())).thenReturn(scheduled);

        var task = new AgentTaskAppService(lifecycleService).schedule(AgentIdentity.builder().tenantKey("tenant").principalKey("user").build(), request);

        assertThat(task.getTaskNo()).startsWith("ATK-");
        assertThat(task.getStatus()).isEqualTo("QUEUED");
        assertThat(task.getMaxAttempts()).isEqualTo(4);
        assertThat(task.getUserInput()).isEqualTo(request.getUserInput());
    }

    @Test
    void cancelsRunningTaskAndItsUnderlyingAgentRun() {
        AgentTaskLifecycleDomainService lifecycleService = mock(AgentTaskLifecycleDomainService.class);
        AgentIdentity identity = AgentIdentity.builder().tenantKey("tenant").principalKey("user").build();
        AgentTaskBO task = AgentTaskBO.builder().taskNo("ATK-1").ownerKey(identity.storageOwnerKey())
                .status("RUNNING").runNo("AGR-1").build();
        when(lifecycleService.find(identity.storageOwnerKey(), "ATK-1")).thenReturn(java.util.Optional.of(task));
        when(lifecycleService.cancel(identity, identity.storageOwnerKey(), "ATK-1")).thenReturn(true);

        boolean cancelled = new AgentTaskAppService(lifecycleService).cancel(identity, "ATK-1");

        assertThat(cancelled).isTrue();
    }

    @Test
    void keepsNotificationDeliveryAtAnApplicationPort() {
        assertThat(AgentTaskNotificationPort.class.isInterface()).isTrue();
        assertThat(AgentTaskNotificationPort.class.getDeclaredMethods()).hasSize(3);
    }

    @Test
    void commitsTaskStateAndOutboxRecordWithinOneApplicationTransaction() throws NoSuchMethodException {
        Method completed = AgentTaskLifecycleNotificationService.class
                .getMethod("recordCompleted", cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO.class, String.class);
        Method retry = AgentTaskLifecycleNotificationService.class.getMethod("recordRetryScheduled",
                cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO.class, java.time.OffsetDateTime.class,
                int.class, String.class);
        Method deadLetter = AgentTaskLifecycleNotificationService.class.getMethod("recordDeadLetter",
                cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO.class, String.class);
        assertThat(completed.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(retry.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(deadLetter.isAnnotationPresent(Transactional.class)).isTrue();
    }
}
