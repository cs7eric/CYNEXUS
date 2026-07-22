package cn.cccs7.cynexus.ai.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentCancellationResult {
    private String runNo;
    private String status;
    private String runStatus;
    private String message;

    public static AgentCancellationResult requested(String runNo, String runStatus) {
        return AgentCancellationResult.builder().runNo(runNo).status("CANCEL_REQUESTED")
                .runStatus(runStatus).message("Cancellation was persisted and will be honored by the Agent runtime.").build();
    }

    public static AgentCancellationResult alreadyTerminal(String runNo, String runStatus) {
        return AgentCancellationResult.builder().runNo(runNo).status("ALREADY_TERMINAL")
                .runStatus(runStatus).message("The Agent run is already terminal.").build();
    }

    public static AgentCancellationResult notFound(String runNo) {
        return AgentCancellationResult.builder().runNo(runNo).status("NOT_FOUND")
                .message("The Agent run was not found for the current owner.").build();
    }
}
