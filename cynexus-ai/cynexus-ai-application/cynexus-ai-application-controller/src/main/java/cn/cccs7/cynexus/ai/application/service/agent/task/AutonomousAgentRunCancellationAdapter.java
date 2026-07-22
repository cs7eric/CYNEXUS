package cn.cccs7.cynexus.ai.application.service.agent.task;

import cn.cccs7.cynexus.ai.application.service.agent.execution.AutonomousAgentAppService;
import cn.cccs7.cynexus.ai.domain.port.agent.AgentRunCancellationPort;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import org.springframework.stereotype.Component;

/** Adapts the existing agent execution use case to the task lifecycle port. */
@Component
public class AutonomousAgentRunCancellationAdapter implements AgentRunCancellationPort {
    private final AutonomousAgentAppService agentService;

    public AutonomousAgentRunCancellationAdapter(AutonomousAgentAppService agentService) {
        this.agentService = agentService;
    }

    @Override
    public void cancel(AgentIdentity identity, String runNo) {
        agentService.cancel(identity, runNo);
    }
}
