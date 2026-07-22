package cn.cccs7.cynexus.ai.domain.agent;

public class AgentQuotaExceededException extends RuntimeException {
    public AgentQuotaExceededException() {
        super("Daily Agent quota exceeded for this owner");
    }
}
