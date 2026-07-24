package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** Result of applying the soft tool-call budget to a model tool batch. */
@Value
@Builder
public class AgentToolCallAdmission {

    List<AgentModelToolCall> accepted;
    List<AgentModelToolCall> rejected;
    boolean budgetExhausted;
    int remainingBudget;

    public static AgentToolCallAdmission noneRemaining() {
        return AgentToolCallAdmission.builder()
                .accepted(List.of())
                .rejected(List.of())
                .budgetExhausted(true)
                .remainingBudget(0)
                .build();
    }
}
