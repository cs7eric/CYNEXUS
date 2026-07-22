package cn.cccs7.cynexus.ai.domain.agent;

public interface AgentRuntimeLeasePort {

    boolean tryAcquire(String ownerKey, String runNo, int maxActiveRuns, int ttlSeconds);

    void release(String ownerKey, String runNo);

    boolean requestCancellation(String ownerKey, String runNo, int ttlSeconds);

    boolean isCancellationRequested(String runNo);

    boolean isLeaseActive(String runNo);
}
