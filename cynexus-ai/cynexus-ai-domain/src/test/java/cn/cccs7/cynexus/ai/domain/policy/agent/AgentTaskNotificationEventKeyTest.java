package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AgentTaskNotificationEventKeyTest {
    @Test
    void createsStableLifecycleKeys() {
        assertThat(AgentTaskNotificationEventKey.completed(" ATK-1 ")).isEqualTo("ATK-1:COMPLETED");
        assertThat(AgentTaskNotificationEventKey.retryScheduled("ATK-1", 2)).isEqualTo("ATK-1:RETRY:2");
        assertThat(AgentTaskNotificationEventKey.deadLetter("ATK-1")).isEqualTo("ATK-1:DEAD_LETTER");
    }

    @Test
    void rejectsInvalidRetryAndTaskKeys() {
        assertThatThrownBy(() -> AgentTaskNotificationEventKey.retryScheduled("ATK-1", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AgentTaskNotificationEventKey.completed(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesPersistedEventContract() {
        assertThat(AgentTaskNotificationEventKey.requireEventKey(" ATK-1:COMPLETED "))
                .isEqualTo("ATK-1:COMPLETED");
        assertThat(AgentTaskNotificationEventKey.requireEventType(" RETRY_SCHEDULED "))
                .isEqualTo(AgentTaskNotificationEventKey.RETRY_SCHEDULED);
        assertThatThrownBy(() -> AgentTaskNotificationEventKey.requireEventType("SENT"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AgentTaskNotificationEventKey.requireEventKey("x".repeat(257)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
