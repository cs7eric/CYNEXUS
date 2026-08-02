package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessEventType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryRepository;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportProcessHistoryPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioImportProcessHistoryMapper;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioIdGenerator;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioImportProcessHistoryRepositoryAdapter implements PortfolioImportProcessHistoryRepository {
    private final PortfolioImportProcessHistoryMapper mapper;
    private final PortfolioIdGenerator idGenerator;

    public PortfolioImportProcessHistoryRepositoryAdapter(PortfolioImportProcessHistoryMapper mapper,
                                                           PortfolioIdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void append(PortfolioImportProcessHistoryBO history) {
        PortfolioImportProcessHistoryPO po = new PortfolioImportProcessHistoryPO();
        po.setId(history.id() > 0 ? history.id() : idGenerator.nextId());
        po.setTaskId(history.taskId());
        po.setPrincipalKey(history.principalKey());
        po.setTenantKey(history.tenantKey());
        po.setAttemptNo(history.attemptNo());
        po.setEventType(history.eventType().name());
        po.setFromStatus(history.fromStatus());
        po.setToStatus(history.toStatus());
        po.setMessageId(history.messageId());
        po.setCorrelationId(history.correlationId());
        po.setProviderCode(history.providerCode());
        po.setModelCode(history.modelCode());
        po.setErrorCode(history.errorCode());
        po.setErrorMessage(history.errorMessage());
        po.setDetails(history.details());
        po.setOccurredAt(history.occurredAt());
        po.setDurationMs(history.durationMs());
        if (mapper.insert(po) != 1) throw new IllegalStateException("Unable to persist portfolio process history");
    }

    @Override
    public List<PortfolioImportProcessHistoryBO> listByTaskId(String principalKey, String tenantKey, long taskId) {
        return mapper.listByTaskId(principalKey, tenantKey, taskId).stream().map(this::toDomain).toList();
    }

    private PortfolioImportProcessHistoryBO toDomain(PortfolioImportProcessHistoryPO po) {
        return new PortfolioImportProcessHistoryBO(po.getId(), po.getTaskId(), po.getPrincipalKey(), po.getTenantKey(),
                po.getAttemptNo(), PortfolioImportProcessEventType.valueOf(po.getEventType()), po.getFromStatus(),
                po.getToStatus(), po.getMessageId(), po.getCorrelationId(), po.getProviderCode(), po.getModelCode(),
                po.getErrorCode(), po.getErrorMessage(), po.getDetails() == null ? "{}" : po.getDetails(),
                po.getOccurredAt(), po.getDurationMs());
    }
}
