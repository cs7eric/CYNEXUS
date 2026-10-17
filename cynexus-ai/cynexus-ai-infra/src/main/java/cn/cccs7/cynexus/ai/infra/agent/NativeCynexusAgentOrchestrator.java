package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.domain.port.AgentOrchestratorPort;
import org.springframework.stereotype.Component;

/** Boundary adapter for the existing Agent runtime; ordinary calls keep using its API. */
@Component("nativeCynexusAgentOrchestrator")
public class NativeCynexusAgentOrchestrator implements AgentOrchestratorPort {
    @Override
    public AgentRunResult execute(AgentRunCommand command) {
        throw new UnsupportedOperationException("Use the Agent API runtime for native Agent runs");
    }
}
