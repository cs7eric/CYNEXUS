package cn.cccs7.cynexus.ai.application.service.agent.task;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.port.agent.AgentTaskNotificationPort;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentTaskRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AgentTaskLifecycleNotificationServiceTest {
    @Test
    void recordsStateBeforeOutboxEventInTheSameApplicationBoundary() {
        AgentTaskRepository taskRepository = mock(AgentTaskRepository.class);
        AgentTaskNotificationPort notificationPort = mock(AgentTaskNotificationPort.class);
        AgentTaskLifecycleNotificationService service =
                new AgentTaskLifecycleNotificationService(taskRepository, notificationPort);
        AgentTaskBO task = AgentTaskBO.builder().taskNo("ATK-1").build();

        service.recordRetryScheduled(task, OffsetDateTime.now(), 2, "provider failed");

        InOrder order = inOrder(taskRepository, notificationPort);
        order.verify(taskRepository).retry(org.mockito.ArgumentMatchers.eq(task),
                org.mockito.ArgumentMatchers.any(OffsetDateTime.class),
                org.mockito.ArgumentMatchers.eq("provider failed"));
        order.verify(notificationPort).notifyRetryScheduled(task, 2, "provider failed");
    }
}
