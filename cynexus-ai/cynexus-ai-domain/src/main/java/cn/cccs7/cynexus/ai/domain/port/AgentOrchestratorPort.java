package cn.cccs7.cynexus.ai.domain.port;

/** Boundary for selecting a native or external multi-agent orchestrator. */
public interface AgentOrchestratorPort {
    AgentRunResult execute(AgentRunCommand command);

    record AgentRunCommand(String runNo, String capability, String input, String tenantKey, String principalKey) { }
    record AgentRunResult(String runNo, String status, String content) { }
}
