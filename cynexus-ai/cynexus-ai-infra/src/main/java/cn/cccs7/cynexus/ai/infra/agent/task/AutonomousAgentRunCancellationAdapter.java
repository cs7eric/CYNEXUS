package cn.cccs7.cynexus.ai.infra.agent.task;

import cn.cccs7.cynexus.ai.domain.port.agent.AgentRunCancellationPort;
import cn.cccs7.cynexus.ai.domain.service.agent.execution.AgentRunExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import org.springframework.stereotype.Component;

/** Adapts task cancellation to the domain-owned Agent run lifecycle. */
@Component
public class AutonomousAgentRunCancellationAdapter implements AgentRunCancellationPort {

    private final AgentRunExecutionDomainService runExecutionDomainService;

    public AutonomousAgentRunCancellationAdapter(AgentRunExecutionDomainService runExecutionDomainService) {
        this.runExecutionDomainService = runExecutionDomainService;
    }

    @Override
    public void cancel(AgentIdentity identity, String runNo) {
        runExecutionDomainService.cancel(identity == null ? null : identity.storageOwnerKey(), runNo);
    }
}
