package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentExecutionLimits {

    int maxTurns;
    int maxToolCalls;
    int maxUserInputChars;
    int maxToolOutputChars;
    int maxRunSeconds;
    int maxRetryAttempts;

    public static AgentExecutionLimits defaults() {
        return AgentExecutionLimits.builder()
                .maxTurns(8)
                .maxToolCalls(12)
                .maxUserInputChars(12_000)
                .maxToolOutputChars(24_000)
                .maxRunSeconds(600)
                .maxRetryAttempts(2)
                .build();
    }
}
