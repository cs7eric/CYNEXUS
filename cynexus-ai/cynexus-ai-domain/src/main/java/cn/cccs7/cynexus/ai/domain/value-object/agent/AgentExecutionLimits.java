package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentExecutionLimits {

    int maxTurns;
    /** Absolute ceiling for one run. Soft stop happens earlier via simple/deep budgets. */
    int maxToolCalls;
    /** Soft budget for short factual questions. */
    int simpleMaxToolCalls;
    /** Soft budget for deeper multi-step analysis. */
    int deepMaxToolCalls;
    int maxUserInputChars;
    int maxToolOutputChars;
    int maxRunSeconds;
    int maxRetryAttempts;

    public static AgentExecutionLimits defaults() {
        return AgentExecutionLimits.builder()
                .maxTurns(12)
                .maxToolCalls(50)
                .simpleMaxToolCalls(12)
                .deepMaxToolCalls(30)
                .maxUserInputChars(12_000)
                .maxToolOutputChars(24_000)
                .maxRunSeconds(600)
                .maxRetryAttempts(2)
                .build();
    }
}
