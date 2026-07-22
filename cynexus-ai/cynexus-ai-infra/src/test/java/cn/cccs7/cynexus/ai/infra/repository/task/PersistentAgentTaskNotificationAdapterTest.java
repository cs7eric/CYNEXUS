package cn.cccs7.cynexus.ai.infra.repository.task;

import static org.mockito.Mockito.verify;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.infra.notification.agent.task.PersistentAgentTaskNotificationAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PersistentAgentTaskNotificationAdapterTest {
    @Test
    void writesIdempotentLifecycleEventsThroughTheRepository() {
        AgentTaskNotificationRepository repository = org.mockito.Mockito.mock(AgentTaskNotificationRepository.class);
        PersistentAgentTaskNotificationAdapter adapter =
                new PersistentAgentTaskNotificationAdapter(repository);
        AgentTaskBO task = AgentTaskBO.builder().taskNo("ATK-1").ownerKey("tenant:user")
                .tenantKey("tenant").principalKey("user").build();

        adapter.notifyCompleted(task, "SUCCEEDED");
        adapter.notifyCompleted(task, null);
        adapter.notifyRetryScheduled(task, 2, "provider failed");
        adapter.notifyDeadLetter(task, "x".repeat(3000));

        ArgumentCaptor<String> eventKeys = ArgumentCaptor.forClass(String.class);
        verify(repository, org.mockito.Mockito.times(4)).append(eventKeys.capture(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(task),
                org.mockito.ArgumentMatchers.anyMap());
        org.assertj.core.api.Assertions.assertThat(eventKeys.getAllValues())
                .containsExactly("ATK-1:COMPLETED", "ATK-1:COMPLETED", "ATK-1:RETRY:2", "ATK-1:DEAD_LETTER");
    }
}
