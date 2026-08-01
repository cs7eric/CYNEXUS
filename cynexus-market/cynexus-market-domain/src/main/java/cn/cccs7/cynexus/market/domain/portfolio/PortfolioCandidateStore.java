package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;

public interface PortfolioCandidateStore {
    List<PortfolioCandidateRecord> listByTaskId(long taskId);

    boolean markAccepted(long candidateId, long expectedVersion);
}
