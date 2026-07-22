package cn.cccs7.cynexus.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.request.AgentTaskRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentTaskRepository;
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

        var task = new AgentTaskAppService(repository).schedule(AgentIdentity.builder().tenantKey("tenant").principalKey("user").build(), request);

        assertThat(task.getTaskNo()).startsWith("ATK-");
        assertThat(task.getStatus()).isEqualTo("QUEUED");
        assertThat(task.getMaxAttempts()).isEqualTo(4);
        assertThat(task.getUserInput()).isEqualTo(request.getUserInput());
    }
}
