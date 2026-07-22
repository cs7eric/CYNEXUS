package cn.cccs7.cynexus.ai.domain.exception.quota;

public class AgentQuotaExceededException extends RuntimeException {
    public AgentQuotaExceededException() {
        super("Daily Agent quota exceeded for this owner");
    }
}
