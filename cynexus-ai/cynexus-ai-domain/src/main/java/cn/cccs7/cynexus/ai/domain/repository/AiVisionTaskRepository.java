package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiVisionTaskBO;
import java.time.OffsetDateTime;
import java.util.List;

public interface AiVisionTaskRepository {
    AiVisionTaskBO create(AiVisionTaskBO task);

    AiVisionTaskBO find(Long id, String tenantKey, String principalKey);

    List<AiVisionTaskBO> listByOwner(int limit, String tenantKey, String principalKey);

    AiVisionTaskBO retryFailed(Long id, String tenantKey, String principalKey, OffsetDateTime failedAfter);

    int purgeTerminalInputs(OffsetDateTime failedBefore);

    AiVisionTaskBO claimNext(String workerId);

    /** Releases a claim that never reached execution (for example, a full worker pool). */
    void releaseClaim(AiVisionTaskBO task);

    int recoverStaleTasks(int timeoutMinutes);

    void markSucceeded(AiVisionTaskBO task);

    void markFailed(AiVisionTaskBO task, String errorMessage);
}
