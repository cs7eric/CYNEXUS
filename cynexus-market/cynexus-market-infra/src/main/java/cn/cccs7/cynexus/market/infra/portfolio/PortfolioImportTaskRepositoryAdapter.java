package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportStatus;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskPageBO;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportTaskPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioImportMapper;
import java.util.Optional;
import java.util.List;
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
    public void update(PortfolioImportTaskBO task) {
        PortfolioImportTaskPO po = new PortfolioImportTaskPO();
        po.setId(task.getId());
        po.setStatus(task.getStatus().name());
        po.setVersion(task.getVersion());
        po.setAttemptCount(task.getAttemptCount());
        po.setQueuedAt(task.getQueuedAt());
        po.setStartedAt(task.getStartedAt());
        po.setCompletedAt(task.getCompletedAt());
        po.setConfirmedAt(task.getConfirmedAt());
        po.setLastErrorCode(task.getLastErrorCode());
        po.setLastErrorMessage(task.getLastErrorMessage());
        if (mapper.updateTask(po) != 1) {
            throw new IllegalStateException("Portfolio import task was changed concurrently: " + task.getId());
        }
    }

    @Override
    public Optional<PortfolioImportTaskBO> findByOwnerAndId(String principalKey, String tenantKey, long taskId) {
        return Optional.ofNullable(mapper.findTaskByOwnerAndId(principalKey, tenantKey, taskId)).map(this::toDomain);
    }

    @Override
    public Optional<PortfolioImportTaskBO> findByIdempotencyKey(String principalKey, String tenantKey, String idempotencyKey) {
        return Optional.ofNullable(mapper.findTaskByIdempotency(principalKey, tenantKey, idempotencyKey)).map(this::toDomain);
    }

    @Override
    public List<PortfolioImportTaskBO> findRecentByOwner(String principalKey, String tenantKey, int limit) {
        return mapper.findRecentTasks(principalKey, tenantKey, Math.max(1, Math.min(limit, 50))).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public PortfolioImportTaskPageBO findPageByOwner(String principalKey, String tenantKey, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 2_000));
        int offset = (safePage - 1) * safePageSize;
        List<PortfolioImportTaskBO> items = mapper.findTasksByOwner(principalKey, tenantKey, offset, safePageSize).stream()
                .map(this::toDomain).toList();
        return new PortfolioImportTaskPageBO(items, mapper.countTasksByOwner(principalKey, tenantKey), safePage, safePageSize);
    }

    private PortfolioImportTaskBO toDomain(PortfolioImportTaskPO po) {
        return PortfolioImportTaskBO.rehydrate(po.getId(), po.getPrincipalKey(), po.getTenantKey(), po.getAssetId(),
                po.getIdempotencyKey(), PortfolioImportStatus.valueOf(po.getStatus()), po.getVersion(), po.getAttemptCount(), po.getCreatedAt(),
                po.getQueuedAt(), po.getStartedAt(), po.getCompletedAt(), po.getConfirmedAt(),
                po.getLastErrorCode(), po.getLastErrorMessage());
    }
}
