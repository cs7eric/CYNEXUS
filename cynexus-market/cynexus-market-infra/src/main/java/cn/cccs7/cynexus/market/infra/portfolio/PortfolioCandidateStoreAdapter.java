package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioCandidateRecord;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioCandidateStore;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportCandidatePO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioImportCandidateMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioCandidateStoreAdapter implements PortfolioCandidateStore {
    private final PortfolioImportCandidateMapper mapper;
    private final ObjectMapper objectMapper;

    public PortfolioCandidateStoreAdapter(PortfolioImportCandidateMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PortfolioCandidateRecord> listByTaskId(long taskId) {
        return mapper.listByTaskId(taskId).stream().map(this::toRecord).toList();
    }

    @Override
    public boolean markAccepted(long candidateId, long expectedVersion) {
        return mapper.markAccepted(candidateId, expectedVersion) == 1;
    }

    private PortfolioCandidateRecord toRecord(PortfolioImportCandidatePO po) {
        try {
            JsonNode normalized = objectMapper.readTree(po.getNormalizedPayload());
            return new PortfolioCandidateRecord(po.getId(), po.getTaskId(), po.getRowNo(), normalized,
                    po.getReviewStatus(), po.getVersion());
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid portfolio candidate payload " + po.getId(), exception);
        }
    }
}
