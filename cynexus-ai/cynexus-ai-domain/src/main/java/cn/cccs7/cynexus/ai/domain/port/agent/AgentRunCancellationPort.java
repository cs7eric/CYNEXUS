package cn.cccs7.cynexus.ai.domain.port.agent;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;

/** Domain port for cancelling the agent run attached to a task. */
public interface AgentRunCancellationPort {
    void cancel(AgentIdentity identity, String runNo);
}
