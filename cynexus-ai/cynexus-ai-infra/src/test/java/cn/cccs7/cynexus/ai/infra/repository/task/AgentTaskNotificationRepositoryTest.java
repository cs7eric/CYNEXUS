package cn.cccs7.cynexus.ai.infra.repository.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentTaskNotificationPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentTaskNotificationMapper;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AgentTaskNotificationRepositoryTest {
    @Test
    void persistsOwnerScopedOutboxRecordAndReportsInsertResult() {
        AgentTaskNotificationMapper mapper = mock(AgentTaskNotificationMapper.class);
        when(mapper.insertIfAbsent(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        AgentTaskNotificationRepository repository = new AgentTaskNotificationRepository(
                mapper, new SnowflakeIdGenerator(), new ObjectMapper());
        AgentTaskBO task = AgentTaskBO.builder().taskNo("ATK-1").ownerKey("tenant:user")
                .tenantKey("tenant").principalKey("user").build();

        boolean inserted = repository.append("ATK-1:COMPLETED", "COMPLETED", task,
                Map.of("resultStatus", "SUCCEEDED"));

        ArgumentCaptor<AgentTaskNotificationPO> captor = ArgumentCaptor.forClass(AgentTaskNotificationPO.class);
        org.mockito.Mockito.verify(mapper).insertIfAbsent(captor.capture());
        AgentTaskNotificationPO persisted = captor.getValue();
        assertThat(inserted).isTrue();
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getEventKey()).isEqualTo("ATK-1:COMPLETED");
        assertThat(persisted.getOwnerKey()).isEqualTo("tenant:user");
        assertThat(persisted.getPayloadJson()).contains("SUCCEEDED");
    }

    @Test
    void treatsDatabaseConflictAsAnIdempotentNoOp() {
        AgentTaskNotificationMapper mapper = mock(AgentTaskNotificationMapper.class);
        when(mapper.insertIfAbsent(org.mockito.ArgumentMatchers.any())).thenReturn(0);
        AgentTaskNotificationRepository repository = new AgentTaskNotificationRepository(
                mapper, new SnowflakeIdGenerator(), new ObjectMapper());
        AgentTaskBO task = AgentTaskBO.builder().taskNo("ATK-1").ownerKey("tenant:user")
                .tenantKey("tenant").principalKey("user").build();

        assertThat(repository.append("ATK-1:COMPLETED", "COMPLETED", task, Map.of())).isFalse();
    }

    @Test
    void rejectsInvalidEventAndOwnerDataBeforeCallingTheMapper() {
        AgentTaskNotificationMapper mapper = mock(AgentTaskNotificationMapper.class);
        AgentTaskNotificationRepository repository = new AgentTaskNotificationRepository(
                mapper, new SnowflakeIdGenerator(), new ObjectMapper());
        AgentTaskBO task = AgentTaskBO.builder().taskNo("ATK-1").ownerKey("tenant:user")
                .tenantKey("tenant").principalKey("user").build();

        assertThatThrownBy(() -> repository.append("ATK-1:UNKNOWN", "UNKNOWN", task, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.append("ATK-1:COMPLETED", "COMPLETED",
                AgentTaskBO.builder().taskNo("ATK-1").ownerKey(" ").tenantKey("tenant")
                        .principalKey("user").build(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.verifyNoInteractions(mapper);
    }
}
