package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;

public interface PortfolioCandidateStore {
    List<PortfolioCandidateRecord> listByTaskId(long taskId);

    boolean updateNormalized(long taskId, long candidateId, long expectedVersion, String normalizedPayload);

    boolean markAccepted(long candidateId, long expectedVersion);
}
