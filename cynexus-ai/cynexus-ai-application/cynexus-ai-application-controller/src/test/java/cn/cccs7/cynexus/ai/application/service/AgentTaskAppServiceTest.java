package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.service.agent.execution.AutonomousAgentAppService;
import cn.cccs7.cynexus.ai.application.service.agent.task.AgentTaskAppService;
import cn.cccs7.cynexus.ai.application.service.agent.task.AgentTaskNotificationPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.request.AgentTaskRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentTaskRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class AgentTaskAppServiceTest {
    @Test
    void schedulesOwnerScopedQueuedTaskWithBoundedRetryPolicy() {
        AgentTaskRepository repository = mock(AgentTaskRepository.class);
        when(repository.save(any(AgentTaskBO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AgentTaskRequest request = new AgentTaskRequest();
        request.setUserInput("Review the latest market snapshot");
        request.setScheduleAt(OffsetDateTime.now().plusMinutes(2));
        request.setMaxAttempts(4);

        var task = new AgentTaskAppService(repository, mock(AutonomousAgentAppService.class)).schedule(AgentIdentity.builder().tenantKey("tenant").principalKey("user").build(), request);

        assertThat(task.getTaskNo()).startsWith("ATK-");
        assertThat(task.getStatus()).isEqualTo("QUEUED");
        assertThat(task.getMaxAttempts()).isEqualTo(4);
        assertThat(task.getUserInput()).isEqualTo(request.getUserInput());
    }

    @Test
    void cancelsRunningTaskAndItsUnderlyingAgentRun() {
        AgentTaskRepository repository = mock(AgentTaskRepository.class);
        AutonomousAgentAppService agentService = mock(AutonomousAgentAppService.class);
        AgentIdentity identity = AgentIdentity.builder().tenantKey("tenant").principalKey("user").build();
        AgentTaskBO task = AgentTaskBO.builder().taskNo("ATK-1").ownerKey(identity.storageOwnerKey())
                .status("RUNNING").runNo("AGR-1").build();
        when(repository.find(identity.storageOwnerKey(), "ATK-1")).thenReturn(java.util.Optional.of(task));
        when(repository.cancel(identity.storageOwnerKey(), "ATK-1")).thenReturn(true);

        boolean cancelled = new AgentTaskAppService(repository, agentService).cancel(identity, "ATK-1");

        assertThat(cancelled).isTrue();
        org.mockito.Mockito.verify(agentService).cancel(identity, "AGR-1");
        org.mockito.Mockito.verify(repository).cancel(identity.storageOwnerKey(), "ATK-1");
    }

    @Test
    void keepsNotificationDeliveryAtAnApplicationPort() {
        assertThat(AgentTaskNotificationPort.class.isInterface()).isTrue();
        assertThat(AgentTaskNotificationPort.class.getDeclaredMethods()).hasSize(3);
    }
}
