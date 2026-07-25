package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentRunCancellationOutcome {
    String runNo;
    String status;
    String runStatus;

    public static AgentRunCancellationOutcome requested(String runNo, String runStatus) {
        return AgentRunCancellationOutcome.builder()
                .runNo(runNo)
                .status("CANCEL_REQUESTED")
                .runStatus(runStatus)
                .build();
    }

    public static AgentRunCancellationOutcome alreadyTerminal(String runNo, String runStatus) {
        return AgentRunCancellationOutcome.builder()
                .runNo(runNo)
                .status("ALREADY_TERMINAL")
                .runStatus(runStatus)
                .build();
    }

    public static AgentRunCancellationOutcome notFound(String runNo) {
        return AgentRunCancellationOutcome.builder()
                .runNo(runNo)
                .status("NOT_FOUND")
                .build();
    }
}
