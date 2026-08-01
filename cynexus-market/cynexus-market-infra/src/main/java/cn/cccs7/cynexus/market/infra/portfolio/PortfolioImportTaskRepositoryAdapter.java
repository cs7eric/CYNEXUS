package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportStatus;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskRepository;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportTaskPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioImportMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioImportTaskRepositoryAdapter implements PortfolioImportTaskRepository {
    private final PortfolioImportMapper mapper;

    public PortfolioImportTaskRepositoryAdapter(PortfolioImportMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(PortfolioImportTaskBO task) {
        PortfolioImportTaskPO po = new PortfolioImportTaskPO();
        po.setId(task.getId());
        po.setPrincipalKey(task.getPrincipalKey());
        po.setTenantKey(task.getTenantKey());
        po.setAssetId(task.getAssetId());
        po.setIdempotencyKey(task.getIdempotencyKey());
        po.setStatus(task.getStatus().name());
        po.setVersion(task.getVersion());
        po.setAttemptCount(task.getAttemptCount());
        po.setQueuedAt(task.getQueuedAt());
        po.setStartedAt(task.getStartedAt());
        po.setCompletedAt(task.getCompletedAt());
        po.setConfirmedAt(task.getConfirmedAt());
        po.setLastErrorCode(task.getLastErrorCode());
        po.setLastErrorMessage(task.getLastErrorMessage());
        mapper.insertTask(po);
    }

    @Override
    public Optional<PortfolioImportTaskBO> findByOwnerAndId(String principalKey, String tenantKey, long taskId) {
        return Optional.ofNullable(mapper.findTaskByOwnerAndId(principalKey, tenantKey, taskId)).map(this::toDomain);
    }

    @Override
    public Optional<PortfolioImportTaskBO> findByIdempotencyKey(String principalKey, String tenantKey, String idempotencyKey) {
        return Optional.ofNullable(mapper.findTaskByIdempotency(principalKey, tenantKey, idempotencyKey)).map(this::toDomain);
    }

    private PortfolioImportTaskBO toDomain(PortfolioImportTaskPO po) {
        return PortfolioImportTaskBO.rehydrate(po.getId(), po.getPrincipalKey(), po.getTenantKey(), po.getAssetId(),
                po.getIdempotencyKey(), PortfolioImportStatus.valueOf(po.getStatus()), po.getVersion(), po.getAttemptCount(),
                po.getQueuedAt(), po.getStartedAt(), po.getCompletedAt(), po.getConfirmedAt(),
                po.getLastErrorCode(), po.getLastErrorMessage());
    }
}
