package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioIdGenerator;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportCandidateBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportCandidateRepository;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportCandidatePO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioImportCandidateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioImportCandidateRepositoryAdapter implements PortfolioImportCandidateRepository {
    private final PortfolioImportCandidateMapper mapper;
    private final PortfolioIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public PortfolioImportCandidateRepositoryAdapter(PortfolioImportCandidateMapper mapper,
                                                     PortfolioIdGenerator idGenerator,
                                                     ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void replace(long taskId, List<PortfolioImportCandidateBO> candidates) {
        mapper.deleteByTaskId(taskId);
        for (PortfolioImportCandidateBO candidate : candidates) {
            try {
                PortfolioImportCandidatePO po = new PortfolioImportCandidatePO();
                po.setId(idGenerator.nextId());
                po.setTaskId(taskId);
                po.setRowNo(candidate.rowNo());
                po.setRawPayload(objectMapper.writeValueAsString(candidate.rawPayload()));
                po.setNormalizedPayload(objectMapper.writeValueAsString(candidate.normalizedPayload()));
                po.setConfidence(candidate.confidence());
                po.setWarnings(objectMapper.writeValueAsString(candidate.warnings()));
                po.setReviewStatus("PENDING");
                po.setVersion(0L);
                mapper.insert(po);
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to persist portfolio candidate", exception);
            }
        }
    }
}
